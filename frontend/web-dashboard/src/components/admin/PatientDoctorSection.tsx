import { Card, CardContent, Typography } from "@mui/material";

const PatientDoctorSection = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Patient ↔ Doctor Assignments
        </Typography>
        
        {/* TODO: Integrate clinician patient assignments mapping endpoints */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Configure clinician access policies. Assign patients to clinicians for remote monitoring, alerts reviews, and prescription authorization.
        </Typography>
        <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
          [Placeholder Registry: Clinician Mappings]
        </Typography>
      </CardContent>
    </Card>
  );
};

export default PatientDoctorSection;
