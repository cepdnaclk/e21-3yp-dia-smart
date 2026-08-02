export const UserRole = {
  ADMIN: "ADMIN",
  DOCTOR: "DOCTOR",
  CAREGIVER: "CAREGIVER",
  PATIENT: "PATIENT",
} as const;

export type UserRole = (typeof UserRole)[keyof typeof UserRole];