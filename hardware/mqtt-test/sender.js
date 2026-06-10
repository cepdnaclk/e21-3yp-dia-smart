const mqtt = require('mqtt');
const fs = require('fs');

// =========================
// AWS IoT Configuration
// =========================

const client = mqtt.connect({
    host: 'a36biie00zvpfg-ats.iot.eu-north-1.amazonaws.com',
    port: 8883,
    protocol: 'mqtts',

    key: fs.readFileSync('./certs/private.pem.key'),
    cert: fs.readFileSync('./certs/certificate.pem.crt'),
    ca: fs.readFileSync('./certs/AmazonRootCA1.pem'),

    clientId: 'diasmart-simulator',

    rejectUnauthorized: true
});

// =========================
// MQTT Topic
// =========================

const topic = 'diasmart/device/telemetry';

// =========================
// Connected
// =========================

client.on('connect', () => {
    console.log('Connected to AWS IoT Core');

    const sequenceNumber = Date.now();

    const payload = {
        eventId: `EVT-${Date.now()}`,
        eventType: 'COMBINED_TELEMETRY',
        timestamp: new Date().toISOString(),
        schemaVersion: 1,
        sequenceNumber,
        replayedEvent: false,

        patient: {
            patientId: 1
        },

        gateway: {
            deviceUid: 'DS-OUTER-0001',
            firmwareVersion: 'v1.0.0'
        },

        storage: {
            temperatureC: 16.0,
            doorStatus: 'CLOSED',
        },
    //     storage: {
    //     temperatureC: 30,
    //     doorStatus: 'CLOSED',
    //     temperatureStatus: 'HIGH'
    // },

        glucose: {
            valueMgDl: 150,
            source: 'BLE_GLUCOMETER'
        },

        dose: {
            insulinDoseUnits: 18,
            detectionMethod: 'AS5600'
        },

        inventory: {
            weightG: 550.0
        },

        battery: {
            innerUnitPercent: 19,
            penUnitPercent: 90,
            outerUnitPercent: 39
        }
    };

    client.publish(
        topic,
        JSON.stringify(payload),
        { qos: 1 },
        (err) => {
            if (err) {
                console.log('Publish error');
                console.log(err);
            } else {
                console.log('Payload sent');
                console.log(payload);
            }

            client.end();
        }
    );
});

// =========================
// Error Handling
// =========================

client.on('error', (error) => {
    console.log('MQTT error');
    console.log(error);
});
