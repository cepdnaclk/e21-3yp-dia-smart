import { Card, CardContent, Typography, Box, Button } from "@mui/material";
import { Link } from "react-router-dom";

const AssignedPatientsSummary = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent sx={{ height: "100%", display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
        <Box>
          <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
            Assigned Patients Summary
          </Typography>

          {/* TODO: Fetch caregiver assigned patients overview count and active alerts */}
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Overview summary of patients under your care. Displays the number of active, warning, and stable status patients.
          </Typography>
        </Box>

        <Box sx={{ display: "flex", justifyContent: "flex-end", mt: 2 }}>
          <Button component={Link} to="/caregiver/patients" variant="text" size="small" sx={{ textTransform: "none" }}>
            View All Patients
          </Button>
        </Box>
      </CardContent>
    </Card>
  );
};

export default AssignedPatientsSummary;
