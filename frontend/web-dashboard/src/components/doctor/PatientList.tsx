import { Card, CardContent, Typography, Box } from "@mui/material";

const PatientList = () => {
  return (
    <Card elevation={2} sx={{ borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Patient List
        </Typography>

        {/* TODO: Integrate with patient catalog APIs */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          This section will present a complete grid/list of patients assigned to you, including their current monitoring statistics, device battery levels, last active timelines, and links to detailed analytics reports.
        </Typography>

        <Box sx={{ p: 4, bgcolor: "action.hover", borderRadius: 1, textAlign: "center" }}>
          <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
            [Placeholder: Patient table grid with columns (Name, Age, Glucose Status, Device Info, Last Active, Actions)]
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
};

export default PatientList;
