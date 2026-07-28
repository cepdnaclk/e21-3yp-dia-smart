import { useState, useEffect } from "react";
import { Card, CardContent, Typography, Box, CircularProgress } from "@mui/material";
import GlucoseChart from "../charts/GlucoseChart";
import { analyticsService } from "../../services/analyticsService";
import type { GlucoseReading } from "../../types/analytics";

interface GlucoseTrendsCardProps {
  patientId: number;
  refreshTrigger?: number;
}

const GlucoseTrendsCard = ({ patientId, refreshTrigger }: GlucoseTrendsCardProps) => {
  const [loading, setLoading] = useState(true);
  const [readings, setReadings] = useState<GlucoseReading[]>([]);

  useEffect(() => {
    if (!patientId) return;
    const fetchHistory = async (silent = false) => {
      try {
        if (!silent) setLoading(true);
        const history = await analyticsService.getGlucoseHistory(patientId);
        setReadings(history || []);
      } catch (err) {
        console.error("Failed to load glucose history", err);
      } finally {
        if (!silent) setLoading(false);
      }
    };

    const isInitial = !refreshTrigger || refreshTrigger === 0;
    fetchHistory(!isInitial);
  }, [patientId, refreshTrigger]);

  const chartData = readings.map((r) => ({
    date: new Date(r.measuredAt).toLocaleDateString(),
    glucose: r.glucoseValueMgDl,
  }));

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography
          variant="h6"
          sx={{ mb: 2, fontWeight: "medium" }}
        >
          Glucose Trends
        </Typography>

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
            <CircularProgress />
          </Box>
        ) : chartData.length === 0 ? (
          <Typography color="text.secondary" sx={{ py: 2 }}>
            No glucose records found
          </Typography>
        ) : (
          <GlucoseChart data={chartData} />
        )}
      </CardContent>
    </Card>
  );
};

export default GlucoseTrendsCard;
