import { useEffect, useState } from "react";

import {
  Typography,
  Grid,
  CircularProgress,
  Alert,
  Box,
  Card,
  CardContent,
} from "@mui/material";

import StatCard from "../../components/dashboard/StatCard";
import GlucoseChart from "../../components/charts/GlucoseChart";

import { analyticsService } from "../../services/analyticsService";

import type {
  AnalyticsData,
  GlucoseReading,
} from "../../types/analytics";

const AnalyticsPage = () => {
  const [analytics, setAnalytics] =
    useState<AnalyticsData | null>(
      null
    );

  const [glucoseHistory, setGlucoseHistory] =
    useState<
      {
        date: string;
        glucose: number;
      }[]
    >([]);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState("");

  useEffect(() => {
    const loadAnalytics =
      async () => {
        try {
          const analyticsData =
            await analyticsService.getAnalytics();

          const glucoseData =
            await analyticsService.getGlucoseHistory();

          setAnalytics(
            analyticsData
          );

          setGlucoseHistory(
            glucoseData.map(
              (
                item: GlucoseReading
              ) => ({
                date: new Date(
                  item.measuredAt
                ).toLocaleDateString(),

                glucose:
                  item.glucoseValueMgDl,
              })
            )
          );
        } catch (err) {
          console.error(err);

          setError(
            "Failed to load analytics"
          );
        } finally {
          setLoading(false);
        }
      };

    loadAnalytics();
  }, []);

  if (loading) {
    return (
      <Box
        sx={{
          display: "flex",
          justifyContent:
            "center",
          alignItems: "center",
          minHeight: "300px",
        }}
      >
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error">
        {error}
      </Alert>
    );
  }

  if (!analytics) {
    return null;
  }

  return (
    <>
      <Typography
        variant="h4"
        sx={{ mb: 3 }}
      >
        Analytics
      </Typography>

      <Grid
        container
        spacing={3}
      >
        <Grid
          size={{
            xs: 12,
            md: 4,
          }}
        >
          <StatCard
            title="Adherence Rate"
            value={`${analytics.adherenceRate}%`}
          />
        </Grid>

        <Grid
          size={{
            xs: 12,
            md: 4,
          }}
        >
          <StatCard
            title="On Time Doses"
            value={String(
              analytics.onTime
            )}
          />
        </Grid>

        <Grid
          size={{
            xs: 12,
            md: 4,
          }}
        >
          <StatCard
            title="Late Doses"
            value={String(
              analytics.late
            )}
          />
        </Grid>

        <Grid
          size={{
            xs: 12,
            md: 4,
          }}
        >
          <StatCard
            title="Missed Doses"
            value={String(
              analytics.missed
            )}
          />
        </Grid>

        <Grid
          size={{
            xs: 12,
            md: 4,
          }}
        >
          <StatCard
            title="Unscheduled Doses"
            value={String(
              analytics.unscheduled
            )}
          />
        </Grid>

        <Grid
          size={{
            xs: 12,
            md: 4,
          }}
        >
          <StatCard
            title="Total Scheduled"
            value={String(
              analytics.totalScheduled
            )}
          />
        </Grid>
      </Grid>

      <Card sx={{ mt: 4 }}>
        <CardContent>
          <Typography
            variant="h6"
            sx={{ mb: 2 }}
          >
            Glucose Trend
          </Typography>

          <GlucoseChart
            data={glucoseHistory}
          />
        </CardContent>
      </Card>
    </>
  );
};

export default AnalyticsPage;