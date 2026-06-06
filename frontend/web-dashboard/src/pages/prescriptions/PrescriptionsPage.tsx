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

const prescriptions = [
  {
    id: 1,
    medication: "Insulin Glargine",
    dosage: "10 Units",
    frequency: "Daily",
    status: "Active",
  },
  {
    id: 2,
    medication: "Rapid Insulin",
    dosage: "5 Units",
    frequency: "Before Meals",
    status: "Active",
  },
];

const PrescriptionsPage = () => {
  return (
    <>
      <Typography variant="h4" sx={{ mb: 3 }}>
        Prescriptions
      </Typography>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Medication</TableCell>
              <TableCell>Dosage</TableCell>
              <TableCell>Frequency</TableCell>
              <TableCell>Status</TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            {prescriptions.map((item) => (
              <TableRow key={item.id}>
                <TableCell>{item.medication}</TableCell>
                <TableCell>{item.dosage}</TableCell>
                <TableCell>{item.frequency}</TableCell>
                <TableCell>
                  <Chip
                    label={item.status}
                    color="success"
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

export default PrescriptionsPage;