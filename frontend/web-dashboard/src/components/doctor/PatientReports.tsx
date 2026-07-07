import { Card, CardContent, Typography, Box } from "@mui/material";

import { Link } from "react-router-dom";
import { Button } from "@mui/material";

const PatientReports = () => {
  return (
    <Card elevation={2} sx={{ borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Patient Reports
        </Typography>

        {/* TODO: Integrate with patient report endpoints to list generated PDFs/documents */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          List of medical histories, patient summaries, glucose level graphs, and clinician review logs will appear here. Doctors will be able to download, regenerate, or annotate patient reports.
        </Typography>

        <Box sx={{ p: 3, bgcolor: "action.hover", borderRadius: 1, textAlign: "center" }}>
          <Typography variant="caption" color="text.disabled" sx={{ display: "block", mb: 2 }}>
            [Placeholder: Patient reports table with download, print, and regenerate controls]
          </Typography>

          <Box sx={{ display: "flex", justifyContent: "center", gap: 2, mt: 1, flexWrap: "wrap" }}>
            <Button component={Link} to="/workspace/1" variant="text" size="small" sx={{ textTransform: "none" }}>
              Patient #1 Report Workspace
            </Button>
            <Button component={Link} to="/workspace/2" variant="text" size="small" sx={{ textTransform: "none" }}>
              Patient #2 Report Workspace
            </Button>
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
};

export default PatientReports;
