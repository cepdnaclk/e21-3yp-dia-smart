import { Card, CardContent, Typography } from "@mui/material";

const ReportsCard = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Reports
        </Typography>

        {/* TODO: Integrate historical generated summary downloads and export APIs */}
        <Typography variant="body2" color="text.secondary">
          Access to historically generated clinical summary sheets, compliance report cards, and doctor/caregiver review annotations for download.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default ReportsCard;
