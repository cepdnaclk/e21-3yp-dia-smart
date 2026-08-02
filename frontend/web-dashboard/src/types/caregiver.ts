import type { RelationshipRole } from "./careTeam";

export interface CaregiverAssignedPatient {
  requestId: number;
  userId: number;
  displayName: string;
  email: string;
  patientId: number;
  patientName: string;
  relationshipRole: RelationshipRole;
  createdAt: string;
}

export interface CaregiverDashboardData {
  // Placeholder for caregiver dashboard overview metrics
}
