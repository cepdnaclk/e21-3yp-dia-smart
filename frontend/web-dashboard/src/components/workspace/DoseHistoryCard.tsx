import { useState, useEffect } from "react";
import { Card, CardContent, Typography, Box, CircularProgress } from "@mui/material";
import DoseHistoryChart from "../charts/DoseHistoryChart";
import { analyticsService } from "../../services/analyticsService";
import type { DoseReading } from "../../types/analytics";

interface DoseHistoryCardProps {
  patientId: number;
}

const DoseHistoryCard = ({ patientId }: DoseHistoryCardProps) => {
  const [loading, setLoading] = useState(true);
  const [readings, setReadings] = useState<DoseReading[]>([]);

  useEffect(() => {
    if (!patientId) return;
    const fetchHistory = async () => {
      try {
        const history = await analyticsService.getDoseHistory(patientId);
        setReadings(history || []);
      } catch (err) {
        console.error("Failed to load dose history", err);
      } finally {
        setLoading(false);
      }
    };
    fetchHistory();
  }, [patientId]);

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography
          variant="h6"
          sx={{ mb: 2, fontWeight: "medium" }}
        >
          Dose History
        </Typography>

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
            <CircularProgress />
          </Box>
        ) : readings.length === 0 ? (
          <Typography color="text.secondary" sx={{ py: 2 }}>
            No dosing records found
          </Typography>
        ) : (
          <DoseHistoryChart data={readings} />
        )}
      </CardContent>
    </Card>
  );
};

export default DoseHistoryCard;
