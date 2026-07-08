import { useState, useEffect } from "react";
import { Card, CardContent, Typography, Box, CircularProgress } from "@mui/material";
import { doctorService } from "../../services/doctorService";

const AssignedPatientsSummary = () => {
  const [loading, setLoading] = useState(true);
  const [count, setCount] = useState<number | null>(null);

  useEffect(() => {
    const fetchCount = async () => {
      try {
        const patients = await doctorService.getAssignedPatients();
        setCount(patients.length);
      } catch (err) {
        console.error("Failed to load patient count", err);
      } finally {
        setLoading(false);
      }
    };
    fetchCount();
  }, []);

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography
          variant="h6"
          sx={{ mb: 2, fontWeight: "medium" }}
        >
          Assigned Patients Summary
        </Typography>

        <Box sx={{ p: 2, bgcolor: "action.hover", borderRadius: 1 }}>
          <Typography
            variant="body2"
            color="text.secondary"
            sx={{ mb: 1 }}
          >
            Total Assigned Patients
          </Typography>
          {loading ? (
            <CircularProgress size={20} />
          ) : (
            <Typography
              variant="h4"
              sx={{ fontWeight: "bold", color: "primary.main" }}
            >
              {count !== null ? count : "N/A"}
            </Typography>
          )}
        </Box>
      </CardContent>
    </Card>
  );
};

export default AssignedPatientsSummary;
