export type Reading = {
  record_id: number;
  datetime_sl: string;
  glucose_mg_dl: number;
};

export type UserRole = "caregiver" | "doctor" | "patient";

export type AppUser = {
  fullName: string;
  email: string;
  password: string;
  role: UserRole;
};

export const demoUsers: AppUser[] = [
  { fullName: "Maria Perera", email: "caregiver@diasmart.com", password: "Care1234", role: "caregiver" },
  { fullName: "Dr. Silva", email: "doctor@diasmart.com", password: "Doctor1234", role: "doctor" },
  { fullName: "John Fernando", email: "patient@diasmart.com", password: "Patient1234", role: "patient" }
];

export const sampleReadings: Reading[] = [
  { record_id: 101, datetime_sl: "2026-03-01 06:30:00", glucose_mg_dl: 92 },
  { record_id: 102, datetime_sl: "2026-03-01 12:55:00", glucose_mg_dl: 146 },
  { record_id: 103, datetime_sl: "2026-03-01 21:40:00", glucose_mg_dl: 171 },
  { record_id: 104, datetime_sl: "2026-03-02 06:45:00", glucose_mg_dl: 88 },
  { record_id: 105, datetime_sl: "2026-03-02 12:38:00", glucose_mg_dl: 139 },
  { record_id: 106, datetime_sl: "2026-03-02 20:12:00", glucose_mg_dl: 162 },
  { record_id: 107, datetime_sl: "2026-03-03 07:02:00", glucose_mg_dl: 95 },
  { record_id: 108, datetime_sl: "2026-03-03 13:06:00", glucose_mg_dl: 154 },
  { record_id: 109, datetime_sl: "2026-03-03 22:15:00", glucose_mg_dl: 182 },
  { record_id: 110, datetime_sl: "2026-03-04 06:40:00", glucose_mg_dl: 90 },
  { record_id: 111, datetime_sl: "2026-03-04 12:42:00", glucose_mg_dl: 148 },
  { record_id: 112, datetime_sl: "2026-03-04 21:23:00", glucose_mg_dl: 194 }
];

export function zoneFromValue(value: number): "low" | "normal" | "high" | "critical" {
  if (value <= 69) return "low";
  if (value <= 140) return "normal";
  if (value <= 199) return "high";
  return "critical";
}

export function buildMetrics(readings: Reading[]) {
  const total = readings.length;
  const avg = total ? readings.reduce((sum, item) => sum + item.glucose_mg_dl, 0) / total : 0;
  const latest = total ? readings[total - 1] : null;
  const inRange = readings.filter((item) => item.glucose_mg_dl >= 70 && item.glucose_mg_dl <= 140).length;

  const zones = { low: 0, normal: 0, high: 0, critical: 0 };
  for (const reading of readings) {
    zones[zoneFromValue(reading.glucose_mg_dl)] += 1;
  }

  return {
    total,
    avg,
    latest,
    inRangePct: total ? Math.round((inRange / total) * 100) : 0,
    zones
  };
}
