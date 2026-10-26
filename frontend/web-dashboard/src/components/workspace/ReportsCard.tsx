import { useState } from "react";
import { Grid, Box, TextField, Card, CardContent } from "@mui/material";
import AssessmentOutlinedIcon from "@mui/icons-material/AssessmentOutlined";
import CardHeading from "../common/CardHeading";
import PatientReports from "../doctor/PatientReports";
import ExportReports from "../doctor/ExportReports";

interface ReportsCardProps {
  patientId: number;
  patientProfile?: any;
}

const ReportsCard = ({ patientId, patientProfile }: ReportsCardProps) => {
  const [startDate, setStartDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() - 14); // 14 days default
    return d.toISOString().split("T")[0];
  });
  const [endDate, setEndDate] = useState(() => {
    return new Date().toISOString().split("T")[0];
  });

  const patientName = patientProfile?.displayName || `Patient ID: ${patientId}`;

  return (
    <Box sx={{ flexGrow: 1, mt: 2 }}>
      <Card sx={{ mb: 3 }}>
        <CardContent
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            flexWrap: "wrap",
            gap: 2,
            "&:last-child": { pb: 2 },
          }}
        >
          <CardHeading
            title="Patient Reports & Exports"
            icon={<AssessmentOutlinedIcon />}
            color="#12233b"
            sx={{ mb: 0, flex: "1 1 240px", minWidth: 0 }}
          />
          <Box sx={{ display: "flex", gap: 1.5, flexWrap: "wrap", flex: { xs: "1 1 100%", sm: "0 1 auto" } }}>
            <TextField
              label="Start Date"
              type="date"
              size="small"
              slotProps={{ inputLabel: { shrink: true } }}
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              sx={{ flex: "1 1 140px" }}
            />
            <TextField
              label="End Date"
              type="date"
              size="small"
              slotProps={{ inputLabel: { shrink: true } }}
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              sx={{ flex: "1 1 140px" }}
            />
          </Box>
        </CardContent>
      </Card>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 7 }}>
          <PatientReports
            patientId={patientId}
            startDate={startDate}
            endDate={endDate}
            patientName={patientName}
          />
        </Grid>
        <Grid size={{ xs: 12, md: 5 }}>
          <ExportReports
            patientId={patientId}
            startDate={startDate}
            endDate={endDate}
            patientName={patientName}
          />
        </Grid>
      </Grid>
    </Box>
  );
};

export default ReportsCard;
