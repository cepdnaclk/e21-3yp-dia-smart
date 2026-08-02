export type RelationshipRole = "DOCTOR" | "CAREGIVER";

export type RelationshipRequestStatus = "PENDING" | "ACCEPTED" | "REJECTED" | "REVOKED";

export interface CreateRelationshipRequestDto {
  targetUserId?: number;
  targetEmail?: string;
  patientId?: number;
  relationshipRole: RelationshipRole;
  message?: string;
}

export interface RelationshipRequestDto {
  requestId: number;
  requesterUserId: number;
  requesterName: string;
  targetUserId: number;
  targetName: string;
  patientId: number;
  patientName: string;
  relationshipRole: RelationshipRole;
  status: RelationshipRequestStatus;
  message?: string;
  createdAt: string;
  respondedAt?: string;
}

export interface RelationshipSummaryDto {
  requestId: number;
  userId: number;
  displayName: string;
  email: string;
  patientId: number;
  patientName: string;
  relationshipRole: RelationshipRole;
  createdAt: string;
}
