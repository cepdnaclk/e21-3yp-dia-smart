export interface DoseSchedule {
  scheduleId: number;
  prescriptionId: number;
  scheduleLabel: string;
  scheduledTime: string; // "HH:MM:SS"
  doseUnits: number;
  daysOfWeek: string;
  allowedEarlyMinutes: number;
  allowedLateMinutes: number;
  active: boolean;
  createdAt: string;
}

export interface PaginatedDoseSchedulesResponse {
  content: DoseSchedule[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
