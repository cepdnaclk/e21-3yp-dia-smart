import { Card, CardContent, Typography, Box } from "@mui/material";

const AdherenceReports = () => {
  return (
    <Card elevation={2} sx={{ borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Adherence Reports
        </Typography>

        {/* TODO: Integrate with dosing event adherence logs and trends metrics APIs */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Dose compliance rates, skipped doses, delayed entries, and inventory levels reports will appear in this section. Helps the clinician analyze adherence to prescribed insulin schedules.
        </Typography>

        <Box sx={{ p: 3, bgcolor: "action.hover", borderRadius: 1, textAlign: "center" }}>
          <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
            [Placeholder: Compliance percentage analytics graphs, and compliance table lists]
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
};

export default AdherenceReports;
