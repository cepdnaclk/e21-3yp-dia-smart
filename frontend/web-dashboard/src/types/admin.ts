import { UserRole } from "./roles";

export interface AdminDashboardMetrics {
  // Placeholder for admin dashboard summary counts
}

export interface AdminUserRecord {
  userId: number;
  userUuid: string;
  email: string;
  role: UserRole;
  displayName: string;
  contactNumber: string;
  active: boolean;
  lastLoginAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AdminCreateUserRequest {
  displayName: string;
  email: string;
  password?: string;
  role: UserRole;
  contactNumber?: string;
  active?: boolean;
}

export interface AdminDeviceRecord {
  // Placeholder for registered medical/hardware devices
}

export interface PatientAccessResponse {
  accessId: number;
  userId: number;
  patientId: number;
  accessRole: "DOCTOR" | "CAREGIVER" | "PATIENT" | "SELF";
  relationshipLabel?: string;
  canView: boolean;
  canAcknowledgeAlerts: boolean;
  canEditPrescriptions: boolean;
  createdAt: string;
  status: "ACTIVE" | "REVOKED";
  revokedAt?: string;
  revokedBy?: number;
}

export interface CreatePatientAccessRequest {
  userId: number;
  patientId: number;
  accessRole: "DOCTOR" | "CAREGIVER" | "PATIENT" | "SELF";
  relationshipLabel?: string;
  canView?: boolean;
  canAcknowledgeAlerts?: boolean;
  canEditPrescriptions?: boolean;
}
