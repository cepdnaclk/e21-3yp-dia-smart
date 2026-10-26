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
  Box,
  Chip
} from "@mui/material";
import ReceiptLongOutlinedIcon from "@mui/icons-material/ReceiptLongOutlined";
import type { AuditLogRecord } from "../../types/admin";
import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";

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
    <Card sx={{ height: "100%" }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 1 }}>
        <CardHeading
          title="Administrative Audit Logs"
          icon={<ReceiptLongOutlinedIcon />}
          color="#12233b"
          sx={{ mb: 1.5 }}
        />

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 8 }}>
            <CircularProgress size={36} thickness={4} sx={{ color: "secondary.dark" }} />
          </Box>
        ) : (
          <>
            <TableContainer
              sx={{
                maxHeight: 440,
                overflowX: "auto",
                border: "1px solid",
                borderColor: "divider",
              }}
            >
              <Table stickyHeader size="small" sx={{ minWidth: 520 }}>
                <TableHead>
                  <TableRow>
                    <TableCell>Action</TableCell>
                    <TableCell>Entity</TableCell>
                    <TableCell>IP Address</TableCell>
                    <TableCell>Timestamp</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {logs.map((log) => (
                    <TableRow key={log.auditLogId} hover>
                      <TableCell>
                        <Chip
                          label={log.actionType}
                          size="small"
                          variant="outlined"
                          sx={{ color: "#12233b", bgcolor: "#f1f5f9", borderColor: "#e2e8f0" }}
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" sx={{ fontWeight: 500 }}>
                          {log.entityType} (ID: {log.entityId || "N/A"})
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography
                          variant="body2"
                          color="text.secondary"
                          sx={{ fontFamily: "monospace", fontSize: "0.8rem" }}
                        >
                          {log.ipAddress || "Localhost"}
                        </Typography>
                      </TableCell>
                      <TableCell sx={{ whiteSpace: "nowrap" }}>
                        <Typography variant="body2" color="text.secondary">
                          {new Date(log.createdAt).toLocaleString()}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ))}

                  {logs.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={4} sx={{ py: 2 }}>
                        <EmptyState
                          message="No audit records registered."
                          icon={<ReceiptLongOutlinedIcon />}
                          minHeight={120}
                        />
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
