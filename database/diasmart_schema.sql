-- ============================================================
-- Dia-Smart complete PostgreSQL schema
-- Run with: psql -U postgres -f database/diasmart_schema.sql
-- ============================================================

SELECT 'CREATE DATABASE diasmart'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'diasmart')\gexec

\c diasmart;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE TABLE IF NOT EXISTS patient (
        id SERIAL PRIMARY KEY,
        full_name VARCHAR(100) NOT NULL,
        date_of_birth DATE,
        gender VARCHAR(10) CHECK (gender IN ('Male', 'Female', 'Other')),
        contact_number VARCHAR(20),
        address TEXT,
        diabetes_type VARCHAR(20),
        doctor_name VARCHAR(100),
        doctor_contact VARCHAR(20),
        target_glucose_min REAL DEFAULT 70,
        target_glucose_max REAL DEFAULT 140,
        daily_insulin_target INTEGER DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_patient_updated_at ON patient;
CREATE TRIGGER trg_patient_updated_at
BEFORE UPDATE ON patient
FOR EACH ROW EXECUTE FUNCTION update_updated_at();

INSERT INTO patient (
    id, full_name, date_of_birth, gender, contact_number,
    diabetes_type, doctor_name, doctor_contact,
    target_glucose_min, target_glucose_max, daily_insulin_target
) VALUES (
    1, 'Test Patient', '1953-05-12', 'Male', '0771234567',
    'Type2', 'Dr. Silva', '0112345678', 70, 140, 20
)
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS app_users (
        id SERIAL PRIMARY KEY,
        email VARCHAR(200) UNIQUE NOT NULL,
        password_hash VARCHAR(200) NOT NULL,
        role VARCHAR(20) NOT NULL CHECK (role IN ('patient', 'caregiver', 'doctor')),
        display_name VARCHAR(100),
        patient_id INTEGER REFERENCES patient(id) ON DELETE SET NULL,
        is_active BOOLEAN DEFAULT TRUE,
        last_login_at TIMESTAMP,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_users_updated_at ON app_users;
CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON app_users
FOR EACH ROW EXECUTE FUNCTION update_updated_at();

INSERT INTO app_users (email, password_hash, role, display_name, patient_id)
VALUES
    ('caregiver@diasmart.com', crypt('Care1234', gen_salt('bf')), 'caregiver', 'Demo Caregiver', 1),
    ('doctor@diasmart.com', crypt('Doctor1234', gen_salt('bf')), 'doctor', 'Demo Doctor', 1),
    ('patient@diasmart.com', crypt('Patient1234', gen_salt('bf')), 'patient', 'Demo Patient', 1)
ON CONFLICT (email) DO NOTHING;

CREATE TABLE IF NOT EXISTS readings (
        id SERIAL PRIMARY KEY,
        patient_id INTEGER REFERENCES patient(id) DEFAULT 1,
        temperature REAL,
        door_status VARCHAR(10) CHECK (door_status IN ('OPEN', 'CLOSED')),
        insulin_inventory_weight REAL,
        insulin_level_value REAL,
        glucose_value REAL,
        glucose_source VARCHAR(20) CHECK (glucose_source IN ('glucometer', 'manual', 'estimated')),
        glucose_meal_context VARCHAR(20),
        device_mac VARCHAR(20),
        firmware_version VARCHAR(20),
        raw_payload JSONB,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_readings_time ON readings(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_readings_patient_time ON readings(patient_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_readings_glucose ON readings(glucose_value) WHERE glucose_value IS NOT NULL;

DO $$ BEGIN
    CREATE TYPE alert_severity AS ENUM ('info', 'warning', 'critical');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

CREATE TABLE IF NOT EXISTS alerts (
        id SERIAL PRIMARY KEY,
        patient_id INTEGER REFERENCES patient(id) DEFAULT 1,
        alert_type VARCHAR(50) NOT NULL,
        alert_severity alert_severity DEFAULT 'warning',
        alert_message TEXT,
        reading_id INTEGER REFERENCES readings(id) ON DELETE SET NULL,
        is_acknowledged BOOLEAN DEFAULT FALSE,
        acknowledged_at TIMESTAMP,
        acknowledged_by INTEGER REFERENCES app_users(id) ON DELETE SET NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_alert_time ON alerts(created_at DESC);

CREATE TABLE IF NOT EXISTS dosage_timeline (
        id SERIAL PRIMARY KEY,
        patient_id INTEGER REFERENCES patient(id) DEFAULT 1,
        dose_amount INTEGER NOT NULL CHECK (dose_amount > 0 AND dose_amount <= 100),
        injection_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        detection_method VARCHAR(30) DEFAULT 'ai-video',
        confidence_pct REAL,
        notes TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dosage_time ON dosage_timeline(injection_time DESC);

CREATE TABLE IF NOT EXISTS devices (
        id SERIAL PRIMARY KEY,
        patient_id INTEGER REFERENCES patient(id) DEFAULT 1,
        device_type VARCHAR(20) NOT NULL CHECK (device_type IN ('inner_unit', 'outer_unit', 'glucometer')),
        device_name VARCHAR(100),
        mac_address VARCHAR(20) UNIQUE,
        firmware_version VARCHAR(20),
        last_seen_at TIMESTAMP,
        is_active BOOLEAN DEFAULT TRUE,
        notes TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE VIEW v_latest_summary AS
SELECT
    (SELECT temperature FROM readings WHERE temperature IS NOT NULL ORDER BY created_at DESC LIMIT 1) AS temperature,
    (SELECT door_status FROM readings WHERE door_status IS NOT NULL ORDER BY created_at DESC LIMIT 1) AS door_status,
    (SELECT insulin_inventory_weight FROM readings WHERE insulin_inventory_weight IS NOT NULL ORDER BY created_at DESC LIMIT 1) AS insulin_inventory_weight,
    (SELECT insulin_level_value FROM readings WHERE insulin_level_value IS NOT NULL ORDER BY created_at DESC LIMIT 1) AS insulin_level_value,
    (SELECT glucose_value FROM readings WHERE glucose_value IS NOT NULL ORDER BY created_at DESC LIMIT 1) AS glucose_value,
    (SELECT created_at FROM readings WHERE glucose_value IS NOT NULL ORDER BY created_at DESC LIMIT 1) AS glucose_last_at,
    (SELECT created_at FROM readings ORDER BY created_at DESC LIMIT 1) AS latest_event_at;

INSERT INTO readings (door_status, insulin_inventory_weight, created_at)
VALUES
    ('OPEN', 123.45, '2026-03-11 10:00:00'),
    ('CLOSED', 138.20, '2026-03-11 10:00:03')
ON CONFLICT DO NOTHING;

INSERT INTO readings (glucose_value, glucose_source, created_at)
VALUES
    (109, 'glucometer', '2022-03-10 07:15:00'),
    (145, 'glucometer', '2022-03-10 13:45:00'),
    (118, 'glucometer', '2026-03-11 07:00:00')
ON CONFLICT DO NOTHING;

INSERT INTO dosage_timeline (dose_amount, injection_time, detection_method)
VALUES
    (9, '2026-03-11 08:00:00', 'ai-video'),
    (15, '2026-03-11 20:00:00', 'ai-video')
ON CONFLICT DO NOTHING;