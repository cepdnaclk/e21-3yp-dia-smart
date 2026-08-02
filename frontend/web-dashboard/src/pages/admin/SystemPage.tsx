import { useState, useEffect } from "react";
import { Grid, Box, Alert } from "@mui/material";

import PageTitle from "../../components/common/PageTitle";

import AuditLogsSection from "../../components/admin/AuditLogsSection";
import SystemHealthSection from "../../components/admin/SystemHealthSection";

import { adminService } from "../../services/adminService";
import type { AuditLogRecord } from "../../types/admin";

import { useAutoRefresh } from "../../hooks/useAutoRefresh";

const SystemPage = () => {
  const [logs, setLogs] = useState<AuditLogRecord[]>([]);
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const rowsPerPage = 10;

  const fetchLogs = async (currentPage: number, silent = false) => {
    if (!silent) {
      setLoading(true);
      setError(null);
    }
    try {
      const response = await adminService.getAuditLogs(currentPage, rowsPerPage);
      setLogs(response?.content ?? []);
      setTotalElements(response?.totalElements ?? 0);
    } catch (err: any) {
      console.error(err);
      if (!silent) {
        setError("Failed to fetch administrative audit logs.");
      }
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  };

  useEffect(() => {
    fetchLogs(page, false);
  }, [page]);

  useAutoRefresh(() => fetchLogs(page, true), 5000);

  const handlePageChange = (_: any, newPage: number) => {
    setPage(newPage);
  };

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>System Telemetry & Audit Logs</PageTitle>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 7 }}>
          <AuditLogsSection
            logs={logs}
            loading={loading}
            page={page}
            totalElements={totalElements}
            rowsPerPage={rowsPerPage}
            onPageChange={handlePageChange}
          />
        </Grid>

        <Grid size={{ xs: 12, md: 5 }}>
          <SystemHealthSection />
        </Grid>
      </Grid>
    </Box>
  );
};

export default SystemPage;
