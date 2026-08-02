import { useState } from "react";
import { Grid, Box, TextField, Typography } from "@mui/material";
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
      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 2, flexWrap: "wrap", gap: 2 }}>
        <Typography variant="h6" sx={{ fontWeight: "bold" }}>
          Patient Reports & Exports
        </Typography>
        <Box sx={{ display: "flex", gap: 2 }}>
          <TextField
            label="Start Date"
            type="date"
            size="small"
            slotProps={{ inputLabel: { shrink: true } }}
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
          />
          <TextField
            label="End Date"
            type="date"
            size="small"
            slotProps={{ inputLabel: { shrink: true } }}
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
          />
        </Box>
      </Box>

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
