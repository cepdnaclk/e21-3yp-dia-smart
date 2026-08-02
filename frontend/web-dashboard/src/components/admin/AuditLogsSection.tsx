import {
  Card,
  CardContent,
  Typography,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  CircularProgress,
  TablePagination,
  Box
} from "@mui/material";
import type { AuditLogRecord } from "../../types/admin";

interface AuditLogsSectionProps {
  logs: AuditLogRecord[];
  loading: boolean;
  page: number;
  totalElements: number;
  rowsPerPage: number;
  onPageChange: (event: any, newPage: number) => void;
}

const AuditLogsSection: React.FC<AuditLogsSectionProps> = ({
  logs,
  loading,
  page,
  totalElements,
  rowsPerPage,
  onPageChange
}) => {
  return (
    <Card elevation={2} sx={{ borderRadius: 3, height: "100%" }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
        <Typography variant="h6" sx={{ fontWeight: "bold" }}>
          Administrative Audit Logs
        </Typography>

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 8 }}>
            <CircularProgress />
          </Box>
        ) : (
          <>
            <TableContainer sx={{ maxHeight: 440 }}>
              <Table stickyHeader size="small">
                <TableHead>
                  <TableRow>
                    <TableCell sx={{ fontWeight: "bold" }}>Action</TableCell>
                    <TableCell sx={{ fontWeight: "bold" }}>Entity</TableCell>
                    <TableCell sx={{ fontWeight: "bold" }}>IP Address</TableCell>
                    <TableCell sx={{ fontWeight: "bold" }}>Timestamp</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {logs.map((log) => (
                    <TableRow key={log.auditLogId} hover>
                      <TableCell sx={{ fontWeight: "medium" }}>{log.actionType}</TableCell>
                      <TableCell>
                        {log.entityType} (ID: {log.entityId || "N/A"})
                      </TableCell>
                      <TableCell>{log.ipAddress || "Localhost"}</TableCell>
                      <TableCell>{new Date(log.createdAt).toLocaleString()}</TableCell>
                    </TableRow>
                  ))}

                  {logs.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={4} align="center" sx={{ py: 3, color: "text.secondary" }}>
                        No audit records registered.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>

            <TablePagination
              component="div"
              count={totalElements}
              page={page}
              onPageChange={onPageChange}
              rowsPerPage={rowsPerPage}
              rowsPerPageOptions={[rowsPerPage]}
            />
          </>
        )}
      </CardContent>
    </Card>
  );
};

export default AuditLogsSection;
