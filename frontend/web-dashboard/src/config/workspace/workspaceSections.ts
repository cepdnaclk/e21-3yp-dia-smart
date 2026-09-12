import { UserRole } from "../../types/roles";

export interface WorkspaceSectionConfig {
  id: string;
  gridSize: {
    xs: number;
    sm?: number;
    md?: number;
    lg?: number;
  };
}

export const workspaceSections: Record<UserRole, WorkspaceSectionConfig[]> = {
  [UserRole.DOCTOR]: [
    { id: "patient-details", gridSize: { xs: 12, md: 4 } },
    { id: "alerts", gridSize: { xs: 12, md: 8 } },
    { id: "prescriptions", gridSize: { xs: 12, md: 6 } },
    { id: "dose-schedule", gridSize: { xs: 12, md: 6 } },
    { id: "analytics", gridSize: { xs: 12, md: 12 } },
    { id: "reports", gridSize: { xs: 12 } },
  ],
  [UserRole.CAREGIVER]: [
    { id: "patient-details", gridSize: { xs: 12, md: 4 } },
    { id: "today-dose", gridSize: { xs: 12, md: 8 } },
    { id: "alerts", gridSize: { xs: 12, md: 6 } },
    { id: "storage-monitoring", gridSize: { xs: 12, md: 6 } },
    { id: "inventory-monitoring", gridSize: { xs: 12, md: 6 } },
    { id: "timeline", gridSize: { xs: 12, md: 6 } },
  ],
  [UserRole.PATIENT]: [],
  [UserRole.ADMIN]: [],
};
