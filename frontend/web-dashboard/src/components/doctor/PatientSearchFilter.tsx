import { Box, TextField, Grid, Typography, InputAdornment } from "@mui/material";
import SearchRoundedIcon from "@mui/icons-material/SearchRounded";

interface PatientSearchFilterProps {
  query: string;
  onQueryChange: (query: string) => void;
}

const PatientSearchFilter = ({ query, onQueryChange }: PatientSearchFilterProps) => {
  return (
    <Box
      sx={{
        mb: 3,
        p: { xs: 2, sm: 2.5 },
        bgcolor: "background.paper",
        borderRadius: 3,
        border: "1px solid",
        borderColor: "divider",
        boxShadow: "0 1px 2px rgba(15, 23, 42, 0.04)",
      }}
    >
      <Typography
        variant="subtitle2"
        sx={{ mb: 1.5, fontWeight: 600 }}
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
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchRoundedIcon sx={{ color: "#94a3b8" }} />
                  </InputAdornment>
                ),
              },
            }}
          />
        </Grid>
      </Grid>
    </Box>
  );
};

export default PatientSearchFilter;
