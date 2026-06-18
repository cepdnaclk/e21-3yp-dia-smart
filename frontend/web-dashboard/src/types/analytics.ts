export interface AnalyticsData {
  adherenceRate: number;

  totalScheduled: number;

  onTime: number;
  late: number;
  missed: number;
  unscheduled: number;
}

export interface GlucoseReading {
  glucoseReadingId: number;

  glucoseValueMgDl: number;

  measuredAt: string;

  source: string;

  mealContext: string;

  notes: string | null;
}

export interface DoseReading {
  doseEventId: number;
  doseUnits: number;
  injectedAt: string;
}