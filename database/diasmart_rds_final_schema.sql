-- ============================================================
-- Dia-Smart Final PostgreSQL Schema for AWS RDS
-- Final decision: alert/business logic is handled in Spring Boot.
-- Database stores data only. There are NO PostgreSQL alert triggers.
--
-- Target flow:
-- ESP32-S3 Gateway -> AWS IoT Core (MQTTS) -> Spring Boot API (HTTPS)
-- -> AWS RDS PostgreSQL
--
-- Run this inside the RDS database named: diasmart
-- Do NOT run \c, \gexec, or CREATE DATABASE inside RDS Query Editor.
--
-- psql example:
-- psql "host=<RDS_ENDPOINT> port=5432 dbname=diasmart user=<USER> sslmode=require" \
--   -f diasmart_rds_final_schema.sql
-- ============================================================

-- Used for UUID values. Supported by PostgreSQL on Amazon RDS.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- 1. Patients, users, and access control
-- ============================================================

CREATE TABLE IF NOT EXISTS patients (
    patient_id BIGSERIAL PRIMARY KEY,
    patient_uuid UUID NOT NULL DEFAULT gen_random_uuid(),

    -- Do NOT use NIC as the primary key. Keep it as a unique searchable field.
    nic VARCHAR(20),

    full_name VARCHAR(120) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(20) CHECK (gender IN ('MALE', 'FEMALE', 'OTHER', 'UNKNOWN')) DEFAULT 'UNKNOWN',

    contact_number VARCHAR(30),
    emergency_contact_number VARCHAR(30),
    address TEXT,

    diabetes_type VARCHAR(20) CHECK (diabetes_type IN ('TYPE_1', 'TYPE_2', 'GESTATIONAL', 'OTHER', 'UNKNOWN')) DEFAULT 'UNKNOWN',

    target_glucose_min_mg_dl NUMERIC(6,2) NOT NULL DEFAULT 70 CHECK (target_glucose_min_mg_dl >= 0),
    target_glucose_max_mg_dl NUMERIC(6,2) NOT NULL DEFAULT 140 CHECK (target_glucose_max_mg_dl >= target_glucose_min_mg_dl),

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (patient_uuid)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_patients_nic_upper
    ON patients (UPPER(nic))
    WHERE nic IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_patients_contact_number
    ON patients (contact_number);

CREATE TABLE IF NOT EXISTS app_users (
    user_id BIGSERIAL PRIMARY KEY,
    user_uuid UUID NOT NULL DEFAULT gen_random_uuid(),

    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,

    role VARCHAR(30) NOT NULL CHECK (role IN ('ADMIN', 'PATIENT', 'CAREGIVER', 'DOCTOR')),
    display_name VARCHAR(120) NOT NULL,
    contact_number VARCHAR(30),

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (user_uuid)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_app_users_email_lower
    ON app_users (LOWER(email));

CREATE INDEX IF NOT EXISTS idx_app_users_contact_number
    ON app_users (contact_number);

CREATE TABLE IF NOT EXISTS user_patient_access (
    access_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(user_id) ON DELETE CASCADE,
    patient_id BIGINT NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,

    access_role VARCHAR(30) NOT NULL CHECK (access_role IN ('SELF', 'CAREGIVER', 'DOCTOR')),
    relationship_label VARCHAR(80),

    can_view BOOLEAN NOT NULL DEFAULT TRUE,
    can_acknowledge_alerts BOOLEAN NOT NULL DEFAULT FALSE,
    can_edit_prescriptions BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (user_id, patient_id, access_role)
);

-- exra added coloum for above the table
ALTER TABLE user_patient_access
ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE';

ALTER TABLE user_patient_access
ADD COLUMN revoked_at TIMESTAMPTZ;

ALTER TABLE user_patient_access
ADD COLUMN revoked_by BIGINT
REFERENCES app_users(user_id);



CREATE INDEX IF NOT EXISTS idx_user_patient_access_user
    ON user_patient_access(user_id);

CREATE INDEX IF NOT EXISTS idx_user_patient_access_patient
    ON user_patient_access(patient_id);

-- Backend reads these values when deciding alerts.
-- These are settings only, NOT database triggers.
CREATE TABLE IF NOT EXISTS patient_alert_settings (
    settings_id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,

    safe_temperature_min_c NUMERIC(5,2) NOT NULL DEFAULT 2.00,
    safe_temperature_max_c NUMERIC(5,2) NOT NULL DEFAULT 8.00,

    low_inventory_warning_percent NUMERIC(5,2) NOT NULL DEFAULT 20.00,
    low_inventory_critical_percent NUMERIC(5,2) NOT NULL DEFAULT 10.00,

    battery_warning_percent NUMERIC(5,2) NOT NULL DEFAULT 20.00,
    battery_critical_percent NUMERIC(5,2) NOT NULL DEFAULT 10.00,

    door_open_warning_seconds INTEGER NOT NULL DEFAULT 120,
    missed_dose_grace_minutes INTEGER NOT NULL DEFAULT 60,
    duplicate_alert_suppression_minutes INTEGER NOT NULL DEFAULT 10,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (patient_id),

    CHECK (safe_temperature_max_c > safe_temperature_min_c),
    CHECK (low_inventory_warning_percent >= low_inventory_critical_percent),
    CHECK (battery_warning_percent >= battery_critical_percent)
);

-- ============================================================
-- 2. Device registry and battery/health logs
-- ============================================================

CREATE TABLE IF NOT EXISTS devices (
    device_id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT REFERENCES patients(patient_id) ON DELETE SET NULL,

    -- Example: DS-GW-001, DS-INNER-001, DS-CAP-001, DS-GLU-001
    device_uid VARCHAR(80) NOT NULL,

    aws_thing_name VARCHAR(128),
    mqtt_client_id VARCHAR(128),
    mac_address VARCHAR(40),
    serial_number VARCHAR(80),

    device_type VARCHAR(30) NOT NULL CHECK (device_type IN (
        'INNER_UNIT',
        'OUTER_GATEWAY',
        'DOSE_CAP',
        'GLUCOMETER',
        'OTHER'
    )),

    device_name VARCHAR(120),
    communication_type VARCHAR(30) CHECK (communication_type IN (
        'BLE',
        'ESP_NOW',
        'MQTTS',
        'HTTPS',
        'MANUAL',
        'OTHER'
    )) DEFAULT 'OTHER',

    firmware_version VARCHAR(50),
    hardware_version VARCHAR(50),

    last_seen_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (device_uid),
    UNIQUE (aws_thing_name),
    UNIQUE (mqtt_client_id),
    UNIQUE (mac_address),
    UNIQUE (serial_number)
);

CREATE INDEX IF NOT EXISTS idx_devices_patient
    ON devices(patient_id);

CREATE INDEX IF NOT EXISTS idx_devices_type
    ON devices(device_type);

-- Raw events table is created before health/reading tables so every normalized row can link to the original payload.
CREATE TABLE IF NOT EXISTS raw_device_events (
    raw_event_id BIGSERIAL PRIMARY KEY,
    event_uuid UUID NOT NULL DEFAULT gen_random_uuid(),

    -- Optional id generated by ESP32/backend for deduplication.
    source_event_id VARCHAR(120),

    device_id BIGINT REFERENCES devices(device_id) ON DELETE SET NULL,
    device_uid VARCHAR(80) NOT NULL,
    patient_id BIGINT REFERENCES patients(patient_id) ON DELETE SET NULL,

    mqtt_topic VARCHAR(255),
    event_type VARCHAR(50) NOT NULL CHECK (event_type IN (
        'STORAGE_READING',
        'INVENTORY_READING',
        'GLUCOSE_READING',
        'DOSE_EVENT',
        'DEVICE_HEALTH',
        'COMBINED_TELEMETRY',
        'UNKNOWN'
    )),

    event_time TIMESTAMPTZ,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    payload JSONB NOT NULL,

    processing_status VARCHAR(20) NOT NULL CHECK (processing_status IN (
        'RECEIVED',
        'PROCESSED',
        'FAILED',
        'IGNORED'
    )) DEFAULT 'RECEIVED',
    processing_error TEXT,

    UNIQUE (event_uuid)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_raw_events_source_dedupe
    ON raw_device_events(device_uid, source_event_id)
    WHERE source_event_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_raw_events_device_time
    ON raw_device_events(device_uid, received_at DESC);

CREATE INDEX IF NOT EXISTS idx_raw_events_patient_time
    ON raw_device_events(patient_id, received_at DESC);

CREATE INDEX IF NOT EXISTS idx_raw_events_type_time
    ON raw_device_events(event_type, received_at DESC);

CREATE INDEX IF NOT EXISTS idx_raw_events_payload_gin
    ON raw_device_events USING GIN (payload);

-- Battery level is stored here for each device/unit.
-- Inner unit and dose cap usually have battery values.
-- Outer gateway may use adapter power, so battery can be NULL.
CREATE TABLE IF NOT EXISTS device_health_logs (
    health_log_id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL REFERENCES devices(device_id) ON DELETE CASCADE,
    raw_event_id BIGINT REFERENCES raw_device_events(raw_event_id) ON DELETE SET NULL,

    measured_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    battery_percent NUMERIC(5,2) CHECK (battery_percent IS NULL OR (battery_percent >= 0 AND battery_percent <= 100)),
    battery_voltage_v NUMERIC(6,3),
    power_source VARCHAR(20) CHECK (power_source IN ('BATTERY', 'ADAPTER', 'USB', 'UNKNOWN')) DEFAULT 'UNKNOWN',

    wifi_rssi_dbm INTEGER,
    ble_rssi_dbm INTEGER,
    free_heap_bytes INTEGER,

    firmware_version VARCHAR(50),
    is_online BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(30) CHECK (status IN ('ONLINE', 'OFFLINE', 'LOW_BATTERY', 'ERROR', 'UNKNOWN')) DEFAULT 'UNKNOWN',

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_device_health_device_time
    ON device_health_logs(device_id, measured_at DESC);

CREATE INDEX IF NOT EXISTS idx_device_health_battery
    ON device_health_logs(battery_percent)
    WHERE battery_percent IS NOT NULL;

-- ============================================================
-- 3. Insulin products, prescriptions, and schedules
-- ============================================================

CREATE TABLE IF NOT EXISTS insulin_products (
    insulin_product_id BIGSERIAL PRIMARY KEY,
    product_name VARCHAR(120) NOT NULL,
    manufacturer VARCHAR(120),

    insulin_type VARCHAR(40) CHECK (insulin_type IN (
        'RAPID_ACTING',
        'SHORT_ACTING',
        'INTERMEDIATE_ACTING',
        'LONG_ACTING',
        'MIXED',
        'UNKNOWN'
    )) DEFAULT 'UNKNOWN',

    concentration_u_per_ml NUMERIC(8,2) NOT NULL DEFAULT 100 CHECK (concentration_u_per_ml > 0),
    cartridge_capacity_ml NUMERIC(6,2),
    units_per_cartridge NUMERIC(8,2),

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (product_name, concentration_u_per_ml)
);

CREATE TABLE IF NOT EXISTS prescriptions (
    prescription_id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,
    insulin_product_id BIGINT REFERENCES insulin_products(insulin_product_id) ON DELETE SET NULL,
    prescribed_by_user_id BIGINT REFERENCES app_users(user_id) ON DELETE SET NULL,

    prescription_name VARCHAR(120),
    start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    end_date DATE,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX IF NOT EXISTS idx_prescriptions_patient_active
    ON prescriptions(patient_id, is_active);

CREATE TABLE IF NOT EXISTS dose_schedules (
    schedule_id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES prescriptions(prescription_id) ON DELETE CASCADE,
    patient_id BIGINT NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,

    schedule_label VARCHAR(80) NOT NULL,
    scheduled_time TIME NOT NULL,
    dose_units NUMERIC(6,2) NOT NULL CHECK (dose_units > 0 AND dose_units <= 100),

    -- 1=Monday ... 7=Sunday. Backend interprets this text.
    days_of_week VARCHAR(20) NOT NULL DEFAULT '1,2,3,4,5,6,7',

    allowed_early_minutes INTEGER NOT NULL DEFAULT 60 CHECK (allowed_early_minutes >= 0),
    allowed_late_minutes INTEGER NOT NULL DEFAULT 120 CHECK (allowed_late_minutes >= 0),

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (prescription_id, schedule_label)
);

CREATE INDEX IF NOT EXISTS idx_dose_schedules_patient_time
    ON dose_schedules(patient_id, scheduled_time);

CREATE INDEX IF NOT EXISTS idx_dose_schedules_prescription
    ON dose_schedules(prescription_id, is_active);

-- ============================================================
-- 4. Normalized readings/events
-- ============================================================

CREATE TABLE IF NOT EXISTS storage_readings (
    storage_reading_id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,
    device_id BIGINT REFERENCES devices(device_id) ON DELETE SET NULL,
    raw_event_id BIGINT REFERENCES raw_device_events(raw_event_id) ON DELETE SET NULL,

    measured_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    temperature_c NUMERIC(6,2) CHECK (temperature_c IS NULL OR (temperature_c >= -40 AND temperature_c <= 80)),
    humidity_percent NUMERIC(5,2) CHECK (humidity_percent IS NULL OR (humidity_percent >= 0 AND humidity_percent <= 100)),

    door_state VARCHAR(10) CHECK (door_state IN ('OPEN', 'CLOSED', 'UNKNOWN')) DEFAULT 'UNKNOWN',
    door_open_duration_seconds INTEGER CHECK (door_open_duration_seconds IS NULL OR door_open_duration_seconds >= 0),

    temperature_status VARCHAR(20) CHECK (temperature_status IN ('SAFE', 'LOW', 'HIGH', 'UNKNOWN')) DEFAULT 'UNKNOWN',
    notes TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_storage_patient_time
    ON storage_readings(patient_id, measured_at DESC);

CREATE INDEX IF NOT EXISTS idx_storage_device_time
    ON storage_readings(device_id, measured_at DESC);

CREATE INDEX IF NOT EXISTS idx_storage_status_time
    ON storage_readings(temperature_status, measured_at DESC);

CREATE TABLE IF NOT EXISTS inventory_readings (
    inventory_reading_id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,
    device_id BIGINT REFERENCES devices(device_id) ON DELETE SET NULL,
    raw_event_id BIGINT REFERENCES raw_device_events(raw_event_id) ON DELETE SET NULL,

    measured_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    pen_present BOOLEAN,
    cartridge_present BOOLEAN,

    weight_g NUMERIC(9,3) CHECK (weight_g IS NULL OR weight_g >= 0),
    estimated_units_remaining NUMERIC(8,2) CHECK (estimated_units_remaining IS NULL OR estimated_units_remaining >= 0),
    estimated_remaining_percent NUMERIC(5,2) CHECK (estimated_remaining_percent IS NULL OR (estimated_remaining_percent >= 0 AND estimated_remaining_percent <= 100)),

    inventory_status VARCHAR(20) CHECK (inventory_status IN ('OK', 'LOW', 'CRITICAL', 'EMPTY', 'REMOVED', 'UNKNOWN')) DEFAULT 'UNKNOWN',
    notes TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_inventory_patient_time
    ON inventory_readings(patient_id, measured_at DESC);

CREATE INDEX IF NOT EXISTS idx_inventory_device_time
    ON inventory_readings(device_id, measured_at DESC);

CREATE INDEX IF NOT EXISTS idx_inventory_status_time
    ON inventory_readings(inventory_status, measured_at DESC);

CREATE TABLE IF NOT EXISTS glucose_readings (
    glucose_reading_id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,
    device_id BIGINT REFERENCES devices(device_id) ON DELETE SET NULL,
    raw_event_id BIGINT REFERENCES raw_device_events(raw_event_id) ON DELETE SET NULL,

    measured_at TIMESTAMPTZ NOT NULL,

    glucose_value_mg_dl NUMERIC(6,2) NOT NULL CHECK (glucose_value_mg_dl >= 20 AND glucose_value_mg_dl <= 600),

    source VARCHAR(30) NOT NULL CHECK (source IN ('BLE_GLUCOMETER', 'MANUAL', 'ESTIMATED', 'TEST')) DEFAULT 'BLE_GLUCOMETER',
    meal_context VARCHAR(30) CHECK (meal_context IN ('FASTING', 'BEFORE_MEAL', 'AFTER_MEAL', 'BEDTIME', 'RANDOM', 'UNKNOWN')) DEFAULT 'UNKNOWN',

    glucometer_sequence_number INTEGER,
    notes TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- exra added coloum for above the table

 ALTER TABLE glucose_readings
ADD COLUMN entered_by_user_id BIGINT
REFERENCES app_users(user_id);


CREATE INDEX IF NOT EXISTS idx_glucose_patient_time
    ON glucose_readings(patient_id, measured_at DESC);

CREATE INDEX IF NOT EXISTS idx_glucose_device_time
    ON glucose_readings(device_id, measured_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS ux_glucose_device_sequence
    ON glucose_readings(device_id, glucometer_sequence_number)
    WHERE device_id IS NOT NULL AND glucometer_sequence_number IS NOT NULL;

CREATE TABLE IF NOT EXISTS dose_events (
    dose_event_id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,
    device_id BIGINT REFERENCES devices(device_id) ON DELETE SET NULL,
    raw_event_id BIGINT REFERENCES raw_device_events(raw_event_id) ON DELETE SET NULL,

    prescription_id BIGINT REFERENCES prescriptions(prescription_id) ON DELETE SET NULL,
    schedule_id BIGINT REFERENCES dose_schedules(schedule_id) ON DELETE SET NULL,

    injected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dose_units NUMERIC(6,2) NOT NULL CHECK (dose_units > 0 AND dose_units <= 100),

    detection_method VARCHAR(30) NOT NULL CHECK (detection_method IN ('AS5600', 'MANUAL', 'BLE_NOTIFY', 'ESTIMATED', 'TEST')) DEFAULT 'AS5600',
    angle_degrees NUMERIC(8,2),
    confidence_percent NUMERIC(5,2) CHECK (confidence_percent IS NULL OR (confidence_percent >= 0 AND confidence_percent <= 100)),

    event_status VARCHAR(20) NOT NULL CHECK (event_status IN ('CONFIRMED', 'PENDING', 'REJECTED')) DEFAULT 'CONFIRMED',
    notes TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- exra added coloum for above the table
ALTER TABLE dose_events
ADD COLUMN entered_by_user_id BIGINT
REFERENCES app_users(user_id);


CREATE INDEX IF NOT EXISTS idx_dose_patient_time
    ON dose_events(patient_id, injected_at DESC);

CREATE INDEX IF NOT EXISTS idx_dose_device_time
    ON dose_events(device_id, injected_at DESC);

CREATE INDEX IF NOT EXISTS idx_dose_schedule_time
    ON dose_events(schedule_id, injected_at DESC);

-- ============================================================
-- 5. Alerts and notifications
-- ============================================================
-- IMPORTANT:
-- The backend creates records in this table.
-- PostgreSQL does NOT generate alerts automatically in this design.

CREATE TABLE IF NOT EXISTS alerts (
    alert_id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,
    device_id BIGINT REFERENCES devices(device_id) ON DELETE SET NULL,
    raw_event_id BIGINT REFERENCES raw_device_events(raw_event_id) ON DELETE SET NULL,

    related_table VARCHAR(60),
    related_id BIGINT,

    alert_type VARCHAR(50) NOT NULL CHECK (alert_type IN (
        'TEMP_LOW',
        'TEMP_HIGH',
        'DOOR_LEFT_OPEN',
        'LOW_INVENTORY',
        'CRITICAL_INVENTORY',
        'INSULIN_REMOVED',
        'MISSED_DOSE',
        'DOUBLE_DOSE',
        'ABNORMAL_DOSE',
        'GLUCOSE_LOW',
        'GLUCOSE_HIGH',
        'DEVICE_OFFLINE',
        'LOW_BATTERY',
        'CRITICAL_BATTERY',
        'SYSTEM'
    )),

    severity VARCHAR(20) NOT NULL CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')) DEFAULT 'WARNING',
    title VARCHAR(160) NOT NULL,
    message TEXT NOT NULL,

    -- Backend can use dedupe_key to avoid repeating same alert continuously.
    -- Example: device_uid + alert_type + date/hour window.
    dedupe_key VARCHAR(200),

    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'DISMISSED')) DEFAULT 'OPEN',

    first_detected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_detected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    acknowledged_at TIMESTAMPTZ,
    acknowledged_by BIGINT REFERENCES app_users(user_id) ON DELETE SET NULL,

    resolved_at TIMESTAMPTZ,
    resolved_by BIGINT REFERENCES app_users(user_id) ON DELETE SET NULL,
    resolution_note TEXT
);

CREATE INDEX IF NOT EXISTS idx_alerts_patient_time
    ON alerts(patient_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_alerts_status_severity
    ON alerts(status, severity, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_alerts_type_time
    ON alerts(alert_type, created_at DESC);

-- This is not an alert trigger. It only helps backend avoid duplicate open alerts.
CREATE UNIQUE INDEX IF NOT EXISTS ux_alerts_open_dedupe
    ON alerts(patient_id, alert_type, dedupe_key)
    WHERE status = 'OPEN' AND dedupe_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS notification_logs (
    notification_log_id BIGSERIAL PRIMARY KEY,
    alert_id BIGINT NOT NULL REFERENCES alerts(alert_id) ON DELETE CASCADE,
    recipient_user_id BIGINT REFERENCES app_users(user_id) ON DELETE SET NULL,

    channel VARCHAR(20) NOT NULL CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'IN_APP', 'WEBHOOK')),
    destination VARCHAR(255),

    delivery_status VARCHAR(20) NOT NULL CHECK (delivery_status IN ('PENDING', 'SENT', 'FAILED', 'SKIPPED')) DEFAULT 'PENDING',
    provider_message_id VARCHAR(255),
    error_message TEXT,

    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_alert
    ON notification_logs(alert_id);

CREATE INDEX IF NOT EXISTS idx_notification_recipient_time
    ON notification_logs(recipient_user_id, created_at DESC);

-- ============================================================
-- 6. Useful views for testing, backend APIs, and future dashboard
-- ============================================================

CREATE OR REPLACE VIEW v_device_latest_health AS
SELECT
    d.device_id,
    d.patient_id,
    d.device_uid,
    d.device_type,
    d.device_name,
    d.last_seen_at,
    d.is_active,
    h.measured_at AS latest_health_at,
    h.battery_percent,
    h.battery_voltage_v,
    h.power_source,
    h.wifi_rssi_dbm,
    h.ble_rssi_dbm,
    h.status AS latest_health_status
FROM devices d
LEFT JOIN LATERAL (
    SELECT
        measured_at,
        battery_percent,
        battery_voltage_v,
        power_source,
        wifi_rssi_dbm,
        ble_rssi_dbm,
        status
    FROM device_health_logs h2
    WHERE h2.device_id = d.device_id
    ORDER BY h2.measured_at DESC
    LIMIT 1
) h ON TRUE;

CREATE OR REPLACE VIEW v_open_alerts AS
SELECT
    a.alert_id,
    a.patient_id,
    p.full_name,
    p.nic,
    p.contact_number,
    a.device_id,
    d.device_uid,
    d.device_type,
    a.alert_type,
    a.severity,
    a.title,
    a.message,
    a.status,
    a.first_detected_at,
    a.last_detected_at,
    a.created_at
FROM alerts a
JOIN patients p ON p.patient_id = a.patient_id
LEFT JOIN devices d ON d.device_id = a.device_id
WHERE a.status = 'OPEN'
ORDER BY
    CASE a.severity
        WHEN 'CRITICAL' THEN 1
        WHEN 'WARNING' THEN 2
        ELSE 3
    END,
    a.created_at DESC;

CREATE OR REPLACE VIEW v_patient_latest_summary AS
SELECT
    p.patient_id,
    p.patient_uuid,
    p.nic,
    p.full_name,
    p.contact_number,

    g.glucose_value_mg_dl AS latest_glucose_mg_dl,
    g.measured_at AS latest_glucose_at,

    s.temperature_c AS latest_temperature_c,
    s.temperature_status AS latest_temperature_status,
    s.door_state AS latest_door_state,
    s.measured_at AS latest_storage_at,

    i.weight_g AS latest_inventory_weight_g,
    i.estimated_units_remaining AS latest_units_remaining,
    i.estimated_remaining_percent AS latest_inventory_percent,
    i.inventory_status AS latest_inventory_status,
    i.measured_at AS latest_inventory_at,

    de.dose_units AS latest_dose_units,
    de.injected_at AS latest_dose_at,

    COALESCE(al.open_alert_count, 0) AS open_alert_count,
    COALESCE(al.critical_alert_count, 0) AS critical_alert_count
FROM patients p
LEFT JOIN LATERAL (
    SELECT glucose_value_mg_dl, measured_at
    FROM glucose_readings gr
    WHERE gr.patient_id = p.patient_id
    ORDER BY measured_at DESC
    LIMIT 1
) g ON TRUE
LEFT JOIN LATERAL (
    SELECT temperature_c, temperature_status, door_state, measured_at
    FROM storage_readings sr
    WHERE sr.patient_id = p.patient_id
    ORDER BY measured_at DESC
    LIMIT 1
) s ON TRUE
LEFT JOIN LATERAL (
    SELECT weight_g, estimated_units_remaining, estimated_remaining_percent, inventory_status, measured_at
    FROM inventory_readings ir
    WHERE ir.patient_id = p.patient_id
    ORDER BY measured_at DESC
    LIMIT 1
) i ON TRUE
LEFT JOIN LATERAL (
    SELECT dose_units, injected_at
    FROM dose_events dx
    WHERE dx.patient_id = p.patient_id
    ORDER BY injected_at DESC
    LIMIT 1
) de ON TRUE
LEFT JOIN LATERAL (
    SELECT
        COUNT(*) FILTER (WHERE status = 'OPEN') AS open_alert_count,
        COUNT(*) FILTER (WHERE status = 'OPEN' AND severity = 'CRITICAL') AS critical_alert_count
    FROM alerts ax
    WHERE ax.patient_id = p.patient_id
) al ON TRUE;

CREATE OR REPLACE VIEW v_daily_patient_metrics AS
SELECT
    p.patient_id,
    p.full_name,
    day_series.metric_day::date AS metric_date,

    COUNT(DISTINCT gr.glucose_reading_id) AS glucose_count,
    ROUND(AVG(gr.glucose_value_mg_dl), 2) AS avg_glucose_mg_dl,

    COUNT(DISTINCT de.dose_event_id) AS dose_count,
    ROUND(COALESCE(SUM(de.dose_units), 0), 2) AS total_dose_units,

    COUNT(DISTINCT CASE WHEN al.status = 'OPEN' THEN al.alert_id END) AS open_alert_count
FROM patients p
CROSS JOIN LATERAL generate_series(
    CURRENT_DATE - INTERVAL '30 days',
    CURRENT_DATE,
    INTERVAL '1 day'
) AS day_series(metric_day)
LEFT JOIN glucose_readings gr
    ON gr.patient_id = p.patient_id
   AND gr.measured_at >= day_series.metric_day
   AND gr.measured_at < day_series.metric_day + INTERVAL '1 day'
LEFT JOIN dose_events de
    ON de.patient_id = p.patient_id
   AND de.injected_at >= day_series.metric_day
   AND de.injected_at < day_series.metric_day + INTERVAL '1 day'
LEFT JOIN alerts al
    ON al.patient_id = p.patient_id
   AND al.created_at >= day_series.metric_day
   AND al.created_at < day_series.metric_day + INTERVAL '1 day'
GROUP BY p.patient_id, p.full_name, day_series.metric_day
ORDER BY p.patient_id, metric_date DESC;

-- ============================================================
-- relationship_requests table ADD
-- ============================================================

CREATE TABLE relationship_requests (
    request_id BIGSERIAL PRIMARY KEY,

    requester_user_id BIGINT NOT NULL
        REFERENCES app_users(user_id),

    target_user_id BIGINT
        REFERENCES app_users(user_id),

    patient_id BIGINT NOT NULL
        REFERENCES patients(patient_id),

    relationship_role VARCHAR(30) NOT NULL CHECK (
        relationship_role IN ('CAREGIVER', 'DOCTOR')
    ),

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (
        status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'REVOKED')
    ),

    message TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMPTZ
);

-- ============================================================
-- audit_logs table ADD
-- ============================================================

CREATE TABLE audit_logs (
    audit_log_id BIGSERIAL PRIMARY KEY,

    user_id BIGINT
        REFERENCES app_users(user_id),

    patient_id BIGINT
        REFERENCES patients(patient_id),

    action_type VARCHAR(100) NOT NULL,

    entity_type VARCHAR(100),
    entity_id BIGINT,

    ip_address VARCHAR(64),

    details JSONB,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- notification_preferences table ADD
-- ============================================================

CREATE TABLE notification_preferences (
    preference_id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL UNIQUE
        REFERENCES app_users(user_id),

    reminder_enabled BOOLEAN DEFAULT TRUE,
    buzzer_enabled BOOLEAN DEFAULT TRUE,

    reminder_minutes_before INTEGER DEFAULT 15,

    push_enabled BOOLEAN DEFAULT TRUE,
    sms_enabled BOOLEAN DEFAULT FALSE,
    email_enabled BOOLEAN DEFAULT FALSE,

    caregiver_notifications_enabled BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);




-- ============================================================
-- 7. Optional seed data for local testing only
-- ============================================================
-- Keep this commented in production. Insert real users using your Spring Boot registration/admin flow.

-- INSERT INTO patients (
--     nic, full_name, date_of_birth, gender, contact_number,
--     emergency_contact_number, diabetes_type, target_glucose_min_mg_dl, target_glucose_max_mg_dl
-- ) VALUES (
--     '195312345678', 'Test Patient', '1953-05-12', 'MALE', '0771234567',
--     '0717654321', 'TYPE_2', 70, 140
-- ) ON CONFLICT DO NOTHING;

-- INSERT INTO patient_alert_settings (patient_id)
-- SELECT patient_id FROM patients WHERE nic = '195312345678'
-- ON CONFLICT (patient_id) DO NOTHING;

-- INSERT INTO devices (patient_id, device_uid, aws_thing_name, device_type, device_name, communication_type)
-- SELECT patient_id, 'DS-GW-001', 'diasmart-gateway-001', 'OUTER_GATEWAY', 'Dia-Smart Outer Gateway', 'MQTTS'
-- FROM patients WHERE nic = '195312345678'
-- ON CONFLICT DO NOTHING;

-- INSERT INTO devices (patient_id, device_uid, device_type, device_name, communication_type)
-- SELECT patient_id, 'DS-INNER-001', 'INNER_UNIT', 'Dia-Smart Inner Unit', 'ESP_NOW'
-- FROM patients WHERE nic = '195312345678'
-- ON CONFLICT DO NOTHING;

-- ============================================================
-- 8. Optional app user grants
-- ============================================================
-- Create a separate RDS user for Spring Boot and grant only needed access.
-- Replace username/password before running manually.

-- CREATE ROLE diasmart_app LOGIN PASSWORD 'CHANGE_THIS_STRONG_PASSWORD';
-- GRANT CONNECT ON DATABASE diasmart TO diasmart_app;
-- GRANT USAGE ON SCHEMA public TO diasmart_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO diasmart_app;
-- GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO diasmart_app;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO diasmart_app;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO diasmart_app;

-- ============================================================
-- End of Dia-Smart final RDS schema.
-- ============================================================
CREATE TABLE buyers (
    buyer_id BIGSERIAL PRIMARY KEY,

    full_name VARCHAR(150) NOT NULL,

    nic VARCHAR(30) NOT NULL UNIQUE,

    contact_number VARCHAR(20) NOT NULL,

    address TEXT,

    purchase_date DATE NOT NULL DEFAULT CURRENT_DATE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE devices
ADD COLUMN buyer_id BIGINT;

ALTER TABLE devices
ADD CONSTRAINT fk_devices_buyer
FOREIGN KEY (buyer_id)
REFERENCES buyers(buyer_id)
ON DELETE SET NULL;

INSERT INTO buyers (
    full_name,
    nic,
    contact_number,
    address,
    purchase_date
)
VALUES
(
    'John Silva',
    '991234567V',
    '0771234567',
    'Colombo',
    CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS device_configurations (

    configuration_id BIGSERIAL PRIMARY KEY,

    outer_device_id BIGINT NOT NULL
        REFERENCES devices(device_id) ON DELETE CASCADE,

    patient_id BIGINT NOT NULL
        REFERENCES patients(patient_id) ON DELETE CASCADE,

    wifi_ssid VARCHAR(100) NOT NULL,

    wifi_password TEXT NOT NULL,

    configuration_status VARCHAR(20)
        DEFAULT 'PENDING'
        CHECK (configuration_status IN (
            'PENDING',
            'SENT',
            'APPLIED',
            'FAILED'
        )),

    last_synced_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (outer_device_id)
);

CREATE INDEX idx_device_config_patient
ON device_configurations(patient_id);

CREATE INDEX idx_device_config_outer
ON device_configurations(outer_device_id);

CREATE TABLE IF NOT EXISTS device_commands (

    command_id BIGSERIAL PRIMARY KEY,

    device_id BIGINT NOT NULL
        REFERENCES devices(device_id) ON DELETE CASCADE,

    patient_id BIGINT
        REFERENCES patients(patient_id) ON DELETE SET NULL,

    command_type VARCHAR(40) NOT NULL
        CHECK (command_type IN (
            'CONFIG_UPDATE',
            'CARE_PLAN_UPDATE',
            'REMINDER_UPDATE',
            'SYNC_REQUEST',
            'RESTART_DEVICE'
        )),

    payload JSONB NOT NULL,

    command_status VARCHAR(20)
        DEFAULT 'PENDING'
        CHECK (command_status IN (
            'PENDING',
            'SENT',
            'RECEIVED',
            'APPLIED',
            'FAILED',
            'EXPIRED'
        )),

    published_at TIMESTAMPTZ,

    acknowledged_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_device_commands_device
ON device_commands(device_id);

CREATE INDEX idx_device_commands_status
ON device_commands(command_status);

CREATE TABLE IF NOT EXISTS device_command_acknowledgements (

    acknowledgement_id BIGSERIAL PRIMARY KEY,

    command_id BIGINT NOT NULL
        REFERENCES device_commands(command_id)
        ON DELETE CASCADE,

    device_id BIGINT NOT NULL
        REFERENCES devices(device_id)
        ON DELETE CASCADE,

    ack_status VARCHAR(20)
        CHECK (ack_status IN (
            'RECEIVED',
            'APPLIED',
            'REJECTED',
            'FAILED'
        )),

    response_message TEXT,

    acknowledged_at TIMESTAMPTZ
        DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_device_ack_command
ON device_command_acknowledgements(command_id);