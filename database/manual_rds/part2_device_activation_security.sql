-- Part 2 - Complete Four-Device Kit Activation and Rate Limiting
-- Manual PostgreSQL script for Dia-Smart RDS.
-- Run after database/manual_rds/part1_device_kit_model.sql.
-- Review before running against any shared or production database.

BEGIN;

ALTER TABLE device_kits
ADD COLUMN IF NOT EXISTS patient_id BIGINT;

ALTER TABLE device_kits
ADD COLUMN IF NOT EXISTS activated_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS ix_device_kits_patient_id
ON device_kits (patient_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_device_kits_patient'
    ) THEN
        ALTER TABLE device_kits
        ADD CONSTRAINT fk_device_kits_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(patient_id)
        ON DELETE SET NULL;
    END IF;
END
$$;

CREATE TABLE IF NOT EXISTS device_activation_attempts (
    activation_attempt_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    patient_id BIGINT,
    kit_id BIGINT,
    ip_address VARCHAR(64),
    success BOOLEAN NOT NULL DEFAULT FALSE,
    failure_category VARCHAR(40),
    request_fingerprint VARCHAR(64),
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    blocked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_device_activation_attempts_user_time
ON device_activation_attempts (user_id, attempted_at DESC);

CREATE INDEX IF NOT EXISTS ix_device_activation_attempts_ip_time
ON device_activation_attempts (ip_address, attempted_at DESC)
WHERE ip_address IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_device_activation_attempts_user_failed_time
ON device_activation_attempts (user_id, attempted_at DESC)
WHERE success = FALSE;

CREATE INDEX IF NOT EXISTS ix_device_activation_attempts_ip_failed_time
ON device_activation_attempts (ip_address, attempted_at DESC)
WHERE success = FALSE AND ip_address IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_device_activation_attempts_blocked_until
ON device_activation_attempts (blocked_until)
WHERE blocked_until IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_device_activation_attempts_failure_category'
    ) THEN
        ALTER TABLE device_activation_attempts
        ADD CONSTRAINT chk_device_activation_attempts_failure_category
        CHECK (
            failure_category IS NULL OR failure_category IN (
                'INVALID_KIT',
                'UNAUTHORIZED_PATIENT',
                'DEVICE_CONFLICT',
                'INACTIVE_DEVICE',
                'TYPE_MISMATCH',
                'RATE_LIMITED',
                'INTEGRITY_ERROR'
            )
        );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_device_activation_attempts_success_category'
    ) THEN
        ALTER TABLE device_activation_attempts
        ADD CONSTRAINT chk_device_activation_attempts_success_category
        CHECK (
            (success = TRUE AND failure_category IS NULL)
            OR (success = FALSE AND failure_category IS NOT NULL)
        );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_device_activation_attempts_user'
    ) THEN
        ALTER TABLE device_activation_attempts
        ADD CONSTRAINT fk_device_activation_attempts_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(user_id)
        ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_device_activation_attempts_patient'
    ) THEN
        ALTER TABLE device_activation_attempts
        ADD CONSTRAINT fk_device_activation_attempts_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(patient_id)
        ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_device_activation_attempts_kit'
    ) THEN
        ALTER TABLE device_activation_attempts
        ADD CONSTRAINT fk_device_activation_attempts_kit
        FOREIGN KEY (kit_id)
        REFERENCES device_kits(device_kit_id)
        ON DELETE SET NULL;
    END IF;
END
$$;

COMMENT ON COLUMN device_kits.patient_id
IS 'Patient currently linked to this complete registered kit after secure activation.';

COMMENT ON COLUMN device_kits.activated_at
IS 'UTC timestamp when the complete kit was first activated for a patient.';

COMMENT ON TABLE device_activation_attempts
IS 'Security log for device-kit activation attempts. Stores safe metadata and hashed request fingerprints, not raw device UIDs.';

COMMIT;
