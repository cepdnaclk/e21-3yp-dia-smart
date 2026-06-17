export interface Alert {
  alertId: number;
  patientId?: number;

  alertType: string;
  alertDomain?: string;

  severity: string;

  title: string;
  message: string;

  status: string;

  createdAt: string;
  acknowledgedAt?: string | null;
  resolvedAt?: string | null;
}

export interface AlertsPageResponse {
  content: Alert[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
