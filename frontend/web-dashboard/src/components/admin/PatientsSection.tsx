import { Card, CardContent, Typography } from "@mui/material";

const PatientsSection = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Patients Directory
        </Typography>
        
        {/* TODO: Integrate Patient users list management api */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Lists all patient user accounts. Admins can view diagnostic statuses, toggle account states, and manage device mappings.
        </Typography>
        <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
          [Placeholder Table: Patient Accounts]
        </Typography>
      </CardContent>
    </Card>
  );
};

export default PatientsSection;
