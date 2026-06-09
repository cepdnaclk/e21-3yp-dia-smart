export const getPatientId = () => {
  const patientId =
    localStorage.getItem(
      "patientId"
    );

  if (!patientId) {
    throw new Error(
      "Patient ID not found"
    );
  }

  return patientId;
};