import { Box, TextField, Grid, Typography } from "@mui/material";

const PatientSearchFilter = () => {
  return (
    <Box sx={{ mb: 4, p: 2, bgcolor: "background.paper", borderRadius: 2, boxShadow: 1 }}>
      <Typography variant="subtitle2" sx={{ mb: 2, fontWeight: "medium" }} color="text.secondary">
        Search & Filter Patients
      </Typography>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 6 }}>
          <TextField
            fullWidth
            disabled
            placeholder="Search patients by name (disabled)..."
            helperText="TODO: Implement caregiver search filter logic"
          />
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <TextField
            select
            fullWidth
            disabled
            label="Filter by Status"
            value=""
            helperText="TODO: Implement caregiver status filter logic"
          />
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <TextField
            select
            fullWidth
            disabled
            label="Filter by Alerts"
            value=""
            helperText="TODO: Implement caregiver alerts filter logic"
          />
        </Grid>
      </Grid>
    </Box>
  );
};

export default PatientSearchFilter;
