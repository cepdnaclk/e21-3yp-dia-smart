export type CareTeamRole =
  | "DOCTOR"
  | "CAREGIVER";

export type RelationshipRequestStatus =
  | "PENDING"
  | "APPROVED"
  | "REJECTED";

export interface CareTeamMember {
  id: number;
  displayName: string;
  role: CareTeamRole;
  email?: string;
  contactNumber?: string;
}

export interface RelationshipRequest {
  id: number;
  requesterName: string;
  requesterRole: CareTeamRole;
  status: RelationshipRequestStatus;
  requestedAt?: string;
}
