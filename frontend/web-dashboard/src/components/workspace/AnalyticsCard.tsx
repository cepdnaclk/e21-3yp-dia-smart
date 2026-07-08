import { useState, useEffect } from "react";
import { Card, CardContent, Typography, Box, Grid, CircularProgress } from "@mui/material";
import { analyticsService } from "../../services/analyticsService";
import type { AnalyticsData } from "../../types/analytics";

interface AnalyticsCardProps {
  patientId: number;
}

const AnalyticsCard = ({ patientId }: AnalyticsCardProps) => {
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<AnalyticsData | null>(null);

  useEffect(() => {
    if (!patientId) return;
    const fetchAnalytics = async () => {
      try {
        const response = await analyticsService.getAnalytics(patientId);
        setData(response);
      } catch (err) {
        console.error("Failed to load analytics", err);
      } finally {
        setLoading(false);
      }
    };
    fetchAnalytics();
  }, [patientId]);

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography
          variant="h6"
          sx={{ mb: 2, fontWeight: "medium" }}
        >
          Adherence Analytics
        </Typography>

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
            <CircularProgress />
          </Box>
        ) : !data ? (
          <Typography color="text.secondary" sx={{ py: 2 }}>
            No analytics data available
          </Typography>
        ) : (
          <Grid container spacing={2}>
            <Grid size={{ xs: 12 }}>
              <Box
                sx={{
                  p: 2,
                  bgcolor: "action.hover",
                  borderRadius: 1,
                  textAlign: "center",
                }}
              >
                <Typography variant="body2" color="text.secondary">
                  Adherence Rate
                </Typography>
                <Typography
                  variant="h4"
                  sx={{ fontWeight: "bold", color: "primary.main" }}
                >
                  {(data.adherenceRate * 100).toFixed(1)}%
                </Typography>
              </Box>
            </Grid>
            <Grid size={{ xs: 6 }}>
              <Box>
                <Typography
                  variant="caption"
                  color="text.secondary"
                  sx={{ display: "block" }}
                >
                  Total Scheduled
                </Typography>
                <Typography variant="body1" sx={{ fontWeight: 500 }}>
                  {data.totalScheduled}
                </Typography>
              </Box>
            </Grid>
            <Grid size={{ xs: 6 }}>
              <Box>
                <Typography
                  variant="caption"
                  color="text.secondary"
                  sx={{ display: "block" }}
                >
                  On Time
                </Typography>
                <Typography variant="body1" sx={{ fontWeight: 500, color: "success.main" }}>
                  {data.onTime}
                </Typography>
              </Box>
            </Grid>
            <Grid size={{ xs: 6 }}>
              <Box>
                <Typography
                  variant="caption"
                  color="text.secondary"
                  sx={{ display: "block" }}
                >
                  Late
                </Typography>
                <Typography variant="body1" sx={{ fontWeight: 500, color: "warning.main" }}>
                  {data.late}
                </Typography>
              </Box>
            </Grid>
            <Grid size={{ xs: 6 }}>
              <Box>
                <Typography
                  variant="caption"
                  color="text.secondary"
                  sx={{ display: "block" }}
                >
                  Missed
                </Typography>
                <Typography variant="body1" sx={{ fontWeight: 500, color: "error.main" }}>
                  {data.missed}
                </Typography>
              </Box>
            </Grid>
          </Grid>
        )}
      </CardContent>
    </Card>
  );
};

export default AnalyticsCard;
