# Dia-Smart Dosage Detection (Video PoC)

This module records a short insulin-pen video, sends it to Gemini for analysis, extracts the injected dose, and posts the result to the local backend.

## What It Does
- Captures webcam video manually (start/stop with keyboard).
- Uploads the video to Gemini (`gemini-2.5-flash`).
- Extracts a numeric result from `FINAL_DOSE_RESULT: [number]`.
- Sends the dose to backend: `POST /api/dosage`.

## Prerequisites
- Python 3.10+ (recommended)
- Webcam connected
- Google Gemini API key
- Backend server running (`backend/sample_backend/server.js`)

## Setup
1. Open terminal in this folder:
   `code/dosage_detection/poc`
2. Create virtual environment and install dependencies:
   ```bash
   python -m venv .venv
   .venv\Scripts\activate
   pip install -r requirements.txt
   ```
3. Create local env file:
   ```bash
   copy .env.example .env
   ```
4. Edit `.env`:
   - `GEMINI_API_KEY`: your API key
   - `BACKEND_DOSAGE_URL`: usually `http://localhost:3000/api/dosage`

## Run
```bash
python dose_detection_video_poc.py
```

Controls:
- `Space`: start/stop recording
- `q`: quit

## Expected Backend Payload
This script posts JSON to backend:
```json
{
  "type": "insulin_dose",
  "value": 15,
  "timestamp": "2026-03-11T10:11:12Z"
}
```

## Troubleshooting
- `Failed to connect to backend`: ensure backend is running on port `3000` (or change `BACKEND_DOSAGE_URL`).
- `API Error` from Gemini: validate `GEMINI_API_KEY` and internet access.
- No webcam frame: verify camera permissions / close other apps using the camera.

## Files
- `dose_detection_video_poc.py`: main capture + AI + sync flow
- `requirements.txt`: Python dependencies
- `.env.example`: safe environment template
- `.gitignore`: ignores local secrets and temp recording output
