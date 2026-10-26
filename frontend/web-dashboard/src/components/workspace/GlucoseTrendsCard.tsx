import { useState, useEffect } from "react";
import { Card, CardContent, Box, CircularProgress } from "@mui/material";
import WaterDropOutlinedIcon from "@mui/icons-material/WaterDropOutlined";
import ShowChartIcon from "@mui/icons-material/ShowChart";
import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";
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
    <Card sx={{ height: "100%" }}>
      <CardContent>
        <CardHeading title="Glucose Trends" icon={<WaterDropOutlinedIcon />} color="#ef4444" />

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", minHeight: 160 }}>
            <CircularProgress size={28} sx={{ color: "secondary.dark" }} />
          </Box>
        ) : chartData.length === 0 ? (
          <EmptyState message="No glucose records found" icon={<ShowChartIcon />} />
        ) : (
          <GlucoseChart data={chartData} />
        )}
      </CardContent>
    </Card>
  );
};

export default GlucoseTrendsCard;
