import { Card, CardContent, Typography, Box } from "@mui/material";

import { Link } from "react-router-dom";
import { Button } from "@mui/material";

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
          <Typography variant="caption" color="text.disabled" sx={{ display: "block", mb: 2 }}>
            [Placeholder: Patient table grid with columns (Name, Age, Glucose Status, Device Info, Last Active, Actions)]
          </Typography>

          <Box sx={{ display: "flex", justifyContent: "center", gap: 2, mt: 2, flexWrap: "wrap" }}>
            <Button component={Link} to="/workspace/1" variant="outlined" size="small" sx={{ textTransform: "none" }}>
              Open Patient #1 Workspace (John Silva)
            </Button>
            <Button component={Link} to="/workspace/2" variant="outlined" size="small" sx={{ textTransform: "none" }}>
              Open Patient #2 Workspace (Nimal Perera)
            </Button>
            <Button component={Link} to="/workspace/3" variant="outlined" size="small" sx={{ textTransform: "none" }}>
              Open Patient #3 Workspace (Kamala Fernando)
            </Button>
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
};

export default PatientList;
