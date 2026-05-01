# Dia-Smart Local Setup Guide

This guide is for teammates who pull this repo and want to run the integrated backend, PostgreSQL database, shared data history, and mobile dashboard.

## 0. Central Configuration

Edit one file only:

- `config/diasmart.config.js`

This one file contains all common machine-specific values:
- Wi-Fi SSID/password
- Backend IP/port
- PostgreSQL host/port/database/user/password
- MQTT enable/broker/credentials/topic prefix
- Runtime options (like Python binary for dose capture)

Then generate all downstream config files:

```bash
node config/apply-config.js
```

This updates:
- `backend/sample_backend/.env`
- `config/firmware_config.h`
- `frontend/rn-app/src/config/appConfig.ts`

## 1. Backend API
Directory: `backend/sample_backend`

1. Install dependencies:
   ```bash
   npm install
   ```
2. Start backend:
   ```bash
   npm start
   ```

Reference: [backend/sample_backend/README.md](backend/sample_backend/README.md)

## 2. Database Setup

```bash
psql -U postgres -f database/diasmart_schema.sql
```

## 3. Replay Shared Git Data Into SQL

After backend startup, load the repo-tracked history into PostgreSQL:

```bash
curl -X POST http://localhost:3000/api/replay/raw-to-db
```

## 4. Frontend Dashboard
Directory: `frontend/rn-app`

```bash
cd frontend/rn-app
npm install
npm run web
```

## 5. Dosage Video Detection Node
Directory: `code/dosage_detection/poc`

1. Create virtual environment and install:
   ```bash
   python -m venv .venv
   .venv\Scripts\activate
   pip install -r requirements.txt
   ```
2. Create env file:
   ```bash
   copy .env.example .env
   ```
3. Set `GEMINI_API_KEY` in `.env`.
4. Run:
   ```bash
   python dose_detection_video_poc.py
   ```

Reference: [code/dosage_detection/poc/README.md](code/dosage_detection/poc/README.md)

## Shared Local Data
JSONL files in `backend/sample_backend/data` keep timeline snapshots:
- `glucometer_raw.jsonl`
- `sensor_raw.jsonl`
- `dosage_raw.jsonl`

Commit these when you want teammates to receive the same shared history after `git pull`.

## Team JSON Collaboration Flow (Current Approach)

Use this exact sequence when collaborating:

1. Pull latest git changes.
2. Start backend (`npm start` in `backend/sample_backend`).
3. Rebuild local PostgreSQL from committed JSON history:
   ```bash
   npm run replay:json
   ```
4. Test sensors/glucometer normally (backend appends to JSONL files).
5. Check ingest counts:
   ```bash
   npm run status:ingest
   ```
6. Commit updated JSONL files and push.

This ensures each developer can pull, replay, and see the full shared history.
