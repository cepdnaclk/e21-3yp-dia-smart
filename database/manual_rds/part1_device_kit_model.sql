-- Part 1 - Secure Device Access and Device-Kit Data Model
-- Manual PostgreSQL script for Dia-Smart RDS.
-- Review before running against any shared or production database.

BEGIN;

CREATE TABLE IF NOT EXISTS device_kits (
    device_kit_id BIGSERIAL PRIMARY KEY,
    kit_uid VARCHAR(80) NOT NULL,
    buyer_id BIGINT NOT NULL,
    purchase_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS device_kit_devices (
    device_kit_device_id BIGSERIAL PRIMARY KEY,
    device_kit_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    kit_device_role VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_device_kits_kit_uid
ON device_kits (kit_uid);

CREATE INDEX IF NOT EXISTS ix_device_kits_buyer_id
ON device_kits (buyer_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_device_kit_devices_device
ON device_kit_devices (device_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_device_kit_devices_role
ON device_kit_devices (device_kit_id, kit_device_role);

CREATE INDEX IF NOT EXISTS ix_device_kit_devices_kit_id
ON device_kit_devices (device_kit_id);

CREATE INDEX IF NOT EXISTS ix_device_kit_devices_device_id
ON device_kit_devices (device_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_device_kits_status'
    ) THEN
        ALTER TABLE device_kits
        ADD CONSTRAINT chk_device_kits_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DEACTIVATED'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_device_kits_buyer'
    ) THEN
        ALTER TABLE device_kits
        ADD CONSTRAINT fk_device_kits_buyer
        FOREIGN KEY (buyer_id)
        REFERENCES buyers(buyer_id)
        ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_device_kit_devices_role'
    ) THEN
        ALTER TABLE device_kit_devices
        ADD CONSTRAINT chk_device_kit_devices_role
        CHECK (kit_device_role IN (
            'OUTER_GATEWAY',
            'INNER_UNIT',
            'DOSE_CAP',
            'GLUCOMETER'
        ));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_device_kit_devices_kit'
    ) THEN
        ALTER TABLE device_kit_devices
        ADD CONSTRAINT fk_device_kit_devices_kit
        FOREIGN KEY (device_kit_id)
        REFERENCES device_kits(device_kit_id)
        ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_device_kit_devices_device'
    ) THEN
        ALTER TABLE device_kit_devices
        ADD CONSTRAINT fk_device_kit_devices_device
        FOREIGN KEY (device_id)
        REFERENCES devices(device_id)
        ON DELETE CASCADE;
    END IF;
END
$$;

COMMENT ON TABLE device_kits
IS 'Admin-registered Dia-Smart purchase kits. Legacy buyer-linked devices without memberships remain ungrouped.';

COMMENT ON TABLE device_kit_devices
IS 'Membership table linking each device to exactly one kit role within one device kit.';

COMMIT;
