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

export interface AdherenceEntry {
  scheduleId?: number;
  scheduleLabel?: string;
  scheduledTime: string; // "HH:MM:SS"
  status: "ON_TIME" | "LATE" | "MISSED" | "UNSCHEDULED";
  doseEventId?: number;
  injectedAt?: string;
}

export interface DailyAdherenceBreakdown {
  date: string; // "YYYY-MM-DD"
  entries: AdherenceEntry[];
}

export interface AdherenceAnalyticsResponse {
  patientId: number;
  startDate: string;
  endDate: string;
  totalScheduled: number;
  onTime: number;
  late: number;
  missed: number;
  unscheduled: number;
  adherenceRate: number;
  dailyBreakdown: DailyAdherenceBreakdown[];
}

export interface ScheduleAdherenceResponse {
  scheduleId: number;
  scheduleLabel: string;
  scheduledTime: string; // "HH:MM:SS"
  doseUnits: number;
  status: "ON_TIME" | "LATE" | "MISSED" | "UNSCHEDULED";
  injectedAt?: string;
  minutesOffset?: number;
}