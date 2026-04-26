/**
 * Dia-Smart master configuration.
 * Edit this single file, then run:
 *   node config/apply-config.js
 */

module.exports = {
  network: {
    backendIp: '192.168.1.187',
    backendPort: 3000,
    espNowChannel: 1,
  },

  wifi: {
    ssid: 'SLT-4G-74699C',
    password: 'Arnikan18',
  },

  postgres: {
    host: 'localhost',
    port: 5432,
    database: 'diasmart',
    user: 'postgres',
    password: 'Arnikan18',
  },

  server: {
    port: 3000,
    fileOnlyMode: false,
    corsOrigin: '*',
  },

  mqtt: {
    enabled: true,
    brokerUrl: 'mqtt://192.168.1.187:1883',
    username: '',
    password: '',
    clientId: '',
    topicPrefix: 'diasmart',
  },

  runtime: {
    dosePythonBin: 'python',
  },

  sensors: {
    loadCellCalibration: 245.0,
    tempSensorPin: 21,
    doorSensorPin: 4,
    hx711DoutPin: 5,
    hx711ClkPin: 18,
  },

  glucometer: {
    blePin: '836337',
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
