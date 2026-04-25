const express = require('express');
const cors = require('cors');
const fs = require('fs/promises');
const path = require('path');
const mqtt = require('mqtt');
const pool = require('./db');

const app = express();
const CORS_ORIGIN = String(process.env.CORS_ORIGIN || '*').trim();
app.use(cors(CORS_ORIGIN === '*' ? {} : { origin: CORS_ORIGIN }));
app.use(express.json());

const DATA_DIR = path.join(__dirname, 'data');
const GLUCO_RAW_FILE = path.join(DATA_DIR, 'glucometer_raw.jsonl');
const SENSOR_RAW_FILE = path.join(DATA_DIR, 'sensor_raw.jsonl');
const DOSAGE_RAW_FILE = path.join(DATA_DIR, 'dosage_raw.jsonl');
const HTTP_PORT = Number(process.env.PORT || 3000);
const FORCE_FILE_ONLY = ['1', 'true', 'yes', 'on'].includes(
  String(process.env.GLUCO_FILE_ONLY || '').toLowerCase()
);
const MQTT_ENABLED = ['1', 'true', 'yes', 'on'].includes(
  String(process.env.MQTT_ENABLED || '').toLowerCase()
);
const MQTT_BROKER_URL = String(process.env.MQTT_BROKER_URL || '').trim();
const MQTT_TOPIC_PREFIX = String(process.env.MQTT_TOPIC_PREFIX || 'diasmart').trim().replace(/\/+$/, '');

const MQTT_TOPICS = {
  ingestReadings: `${MQTT_TOPIC_PREFIX}/ingest/readings`,
  ingestGlucometer: `${MQTT_TOPIC_PREFIX}/ingest/glucometer`,
  ingestGlucometerBatch: `${MQTT_TOPIC_PREFIX}/ingest/glucometer/batch`,
  ingestDosage: `${MQTT_TOPIC_PREFIX}/ingest/dosage`,
  eventsReadings: `${MQTT_TOPIC_PREFIX}/events/readings`,
  eventsGlucometer: `${MQTT_TOPIC_PREFIX}/events/glucometer`,
  eventsGlucometerBatch: `${MQTT_TOPIC_PREFIX}/events/glucometer/batch`,
  eventsDosage: `${MQTT_TOPIC_PREFIX}/events/dosage`
};

const mqttState = {
  enabled: MQTT_ENABLED,
  broker: MQTT_BROKER_URL || null,
  connected: false,
  lastError: null,
  lastMessageAt: null,
  subscriptions: []
};

let mqttClient = null;

app.get('/api/ping', (_req, res) => {
  res.status(200).json({ ok: true, service: 'samble_backend' });
});

function toNullableNumber(value) {
  if (value === null || value === undefined || value === "") {
    return null;
  }

  const parsed = Number(value);
  return Number.isNaN(parsed) ? null : parsed;
}

function parseSriLankaDateTime(datetimeSl) {
  if (!datetimeSl || typeof datetimeSl !== "string") {
    return null;
  }

  // Expected input format from ESP: YYYY-MM-DD HH:MM:SS (Asia/Colombo local time)
  const candidate = datetimeSl.trim().replace(" ", "T");
  const parsed = new Date(`${candidate}+05:30`);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

function isTruthyFlag(value) {
  return value === true || value === 1 || value === '1' || value === 'true';
}

function buildMqttConnectionOptions() {
  const username = String(process.env.MQTT_USERNAME || '').trim();
  const password = String(process.env.MQTT_PASSWORD || '').trim();
  const clientId = String(process.env.MQTT_CLIENT_ID || `diasmart-backend-${Math.random().toString(16).slice(2, 10)}`).trim();

  const options = {
    clientId,
    reconnectPeriod: 5000,
    connectTimeout: 15000
  };

  if (username) {
    options.username = username;
  }

  if (password) {
    options.password = password;
  }

  return options;
}

async function forwardMqttIngestToHttp(topic, payload) {
  const routeByTopic = {
    [MQTT_TOPICS.ingestReadings]: '/api/readings',
    [MQTT_TOPICS.ingestGlucometer]: '/api/glucometer',
    [MQTT_TOPICS.ingestGlucometerBatch]: '/api/glucometer/batch',
    [MQTT_TOPICS.ingestDosage]: '/api/dosage'
  };

  const route = routeByTopic[topic];
  if (!route) {
    return;
  }

  const response = await fetch(`http://127.0.0.1:${HTTP_PORT}${route}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ...(payload || {}), mqtt_ingest: true })
  });

  if (!response.ok) {
    const text = await response.text();
    console.warn(`[MQTT] Ingest forward failed for ${route}. status=${response.status} body=${text}`);
  }
}

function safePublishMqtt(topic, payload) {
  if (!mqttClient || !mqttState.connected || !topic) {
    return;
  }

  const event = {
    ...(payload || {}),
    published_at: new Date().toISOString(),
    source: 'backend'
  };

  mqttClient.publish(topic, JSON.stringify(event), { qos: 1, retain: false }, (err) => {
    if (err) {
      console.warn(`[MQTT] Publish failed for topic=${topic}`, err.message || err);
    }
  });
}

function startMqttBridge() {
  if (!MQTT_ENABLED) {
    console.log('[MQTT] Disabled (MQTT_ENABLED is false)');
    return;
  }

  if (!MQTT_BROKER_URL) {
    console.warn('[MQTT] MQTT_ENABLED=true but MQTT_BROKER_URL is empty. Bridge not started.');
    mqttState.lastError = 'MQTT_BROKER_URL is empty';
    return;
  }

  mqttClient = mqtt.connect(MQTT_BROKER_URL, buildMqttConnectionOptions());

  mqttClient.on('connect', () => {
    mqttState.connected = true;
    mqttState.lastError = null;

    const ingestTopics = [
      MQTT_TOPICS.ingestReadings,
      MQTT_TOPICS.ingestGlucometer,
      MQTT_TOPICS.ingestGlucometerBatch,
      MQTT_TOPICS.ingestDosage
    ];

    mqttClient.subscribe(ingestTopics, { qos: 1 }, (err) => {
      if (err) {
        mqttState.lastError = err.message || 'subscribe failed';
        console.warn('[MQTT] Subscribe failed', err);
        return;
      }

      mqttState.subscriptions = ingestTopics;
      console.log(`[MQTT] Connected and subscribed to ${ingestTopics.length} ingest topics`);
    });
  });

  mqttClient.on('reconnect', () => {
    mqttState.connected = false;
    console.log('[MQTT] Reconnecting...');
  });

  mqttClient.on('offline', () => {
    mqttState.connected = false;
    console.log('[MQTT] Offline');
  });

  mqttClient.on('error', (err) => {
    mqttState.lastError = err && err.message ? err.message : 'unknown mqtt error';
    console.warn('[MQTT] Client error', err);
  });

  mqttClient.on('message', async (topic, buffer) => {
    mqttState.lastMessageAt = new Date().toISOString();

    let payload;
    try {
      payload = JSON.parse(buffer.toString('utf8'));
    } catch (_err) {
      console.warn(`[MQTT] Dropped non-JSON payload from ${topic}`);
      return;
    }

    try {
      await forwardMqttIngestToHttp(topic, payload);
    } catch (err) {
      console.warn(`[MQTT] Forward to HTTP failed for topic=${topic}`, err.message || err);
    }
  });
}

async function persistRawGlucometerEvent(eventPayload) {
  await fs.mkdir(DATA_DIR, { recursive: true });
  await fs.appendFile(GLUCO_RAW_FILE, `${JSON.stringify(eventPayload)}\n`, 'utf8');
}

async function readJsonLines(filePath) {
  try {
    const content = await fs.readFile(filePath, 'utf8');
    return content
      .split('\n')
      .filter(Boolean)
      .map((line) => {
        try {
          return JSON.parse(line);
        } catch (_err) {
          return null;
        }
      })
      .filter(Boolean)
      .sort((a, b) => new Date(a.received_at) - new Date(b.received_at));
  } catch (err) {
    if (err && err.code === 'ENOENT') {
      return [];
    }
    throw err;
  }
}

async function readRawGlucometerEvents() {
  return readJsonLines(GLUCO_RAW_FILE);
}

async function persistRawSensorEvent(eventPayload) {
  await fs.mkdir(DATA_DIR, { recursive: true });
  await fs.appendFile(SENSOR_RAW_FILE, `${JSON.stringify(eventPayload)}\n`, 'utf8');
}

async function readRawSensorEvents() {
  return readJsonLines(SENSOR_RAW_FILE);
}

async function persistRawDosageEvent(eventPayload) {
  await fs.mkdir(DATA_DIR, { recursive: true });
  await fs.appendFile(DOSAGE_RAW_FILE, `${JSON.stringify(eventPayload)}\n`, 'utf8');
}

async function readRawDosageEvents() {
  return readJsonLines(DOSAGE_RAW_FILE);
}

function chooseLatestNonNull(events, key) {
  for (let i = events.length - 1; i >= 0; i -= 1) {
    const value = events[i] && events[i][key];
    if (value !== null && value !== undefined && value !== '') {
      return { value, at: events[i].received_at || null };
    }
  }
  return { value: null, at: null };
}

async function buildSummaryFromFiles() {
  const sensorEvents = await readRawSensorEvents();
  const glucoEvents = await readRawGlucometerEvents();
  const allEvents = sensorEvents.concat(glucoEvents).sort((a, b) => new Date(a.received_at) - new Date(b.received_at));

  if (allEvents.length === 0) {
    return {};
  }

  const latestTemp = chooseLatestNonNull(sensorEvents, 'temperature');
  const latestDoor = chooseLatestNonNull(sensorEvents, 'door_status');
  const latestWeight = chooseLatestNonNull(sensorEvents, 'insulin_inventory_weight');
  const latestInsulin = chooseLatestNonNull(sensorEvents, 'insulin_level_value');

  const latestGlucoEvent = glucoEvents.length ? glucoEvents[glucoEvents.length - 1] : null;
  const measuredAt = latestGlucoEvent
    ? (parseSriLankaDateTime(latestGlucoEvent.datetime_sl) ||
      (latestGlucoEvent.timestamp ? new Date(latestGlucoEvent.timestamp) : null) ||
      new Date(latestGlucoEvent.received_at))
    : null;

  const latestAll = allEvents[allEvents.length - 1];

  return {
    temperature: toNullableNumber(latestTemp.value),
    door_status: latestDoor.value,
    insulin_inventory_weight: toNullableNumber(latestWeight.value),
    insulin_level_value: toNullableNumber(latestInsulin.value),
    glucose_value: latestGlucoEvent ? toNullableNumber(latestGlucoEvent.glucose_mg_dl) : null,
    glucose_synced_at: measuredAt,
    latest_event_at: new Date(latestAll.received_at),
    source: 'file-fallback'
  };
}

async function buildHistoryFromFiles(limit) {
  const sensorEvents = await readRawSensorEvents();
  const glucoEvents = await readRawGlucometerEvents();

  const sensorRows = sensorEvents.map((event) => ({
    created_at: new Date(event.received_at),
    temperature: toNullableNumber(event.temperature),
    glucose_value: toNullableNumber(event.glucose_value),
    insulin_inventory_weight: toNullableNumber(event.insulin_inventory_weight)
  }));

  const glucoRows = glucoEvents.map((event) => {
    const measuredAt =
      parseSriLankaDateTime(event.datetime_sl) ||
      (event.timestamp ? new Date(event.timestamp) : null) ||
      new Date(event.received_at);

    return {
      created_at: measuredAt,
      temperature: null,
      glucose_value: toNullableNumber(event.glucose_mg_dl),
      insulin_inventory_weight: null
    };
  });

  return sensorRows.concat(glucoRows)
    .sort((a, b) => new Date(a.created_at) - new Date(b.created_at))
    .slice(-limit);
}

async function insertGlucometerRecordToDbOrSkip({ glucoseValue, measuredAtValid }) {
  try {
    if (measuredAtValid) {
      const result = await pool.query(
        `INSERT INTO readings (glucose_value, created_at)
         VALUES ($1, $2)
         RETURNING id, glucose_value, created_at`,
        [glucoseValue, measuredAtValid]
      );
      return { saved: true, row: result.rows[0], errorCode: null };
    }

    const result = await pool.query(
      `INSERT INTO readings (glucose_value)
       VALUES ($1)
       RETURNING id, glucose_value, created_at`,
      [glucoseValue]
    );
    return { saved: true, row: result.rows[0], errorCode: null };
  } catch (err) {
    console.error(err);
    return { saved: false, row: null, errorCode: err && err.code ? err.code : null };
  }
}

/* ===============================
   POST - Insert Sensor Data
================================= */
app.post('/api/readings', async (req, res) => {

  const {
    temperature,
    door_status,
    insulin_inventory_weight,
    insulin_level_value,
    glucose_value
  } = req.body;

  const rawEvent = {
    temperature: toNullableNumber(temperature),
    door_status: door_status || null,
    insulin_inventory_weight: toNullableNumber(insulin_inventory_weight),
    insulin_level_value: toNullableNumber(insulin_level_value),
    glucose_value: toNullableNumber(glucose_value),
    received_at: new Date().toISOString()
  };

  try {
    await persistRawSensorEvent(rawEvent);
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: 'Could not persist sensor payload' });
  }

  safePublishMqtt(MQTT_TOPICS.eventsReadings, rawEvent);

  if (FORCE_FILE_ONLY || isTruthyFlag(req.body && req.body.skip_db)) {
    return res.status(202).json({
      message: 'Sensor payload stored to file (DB intentionally skipped)',
      stored_in_file: true,
      db_saved: false,
      db_skipped: true
    });
  }

  try {
    await pool.query(
      `INSERT INTO readings 
      (temperature, door_status, insulin_inventory_weight, insulin_level_value, glucose_value)
      VALUES ($1, $2, $3, $4, $5)`,
      [temperature, door_status, insulin_inventory_weight, insulin_level_value, glucose_value]
    );

    res.status(200).json({ message: "Data inserted successfully", stored_in_file: true });

  } catch (err) {
    console.error(err);
    res.status(202).json({
      message: 'Sensor payload stored to file; database insert failed',
      stored_in_file: true,
      db_saved: false,
      db_error_code: err && err.code ? err.code : null
    });
  }
});


/* ===============================
   GET - All Readings
================================= */
app.get('/api/readings', async (req, res) => {

  const result = await pool.query(
    `SELECT * FROM readings ORDER BY created_at DESC LIMIT 100`
  );

  res.json(result.rows);
});

// Get latest reading
app.get('/api/latest', async (req, res) => {
  const result = await pool.query(
    `SELECT * FROM readings
     ORDER BY created_at DESC
     LIMIT 1`
  );
  res.json(result.rows[0]);
});

// Get latest non-null value for each signal (safe for mixed sensor update rates)
app.get('/api/latest-summary', async (req, res) => {
  if (String(req.query.source || '').toLowerCase() === 'file') {
    try {
      return res.json(await buildSummaryFromFiles());
    } catch (err) {
      console.error(err);
      return res.status(500).json({ error: 'File summary build failed' });
    }
  }

  try {
    const result = await pool.query(
      `SELECT
         (SELECT temperature FROM readings WHERE temperature IS NOT NULL ORDER BY created_at DESC LIMIT 1) AS temperature,
         (SELECT door_status FROM readings WHERE door_status IS NOT NULL ORDER BY created_at DESC LIMIT 1) AS door_status,
         (SELECT insulin_inventory_weight FROM readings WHERE insulin_inventory_weight IS NOT NULL ORDER BY created_at DESC LIMIT 1) AS insulin_inventory_weight,
         (SELECT insulin_level_value FROM readings WHERE insulin_level_value IS NOT NULL ORDER BY created_at DESC LIMIT 1) AS insulin_level_value,
         (SELECT glucose_value FROM readings WHERE glucose_value IS NOT NULL ORDER BY created_at DESC LIMIT 1) AS glucose_value,
         (SELECT created_at FROM readings WHERE glucose_value IS NOT NULL ORDER BY created_at DESC LIMIT 1) AS glucose_synced_at,
         (SELECT created_at FROM readings ORDER BY created_at DESC LIMIT 1) AS latest_event_at`
    );

    res.json(result.rows[0] || {});
  } catch (err) {
    console.error(err);
    try {
      return res.json(await buildSummaryFromFiles());
    } catch (fallbackErr) {
      console.error(fallbackErr);
      if (err && err.code === '28P01') {
        return res.status(500).json({
          error: 'Database authentication failed. Check PGUSER/PGPASSWORD/PGDATABASE/PGHOST/PGPORT.'
        });
      }
      return res.status(500).json({ error: 'Database error' });
    }
  }
});

// Get readings for graph
app.get('/api/history', async (req, res) => {
  const limitRaw = req.query.limit;
  const parsedLimit = Number(limitRaw);
  const limit = !limitRaw || Number.isNaN(parsedLimit) || parsedLimit <= 0
    ? 1000
    : Math.min(parsedLimit, 5000);

  if (String(req.query.source || '').toLowerCase() === 'file') {
    try {
      return res.json(await buildHistoryFromFiles(limit));
    } catch (err) {
      console.error(err);
      return res.status(500).json({ error: 'File history build failed' });
    }
  }

  try {
    const result = await pool.query(
      `SELECT created_at, temperature, glucose_value,
              insulin_inventory_weight
       FROM readings
       ORDER BY created_at ASC
       LIMIT $1`,
      [limit]
    );
    res.json(result.rows);
  } catch (err) {
    console.error(err);
    try {
      res.json(await buildHistoryFromFiles(limit));
    } catch (fallbackErr) {
      console.error(fallbackErr);
      res.status(500).json({ error: 'Database error' });
    }
  }
});


/* ===============================
   POST - Insert Glucometer Data From ESP Wi-Fi
================================= */
app.post('/api/glucometer', async (req, res) => {
  const {
    device,
    record_id,
    glucose_mg_dl,
    glucose_mmol_L,
    datetime_sl,
    timestamp,
    skip_db
  } = req.body;

  const glucoseValue = toNullableNumber(glucose_mg_dl);
  if (glucoseValue === null) {
    return res.status(400).json({
      error: 'Invalid payload. glucose_mg_dl is required and must be numeric.'
    });
  }

  // Prefer datetime from ESP payload, fall back to ISO timestamp, else DB default NOW().
  const measuredAt =
    parseSriLankaDateTime(datetime_sl) ||
    (timestamp ? new Date(timestamp) : null);
  const measuredAtValid =
    measuredAt && !Number.isNaN(measuredAt.getTime()) ? measuredAt : null;

  const rawEvent = {
    device: device || 'unknown',
    record_id: record_id ?? null,
    datetime_sl: datetime_sl || null,
    glucose_mg_dl: glucoseValue,
    glucose_mmol_L: toNullableNumber(glucose_mmol_L),
    timestamp: timestamp || null,
    received_at: new Date().toISOString()
  };

  try {
    await persistRawGlucometerEvent(rawEvent);
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: 'Could not persist glucometer payload' });
  }

  safePublishMqtt(MQTT_TOPICS.eventsGlucometer, rawEvent);

  try {
    if (FORCE_FILE_ONLY || isTruthyFlag(skip_db)) {
      return res.status(202).json({
        message: 'Glucometer payload stored to file (DB intentionally skipped)',
        stored_in_file: true,
        db_saved: false,
        db_skipped: true
      });
    }

    const dbWrite = await insertGlucometerRecordToDbOrSkip({
      glucoseValue,
      measuredAtValid
    });

    if (!dbWrite.saved) {
      return res.status(202).json({
        message: 'Glucometer payload stored to file; database insert failed',
        stored_in_file: true,
        db_saved: false,
        db_error_code: dbWrite.errorCode
      });
    }

    res.status(201).json({
      message: 'Glucometer data inserted successfully',
      reading: dbWrite.row,
      stored_in_file: true,
      source: {
        device: device || 'unknown',
        record_id: record_id ?? null,
        glucose_mmol_L: toNullableNumber(glucose_mmol_L)
      }
    });
  } catch (err) {
    console.error(err);
    res.status(202).json({
      message: 'Glucometer payload stored to file; database insert failed',
      stored_in_file: true,
      db_saved: false,
      db_error_code: err && err.code ? err.code : null
    });
  }
});

/* ===============================
   POST - Insert Glucometer Batch From ESP Wi-Fi
================================= */
app.post('/api/glucometer/batch', async (req, res) => {
  const { device, sync_id, records, skip_db } = req.body || {};
  const skipDbWrites = FORCE_FILE_ONLY || isTruthyFlag(skip_db);

  if (!Array.isArray(records) || records.length === 0) {
    return res.status(400).json({ error: 'Invalid payload. records[] is required.' });
  }

  let fileSavedCount = 0;
  let dbSavedCount = 0;
  let skippedCount = 0;
  const dbErrorCodes = new Set();

  for (const record of records) {
    const glucoseValue = toNullableNumber(record?.glucose_mg_dl);
    if (glucoseValue === null) {
      skippedCount += 1;
      continue;
    }

    const measuredAt =
      parseSriLankaDateTime(record?.datetime_sl) ||
      (record?.timestamp ? new Date(record.timestamp) : null);
    const measuredAtValid = measuredAt && !Number.isNaN(measuredAt.getTime()) ? measuredAt : null;

    const rawEvent = {
      device: record?.device || device || 'unknown',
      record_id: record?.record_id ?? null,
      datetime_sl: record?.datetime_sl || null,
      glucose_mg_dl: glucoseValue,
      glucose_mmol_L: toNullableNumber(record?.glucose_mmol_L),
      timestamp: record?.timestamp || null,
      sync_id: sync_id || null,
      received_at: new Date().toISOString()
    };

    try {
      await persistRawGlucometerEvent(rawEvent);
      fileSavedCount += 1;
    } catch (err) {
      console.error(err);
      continue;
    }

    if (!skipDbWrites) {
      const dbWrite = await insertGlucometerRecordToDbOrSkip({ glucoseValue, measuredAtValid });
      if (dbWrite.saved) {
        dbSavedCount += 1;
      } else if (dbWrite.errorCode) {
        dbErrorCodes.add(dbWrite.errorCode);
      }
    }
  }

  const dbSaved = skipDbWrites ? false : (dbSavedCount === fileSavedCount && fileSavedCount > 0);

  safePublishMqtt(MQTT_TOPICS.eventsGlucometerBatch, {
    device: device || 'unknown',
    sync_id: sync_id || null,
    records_received: records.length,
    records_saved_to_file: fileSavedCount,
    records_saved_to_db: dbSavedCount,
    records_skipped: skippedCount,
    db_saved: dbSaved,
    db_skipped: skipDbWrites,
    db_error_codes: Array.from(dbErrorCodes)
  });

  res.status(dbSaved ? 201 : 202).json({
    message: dbSaved
      ? 'Batch saved to file and database'
      : 'Batch saved to file; database saved partially or failed',
    records_received: records.length,
    records_saved_to_file: fileSavedCount,
    records_saved_to_db: dbSavedCount,
    records_skipped: skippedCount,
    db_saved: dbSaved,
    db_skipped: skipDbWrites,
    db_error_codes: Array.from(dbErrorCodes)
  });
});

app.get('/api/glucometer/raw', async (_req, res) => {
  try {
    const events = await readRawGlucometerEvents();
    const limitRaw = _req.query.limit;
    const limit = limitRaw === undefined ? null : Number(limitRaw);

    if (limit === null || Number.isNaN(limit) || limit <= 0) {
      return res.json(events);
    }

    res.json(events.slice(-limit));
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Could not read raw glucometer data' });
  }
});

app.get('/api/readings/raw', async (_req, res) => {
  try {
    const events = await readRawSensorEvents();
    const limitRaw = _req.query.limit;
    const limit = limitRaw === undefined ? null : Number(limitRaw);

    if (limit === null || Number.isNaN(limit) || limit <= 0) {
      return res.json(events);
    }

    res.json(events.slice(-limit));
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Could not read raw sensor data' });
  }
});

app.post('/api/replay/raw-to-db', async (_req, res) => {
  let sensorInserted = 0;
  let glucoInserted = 0;
  let dosageInserted = 0;
  let sensorSkipped = 0;
  let glucoSkipped = 0;
  let dosageSkipped = 0;
  const errors = [];

  try {
    const sensorEvents = await readRawSensorEvents();
    for (const e of sensorEvents) {
      try {
        const insertResult = await pool.query(
          `INSERT INTO readings (temperature, door_status, insulin_inventory_weight, insulin_level_value, glucose_value, created_at)
           SELECT $1, $2, $3, $4, $5, $6
           WHERE NOT EXISTS (
             SELECT 1 FROM readings
             WHERE created_at = $6
               AND temperature IS NOT DISTINCT FROM $1
               AND door_status IS NOT DISTINCT FROM $2
               AND insulin_inventory_weight IS NOT DISTINCT FROM $3
               AND insulin_level_value IS NOT DISTINCT FROM $4
               AND glucose_value IS NOT DISTINCT FROM $5
           )`,
          [
            toNullableNumber(e.temperature),
            e.door_status || null,
            toNullableNumber(e.insulin_inventory_weight),
            toNullableNumber(e.insulin_level_value),
            toNullableNumber(e.glucose_value),
            e.received_at ? new Date(e.received_at) : new Date()
          ]
        );
        if (insertResult.rowCount > 0) {
          sensorInserted += 1;
        } else {
          sensorSkipped += 1;
        }
      } catch (err) {
        errors.push({ type: 'sensor', code: err && err.code ? err.code : 'unknown' });
      }
    }

    const glucoEvents = await readRawGlucometerEvents();
    for (const e of glucoEvents) {
      try {
        const measuredAt =
          parseSriLankaDateTime(e.datetime_sl) ||
          (e.timestamp ? new Date(e.timestamp) : null) ||
          (e.received_at ? new Date(e.received_at) : new Date());

        const insertResult = await pool.query(
          `INSERT INTO readings (glucose_value, created_at)
           SELECT $1, $2
           WHERE NOT EXISTS (
             SELECT 1 FROM readings
             WHERE created_at = $2
               AND glucose_value IS NOT DISTINCT FROM $1
               AND temperature IS NULL
               AND door_status IS NULL
               AND insulin_inventory_weight IS NULL
               AND insulin_level_value IS NULL
           )`,
          [toNullableNumber(e.glucose_mg_dl), measuredAt]
        );
        if (insertResult.rowCount > 0) {
          glucoInserted += 1;
        } else {
          glucoSkipped += 1;
        }
      } catch (err) {
        errors.push({ type: 'gluco', code: err && err.code ? err.code : 'unknown' });
      }
    }

    const dosageEvents = await readRawDosageEvents();
    for (const e of dosageEvents) {
      try {
        const doseAmount = toNullableNumber(e.dose_amount);
        const injectionTime = e.injection_time
          ? new Date(e.injection_time)
          : (e.received_at ? new Date(e.received_at) : new Date());

        if (doseAmount === null) {
          dosageSkipped += 1;
          continue;
        }

        const insertResult = await pool.query(
          `INSERT INTO dosage_timeline (dose_amount, injection_time)
           SELECT $1, $2
           WHERE NOT EXISTS (
             SELECT 1 FROM dosage_timeline
             WHERE dose_amount = $1
               AND injection_time = $2
           )`,
          [Math.round(doseAmount), injectionTime]
        );

        if (insertResult.rowCount > 0) {
          dosageInserted += 1;
        } else {
          dosageSkipped += 1;
        }
      } catch (err) {
        errors.push({ type: 'dosage', code: err && err.code ? err.code : 'unknown' });
      }
    }

    res.json({
      message: 'Replay completed',
      sensor_inserted: sensorInserted,
      sensor_skipped_as_duplicate: sensorSkipped,
      gluco_inserted: glucoInserted,
      gluco_skipped_as_duplicate: glucoSkipped,
      dosage_inserted: dosageInserted,
      dosage_skipped_as_duplicate_or_invalid: dosageSkipped,
      errors_count: errors.length,
      errors_sample: errors.slice(0, 5)
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Replay failed' });
  }
});

app.get('/api/debug/ingest-status', async (_req, res) => {
  try {
    const sensors = await readRawSensorEvents();
    const gluco = await readRawGlucometerEvents();
    const dosage = await readRawDosageEvents();
    res.json({
      sensors_count: sensors.length,
      gluco_count: gluco.length,
      dosage_count: dosage.length,
      latest_sensor: sensors.length ? sensors[sensors.length - 1] : null,
      latest_gluco: gluco.length ? gluco[gluco.length - 1] : null,
      latest_dosage: dosage.length ? dosage[dosage.length - 1] : null,
      file_mode: FORCE_FILE_ONLY,
      mqtt: {
        enabled: mqttState.enabled,
        broker: mqttState.broker,
        connected: mqttState.connected,
        subscriptions: mqttState.subscriptions,
        last_message_at: mqttState.lastMessageAt,
        last_error: mqttState.lastError
      }
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Debug status failed' });
  }
});

/* ===============================
   POST - Insert BLE Dosage Data
================================= */
app.post('/api/dosage', async (req, res) => {
  // The BLE dosage node sends: { value: 15, timestamp: "..." }
  const { value, timestamp } = req.body;

  if (value === undefined || value === null) {
    return res.status(400).json({ error: 'Missing dose value' });
  }

  const doseValue = Number(value);

  if (!Number.isFinite(doseValue) || doseValue <= 0 || doseValue > 100) {
    return res.status(400).json({
      error: 'Invalid dose value. Dose must be a number between 1 and 100 units.'
    });
  }

  // Use the device-provided timestamp if available; otherwise use backend receive time
  const injectionTime = timestamp ? new Date(timestamp) : new Date();

  if (Number.isNaN(injectionTime.getTime())) {
    return res.status(400).json({ error: 'Invalid timestamp format' });
  }

  const payload = {
    dose_amount: doseValue,
    injection_time: injectionTime.toISOString(),
    received_at: new Date().toISOString()
  };

  try {
    const result = await pool.query(
      `INSERT INTO dosage_timeline (dose_amount, injection_time) 
       VALUES ($1, $2) RETURNING *`,
      [doseValue, injectionTime]
    );

    await persistRawDosageEvent({
      ...payload,
      ...(result.rows[0] || {})
    });

    safePublishMqtt(MQTT_TOPICS.eventsDosage, {
      ...payload,
      ...(result.rows[0] || {})
    });

    console.log(`[Dia-Smart Dosage] Successfully logged ${doseValue} Units at ${injectionTime.toLocaleTimeString()}`);

    res.status(201).json({
      message: "Dosage logged successfully",
      reading: result.rows[0]
    });

  } catch (err) {
    console.error("Database error saving dosage:", err);

    try {
      await persistRawDosageEvent(payload);

      safePublishMqtt(MQTT_TOPICS.eventsDosage, {
        ...payload,
        source: 'file-fallback'
      });

      res.status(201).json({
        message: "Dosage logged to file fallback",
        reading: payload,
        source: "file-fallback"
      });
    } catch (fileErr) {
      console.error("File fallback error saving dosage:", fileErr);

      res.status(500).json({
        error: 'Failed to save dosage to database and file fallback'
      });
    }
  }
});

/* ===============================
   GET - Dosage Timeline
================================= */
app.get('/api/dosage', async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT * FROM dosage_timeline ORDER BY injection_time DESC LIMIT 100`
    );
    if (Array.isArray(result.rows) && result.rows.length > 0) {
      return res.json(result.rows);
    }

    const fallbackRows = await readRawDosageEvents();
    const sorted = fallbackRows
      .slice()
      .sort((a, b) => new Date(b.injection_time || b.received_at) - new Date(a.injection_time || a.received_at))
      .slice(0, 100);
    return res.json(sorted);
  } catch (err) {
    console.error("Database error fetching dosage:", err);
    try {
      const fallbackRows = await readRawDosageEvents();
      const sorted = fallbackRows
        .slice()
        .sort((a, b) => new Date(b.injection_time || b.received_at) - new Date(a.injection_time || a.received_at))
        .slice(0, 100);
      res.json(sorted);
    } catch (fileErr) {
      console.error("File fallback error fetching dosage:", fileErr);
      res.status(500).json({ error: 'Failed to fetch dosage timeline' });
    }
  }
});

app.get('/api/mqtt/status', (_req, res) => {
  res.json({
    enabled: mqttState.enabled,
    broker: mqttState.broker,
    connected: mqttState.connected,
    subscriptions: mqttState.subscriptions,
    last_message_at: mqttState.lastMessageAt,
    last_error: mqttState.lastError,
    topics: MQTT_TOPICS
  });
});

app.listen(HTTP_PORT, () => {
  console.log(`Server running on port ${HTTP_PORT}`);
  startMqttBridge();
});
