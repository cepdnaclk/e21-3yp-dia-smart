import {
  Card,
  CardContent,
  Typography,
  Box,
  CircularProgress,
  Grid,
  Divider
} from "@mui/material";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  ResponsiveContainer,
  CartesianGrid
} from "recharts";

import type { AdherenceAnalyticsResponse } from "../../types/analytics";

interface AdherenceReportsProps {
  adherenceData: AdherenceAnalyticsResponse | null;
  loading: boolean;
}

const AdherenceReports: React.FC<AdherenceReportsProps> = ({
  adherenceData,
  loading
}) => {
  if (loading) {
    return (
      <Card elevation={2} sx={{ borderRadius: 3, minHeight: 250, display: "flex", justifyContent: "center", alignItems: "center" }}>
        <CircularProgress />
      </Card>
    );
  }

  if (!adherenceData) {
    return (
      <Card elevation={2} sx={{ borderRadius: 3 }}>
        <CardContent sx={{ textAlign: "center", py: 6 }}>
          <Typography color="text.secondary">
            No patient adherence records loaded. Please select a patient to view analytical reports.
          </Typography>
        </CardContent>
      </Card>
    );
  }

  // Calculate daily totals for Recharts
  const chartData = adherenceData.dailyBreakdown.map((day) => {
    const onTimeCount = day.entries.filter((e) => e.status === "ON_TIME").length;
    const lateCount = day.entries.filter((e) => e.status === "LATE").length;
    const missedCount = day.entries.filter((e) => e.status === "MISSED").length;
    const unscheduledCount = day.entries.filter((e) => e.status === "UNSCHEDULED").length;
    return {
      date: new Date(day.date).toLocaleDateString(undefined, { month: "short", day: "numeric" }),
      "On Time": onTimeCount,
      Late: lateCount,
      Missed: missedCount,
      Unscheduled: unscheduledCount
    };
  });

  const ratePercent = (adherenceData.adherenceRate * 100).toFixed(1);

  return (
    <Card elevation={2} sx={{ borderRadius: 3 }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 3 }}>
        <Box>
          <Typography variant="h6" sx={{ fontWeight: "bold" }}>
            Adherence & Intake Compliance Reports
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Analysis of insulin dosing schedules compliance. Displays on-time completion rates vs missed or delayed injections.
          </Typography>
        </Box>

        {/* Adherence Summary Grid */}
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
            <Box sx={{ p: 2, bgcolor: "action.hover", borderRadius: 2, textAlign: "center" }}>
              <Typography variant="caption" color="text.secondary">Adherence Rate</Typography>
              <Typography variant="h5" sx={{ fontWeight: "bold", mt: 0.5, color: "primary.main" }}>
                {ratePercent}%
              </Typography>
            </Box>
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
            <Box sx={{ p: 2, bgcolor: "action.hover", borderRadius: 2, textAlign: "center" }}>
              <Typography variant="caption" color="text.secondary">Total Scheduled</Typography>
              <Typography variant="h5" sx={{ fontWeight: "bold", mt: 0.5 }}>
                {adherenceData.totalScheduled}
              </Typography>
            </Box>
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
            <Box sx={{ p: 2, bgcolor: "success.light", borderRadius: 2, textAlign: "center", color: "success.dark", opacity: 0.9 }}>
              <Typography variant="caption" sx={{ fontWeight: "medium" }}>On Time Doses</Typography>
              <Typography variant="h5" sx={{ fontWeight: "bold", mt: 0.5 }}>
                {adherenceData.onTime}
              </Typography>
            </Box>
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
            <Box sx={{ p: 2, bgcolor: "warning.light", borderRadius: 2, textAlign: "center", color: "warning.dark", opacity: 0.9 }}>
              <Typography variant="caption" sx={{ fontWeight: "medium" }}>Late Doses</Typography>
              <Typography variant="h5" sx={{ fontWeight: "bold", mt: 0.5 }}>
                {adherenceData.late}
              </Typography>
            </Box>
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
            <Box sx={{ p: 2, bgcolor: "error.light", borderRadius: 2, textAlign: "center", color: "error.dark", opacity: 0.9 }}>
              <Typography variant="caption" sx={{ fontWeight: "medium" }}>Missed Doses</Typography>
              <Typography variant="h5" sx={{ fontWeight: "bold", mt: 0.5 }}>
                {adherenceData.missed}
              </Typography>
            </Box>
          </Grid>
        </Grid>

        <Divider />

        {/* Stacked Bar Chart */}
        <Box sx={{ width: "100%", height: 350, mt: 1 }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart
              data={chartData}
              margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
            >
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="date" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Legend />
              <Bar dataKey="On Time" stackId="a" fill="#2e7d32" name="On Time" />
              <Bar dataKey="Late" stackId="a" fill="#ed6c02" name="Late" />
              <Bar dataKey="Missed" stackId="a" fill="#d32f2f" name="Missed" />
              <Bar dataKey="Unscheduled" stackId="a" fill="#9c27b0" name="Unscheduled (Correction)" />
            </BarChart>
          </ResponsiveContainer>
        </Box>
      </CardContent>
    </Card>
  );
};

export default AdherenceReports;
