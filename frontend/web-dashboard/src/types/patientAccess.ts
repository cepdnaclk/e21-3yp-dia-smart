export interface PatientAccess {
  accessId: number;

  userId: number;

  patientId: number;

  accessRole: string;

  relationshipLabel: string;

  canView: boolean;

  canAcknowledgeAlerts: boolean;

  canEditPrescriptions: boolean;

  status: string;
}