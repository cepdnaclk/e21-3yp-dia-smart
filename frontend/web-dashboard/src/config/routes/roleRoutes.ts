import { UserRole } from "../../types/roles";

export const DEFAULT_ROLE_ROUTES: Record<UserRole, string> = {
  [UserRole.PATIENT]: "/dashboard",
  [UserRole.DOCTOR]: "/doctor/dashboard",
  [UserRole.CAREGIVER]: "/caregiver/dashboard",
  [UserRole.ADMIN]: "/admin/dashboard",
};
