# Dia-Smart Sample Backend

Express backend used by the dashboard, glucometer sync, sensor ingestion, and BLE dosage logging.

## Features
- Sensor + glucometer ingestion and history APIs
- Latest summary API for dashboard cards
- Dosage timeline API
- File-backed raw event storage under `data/` for repo-shared snapshots

## Prerequisites
- Node.js 18+
- npm
- PostgreSQL (optional but recommended)

## Setup
1. Open terminal in:
   `backend/sample_backend`
2. Install dependencies:
   ```bash
   npm install
   ```
3. Create local environment file:
   ```bash
   copy .env.example .env
   ```
4. Edit `.env`:
   - `PGUSER`, `PGHOST`, `PGDATABASE`, `PGPASSWORD`, `PGPORT`
   - Optional:
     - `GLUCO_FILE_ONLY=true` to force file-backed summary/history
       - MQTT bridge:
          - `MQTT_ENABLED=true`
          - `MQTT_BROKER_URL=mqtt://localhost:1883`
          - `MQTT_USERNAME`, `MQTT_PASSWORD` (if broker requires auth)
          - `MQTT_CLIENT_ID` (optional)
          - `MQTT_TOPIC_PREFIX=diasmart`

Preferred setup from project root:
```bash
node config/apply-config.js
```

## Run
```bash
npm start
```

Backend runs on:
- `http://localhost:3000`

## Main Endpoints
- `GET /api/ping`
- `GET /api/latest-summary`
- `GET /api/history`
- `POST /api/readings`
- `POST /api/glucometer`
- `POST /api/glucometer/batch`
- `GET /api/dosage`
- `POST /api/dosage`
- `POST /api/replay/raw-to-db`
- `GET /api/mqtt/status`

## MQTT Bridge (Optional)
When enabled, backend subscribes to ingest topics and forwards payloads to existing HTTP handlers.

Ingest topics:
- `<prefix>/ingest/readings` -> `/api/readings`
- `<prefix>/ingest/glucometer` -> `/api/glucometer`
- `<prefix>/ingest/glucometer/batch` -> `/api/glucometer/batch`
- `<prefix>/ingest/dosage` -> `/api/dosage`

Event topics published by backend:
- `<prefix>/events/readings`
- `<prefix>/events/glucometer`
- `<prefix>/events/glucometer/batch`
- `<prefix>/events/dosage`

`<prefix>` is `MQTT_TOPIC_PREFIX`.

## Data Folder Notes
- `data/glucometer_raw.jsonl`
- `data/sensor_raw.jsonl`
- `data/dosage_raw.jsonl`

These JSONL files are append-only local history and can be committed so teammates get sample timeline data after pull.
