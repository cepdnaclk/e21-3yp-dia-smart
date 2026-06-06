import {
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
} from "@mui/material";

const patients = [
  {
    id: 1,
    name: "John Silva",
    glucose: 118,
    dose: 10,
    status: "Stable",
  },
  {
    id: 2,
    name: "Mary Perera",
    glucose: 145,
    dose: 12,
    status: "Warning",
  },
  {
    id: 3,
    name: "Nimal Fernando",
    glucose: 105,
    dose: 8,
    status: "Stable",
  },
];

const PatientsPage = () => {
  return (
    <>
      <Typography variant="h4" sx={{ mb: 3 }}>
        Patients
      </Typography>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Patient</TableCell>
              <TableCell>Glucose (mg/dL)</TableCell>
              <TableCell>Last Dose (Units)</TableCell>
              <TableCell>Status</TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            {patients.map((patient) => (
              <TableRow key={patient.id}>
                <TableCell>{patient.name}</TableCell>
                <TableCell>{patient.glucose}</TableCell>
                <TableCell>{patient.dose}</TableCell>

                <TableCell>
                  <Chip
                    label={patient.status}
                    color={
                      patient.status === "Stable"
                        ? "success"
                        : "warning"
                    }
                  />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
};

export default PatientsPage;