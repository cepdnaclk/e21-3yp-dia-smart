import { useState, useEffect } from "react";
import type { ReactNode } from "react";
import {
  Card,
  CardContent,
  Typography,
  Box,
  Grid,
  CircularProgress,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
  Button,
  Tabs,
  Tab,
  Chip
} from "@mui/material";
import { alpha } from "@mui/material/styles";
import TuneIcon from "@mui/icons-material/Tune";
import TrackChangesIcon from "@mui/icons-material/TrackChanges";
import TrendingUpIcon from "@mui/icons-material/TrendingUp";
import TrendingDownIcon from "@mui/icons-material/TrendingDown";
import WaterDropOutlinedIcon from "@mui/icons-material/WaterDropOutlined";
import StackedLineChartIcon from "@mui/icons-material/StackedLineChart";
import FactCheckOutlinedIcon from "@mui/icons-material/FactCheckOutlined";
import InsightsOutlinedIcon from "@mui/icons-material/InsightsOutlined";
import ShowChartIcon from "@mui/icons-material/ShowChart";
import VaccinesOutlinedIcon from "@mui/icons-material/VaccinesOutlined";
import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";
import MetricCard from "../common/MetricCard";
import GlucoseChart from "../charts/GlucoseChart";
import DoseHistoryChart from "../charts/DoseHistoryChart";
import GlucoseDoseCorrelationChart from "../charts/GlucoseDoseCorrelationChart";
import { analyticsService } from "../../services/analyticsService";
import type { AnalyticsData, GlucoseReading, DoseReading } from "../../types/analytics";

import { useAutoRefresh } from "../../hooks/useAutoRefresh";

interface AdvancedAnalyticsViewProps {
  patientId: number;
}

const getPastDateStr = (daysAgo: number) => {
  const d = new Date();
  d.setDate(d.getDate() - daysAgo);
  return d.toISOString().split("T")[0];
};

const AdvancedAnalyticsView = ({ patientId }: AdvancedAnalyticsViewProps) => {
  const [loading, setLoading] = useState(true);
  const [adherenceData, setAdherenceData] = useState<AnalyticsData | null>(null);
  const [glucoseHistory, setGlucoseHistory] = useState<GlucoseReading[]>([]);
  const [doseHistory, setDoseHistory] = useState<DoseReading[]>([]);
  const [error, setError] = useState<string | null>(null);

  // Filters State
  const [rangeOption, setRangeOption] = useState("7");
  const [startDate, setStartDate] = useState(getPastDateStr(7));
  const [endDate, setEndDate] = useState(getPastDateStr(0));
  const [customRange, setCustomRange] = useState(false);

  // Thresholds State
  const [targetMinInput, setTargetMinInput] = useState("70");
  const [targetMaxInput, setTargetMaxInput] = useState("180");
  const [targetMin, setTargetMin] = useState(70);
  const [targetMax, setTargetMax] = useState(180);

  // Tabs State
  const [activeTab, setActiveTab] = useState(0);

  // Load custom patient target thresholds from localStorage on mount
  useEffect(() => {
    if (!patientId) return;
    const cachedMin = localStorage.getItem(`diasmart_patient_${patientId}_target_min`);
    const cachedMax = localStorage.getItem(`diasmart_patient_${patientId}_target_max`);
    
    const minVal = cachedMin ? Number(cachedMin) : 70;
    const maxVal = cachedMax ? Number(cachedMax) : 180;

    setTargetMin(minVal);
    setTargetMax(maxVal);
    setTargetMinInput(String(minVal));
    setTargetMaxInput(String(maxVal));
  }, [patientId]);

  // Handle Date Range Preset Changes
  const handleRangeChange = (option: string) => {
    setRangeOption(option);
    if (option === "custom") {
      setCustomRange(true);
    } else {
      setCustomRange(false);
      const days = Number(option);
      setStartDate(getPastDateStr(days));
      setEndDate(getPastDateStr(0));
    }
  };

  const loadTelemetryData = async (silent = false) => {
    if (!silent) {
      setLoading(true);
      setError(null);
    }
    try {
      const [adherence, glucose, doses] = await Promise.all([
        analyticsService.getAnalytics(patientId, startDate, endDate),
        analyticsService.getGlucoseHistory(patientId),
        analyticsService.getDoseHistory(patientId)
      ]);

      setAdherenceData(adherence);
      setGlucoseHistory(glucose || []);
      setDoseHistory(doses || []);
    } catch (err) {
      console.error("Failed to load advanced analytics", err);
      if (!silent) {
        setError("Failed to load patient telemetry data.");
      }
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  };

  // Reload statistics when date bounds shift
  useEffect(() => {
    if (!patientId) return;
    loadTelemetryData(false);
  }, [patientId, startDate, endDate]);

  // Auto-refresh telemetry data every 5 seconds
  useAutoRefresh(() => loadTelemetryData(true), 5000);

  const handleApplyThresholds = () => {
    const min = Number(targetMinInput);
    const max = Number(targetMaxInput);
    
    if (isNaN(min) || isNaN(max) || min <= 0 || max <= min) {
      alert("Please provide valid target parameters. Minimum must be greater than zero and less than Maximum.");
      return;
    }

    setTargetMin(min);
    setTargetMax(max);
    localStorage.setItem(`diasmart_patient_${patientId}_target_min`, String(min));
    localStorage.setItem(`diasmart_patient_${patientId}_target_max`, String(max));
  };

  // Client-side calculations on filtered datasets
  const filteredGlucose = glucoseHistory.filter((item) => {
    const d = item.measuredAt.split("T")[0];
    return d >= startDate && d <= endDate;
  });

  const filteredDoses = doseHistory.filter((item) => {
    const d = item.injectedAt.split("T")[0];
    return d >= startDate && d <= endDate;
  });

  // Calculate TIR / TAR / TBR
  const totalReadings = filteredGlucose.length;
  let inRangeCount = 0;
  let aboveRangeCount = 0;
  let belowRangeCount = 0;
  let sumGlucose = 0;

  filteredGlucose.forEach((item) => {
    sumGlucose += item.glucoseValueMgDl;
    if (item.glucoseValueMgDl < targetMin) {
      belowRangeCount++;
    } else if (item.glucoseValueMgDl > targetMax) {
      aboveRangeCount++;
    } else {
      inRangeCount++;
    }
  });

  const timeInRange = totalReadings > 0 ? Math.round((inRangeCount / totalReadings) * 100) : 0;
  const timeAboveRange = totalReadings > 0 ? Math.round((aboveRangeCount / totalReadings) * 100) : 0;
  const timeBelowRange = totalReadings > 0 ? Math.round((belowRangeCount / totalReadings) * 100) : 0;
  const meanGlucose = totalReadings > 0 ? Math.round(sumGlucose / totalReadings) : 0;

  // Calculate Glycemic Variability Standard Deviation
  let sdVariability = 0;
  if (totalReadings > 1) {
    const varianceSum = filteredGlucose.reduce((acc, item) => acc + Math.pow(item.glucoseValueMgDl - meanGlucose, 2), 0);
    sdVariability = Math.round(Math.sqrt(varianceSum / (totalReadings - 1)));
  }

  // format raw readings for individual charts
  const glucoseChartData = filteredGlucose.map((r) => ({
    date: new Date(r.measuredAt).toLocaleDateString(),
    glucose: r.glucoseValueMgDl,
  })).reverse();

  // format raw combined data for Composed correlation charts
  const mergedMap: Record<string, { date: string; glucoseList: number[]; doseList: number[] }> = {};
  
  filteredGlucose.forEach((item) => {
    const dStr = new Date(item.measuredAt).toLocaleDateString();
    if (!mergedMap[dStr]) {
      mergedMap[dStr] = { date: dStr, glucoseList: [], doseList: [] };
    }
    mergedMap[dStr].glucoseList.push(item.glucoseValueMgDl);
  });
  
  filteredDoses.forEach((item) => {
    const dStr = new Date(item.injectedAt).toLocaleDateString();
    if (!mergedMap[dStr]) {
      mergedMap[dStr] = { date: dStr, glucoseList: [], doseList: [] };
    }
    mergedMap[dStr].doseList.push(item.doseUnits);
  });
  
  const combinedChartData = Object.values(mergedMap).map((m) => {
    const avgGlucose = m.glucoseList.length > 0 ? Math.round(m.glucoseList.reduce((a, b) => a + b, 0) / m.glucoseList.length) : undefined;
    const totalDose = m.doseList.length > 0 ? m.doseList.reduce((a, b) => a + b, 0) : undefined;
    return {
      date: m.date,
      glucose: avgGlucose,
      dose: totalDose
    };
  }).sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());

  if (loading && !adherenceData) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", py: 8 }}>
        <CircularProgress size={28} sx={{ color: "secondary.dark" }} />
      </Box>
    );
  }

  const adherenceTile = (label: string, value: ReactNode, color: string) => (
    <Box
      sx={{
        height: "100%",
        p: 1.5,
        bgcolor: "#ffffff",
        border: "1px solid",
        borderColor: "divider",
        borderRadius: 2.5,
        minWidth: 0,
      }}
    >
      <Typography variant="caption" color="text.secondary" sx={{ display: "block", fontWeight: 600 }}>
        {label}
      </Typography>
      <Typography variant="body1" sx={{ fontWeight: 800, color, fontSize: "1.15rem", mt: 0.25 }}>
        {value}
      </Typography>
    </Box>
  );

  return (
    <Box sx={{ display: "flex", flexDirection: "column", gap: 3, minWidth: 0 }}>
      {/* Filters & Configuration Controls Panel */}
      <Card>
        <CardContent sx={{ display: "flex", flexWrap: "wrap", gap: 2, alignItems: "center" }}>
          <Box
            aria-hidden
            sx={{
              display: { xs: "none", sm: "flex" },
              alignItems: "center",
              justifyContent: "center",
              width: 38,
              height: 38,
              borderRadius: 2,
              color: "#0284c7",
              bgcolor: alpha("#0284c7", 0.1),
              flexShrink: 0,
            }}
          >
            <TuneIcon sx={{ fontSize: 20 }} />
          </Box>

          {/* Time Preset */}
          <FormControl size="small" sx={{ minWidth: 150, flex: { xs: "1 1 150px", sm: "0 0 auto" } }}>
            <InputLabel id="range-preset-label">Time Window</InputLabel>
            <Select
              labelId="range-preset-label"
              value={rangeOption}
              label="Time Window"
              onChange={(e) => handleRangeChange(e.target.value)}
            >
              <MenuItem value="7">Last 7 Days</MenuItem>
              <MenuItem value="14">Last 14 Days</MenuItem>
              <MenuItem value="30">Last 30 Days</MenuItem>
              <MenuItem value="custom">Custom Range</MenuItem>
            </Select>
          </FormControl>

          {/* Custom Date Inputs */}
          {customRange && (
            <>
              <TextField
                size="small"
                label="Start Date"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                slotProps={{ inputLabel: { shrink: true } }}
                sx={{ flex: { xs: "1 1 150px", sm: "0 0 auto" } }}
              />
              <TextField
                size="small"
                label="End Date"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                slotProps={{ inputLabel: { shrink: true } }}
                sx={{ flex: { xs: "1 1 150px", sm: "0 0 auto" } }}
              />
            </>
          )}

          {/* Custom Threshold Settings */}
          <Box sx={{ display: "flex", flexWrap: "wrap", gap: 1.5, alignItems: "center", ml: { md: "auto" } }}>
            <TextField
              size="small"
              label="Min Target"
              type="number"
              value={targetMinInput}
              onChange={(e) => setTargetMinInput(e.target.value)}
              sx={{ width: 100 }}
            />
            <TextField
              size="small"
              label="Max Target"
              type="number"
              value={targetMaxInput}
              onChange={(e) => setTargetMaxInput(e.target.value)}
              sx={{ width: 100 }}
            />
            <Button
              variant="outlined"
              size="medium"
              onClick={handleApplyThresholds}
              sx={{ textTransform: "none", borderColor: "#3ec1fa", color: "#0284c7" }}
            >
              Apply Targets
            </Button>
          </Box>
        </CardContent>
      </Card>

      {error && (
        <Typography color="error" variant="body2" sx={{ textAlign: "center" }}>
          {error}
        </Typography>
      )}

      {/* Clinical Metrics Insights Grid */}
      <Grid container spacing={2.5}>
        <Grid size={{ xs: 12, sm: 6, md: 4, lg: 2.4 }}>
          <MetricCard
            label="Time in Range (TIR)"
            value={`${timeInRange}%`}
            description={<>Target: &gt;70% ({targetMin}-{targetMax})</>}
            icon={<TrackChangesIcon />}
            tone={timeInRange >= 70 ? "success" : "warning"}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4, lg: 2.4 }}>
          <MetricCard
            label="Time Above Range (TAR)"
            value={`${timeAboveRange}%`}
            description={<>Hyperglycemia (&gt;{targetMax})</>}
            icon={<TrendingUpIcon />}
            tone="error"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4, lg: 2.4 }}>
          <MetricCard
            label="Time Below Range (TBR)"
            value={`${timeBelowRange}%`}
            description={<>Hypoglycemia (&lt;{targetMin})</>}
            icon={<TrendingDownIcon />}
            tone="warning"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4, lg: 2.4 }}>
          <MetricCard
            label="Average Glucose"
            value={`${meanGlucose} mg/dL`}
            description="Estimated mean level"
            icon={<WaterDropOutlinedIcon />}
            tone="primary"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4, lg: 2.4 }}>
          <MetricCard
            label="Glycemic Variability"
            value={`${sdVariability} mg/dL`}
            description="Std Dev (SD) spikes"
            icon={<StackedLineChartIcon />}
            tone="info"
          />
        </Grid>
      </Grid>

      {/* Adherence Rates */}
      {adherenceData && (
        <Card sx={{ bgcolor: "#f8fafc" }}>
          <CardContent>
            <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 1.5, flexWrap: "wrap" }}>
              <FactCheckOutlinedIcon aria-hidden sx={{ fontSize: 20, color: "#10b981" }} />
              <Typography variant="subtitle2" sx={{ fontWeight: 700, color: "#12233b" }}>
                Adherence Summary ({startDate} to {endDate})
              </Typography>
            </Box>
            <Grid container spacing={1.5}>
              <Grid size={{ xs: 6, sm: 3 }}>
                {adherenceTile("Adherence Rate", `${(adherenceData.adherenceRate * 100).toFixed(1)}%`, "#0284c7")}
              </Grid>
              <Grid size={{ xs: 6, sm: 3 }}>
                {adherenceTile("On-Time / Total", <>{adherenceData.onTime} / {adherenceData.totalScheduled}</>, "#12233b")}
              </Grid>
              <Grid size={{ xs: 6, sm: 3 }}>
                {adherenceTile("Missed Doses", adherenceData.missed, "#dc2626")}
              </Grid>
              <Grid size={{ xs: 6, sm: 3 }}>
                {adherenceTile("Late / Unscheduled", <>{adherenceData.late} / {adherenceData.unscheduled}</>, "#b45309")}
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      )}

      {/* Visual Charts Tab panels */}
      <Card>
        <Box sx={{ borderBottom: 1, borderColor: "divider", px: { xs: 1, sm: 2 }, pt: 1 }}>
          <Tabs value={activeTab} onChange={(_e, v) => setActiveTab(v)} variant="scrollable" scrollButtons="auto">
            <Tab label="Glucose & Insulin Correlation" sx={{ textTransform: "none", fontWeight: 700 }} />
            <Tab label="Glucose Trend" sx={{ textTransform: "none", fontWeight: 700 }} />
            <Tab label="Dose History" sx={{ textTransform: "none", fontWeight: 700 }} />
          </Tabs>
        </Box>
        <CardContent sx={{ py: 3 }}>
          {activeTab === 0 && (
            <Box>
              <CardHeading
                title="Glucose & Insulin Correlation"
                icon={<InsightsOutlinedIcon />}
                color="#0284c7"
                action={
                  <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
                    <Chip size="small" label="Avg Glucose (Line)" sx={{ bgcolor: "#fee2e2", color: "#b91c1c", fontWeight: 700 }} />
                    <Chip size="small" label="Insulin Doses (Bar)" sx={{ bgcolor: "#e0f2fe", color: "#0369a1", fontWeight: 700 }} />
                  </Box>
                }
              />
              {combinedChartData.length === 0 ? (
                <EmptyState message="No telemetry records available inside this date window." icon={<InsightsOutlinedIcon />} minHeight={220} />
              ) : (
                <GlucoseDoseCorrelationChart data={combinedChartData} targetMin={targetMin} targetMax={targetMax} />
              )}
            </Box>
          )}

          {activeTab === 1 && (
            <Box>
              <CardHeading title="Glucose Trend Log" icon={<WaterDropOutlinedIcon />} color="#ef4444" />
              {glucoseChartData.length === 0 ? (
                <EmptyState message="No glucose records available inside this date window." icon={<ShowChartIcon />} minHeight={220} />
              ) : (
                <GlucoseChart data={glucoseChartData} />
              )}
            </Box>
          )}

          {activeTab === 2 && (
            <Box>
              <CardHeading title="Insulin Dosage History" icon={<VaccinesOutlinedIcon />} color="#0284c7" />
              {filteredDoses.length === 0 ? (
                <EmptyState message="No insulin dose events recorded inside this date window." icon={<VaccinesOutlinedIcon />} minHeight={220} />
              ) : (
                <DoseHistoryChart data={filteredDoses} />
              )}
            </Box>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default AdvancedAnalyticsView;
