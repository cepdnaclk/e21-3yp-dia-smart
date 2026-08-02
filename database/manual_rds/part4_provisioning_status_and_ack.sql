-- Part 4 - Provisioning ACK Handling, Status Tracking and Final Lifecycle
-- Manual PostgreSQL script for Dia-Smart RDS.
-- Run after:
--   database/manual_rds/part1_device_kit_model.sql
--   database/manual_rds/part2_device_activation_security.sql
--   database/manual_rds/part3_secure_wifi_command_publishing.sql
-- Review before running against any shared or production database.

BEGIN;

-- 1. Wi-Fi command ACK correlation and idempotency.
ALTER TABLE device_command_acknowledgements
ADD COLUMN IF NOT EXISTS configuration_version INTEGER;

ALTER TABLE device_command_acknowledgements
ADD COLUMN IF NOT EXISTS reporting_outer_device_uid VARCHAR(80);

ALTER TABLE device_command_acknowledgements
ADD COLUMN IF NOT EXISTS payload_outer_device_uid VARCHAR(80);

ALTER TABLE device_command_acknowledgements
ADD COLUMN IF NOT EXISTS ack_uid VARCHAR(120);

ALTER TABLE device_command_acknowledgements
ADD COLUMN IF NOT EXISTS ack_deduplication_key VARCHAR(200);

ALTER TABLE device_command_acknowledgements
ADD COLUMN IF NOT EXISTS processing_result VARCHAR(60);

ALTER TABLE device_command_acknowledgements
ADD COLUMN IF NOT EXISTS device_timestamp TIMESTAMPTZ;

-- Legacy rows cannot be fully correlated safely. Preserve them for audit and
-- give each one a deterministic historical deduplication key.
UPDATE device_command_acknowledgements
SET ack_deduplication_key = 'LEGACY|' || acknowledgement_id
WHERE ack_deduplication_key IS NULL;

ALTER TABLE device_command_acknowledgements
ALTER COLUMN ack_deduplication_key SET NOT NULL;

DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    FOR constraint_name IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'device_command_acknowledgements'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) LIKE '%ack_status%'
    LOOP
        EXECUTE format(
            'ALTER TABLE device_command_acknowledgements DROP CONSTRAINT IF EXISTS %I',
            constraint_name
        );
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_device_command_ack_status'
    ) THEN
        ALTER TABLE device_command_acknowledgements
        ADD CONSTRAINT chk_device_command_ack_status
        CHECK (
            ack_status IS NULL OR ack_status IN (
                'PENDING',
                'PUBLISHED',
                'RECEIVED',
                'VALIDATED',
                'STAGED',
                'APPLYING',
                'APPLIED',
                'REJECTED',
                'FAILED',
                'ROLLED_BACK'
            )
        );
    END IF;

    FOR constraint_name IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'device_command_acknowledgements'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) LIKE '%processing_result%'
    LOOP
        EXECUTE format(
            'ALTER TABLE device_command_acknowledgements DROP CONSTRAINT IF EXISTS %I',
            constraint_name
        );
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_device_command_ack_processing_result'
    ) THEN
        ALTER TABLE device_command_acknowledgements
        ADD CONSTRAINT chk_device_command_ack_processing_result
        CHECK (
            processing_result IS NULL OR processing_result IN (
                'ACCEPTED',
                'COMMAND_TYPE_MISMATCH',
                'ACK_COMMAND_TYPE_MISMATCH',
                'REPORTING_OUTER_UID_MISSING',
                'REPORTING_OUTER_UID_MISMATCH',
                'PAYLOAD_OUTER_UID_MISMATCH',
                'COMMAND_CONFIGURATION_REFERENCE_MISSING',
                'CONFIGURATION_NOT_FOUND',
                'CONFIGURATION_DEVICE_MISMATCH',
                'ACK_CONFIGURATION_VERSION_MISSING',
                'ACK_CONFIGURATION_VERSION_MISMATCH',
                'COMMAND_SUPERSEDED',
                'STALE_STATUS_TRANSITION'
            )
        );
    END IF;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_device_command_ack_dedup
ON device_command_acknowledgements (ack_deduplication_key);

CREATE INDEX IF NOT EXISTS idx_device_ack_command_version
ON device_command_acknowledgements (command_id, configuration_version);

CREATE INDEX IF NOT EXISTS idx_device_ack_reporting_outer
ON device_command_acknowledgements (reporting_outer_device_uid);

-- 2. Provisioning lifecycle state on the current encrypted configuration row.
ALTER TABLE device_configurations
ADD COLUMN IF NOT EXISTS last_successful_configuration_id BIGINT;

ALTER TABLE device_configurations
ADD COLUMN IF NOT EXISTS last_successful_configuration_version INTEGER;

ALTER TABLE device_configurations
ADD COLUMN IF NOT EXISTS last_successful_at TIMESTAMPTZ;

ALTER TABLE device_configurations
ADD COLUMN IF NOT EXISTS previous_configuration_id BIGINT;

ALTER TABLE device_configurations
ADD COLUMN IF NOT EXISTS previous_configuration_version INTEGER;

ALTER TABLE device_configurations
ADD COLUMN IF NOT EXISTS provisioning_started_at TIMESTAMPTZ;

ALTER TABLE device_configurations
ADD COLUMN IF NOT EXISTS provisioning_completed_at TIMESTAMPTZ;

ALTER TABLE device_configurations
ADD COLUMN IF NOT EXISTS provisioning_timeout_at TIMESTAMPTZ;

ALTER TABLE device_configurations
ADD COLUMN IF NOT EXISTS provisioning_failure_code VARCHAR(60);

ALTER TABLE device_configurations
ADD COLUMN IF NOT EXISTS provisioning_failure_message TEXT;

ALTER TABLE device_configurations
ADD COLUMN IF NOT EXISTS rollback_status VARCHAR(30) DEFAULT 'NOT_REQUIRED';

ALTER TABLE device_configurations
ADD COLUMN IF NOT EXISTS mqtt_status VARCHAR(30) DEFAULT 'PENDING';

ALTER TABLE device_configurations
ADD COLUMN IF NOT EXISTS last_provisioning_command_id BIGINT;

ALTER TABLE device_configurations
ADD COLUMN IF NOT EXISTS last_provisioning_command_uid VARCHAR(80);

UPDATE device_configurations
SET rollback_status = 'NOT_REQUIRED'
WHERE rollback_status IS NULL;

UPDATE device_configurations
SET mqtt_status = 'PENDING'
WHERE mqtt_status IS NULL;

DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_device_config_last_successful'
    ) THEN
        ALTER TABLE device_configurations
        ADD CONSTRAINT fk_device_config_last_successful
        FOREIGN KEY (last_successful_configuration_id)
        REFERENCES device_configurations(configuration_id)
        ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_device_config_previous'
    ) THEN
        ALTER TABLE device_configurations
        ADD CONSTRAINT fk_device_config_previous
        FOREIGN KEY (previous_configuration_id)
        REFERENCES device_configurations(configuration_id)
        ON DELETE SET NULL;
    END IF;

    FOR constraint_name IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'device_configurations'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) LIKE '%configuration_status%'
    LOOP
        EXECUTE format(
            'ALTER TABLE device_configurations DROP CONSTRAINT IF EXISTS %I',
            constraint_name
        );
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_device_configurations_status'
    ) THEN
        ALTER TABLE device_configurations
        ADD CONSTRAINT chk_device_configurations_status
        CHECK (
            configuration_status IS NULL OR configuration_status IN (
                'PENDING',
                'SENT',
                'PUBLISHED',
                'RECEIVED',
                'VALIDATED',
                'STAGED',
                'APPLYING',
                'APPLIED',
                'FAILED',
                'TIMED_OUT',
                'ROLLED_BACK',
                'SUPERSEDED',
                'STALE',
                'OUTDATED'
            )
        );
    END IF;

    FOR constraint_name IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'device_configurations'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) LIKE '%inner_unit_status%'
    LOOP
        EXECUTE format(
            'ALTER TABLE device_configurations DROP CONSTRAINT IF EXISTS %I',
            constraint_name
        );
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_device_configurations_inner_unit_status'
    ) THEN
        ALTER TABLE device_configurations
        ADD CONSTRAINT chk_device_configurations_inner_unit_status
        CHECK (
            inner_unit_status IS NULL OR inner_unit_status IN (
                'NOT_CONFIGURED',
                'PAIRING',
                'CREDENTIALS_SENT',
                'WAITING_FOR_CONFIGURATION',
                'STAGED',
                'CONNECTING',
                'CONNECTED',
                'FAILED',
                'ROLLED_BACK',
                'RECOVERY_CHANNEL'
            )
        );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_device_configurations_rollback_status'
    ) THEN
        ALTER TABLE device_configurations
        ADD CONSTRAINT chk_device_configurations_rollback_status
        CHECK (
            rollback_status IS NULL OR rollback_status IN (
                'NOT_REQUIRED',
                'ROLLBACK_STARTED',
                'ROLLED_BACK',
                'RECOVERY_CHANNEL_ACTIVE'
            )
        );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_device_configurations_mqtt_status'
    ) THEN
        ALTER TABLE device_configurations
        ADD CONSTRAINT chk_device_configurations_mqtt_status
        CHECK (
            mqtt_status IS NULL OR mqtt_status IN (
                'PENDING',
                'PUBLISHED',
                'RECONNECTING',
                'CONNECTED',
                'FAILED',
                'PUBLISH_FAILED',
                'TIMED_OUT'
            )
        );
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_device_config_last_successful
ON device_configurations (last_successful_configuration_id);

CREATE INDEX IF NOT EXISTS idx_device_config_last_command
ON device_configurations (last_provisioning_command_id);

-- 3. Provisioning command completion and timeout state.
ALTER TABLE device_commands
ADD COLUMN IF NOT EXISTS timeout_at TIMESTAMPTZ;

ALTER TABLE device_commands
ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ;

DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    FOR constraint_name IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'device_commands'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) LIKE '%command_status%'
    LOOP
        EXECUTE format(
            'ALTER TABLE device_commands DROP CONSTRAINT IF EXISTS %I',
            constraint_name
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
                'STAGED',
                'APPLYING',
                'APPLIED',
                'FAILED',
                'ROLLED_BACK',
                'TIMED_OUT',
                'EXPIRED'
            )
        );
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_device_commands_wifi_timeout
ON device_commands (command_type, command_status, timeout_at)
WHERE command_type = 'WIFI_CONFIGURATION'
  AND completed_at IS NULL;

-- 4. Inner Unit Wi-Fi result correlation in the existing telemetry ledger.
ALTER TABLE device_telemetry_events
ADD COLUMN IF NOT EXISTS command_id BIGINT;

ALTER TABLE device_telemetry_events
ADD COLUMN IF NOT EXISTS command_uid VARCHAR(80);

ALTER TABLE device_telemetry_events
ADD COLUMN IF NOT EXISTS device_configuration_id BIGINT;

ALTER TABLE device_telemetry_events
ADD COLUMN IF NOT EXISTS configuration_version INTEGER;

ALTER TABLE device_telemetry_events
ADD COLUMN IF NOT EXISTS inner_device_id BIGINT;

ALTER TABLE device_telemetry_events
ADD COLUMN IF NOT EXISTS inner_device_uid VARCHAR(80);

ALTER TABLE device_telemetry_events
ADD COLUMN IF NOT EXISTS processing_result VARCHAR(60);

DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_device_telemetry_events_command'
    ) THEN
        ALTER TABLE device_telemetry_events
        ADD CONSTRAINT fk_device_telemetry_events_command
        FOREIGN KEY (command_id)
        REFERENCES device_commands(command_id)
        ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_device_telemetry_events_config'
    ) THEN
        ALTER TABLE device_telemetry_events
        ADD CONSTRAINT fk_device_telemetry_events_config
        FOREIGN KEY (device_configuration_id)
        REFERENCES device_configurations(configuration_id)
        ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_device_telemetry_events_inner'
    ) THEN
        ALTER TABLE device_telemetry_events
        ADD CONSTRAINT fk_device_telemetry_events_inner
        FOREIGN KEY (inner_device_id)
        REFERENCES devices(device_id)
        ON DELETE SET NULL;
    END IF;

    FOR constraint_name IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'device_telemetry_events'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) LIKE '%processing_result%'
    LOOP
        EXECUTE format(
            'ALTER TABLE device_telemetry_events DROP CONSTRAINT IF EXISTS %I',
            constraint_name
        );
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_device_telemetry_processing_result'
    ) THEN
        ALTER TABLE device_telemetry_events
        ADD CONSTRAINT chk_device_telemetry_processing_result
        CHECK (
            processing_result IS NULL OR processing_result IN (
                'ACCEPTED',
                'COMMAND_ID_MISSING',
                'COMMAND_NOT_FOUND',
                'COMMAND_TYPE_MISMATCH',
                'TOPIC_OUTER_UID_MISSING',
                'REPORTING_OUTER_UID_MISMATCH',
                'PAYLOAD_OUTER_UID_MISMATCH',
                'COMMAND_CONFIGURATION_REFERENCE_MISSING',
                'CONFIGURATION_NOT_FOUND',
                'CONFIGURATION_DEVICE_MISMATCH',
                'COMMAND_SUPERSEDED',
                'RESULT_CONFIGURATION_VERSION_MISSING',
                'RESULT_CONFIGURATION_VERSION_MISMATCH',
                'INNER_DEVICE_UID_MISSING',
                'INNER_DEVICE_NOT_FOUND',
                'INNER_DEVICE_TYPE_MISMATCH',
                'CONFIGURATION_INNER_DEVICE_MISMATCH',
                'DEVICE_PATIENT_MISMATCH',
                'INNER_DEVICE_PATIENT_MISMATCH',
                'OUTER_DEVICE_TYPE_MISMATCH',
                'OUTER_KIT_NOT_FOUND',
                'INNER_KIT_NOT_FOUND',
                'KIT_DEVICE_ROLE_MISMATCH',
                'KIT_MISMATCH',
                'KIT_PATIENT_MISMATCH',
                'FAILED'
            )
        );
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_device_telemetry_command
ON device_telemetry_events (command_id, configuration_version);

CREATE INDEX IF NOT EXISTS idx_device_telemetry_config_type_time
ON device_telemetry_events (device_configuration_id, event_type, received_at DESC);

CREATE INDEX IF NOT EXISTS idx_device_telemetry_inner
ON device_telemetry_events (inner_device_uid, received_at DESC);

COMMENT ON COLUMN device_command_acknowledgements.ack_deduplication_key
IS 'Stable QoS 1 idempotency key for Wi-Fi command acknowledgements. Legacy rows are marked LEGACY|acknowledgement_id.';

COMMENT ON COLUMN device_command_acknowledgements.processing_result
IS 'Controlled backend classification for accepted, rejected, stale or superseded Wi-Fi command acknowledgements.';

COMMENT ON COLUMN device_configurations.last_successful_configuration_version
IS 'Last Wi-Fi configuration version that completed all backend-observable provisioning checks.';

COMMENT ON COLUMN device_configurations.previous_configuration_version
IS 'Previous working Wi-Fi configuration version preserved when a new provisioning attempt starts.';

COMMENT ON COLUMN device_configurations.rollback_status
IS 'Backend-recorded firmware rollback or recovery-channel state.';

COMMENT ON COLUMN device_configurations.mqtt_status
IS 'Outer Unit MQTT reconnect dimension for provisioning status polling.';

COMMENT ON COLUMN device_commands.timeout_at
IS 'Provisioning timeout deadline after successful MQTT publication. Distinct from MQTT publish retry timing.';

COMMENT ON COLUMN device_commands.completed_at
IS 'Terminal provisioning timestamp for successful, failed, rolled-back or timed-out command attempts.';

COMMENT ON COLUMN device_telemetry_events.processing_result
IS 'Controlled backend classification for Inner Unit Wi-Fi result correlation.';

COMMIT;
