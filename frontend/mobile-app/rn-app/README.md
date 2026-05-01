# Dia-Smart React Native + Web App

This is a separate Expo app that runs on:
- Web browser
- Android
- iOS

## Folder structure

- `app/login.tsx` - Login page
- `app/signup.tsx` - Create account page
- `app/(tabs)/home.tsx` - Home tab
- `app/(tabs)/trends.tsx` - Trends tab
- `app/(tabs)/readings.tsx` - Readings tab
- `app/(tabs)/profile.tsx` - Profile tab
- `src/state/authContext.tsx` - Auth state and logic
- `src/data/mockData.ts` - Demo users and glucose mock data
- `src/theme/colors.ts` - Theme colors

## Install

```powershell
cd c:\Personal\3yp\e21-3yp-dia-smart\frontend\rn-app
npm install
```

## Run on web

```powershell
npm run web
```

Then open the URL shown in terminal.

## Run on Android

```powershell
npm run android
```

## Run on iOS (macOS only)

```powershell
npm run ios
```

## Hardcoded demo login

- caregiver@diasmart.com / Care1234
- doctor@diasmart.com / Doctor1234
- patient@diasmart.com / Patient1234
