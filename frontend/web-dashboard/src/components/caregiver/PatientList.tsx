import {
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button,
  Box,
  Divider,
  Chip,
  Avatar,
} from "@mui/material";
import PeopleAltOutlinedIcon from "@mui/icons-material/PeopleAltOutlined";
import PersonSearchOutlinedIcon from "@mui/icons-material/PersonSearchOutlined";
import OpenInNewRoundedIcon from "@mui/icons-material/OpenInNewRounded";
import { Link } from "react-router-dom";

import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";
import type { CaregiverAssignedPatient } from "../../types/caregiver";

interface PatientListProps {
  patients: CaregiverAssignedPatient[];
}

/** Presentational helper: up to two initials for the avatar badge. */
const getInitials = (name?: string) => {
  const parts = (name ?? "").trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  return parts
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join("");
};

const avatarSx = {
  width: 36,
  height: 36,
  fontSize: "0.8rem",
  fontWeight: 700,
  bgcolor: "#e0f2fe",
  color: "#0284c7",
};

const PatientList = ({ patients }: PatientListProps) => {
  return (
    <Card>
      <CardContent>
        <CardHeading title="Patient List" icon={<PeopleAltOutlinedIcon />} color="#12233b" />

        {patients.length === 0 ? (
          <EmptyState message="No assigned patients found" icon={<PersonSearchOutlinedIcon />} />
        ) : (
          <>
            {/* Desktop Table View */}
            <TableContainer
              component={Paper}
              variant="outlined"
              sx={{ display: { xs: "none", md: "block" }, borderRadius: 2, overflowX: "auto" }}
            >
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Patient ID</TableCell>
                    <TableCell>Patient Name</TableCell>
                    <TableCell>Relationship Type</TableCell>
                    <TableCell>Connection Status</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {patients.map((pat) => (
                    <TableRow key={pat.requestId} hover>
                      <TableCell sx={{ color: "text.secondary", fontVariantNumeric: "tabular-nums" }}>
                        {pat.patientId}
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
                          <Avatar sx={avatarSx}>{getInitials(pat.patientName)}</Avatar>
                          <Typography variant="body2" sx={{ fontWeight: 600, color: "text.primary" }}>
                            {pat.patientName}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>{pat.relationshipRole}</TableCell>
                      <TableCell>
                        <Chip size="small" label="Connected" color="success" variant="outlined" sx={{ fontWeight: 600 }} />
                      </TableCell>
                      <TableCell align="right">
                        <Button
                          component={Link}
                          to={`/workspace/${pat.patientId}`}
                          variant="outlined"
                          size="small"
                          endIcon={<OpenInNewRoundedIcon fontSize="small" />}
                          sx={{ textTransform: "none", whiteSpace: "nowrap" }}
                        >
                          Open Workspace
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>

            {/* Mobile Card List View */}
            <Box sx={{ display: { xs: "flex", md: "none" }, flexDirection: "column", gap: 2 }}>
              {patients.map((pat) => (
                <Card key={pat.requestId} variant="outlined" sx={{ boxShadow: "none" }}>
                  <Box sx={{ p: 2 }}>
                    <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 1.5, mb: 1.5, flexWrap: "wrap" }}>
                      <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, minWidth: 0 }}>
                        <Avatar sx={avatarSx}>{getInitials(pat.patientName)}</Avatar>
                        <Typography variant="subtitle1" sx={{ fontWeight: 700, color: "#12233b", overflowWrap: "anywhere" }}>
                          {pat.patientName}
                        </Typography>
                      </Box>
                      <Chip
                        size="small"
                        label="Connected"
                        color="success"
                        variant="outlined"
                        sx={{ fontWeight: 700, fontSize: "0.7rem", height: 22 }}
                      />
                    </Box>

                    <Box sx={{ display: "flex", flexDirection: "column", gap: 1, mb: 1.5 }}>
                      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 1 }}>
                        <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
                          Patient ID
                        </Typography>
                        <Typography variant="body2" sx={{ fontWeight: 700, color: "#12233b" }}>
                          {pat.patientId}
                        </Typography>
                      </Box>
                      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 1 }}>
                        <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
                          Relationship
                        </Typography>
                        <Typography variant="body2" sx={{ fontWeight: 700, color: "#12233b", textAlign: "right", overflowWrap: "anywhere" }}>
                          {pat.relationshipRole}
                        </Typography>
                      </Box>
                    </Box>

                    <Divider sx={{ my: 1.5 }} />

                    <Button
                      component={Link}
                      to={`/workspace/${pat.patientId}`}
                      variant="contained"
                      fullWidth
                      size="small"
                      endIcon={<OpenInNewRoundedIcon fontSize="small" />}
                      sx={{
                        textTransform: "none",
                        backgroundColor: "#12233b",
                        color: "#ffffff",
                        fontWeight: 700,
                        "&:hover": { backgroundColor: "#1b3559" },
                      }}
                    >
                      Open Workspace
                    </Button>
                  </Box>
                </Card>
              ))}
            </Box>
          </>
        )}
      </CardContent>
    </Card>
  );
};

export default PatientList;
