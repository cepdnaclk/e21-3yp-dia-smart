import { Card, CardContent, Typography, Box } from "@mui/material";

const PatientHeader = () => {
  return (
    <Card elevation={2} sx={{ borderRadius: 2 }}>
      <CardContent>
        {/* TODO: Fetch patient header details (name, id, type, age, status) from api */}
        <Typography variant="h5" sx={{ fontWeight: "bold", mb: 1 }}>
          [Patient Name]
        </Typography>

        <Box sx={{ display: "flex", flexWrap: "wrap", gap: 3 }}>
          <Typography variant="body2" color="text.secondary">
            <strong>Patient ID:</strong> [Placeholder ID]
          </Typography>
          <Typography variant="body2" color="text.secondary">
            <strong>Age:</strong> [Placeholder Age]
          </Typography>
          <Typography variant="body2" color="text.secondary">
            <strong>Diabetes Type:</strong> [Placeholder Diabetes Type]
          </Typography>
          <Typography variant="body2" color="text.secondary">
            <strong>Current Status:</strong> [Placeholder Status]
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
};

export default PatientHeader;
