import { Card, CardContent, Typography } from "@mui/material";

const DoctorsSection = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Doctors Directory
        </Typography>
        
        {/* TODO: Integrate Doctor users list management api */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Lists all doctor clinician accounts. Admins can manage clinical licensing flags, clinic associations, and assigned patient limits.
        </Typography>
        <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
          [Placeholder Table: Doctor Accounts]
        </Typography>
      </CardContent>
    </Card>
  );
};

export default DoctorsSection;
