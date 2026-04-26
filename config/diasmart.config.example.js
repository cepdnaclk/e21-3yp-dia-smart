/**
 * Dia-Smart master configuration template.
 *
 * 1. Copy this file:
 *      config/diasmart.config.example.js
 *
 * 2. Rename the copy to:
 *      config/diasmart.config.js
 *
 * 3. Edit config/diasmart.config.js with your own local values.
 *
 * IMPORTANT:
 * - Do not commit config/diasmart.config.js
 * - Do not commit real Wi-Fi passwords
 * - Do not commit real database passwords
 */

module.exports = {
    network: {
        backendIp: 'YOUR_BACKEND_PC_IP',
        backendPort: 3000,
    },

    wifi: {
        ssid: 'YOUR_WIFI_SSID',
        password: 'YOUR_WIFI_PASSWORD',
    },

    postgres: {
        host: 'localhost',
        port: 5432,
        database: 'diasmart',
        user: 'diasmart_app',
        password: 'YOUR_DATABASE_PASSWORD',
    },

    server: {
        port: 3000,
        fileOnlyMode: false,
        corsOrigin: 'http://localhost:3000',
    },

    security: {
        jwtSecret: 'CHANGE_THIS_TO_A_LONG_RANDOM_SECRET_AT_LEAST_32_CHARS',
        jwtExpiresIn: '8h',
    },

    mqtt: {
        enabled: false,
        brokerUrl: 'mqtt://localhost:1883',
        username: '',
        password: '',
        clientId: '',
        topicPrefix: 'diasmart',
    },

    sensors: {
        loadCellCalibration: 245.0,
        tempSensorPin: 21,
        doorSensorPin: 4,
        hx711DoutPin: 5,
        hx711ClkPin: 18,
    },

    glucometer: {
        blePin: 'YOUR_GLUCOMETER_BLE_PIN',
        maxBufferRecords: 400,
        batchSize: 12,
        deviceName: 'Accu-Chek Guide Me',
    },

    dosageBle: {
        deviceName: 'Dose_ESP32_C3',
        serviceUuid: '12345678-1234-1234-1234-1234567890ab',
        characteristicUuid: 'abcd1234-5678-1234-5678-abcdef123456',
    },

    paths: {
        backendEnv: 'backend/sample_backend/.env',
        firmwareHeader: 'config/firmware_config.h',
        appConfig: 'frontend/rn-app/src/config/appConfig.ts',
    },
};