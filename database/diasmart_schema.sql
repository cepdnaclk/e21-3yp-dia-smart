-- ==========================================
-- Dia-Smart Final PostgreSQL Database
-- One Device = One Patient
-- ==========================================

CREATE DATABASE diasmart;
\c diasmart;

-- ==========================================
-- 1️⃣ Patient Details (Doctor Dashboard Info)
-- ==========================================
CREATE TABLE patient (
    id SERIAL PRIMARY KEY,

    full_name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(10),

    contact_number VARCHAR(20),
    address TEXT,

    diabetes_type VARCHAR(20),  -- Type1 / Type2
    doctor_name VARCHAR(100),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default patient (since 1 device = 1 person)
INSERT INTO patient 
(full_name, date_of_birth, gender, contact_number, diabetes_type, doctor_name)
VALUES
('Test Patient', '1953-05-12', 'Male', '0771234567', 'Type2', 'Dr. Silva');


-- ==========================================
-- 2️⃣ All Readings Table (Single Table)
-- ==========================================
CREATE TABLE readings (
    id SERIAL PRIMARY KEY,

    temperature REAL,                      -- °C
    door_status VARCHAR(10) CHECK (door_status IN ('OPEN', 'CLOSED')),

    insulin_inventory_weight REAL,         -- grams (from load cell)
    insulin_level_value REAL,              -- image processing value

    glucose_value REAL,                    -- mg/dL (NULL if not measured)

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_readings_time ON readings(created_at);


-- ==========================================
-- 3️⃣ Alerts Table
-- ==========================================
CREATE TABLE alerts (
    id SERIAL PRIMARY KEY,

    alert_type VARCHAR(50),  
    alert_message TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alert_time ON alerts(created_at);

INSERT INTO readings
(temperature, door_status, insulin_inventory_weight, insulin_level_value)
VALUES
(5.4, 'CLOSED', 42.3, 68.5);

INSERT INTO readings
(temperature, door_status, insulin_inventory_weight, insulin_level_value, glucose_value)
VALUES
(5.5, 'CLOSED', 41.8, 66.2, 145);