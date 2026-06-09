export interface Prescription {
  prescriptionId: number;
  insulinProductId: number;

  prescriptionName: string;

  startDate: string;
  endDate: string;

  active: boolean;

  notes: string | null;

  createdAt: string;
}