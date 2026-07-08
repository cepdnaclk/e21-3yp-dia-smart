import { Box, TextField, Grid, Typography } from "@mui/material";

interface PatientSearchFilterProps {
  query: string;
  onQueryChange: (query: string) => void;
}

const PatientSearchFilter = ({ query, onQueryChange }: PatientSearchFilterProps) => {
  return (
    <Box sx={{ mb: 4, p: 2, bgcolor: "background.paper", borderRadius: 2, boxShadow: 1 }}>
      <Typography
        variant="subtitle2"
        sx={{ mb: 2, fontWeight: "medium" }}
        color="text.secondary"
      >
        Search Patients
      </Typography>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12 }}>
          <TextField
            fullWidth
            placeholder="Search patients by name..."
            value={query}
            onChange={(e) => onQueryChange(e.target.value)}
          />
        </Grid>
      </Grid>
    </Box>
  );
};

export default PatientSearchFilter;
