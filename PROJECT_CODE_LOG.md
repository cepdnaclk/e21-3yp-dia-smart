# Project Code Log

Date: 2026-03-06
Project: Dia-Smart

## Summary Of Work Completed Today

1. Built authentication flow for the frontend prototype:
- Login page
- Create Account page
- Session persistence for logged-in user

2. Added hardcoded demo users for fast testing:
- Caregiver, Doctor, and Patient credentials are always available.
- Kept support for creating additional custom users.

3. Upgraded visual design to a cleaner premium medical style:
- White-first layout
- Soft blue accent system
- Improved forms, cards, buttons, and states

4. Converted UI behavior from long scrolling dashboard to app-style navigation:
- Implemented tabbed flow:
  - Home
  - Trends
  - Readings
  - Profile

5. Created a separate React Native + Web (Expo) app for editable mobile/web development:
- New app path: `frontend/rn-app`
- Split pages into separate files for easier team updates
- Added shared modules for theme, state, data, and reusable components

## Hardcoded Demo Credentials

- Caregiver: `caregiver@diasmart.com` / `Care1234`
- Doctor: `doctor@diasmart.com` / `Doctor1234`
- Patient: `patient@diasmart.com` / `Patient1234`

Note: These are prototype/testing credentials only.

## Key Files Added/Updated

### Static web prototype
- `frontend/index.html`
- `frontend/app.js`
- `frontend/styles.css`

### React Native + Web app
- `frontend/rn-app/app/_layout.tsx`
- `frontend/rn-app/app/index.tsx`
- `frontend/rn-app/app/login.tsx`
- `frontend/rn-app/app/signup.tsx`
- `frontend/rn-app/app/(tabs)/_layout.tsx`
- `frontend/rn-app/app/(tabs)/index.tsx`
- `frontend/rn-app/app/(tabs)/home.tsx`
- `frontend/rn-app/app/(tabs)/trends.tsx`
- `frontend/rn-app/app/(tabs)/readings.tsx`
- `frontend/rn-app/app/(tabs)/profile.tsx`
- `frontend/rn-app/src/state/authContext.tsx`
- `frontend/rn-app/src/data/mockData.ts`
- `frontend/rn-app/src/theme/colors.ts`
- `frontend/rn-app/src/components/AppScaffold.tsx`
- `frontend/rn-app/src/components/StatCard.tsx`
- `frontend/rn-app/src/components/Sparkline.tsx`
- `frontend/rn-app/package.json`
- `frontend/rn-app/app.json`
- `frontend/rn-app/babel.config.js`
- `frontend/rn-app/tsconfig.json`
- `frontend/rn-app/README.md`

## Local Run Guide

### Static frontend
```powershell
cd c:\Personal\3yp\e21-3yp-dia-smart\frontend
python -m http.server 5173
```
Open: `http://localhost:5173`

### React Native + Web app
```powershell
cd c:\Personal\3yp\e21-3yp-dia-smart\frontend\rn-app
npm install
npm run web
```
If there is a port conflict:
```powershell
cd c:\Personal\3yp\e21-3yp-dia-smart\frontend\rn-app
$env:CI=1; npx expo start --web --port 8090
```

## Current Technical Notes

- Auth is currently local prototype auth (storage-based), not production backend auth.
- Functional flows were preserved while UI/UX was upgraded.
- App code is now separated by pages to simplify team edits.

## Next Recommended Steps

1. Connect auth to backend service with secure password hashing.
2. Replace static mock data with backend/cloud device telemetry.
3. Add role-based data visibility and permissions.
4. Add automated tests for auth, tabs, and data rendering.
