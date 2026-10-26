import { useState, useEffect } from "react";
import { Card, CardContent, Box, CircularProgress } from "@mui/material";
import VaccinesOutlinedIcon from "@mui/icons-material/VaccinesOutlined";
import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";
import DoseHistoryChart from "../charts/DoseHistoryChart";
import { analyticsService } from "../../services/analyticsService";
import type { DoseReading } from "../../types/analytics";

interface DoseHistoryCardProps {
  patientId: number;
  refreshTrigger?: number;
}

const DoseHistoryCard = ({ patientId, refreshTrigger }: DoseHistoryCardProps) => {
  const [loading, setLoading] = useState(true);
  const [readings, setReadings] = useState<DoseReading[]>([]);

  useEffect(() => {
    if (!patientId) return;
    const fetchHistory = async (silent = false) => {
      try {
        if (!silent) setLoading(true);
        const history = await analyticsService.getDoseHistory(patientId);
        setReadings(history || []);
      } catch (err) {
        console.error("Failed to load dose history", err);
      } finally {
        if (!silent) setLoading(false);
      }
    };

    const isInitial = !refreshTrigger || refreshTrigger === 0;
    fetchHistory(!isInitial);
  }, [patientId, refreshTrigger]);

  return (
    <Card sx={{ height: "100%" }}>
      <CardContent>
        <CardHeading title="Dose History" icon={<VaccinesOutlinedIcon />} color="#0284c7" />

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", minHeight: 160 }}>
            <CircularProgress size={28} sx={{ color: "secondary.dark" }} />
          </Box>
        ) : readings.length === 0 ? (
          <EmptyState message="No dosing records found" icon={<VaccinesOutlinedIcon />} />
        ) : (
          <DoseHistoryChart data={readings} />
        )}
      </CardContent>
    </Card>
  );
};

export default DoseHistoryCard;
