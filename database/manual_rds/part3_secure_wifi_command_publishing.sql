-- Part 3 - Secure Wi-Fi Command Storage and Reliable MQTT Publishing
-- Manual PostgreSQL script for Dia-Smart RDS.
-- Run after database/manual_rds/part1_device_kit_model.sql and
-- database/manual_rds/part2_device_activation_security.sql.
-- Review before running against any shared or production database.

BEGIN;

ALTER TABLE device_commands
ADD COLUMN IF NOT EXISTS device_configuration_id BIGINT;

ALTER TABLE device_commands
ADD COLUMN IF NOT EXISTS configuration_version INTEGER;

ALTER TABLE device_commands
ADD COLUMN IF NOT EXISTS last_attempt_at TIMESTAMPTZ;

ALTER TABLE device_commands
ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMPTZ;

DO $$
DECLARE
    status_constraint_name TEXT;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_device_commands_configuration'
    ) THEN
        ALTER TABLE device_commands
        ADD CONSTRAINT fk_device_commands_configuration
        FOREIGN KEY (device_configuration_id)
        REFERENCES device_configurations(configuration_id)
        ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_device_commands_configuration_version'
    ) THEN
        ALTER TABLE device_commands
        ADD CONSTRAINT chk_device_commands_configuration_version
        CHECK (configuration_version IS NULL OR configuration_version > 0);
    END IF;

    FOR status_constraint_name IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'device_commands'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) LIKE '%command_status%'
    LOOP
        EXECUTE format(
            'ALTER TABLE device_commands DROP CONSTRAINT IF EXISTS %I',
            status_constraint_name
        );
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_device_commands_status'
    ) THEN
        ALTER TABLE device_commands
        ADD CONSTRAINT chk_device_commands_status
        CHECK (
            command_status IS NULL OR command_status IN (
                'PENDING',
                'SENT',
                'PUBLISHED',
                'RECEIVED',
                'VALIDATED',
                'APPLIED',
                'FAILED',
                'EXPIRED'
            )
        );
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_device_commands_config
ON device_commands (device_configuration_id);

CREATE INDEX IF NOT EXISTS idx_device_commands_config_version
ON device_commands (device_configuration_id, configuration_version);

CREATE INDEX IF NOT EXISTS idx_device_commands_wifi_recovery
ON device_commands (command_type, command_status, next_retry_at, last_attempt_at)
WHERE command_type = 'WIFI_CONFIGURATION';

-- Backfill safe configuration references without selecting or printing payload data.
UPDATE device_commands dc
SET device_configuration_id = cfg.configuration_id
FROM device_configurations cfg
WHERE dc.command_type = 'WIFI_CONFIGURATION'
  AND dc.device_configuration_id IS NULL
  AND cfg.outer_device_id = dc.device_id;

UPDATE device_commands
SET configuration_version = COALESCE(
        CASE
            WHEN payload #>> '{payload,configurationVersion}' ~ '^[0-9]+$'
                THEN (payload #>> '{payload,configurationVersion}')::INTEGER
            ELSE NULL
        END,
        CASE
            WHEN payload ->> 'configurationVersion' ~ '^[0-9]+$'
                THEN (payload ->> 'configurationVersion')::INTEGER
            ELSE NULL
        END
    )
WHERE command_type = 'WIFI_CONFIGURATION'
  AND configuration_version IS NULL;

-- Preserve retryability only for legacy Wi-Fi commands with a safe configuration reference.
UPDATE device_commands dc
SET payload = jsonb_build_object(
        'configurationId', dc.device_configuration_id,
        'configurationVersion', dc.configuration_version,
        'innerDeviceId', cfg.inner_device_id,
        'penDeviceId', cfg.pen_device_id,
        'glucometerDeviceId', cfg.glucometer_device_id
    )
FROM device_configurations cfg
WHERE dc.command_type = 'WIFI_CONFIGURATION'
  AND dc.device_configuration_id = cfg.configuration_id
  AND dc.configuration_version IS NOT NULL;

-- Legacy Wi-Fi command rows that cannot be safely tied to a configuration/version
-- are sanitized and excluded from retry. No plaintext value is copied or returned.
UPDATE device_commands
SET payload = jsonb_build_object(
        'legacySanitized', true,
        'reason', 'missingConfigurationReference'
    ),
    command_status = 'EXPIRED',
    retry_count = GREATEST(COALESCE(retry_count, 0), 3),
    last_error = 'LEGACY_WIFI_COMMAND_SANITIZED',
    next_retry_at = NULL
WHERE command_type = 'WIFI_CONFIGURATION'
  AND (
        device_configuration_id IS NULL
        OR configuration_version IS NULL
    );

COMMENT ON COLUMN device_commands.device_configuration_id
IS 'Safe reference to the encrypted Wi-Fi configuration used to build the MQTT payload at publish time.';

COMMENT ON COLUMN device_commands.configuration_version
IS 'Configuration version snapshot for this command. Prevents retrying a command against newer credentials accidentally.';

COMMENT ON COLUMN device_commands.last_attempt_at
IS 'Last backend MQTT publish attempt timestamp for retry and stale in-flight recovery.';

COMMENT ON COLUMN device_commands.next_retry_at
IS 'Earliest timestamp when a failed Wi-Fi publish command may be retried.';

COMMIT;
