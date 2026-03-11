# Dia-Smart Sample Backend

Express backend used by the sample dashboard and dosage video PoC.

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
     - `DOSE_PYTHON_BIN=python` to select Python executable for capture trigger

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
- `POST /api/sensor`
- `POST /api/glucometer`
- `GET /api/dosage`
- `POST /api/dosage`
- `POST /api/dosage/capture` (starts Python script)

## Data Folder Notes
- `data/glucometer_raw.jsonl`
- `data/sensor_raw.jsonl`
- `data/dosage_raw.jsonl`

These JSONL files are append-only local history and can be committed so teammates get sample timeline data after pull.
