import type { ReactNode } from "react";
import { Card, CardContent, Typography, Box, Avatar } from "@mui/material";
import { alpha } from "@mui/material/styles";
import BadgeOutlinedIcon from "@mui/icons-material/BadgeOutlined";
import CakeOutlinedIcon from "@mui/icons-material/CakeOutlined";
import BloodtypeOutlinedIcon from "@mui/icons-material/BloodtypeOutlined";
import FiberManualRecordIcon from "@mui/icons-material/FiberManualRecord";
import type { PatientProfileResponse } from "../../services/patientsService";

interface PatientHeaderProps {
  patientProfile: PatientProfileResponse;
}

const calculateAge = (dobString: string) => {
  if (!dobString) return "N/A";
  const dob = new Date(dobString);
  const diffMs = Date.now() - dob.getTime();
  const ageDate = new Date(diffMs);
  return Math.abs(ageDate.getUTCFullYear() - 1970);
};

// Presentational only: initials for the avatar badge.
const getInitials = (name?: string) => {
  if (!name) return "?";
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  const first = parts[0].charAt(0);
  const last = parts.length > 1 ? parts[parts.length - 1].charAt(0) : "";
  return `${first}${last}`.toUpperCase();
};

interface InfoChipProps {
  icon: ReactNode;
  label: string;
  value: ReactNode;
  color: string;
}

const InfoChip = ({ icon, label, value, color }: InfoChipProps) => (
  <Box
    sx={{
      display: "inline-flex",
      alignItems: "center",
      gap: 1,
      px: 1.5,
      py: 0.75,
      borderRadius: 999,
      bgcolor: "#f8fafc",
      border: "1px solid",
      borderColor: "divider",
      maxWidth: "100%",
      minWidth: 0,
    }}
  >
    <Box
      aria-hidden
      sx={{
        display: "flex",
        alignItems: "center",
        color,
        "& .MuiSvgIcon-root": { fontSize: 16 },
      }}
    >
      {icon}
    </Box>
    <Typography variant="body2" color="text.secondary" sx={{ minWidth: 0, overflowWrap: "anywhere" }}>
      <strong>{label}</strong>{" "}
      <Box component="span" sx={{ color: "#12233b", fontWeight: 700 }}>
        {value}
      </Box>
    </Typography>
  </Box>
);

const PatientHeader = ({ patientProfile }: PatientHeaderProps) => {
  return (
    <Card sx={{ overflow: "hidden", position: "relative" }}>
      <Box
        aria-hidden
        sx={{
          height: 6,
          background: "linear-gradient(90deg, #12233b 0%, #0284c7 60%, #3ec1fa 100%)",
        }}
      />
      <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
        <Box
          sx={{
            display: "flex",
            alignItems: { xs: "flex-start", sm: "center" },
            gap: { xs: 2, sm: 2.5 },
            flexWrap: "wrap",
          }}
        >
          <Avatar
            sx={{
              width: { xs: 52, sm: 64 },
              height: { xs: 52, sm: 64 },
              fontSize: { xs: 20, sm: 24 },
              fontWeight: 800,
              color: "#ffffff",
              background: "linear-gradient(135deg, #12233b 0%, #0284c7 100%)",
              boxShadow: `0 6px 16px ${alpha("#0284c7", 0.25)}`,
              flexShrink: 0,
            }}
          >
            {getInitials(patientProfile?.fullName)}
          </Avatar>

          <Box sx={{ flex: "1 1 220px", minWidth: 0 }}>
            <Typography
              variant="h5"
              component="h2"
              sx={{ fontWeight: 800, color: "#12233b", mb: 1.25, overflowWrap: "anywhere" }}
            >
              {patientProfile?.fullName || "Unknown Patient"}
            </Typography>

            <Box sx={{ display: "flex", flexWrap: "wrap", gap: 1 }}>
              <InfoChip
                icon={<BadgeOutlinedIcon />}
                label="Patient ID:"
                value={patientProfile?.patientId || "N/A"}
                color="#0284c7"
              />
              <InfoChip
                icon={<CakeOutlinedIcon />}
                label="Age:"
                value={calculateAge(patientProfile?.dateOfBirth)}
                color="#7c3aed"
              />
              <InfoChip
                icon={<BloodtypeOutlinedIcon />}
                label="Diabetes Type:"
                value={patientProfile?.diabetesType || "N/A"}
                color="#ef4444"
              />
              <Box
                sx={{
                  display: "inline-flex",
                  alignItems: "center",
                  gap: 1,
                  px: 1.5,
                  py: 0.75,
                  borderRadius: 999,
                  bgcolor: alpha("#10b981", 0.1),
                  border: "1px solid",
                  borderColor: alpha("#10b981", 0.3),
                }}
              >
                <FiberManualRecordIcon aria-hidden sx={{ fontSize: 10, color: "#10b981" }} />
                <Typography variant="body2" sx={{ color: "#047857" }}>
                  <strong>Current Status:</strong> Active
                </Typography>
              </Box>
            </Box>
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
};

export default PatientHeader;
