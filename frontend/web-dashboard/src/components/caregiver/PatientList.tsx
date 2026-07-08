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
          <TableContainer component={Paper} variant="outlined">
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
        )}
      </CardContent>
    </Card>
  );
};

export default PatientList;
