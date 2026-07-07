import { Card, CardContent, Typography } from "@mui/material";

const DeviceAssignmentSection = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Device Provisioning & Assignments
        </Typography>
        
        {/* TODO: Integrate hardware assignment and provisioning APIs */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Manage allocation of hardware sensors and dosing modules to patient user profiles. Associate serial keys with diagnostic targets.
        </Typography>
        <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
          [Placeholder Form: Provision / Associate Device]
        </Typography>
      </CardContent>
    </Card>
  );
};

export default DeviceAssignmentSection;
