import {
  useCallback,
  useEffect,
  useState,
} from "react";

import {
  Typography,
  Stack,
  Alert as MuiAlert,
  Box,
  Button,
  Pagination,
  Tab,
  Tabs,
} from "@mui/material";

import AlertCard from "../../components/alerts/AlertCard";
import PageError from "../../components/common/PageError";
import PageLoading from "../../components/common/PageLoading";
import PageTitle from "../../components/common/PageTitle";

import {
  alertsService,
  type AlertStatusFilter,
} from "../../services/alertsService";
import type { Alert } from "../../types/alert";

const PAGE_SIZE = 20;

const STATUS_FILTERS: Array<{
  label: string;
  value: AlertStatusFilter;
}> = [
  { label: "All", value: "ALL" },
  { label: "Open", value: "OPEN" },
  {
    label: "Acknowledged",
    value: "ACKNOWLEDGED",
  },
  { label: "Resolved", value: "RESOLVED" },
];

const mapSeverity = (
  severity: string
):
  | "error"
  | "warning"
  | "info"
  | "success" => {
  switch (
    severity?.toUpperCase()
  ) {
    case "CRITICAL":
    case "HIGH":
      return "error";

    case "WARNING":
    case "MEDIUM":
      return "warning";

    case "LOW":
      return "info";

    default:
      return "info";
  }
};

const AlertsPage = () => {
  const [alerts, setAlerts] =
    useState<Alert[]>([]);

  const [status, setStatus] =
    useState<AlertStatusFilter>("ALL");

  const [page, setPage] =
    useState(0);

  const [totalPages, setTotalPages] =
    useState(0);

  const [totalElements, setTotalElements] =
    useState(0);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState("");

  const [actionAlertId, setActionAlertId] =
    useState<number | null>(null);

  const loadAlerts = useCallback(async () => {
    try {
      setLoading(true);
      setError("");

      const data =
        await alertsService.getAlerts(
          page,
          PAGE_SIZE,
          status
        );

      setAlerts(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(
        data.totalElements
      );
    } catch (err) {
      console.error(err);

      setError("Failed to load alerts");
    } finally {
      setLoading(false);
    }
  }, [page, status]);

  useEffect(() => {
    loadAlerts();
  }, [loadAlerts]);

  const handleStatusChange = (
    _event: unknown,
    value: AlertStatusFilter
  ) => {
    setStatus(value);
    setPage(0);
  };

  const handleAcknowledge = async (
    alertId: number
  ) => {
    try {
      setActionAlertId(alertId);
      await alertsService
        .acknowledgeAlert(alertId);
      await loadAlerts();
    } catch (err) {
      console.error(err);
      setError(
        "Failed to update alert"
      );
    } finally {
      setActionAlertId(null);
    }
  };

  const handleResolve = async (
    alertId: number
  ) => {
    try {
      setActionAlertId(alertId);
      await alertsService
        .resolveAlert(alertId);
      await loadAlerts();
    } catch (err) {
      console.error(err);
      setError(
        "Failed to update alert"
      );
    } finally {
      setActionAlertId(null);
    }
  };

  const renderActions = (alert: Alert) => {
    const normalizedStatus =
      alert.status?.toUpperCase();

    if (
      normalizedStatus !== "OPEN" &&
      normalizedStatus !== "ACKNOWLEDGED"
    ) {
      return null;
    }

    return (
      <Stack
        direction="row"
        spacing={1}
      >
        {normalizedStatus === "OPEN" && (
          <Button
            size="small"
            onClick={() =>
              handleAcknowledge(
                alert.alertId
              )
            }
            disabled={
              actionAlertId ===
              alert.alertId
            }
          >
            Acknowledge
          </Button>
        )}

        <Button
          size="small"
          color="success"
          onClick={() =>
            handleResolve(
              alert.alertId
            )
          }
          disabled={
            actionAlertId ===
            alert.alertId
          }
        >
          Resolve
        </Button>
      </Stack>
    );
  };

  if (loading) {
    return <PageLoading minHeight="50vh" />;
  }

  if (error) {
    return <PageError message={error} />;
  }

  return (
    <>
      <PageTitle mb={2}>Alerts</PageTitle>

      <Box sx={{ mb: 3 }}>
        <Tabs
          value={status}
          onChange={
            handleStatusChange
          }
        >
          {STATUS_FILTERS.map(
            (filter) => (
              <Tab
                key={filter.value}
                label={filter.label}
                value={filter.value}
              />
            )
          )}
        </Tabs>
      </Box>

      <Stack spacing={2}>
        {alerts.length === 0 ? (
          <MuiAlert severity="info">
            No alerts available.
          </MuiAlert>
        ) : (
          alerts.map((alert) => (
            <AlertCard
              key={alert.alertId}
              severity={mapSeverity(
                alert.severity
              )}
              title={alert.title}
              description={
                alert.message
              }
              status={alert.status}
              createdAt={
                alert.createdAt
              }
              action={renderActions(
                alert
              )}
            />
          ))
        )}
      </Stack>

      {totalPages > 1 && (
        <Box
          sx={{
            mt: 3,
            display: "flex",
            alignItems: "center",
            justifyContent:
              "space-between",
            gap: 2,
            flexWrap: "wrap",
          }}
        >
          <Typography
            variant="body2"
            color="text.secondary"
          >
            {totalElements} alerts
          </Typography>

          <Pagination
            count={totalPages}
            page={page + 1}
            onChange={(
              _event,
              value
            ) => setPage(value - 1)}
            color="primary"
          />
        </Box>
      )}
    </>
  );
};

export default AlertsPage;
