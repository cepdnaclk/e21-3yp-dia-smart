import { useState, useEffect } from "react";
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
import GlucoseChart from "../charts/GlucoseChart";
import DoseHistoryChart from "../charts/DoseHistoryChart";
import GlucoseDoseCorrelationChart from "../charts/GlucoseDoseCorrelationChart";
import { analyticsService } from "../../services/analyticsService";
import type { AnalyticsData, GlucoseReading, DoseReading } from "../../types/analytics";

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

  const loadTelemetryData = async () => {
    setLoading(true);
    setError(null);
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
      setError("Failed to load patient telemetry data.");
    } finally {
      setLoading(false);
    }
  };

  // Reload statistics when date bounds shift
  useEffect(() => {
    if (!patientId) return;
    loadTelemetryData();
  }, [patientId, startDate, endDate]);

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
      <Box sx={{ display: "flex", justifyContent: "center", py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ display: "flex", flexDirection: "column", gap: 3 }}>
      {/* Filters & Configuration Controls Panel */}
      <Card elevation={1} sx={{ borderRadius: 3, border: "1px solid #e2e8f0" }}>
        <CardContent sx={{ display: "flex", flexWrap: "wrap", gap: 2.5, alignItems: "center" }}>
          
          {/* Time Preset */}
          <FormControl size="small" sx={{ minWidth: 150 }}>
            <InputLabel id="range-preset-label">Time Window</InputLabel>
            <Select
              labelId="range-preset-label"
              value={rangeOption}
              label="Time Window"
              onChange={(e) => handleRangeChange(e.target.value)}
              sx={{ borderRadius: 2 }}
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
                sx={{ "& .MuiOutlinedInput-root": { borderRadius: 2 } }}
              />
              <TextField
                size="small"
                label="End Date"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                slotProps={{ inputLabel: { shrink: true } }}
                sx={{ "& .MuiOutlinedInput-root": { borderRadius: 2 } }}
              />
            </>
          )}

          {/* Custom Threshold Settings */}
          <Box sx={{ display: "flex", gap: 1.5, alignItems: "center", ml: { md: "auto" } }}>
            <TextField
              size="small"
              label="Min Target"
              type="number"
              value={targetMinInput}
              onChange={(e) => setTargetMinInput(e.target.value)}
              sx={{ width: 100, "& .MuiOutlinedInput-root": { borderRadius: 2 } }}
            />
            <TextField
              size="small"
              label="Max Target"
              type="number"
              value={targetMaxInput}
              onChange={(e) => setTargetMaxInput(e.target.value)}
              sx={{ width: 100, "& .MuiOutlinedInput-root": { borderRadius: 2 } }}
            />
            <Button
              variant="outlined"
              size="medium"
              onClick={handleApplyThresholds}
              sx={{ textTransform: "none", borderRadius: 2, borderColor: "#3ec1fa", color: "#0284c7" }}
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
          <Card sx={{ borderRadius: 3, border: "1px solid #e2e8f0" }}>
            <CardContent sx={{ p: 2.5 }}>
              <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 700, textTransform: "uppercase" }}>
                Time in Range (TIR)
              </Typography>
              <Typography variant="h4" sx={{ fontWeight: 800, color: timeInRange >= 70 ? "success.main" : "warning.main", my: 1 }}>
                {timeInRange}%
              </Typography>
              <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
                Target: &gt;70% ({targetMin}-{targetMax})
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4, lg: 2.4 }}>
          <Card sx={{ borderRadius: 3, border: "1px solid #e2e8f0" }}>
            <CardContent sx={{ p: 2.5 }}>
              <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 700, textTransform: "uppercase" }}>
                Time Above Range (TAR)
              </Typography>
              <Typography variant="h4" sx={{ fontWeight: 800, color: "error.main", my: 1 }}>
                {timeAboveRange}%
              </Typography>
              <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
                Hyperglycemia (&gt;{targetMax})
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4, lg: 2.4 }}>
          <Card sx={{ borderRadius: 3, border: "1px solid #e2e8f0" }}>
            <CardContent sx={{ p: 2.5 }}>
              <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 700, textTransform: "uppercase" }}>
                Time Below Range (TBR)
              </Typography>
              <Typography variant="h4" sx={{ fontWeight: 800, color: "warning.main", my: 1 }}>
                {timeBelowRange}%
              </Typography>
              <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
                Hypoglycemia (&lt;{targetMin})
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4, lg: 2.4 }}>
          <Card sx={{ borderRadius: 3, border: "1px solid #e2e8f0" }}>
            <CardContent sx={{ p: 2.5 }}>
              <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 700, textTransform: "uppercase" }}>
                Average Glucose
              </Typography>
              <Typography variant="h4" sx={{ fontWeight: 800, color: "#12233b", my: 1 }}>
                {meanGlucose} mg/dL
              </Typography>
              <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
                Estimated mean level
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4, lg: 2.4 }}>
          <Card sx={{ borderRadius: 3, border: "1px solid #e2e8f0" }}>
            <CardContent sx={{ p: 2.5 }}>
              <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 700, textTransform: "uppercase" }}>
                Glycemic Variability
              </Typography>
              <Typography variant="h4" sx={{ fontWeight: 800, color: "#12233b", my: 1 }}>
                {sdVariability} mg/dL
              </Typography>
              <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
                Std Dev (SD) spikes
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Adherence Rates */}
      {adherenceData && (
        <Card elevation={1} sx={{ borderRadius: 3, border: "1px solid #e2e8f0", bgcolor: "#f8fafc" }}>
          <CardContent sx={{ p: 2 }}>
            <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1.5, color: "#1e293b" }}>
              Adherence Summary ({startDate} to {endDate})
            </Typography>
            <Grid container spacing={2}>
              <Grid size={{ xs: 6, sm: 3 }}>
                <Typography variant="caption" color="text.secondary">Adherence Rate</Typography>
                <Typography variant="body1" sx={{ fontWeight: 800, color: "#0284c7" }}>
                  {(adherenceData.adherenceRate * 100).toFixed(1)}%
                </Typography>
              </Grid>
              <Grid size={{ xs: 6, sm: 3 }}>
                <Typography variant="caption" color="text.secondary">On-Time / Total</Typography>
                <Typography variant="body1" sx={{ fontWeight: 700 }}>
                  {adherenceData.onTime} / {adherenceData.totalScheduled}
                </Typography>
              </Grid>
              <Grid size={{ xs: 6, sm: 3 }}>
                <Typography variant="caption" color="text.secondary">Missed Doses</Typography>
                <Typography variant="body1" sx={{ fontWeight: 700, color: "error.main" }}>
                  {adherenceData.missed}
                </Typography>
              </Grid>
              <Grid size={{ xs: 6, sm: 3 }}>
                <Typography variant="caption" color="text.secondary">Late / Unscheduled</Typography>
                <Typography variant="body1" sx={{ fontWeight: 700, color: "warning.main" }}>
                  {adherenceData.late} / {adherenceData.unscheduled}
                </Typography>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      )}

      {/* Visual Charts Tab panels */}
      <Card elevation={2} sx={{ borderRadius: 3 }}>
        <Box sx={{ borderBottom: 1, borderColor: "divider", px: 2, pt: 1 }}>
          <Tabs value={activeTab} onChange={(_e, v) => setActiveTab(v)} variant="scrollable" scrollButtons="auto">
            <Tab label="Glucose & Insulin Correlation" sx={{ textTransform: "none", fontWeight: "bold" }} />
            <Tab label="Glucose Trend" sx={{ textTransform: "none", fontWeight: "bold" }} />
            <Tab label="Dose History" sx={{ textTransform: "none", fontWeight: "bold" }} />
          </Tabs>
        </Box>
        <CardContent sx={{ py: 3 }}>
          {activeTab === 0 && (
            <Box>
              <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 2 }}>
                <Typography variant="subtitle1" sx={{ fontWeight: "bold" }}>
                  Glucose & Insulin Correlation
                </Typography>
                <Box sx={{ display: "flex", gap: 1 }}>
                  <Chip size="small" label="Avg Glucose (Line)" sx={{ bgcolor: "#fee2e2", color: "#ef4444", fontWeight: 700 }} />
                  <Chip size="small" label="Insulin Doses (Bar)" sx={{ bgcolor: "#e0f2fe", color: "#0284c7", fontWeight: 700 }} />
                </Box>
              </Box>
              {combinedChartData.length === 0 ? (
                <Typography color="text.secondary" sx={{ py: 6, textAlign: "center" }}>
                  No telemetry records available inside this date window.
                </Typography>
              ) : (
                <GlucoseDoseCorrelationChart data={combinedChartData} targetMin={targetMin} targetMax={targetMax} />
              )}
            </Box>
          )}

          {activeTab === 1 && (
            <Box>
              <Typography variant="subtitle1" sx={{ fontWeight: "bold", mb: 2 }}>
                Glucose Trend Log
              </Typography>
              {glucoseChartData.length === 0 ? (
                <Typography color="text.secondary" sx={{ py: 6, textAlign: "center" }}>
                  No glucose records available inside this date window.
                </Typography>
              ) : (
                <GlucoseChart data={glucoseChartData} />
              )}
            </Box>
          )}

          {activeTab === 2 && (
            <Box>
              <Typography variant="subtitle1" sx={{ fontWeight: "bold", mb: 2 }}>
                Insulin Dosage History
              </Typography>
              {filteredDoses.length === 0 ? (
                <Typography color="text.secondary" sx={{ py: 6, textAlign: "center" }}>
                  No insulin dose events recorded inside this date window.
                </Typography>
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
