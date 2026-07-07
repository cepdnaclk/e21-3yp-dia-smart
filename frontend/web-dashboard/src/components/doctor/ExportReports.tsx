import { Card, CardContent, Typography, Box } from "@mui/material";

const ExportReports = () => {
  return (
    <Card elevation={2} sx={{ borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Export Reports
        </Typography>

        {/* TODO: Integrate export triggers for PDF/CSV generation */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Enables downloading data exports for further analysis. Clinicians will be able to customize start/end dates, choose target data types (glucose readings, dose logs, inventory alerts), and download files.
        </Typography>

        <Box sx={{ p: 3, bgcolor: "action.hover", borderRadius: 1, textAlign: "center" }}>
          <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
            [Placeholder: Report custom exporter settings form and file download buttons]
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
};

export default ExportReports;
