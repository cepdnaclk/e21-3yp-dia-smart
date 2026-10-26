import type { ReactNode } from "react";
import { Card, CardContent, Typography, Grid, Box } from "@mui/material";
import PersonOutlineIcon from "@mui/icons-material/PersonOutlined";
import WcOutlinedIcon from "@mui/icons-material/WcOutlined";
import EventOutlinedIcon from "@mui/icons-material/EventOutlined";
import PhoneOutlinedIcon from "@mui/icons-material/PhoneOutlined";
import ContactEmergencyOutlinedIcon from "@mui/icons-material/ContactEmergencyOutlined";
import CardHeading from "../common/CardHeading";
import type { PatientProfileResponse } from "../../services/patientsService";

interface PatientDetailsCardProps {
  patientProfile: PatientProfileResponse;
}

const DetailItem = ({ icon, label, value }: { icon: ReactNode; label: string; value: ReactNode }) => (
  <Box
    sx={{
      display: "flex",
      alignItems: "flex-start",
      gap: 1.25,
      p: 1.5,
      height: "100%",
      bgcolor: "#f8fafc",
      border: "1px solid",
      borderColor: "divider",
      borderRadius: 2.5,
      minWidth: 0,
    }}
  >
    <Box
      aria-hidden
      sx={{ color: "#64748b", display: "flex", mt: 0.25, "& .MuiSvgIcon-root": { fontSize: 18 } }}
    >
      {icon}
    </Box>
    <Box sx={{ minWidth: 0 }}>
      <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
        {label}
      </Typography>
      <Typography variant="body2" sx={{ fontWeight: 600, color: "#12233b", overflowWrap: "anywhere" }}>
        {value}
      </Typography>
    </Box>
  </Box>
);

const PatientDetailsCard = ({ patientProfile }: PatientDetailsCardProps) => {
  return (
    <Card sx={{ height: "100%" }}>
      <CardContent>
        <CardHeading title="Patient Details" icon={<PersonOutlineIcon />} color="#12233b" />

        <Grid container spacing={1.5}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <DetailItem icon={<WcOutlinedIcon />} label="Gender" value={patientProfile?.gender || "N/A"} />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <DetailItem
              icon={<EventOutlinedIcon />}
              label="Date of Birth"
              value={patientProfile?.dateOfBirth || "N/A"}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <DetailItem
              icon={<PhoneOutlinedIcon />}
              label="Contact Number"
              value={patientProfile?.contactNumber || "N/A"}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <DetailItem
              icon={<ContactEmergencyOutlinedIcon />}
              label="Emergency Contact"
              value={patientProfile?.emergencyContactNumber || "N/A"}
            />
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );
};

export default PatientDetailsCard;
