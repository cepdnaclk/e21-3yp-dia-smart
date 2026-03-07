# Dia-Smart Frontend (Phase 1)

This is the first web app dashboard for glucometer infographics.

## Current features

- Load glucose records from `sample-data.json`
- Upload your own JSON export
- KPI summary cards (average, latest, total, in-range %)
- Trend chart over time
- Risk zone distribution bars
- Daily average bar chart
- Recent readings table

## Expected JSON format

```json
[
  {
    "device": "Accu-Chek Guide Me",
    "record_id": 101,
    "datetime_sl": "2026-03-01 06:30:00",
    "glucose_mg_dl": 92,
    "glucose_mmol_L": 5.1
  }
]
```

## Run locally

Because this app loads JSON with `fetch`, use a local HTTP server:

```powershell
cd frontend
python -m http.server 5173
```

Then open `http://localhost:5173`.

## Next phase

- Connect live data source from backend or cloud
- Add filtering by date range and meal context
- Add alert rules for hypo/hyper events
