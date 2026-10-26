// Presentational labels for the signed-in role, used by the app shell only.
export const ROLE_DISPLAY: Record<string, { title: string; portal: string }> = {
  PATIENT: { title: "Patient", portal: "Patient Portal" },
  DOCTOR: { title: "Clinician", portal: "Clinician Portal" },
  CAREGIVER: { title: "Caregiver", portal: "Caregiver Portal" },
  ADMIN: { title: "Administrator", portal: "Admin Console" },
};

export const getRoleDisplay = (role: string) =>
  ROLE_DISPLAY[role] ?? { title: "User", portal: "Dia-Smart Portal" };
