# Dia-Smart Local Setup Guide

This guide is for teammates who pull this repo and want to run the integrated sample dashboard + backend + dosage video node.

## 1. Backend API
Directory: `backend/sample_backend`

1. Install dependencies:
   ```bash
   npm install
   ```
2. Create env file:
   ```bash
   copy .env.example .env
   ```
3. Update database credentials in `.env`.
4. Start backend:
   ```bash
   npm start
   ```

Reference: [backend/sample_backend/README.md](backend/sample_backend/README.md)

## 2. Frontend Dashboard
Directory: `frontend/sample_dashboard`

Open `index.html` in browser (or serve the folder with any static server).
Dashboard calls backend at `http://localhost:3000`.

## 3. Dosage Video Detection Node
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

These can be committed when you intentionally want teammates to receive sample historical data with `git pull`.
