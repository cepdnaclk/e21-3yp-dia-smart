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
} from "@mui/material";
import { Link } from "react-router-dom";
import type { CaregiverAssignedPatient } from "../../types/caregiver";

interface PatientListProps {
  patients: CaregiverAssignedPatient[];
}

const PatientList = ({ patients }: PatientListProps) => {
  return (
    <Card elevation={2} sx={{ borderRadius: 2 }}>
      <CardContent>
        <Typography
          variant="h6"
          sx={{ mb: 2, fontWeight: "medium" }}
        >
          Patient List
        </Typography>

        {patients.length === 0 ? (
          <Typography color="text.secondary" sx={{ py: 2 }}>
            No assigned patients found
          </Typography>
        ) : (
          <>
            {/* Desktop Table View */}
            <TableContainer component={Paper} variant="outlined" sx={{ display: { xs: "none", md: "block" } }}>
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
                    <TableRow key={pat.requestId}>
                      <TableCell>{pat.patientId}</TableCell>
                      <TableCell sx={{ fontWeight: 500 }}>
                        {pat.patientName}
                      </TableCell>
                      <TableCell>{pat.relationshipRole}</TableCell>
                      <TableCell>Connected</TableCell>
                      <TableCell align="right">
                        <Button
                          component={Link}
                          to={`/workspace/${pat.patientId}`}
                          variant="outlined"
                          size="small"
                          sx={{ textTransform: "none" }}
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
                <Card key={pat.requestId} variant="outlined" sx={{ borderRadius: 3, border: "1px solid #e2e8f0" }}>
                  <Box sx={{ p: 2 }}>
                    <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 1.5 }}>
                      <Typography variant="subtitle1" sx={{ fontWeight: 800, color: "#12233b" }}>
                        {pat.patientName}
                      </Typography>
                      <Chip
                        size="small"
                        label="Connected"
                        color="success"
                        sx={{ fontWeight: 700, fontSize: "0.7rem", height: 20 }}
                      />
                    </Box>

                    <Box sx={{ display: "flex", flexDirection: "column", gap: 1, mb: 1.5 }}>
                      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                        <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
                          Patient ID
                        </Typography>
                        <Typography variant="body2" sx={{ fontWeight: 700, color: "#12233b" }}>
                          {pat.patientId}
                        </Typography>
                      </Box>
                      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                        <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
                          Relationship
                        </Typography>
                        <Typography variant="body2" sx={{ fontWeight: 700, color: "#12233b" }}>
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
