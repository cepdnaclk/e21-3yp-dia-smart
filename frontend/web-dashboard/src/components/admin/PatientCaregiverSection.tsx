import { Card, CardContent, Typography } from "@mui/material";

const PatientCaregiverSection = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Patient ↔ Caregiver Mappings
        </Typography>
        
        {/* TODO: Integrate caregiver patient mappings and validation status endpoints */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Manage caregiver patient relations. Register caregiver profiles to grant view access to specific patient vitals.
        </Typography>
        <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
          [Placeholder Registry: Caregiver Mappings]
        </Typography>
      </CardContent>
    </Card>
  );
};

export default PatientCaregiverSection;
