import { Card, CardContent, Typography, Box } from "@mui/material";

const AssignedPatientsSummary = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Assigned Patients Summary
        </Typography>
        
        {/* TODO: Integrate with patient statistics and status APIs */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          This card will display a summary breakdown of your assigned patients, including total count, critical status alerts, and stable patient distributions.
        </Typography>

        <Box sx={{ p: 2, bgcolor: "action.hover", borderRadius: 1 }}>
          <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
            [Placeholder: Patient status distribution breakdown and link to Patient list]
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
};

export default AssignedPatientsSummary;
