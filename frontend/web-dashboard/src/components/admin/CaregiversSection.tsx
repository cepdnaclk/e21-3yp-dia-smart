import { Card, CardContent, Typography } from "@mui/material";

const CaregiversSection = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Caregivers Directory
        </Typography>
        
        {/* TODO: Integrate Caregiver users list management api */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Lists all caregiver support accounts. Admins can view assigned patient relationships and caregiver verification files.
        </Typography>
        <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
          [Placeholder Table: Caregiver Accounts]
        </Typography>
      </CardContent>
    </Card>
  );
};

export default CaregiversSection;
