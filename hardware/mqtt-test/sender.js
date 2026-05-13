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

    console.log('✅ Connected to AWS IoT Core');

    let packetCount = 1;

    // Send packet every 3 seconds
    setInterval(() => {

        const payload = {

            eventId: `EVT-${Date.now()}`,

            eventType: "COMBINED_TELEMETRY",

            trigger: "DOSE_EVENT",

            timestamp: new Date().toISOString(),

            schemaVersion: 1,

            sequenceNumber: packetCount,

            replayedEvent: false,

            patient: {
                patientId: "1"
            },

            gateway: {
                deviceUid: "DS-OUTER-0001",
                firmwareVersion: "v1.0.0"
            },

            storage: {
                temperatureC: parseFloat((Math.random() * 3 + 4).toFixed(1)),
                doorStatus: "CLOSED"
            },

            glucose: {
                valueMgDl: Math.floor(Math.random() * 40 + 100),
                source: "BLE_GLUCOMETER"
            },

            dose: {
                insulinDoseUnits: Math.floor(Math.random() * 10 + 5),
                detectionMethod: "AS5600"
            },

            inventory: {
                weightG: parseFloat((Math.random() * 10 + 40).toFixed(1))
            },

            battery: {
                innerUnitPercent: Math.floor(Math.random() * 20 + 80),
                penUnitPercent: Math.floor(Math.random() * 20 + 70),
                outerUnitPercent: Math.floor(Math.random() * 10 + 90)
            }
        };

        client.publish(
            topic,
            JSON.stringify(payload),
            { qos: 1 },
            (err) => {

                if (err) {

                    console.log('❌ Publish Error');
                    console.log(err);

                } else {

                    console.log(`📤 Packet ${packetCount} Sent`);
                    console.log(payload);
                }
            }
        );

        packetCount++;

    }, 3000);
});

// =========================
// Error Handling
// =========================

client.on('error', (error) => {

    console.log('❌ MQTT Error');
    console.log(error);
});

// =========================
// Reconnect
// =========================

client.on('reconnect', () => {

    console.log('🔄 Reconnecting...');
});

client.on('offline', () => {

    console.log('⚠️ MQTT Offline');
});