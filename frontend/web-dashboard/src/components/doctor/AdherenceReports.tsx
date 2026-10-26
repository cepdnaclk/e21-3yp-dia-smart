import type { ReactNode } from "react";
import {
  Card,
  CardContent,
  Typography,
  Box,
  CircularProgress,
  Grid,
  Divider
} from "@mui/material";
import { alpha } from "@mui/material/styles";
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
import FactCheckOutlinedIcon from "@mui/icons-material/FactCheckOutlined";
import AssessmentOutlinedIcon from "@mui/icons-material/AssessmentOutlined";

import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";
import {
  chartAxisProps,
  chartGridProps,
  chartTooltipContentStyle,
  chartTooltipItemStyle,
  chartTooltipLabelStyle,
} from "../charts/chartStyles";
import type { AdherenceAnalyticsResponse } from "../../types/analytics";

interface AdherenceReportsProps {
  adherenceData: AdherenceAnalyticsResponse | null;
  loading: boolean;
}

/** Presentational summary tile for the adherence breakdown row. */
const SummaryTile = ({ label, value, color }: { label: string; value: ReactNode; color: string }) => (
  <Box
    sx={{
      p: 2,
      height: "100%",
      borderRadius: 2.5,
      textAlign: "center",
      bgcolor: alpha(color, 0.07),
      border: "1px solid",
      borderColor: alpha(color, 0.18),
    }}
  >
    <Typography variant="caption" sx={{ fontWeight: 600, color: "text.secondary", display: "block" }}>
      {label}
    </Typography>
    <Typography variant="h5" sx={{ fontWeight: 800, mt: 0.5, color, letterSpacing: "-0.01em" }}>
      {value}
    </Typography>
  </Box>
);

const AdherenceReports: React.FC<AdherenceReportsProps> = ({
  adherenceData,
  loading
}) => {
  if (loading) {
    return (
      <Card sx={{ minHeight: 250, display: "flex", justifyContent: "center", alignItems: "center" }}>
        <CircularProgress sx={{ color: "secondary.dark" }} />
      </Card>
    );
  }

  if (!adherenceData) {
    return (
      <Card>
        <CardContent>
          <EmptyState
            message="No patient adherence records loaded. Please select a patient to view analytical reports."
            icon={<AssessmentOutlinedIcon />}
            minHeight={200}
          />
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
    <Card>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 2.5 }}>
        <CardHeading
          title="Adherence & Intake Compliance Reports"
          subtitle="Analysis of insulin dosing schedules compliance. Displays on-time completion rates vs missed or delayed injections."
          icon={<FactCheckOutlinedIcon />}
          color="#10b981"
          sx={{ mb: 0 }}
        />

        {/* Adherence Summary Grid */}
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
            <SummaryTile label="Adherence Rate" value={`${ratePercent}%`} color="#12233b" />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
            <SummaryTile label="Total Scheduled" value={adherenceData.totalScheduled} color="#0284c7" />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
            <SummaryTile label="On Time Doses" value={adherenceData.onTime} color="#059669" />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
            <SummaryTile label="Late Doses" value={adherenceData.late} color="#d97706" />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
            <SummaryTile label="Missed Doses" value={adherenceData.missed} color="#dc2626" />
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
              <CartesianGrid {...chartGridProps} />
              <XAxis dataKey="date" {...chartAxisProps} />
              <YAxis allowDecimals={false} {...chartAxisProps} />
              <Tooltip
                cursor={{ fill: "#f1f5f9" }}
                contentStyle={chartTooltipContentStyle}
                labelStyle={chartTooltipLabelStyle}
                itemStyle={chartTooltipItemStyle}
              />
              <Legend iconType="circle" iconSize={8} wrapperStyle={{ fontSize: 12, paddingTop: 8 }} />
              <Bar dataKey="On Time" stackId="a" fill="#10b981" name="On Time" />
              <Bar dataKey="Late" stackId="a" fill="#f59e0b" name="Late" />
              <Bar dataKey="Missed" stackId="a" fill="#ef4444" name="Missed" />
              <Bar dataKey="Unscheduled" stackId="a" fill="#8b5cf6" name="Unscheduled (Correction)" />
            </BarChart>
          </ResponsiveContainer>
        </Box>
      </CardContent>
    </Card>
  );
};

export default AdherenceReports;
