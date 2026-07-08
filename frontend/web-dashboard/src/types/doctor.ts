import type { RelationshipRole } from "./careTeam";

export interface DoctorAssignedPatient {
  requestId: number;
  userId: number;
  displayName: string;
  email: string;
  patientId: number;
  patientName: string;
  relationshipRole: RelationshipRole;
  createdAt: string;
}

export interface DoctorOverviewStats {
  // Placeholder for overview stats
}

export interface DoctorRecentActivity {
  // Placeholder for recent activity log
}

export interface DoctorDashboardData {
  // Placeholder for dashboard summary data
}

export interface DoctorReportSummary {
  // Placeholder for report summaries
}
