import {
  Alert,
  AlertTitle,
  Box,
  Chip,
  Stack,
  Typography,
} from "@mui/material";
import type { ReactNode } from "react";

interface AlertCardProps {
  severity: "error" | "warning" | "info" | "success";
  title: string;
  description: string;
  status?: string;
  createdAt?: string;
  action?: ReactNode;
}

const AlertCard = ({
  severity,
  title,
  description,
  status,
  createdAt,
  action,
}: AlertCardProps) => {
  // Determine severity boundary colors
  const getSeverityStyles = () => {
    switch (severity) {
      case "error":
        return { border: "#ef4444", iconColor: "#ef4444" };
      case "warning":
        return { border: "#f59e0b", iconColor: "#f59e0b" };
      case "success":
        return { border: "#10b981", iconColor: "#10b981" };
      case "info":
      default:
        return { border: "#3ec1fa", iconColor: "#3ec1fa" };
    }
  };

  const styleConfig = getSeverityStyles();

  return (
    <Alert
      severity={severity}
      sx={{
        backgroundColor: "#ffffff !important",
        color: "#1e293b",
        border: "1px solid #e2e8f0",
        borderLeft: `5px solid ${styleConfig.border}`,
        borderRadius: 3,
        boxShadow: "0px 4px 12px rgba(0, 0, 0, 0.015)",
        p: 2,
        alignItems: "flex-start",
        "& .MuiAlert-icon": {
          color: styleConfig.iconColor,
          pt: 0.5,
        },
        "& .MuiAlert-message": {
          width: "100%",
        },
      }}
    >
      <Stack spacing={1} sx={{ width: "100%" }}>
        <Box sx={{ width: "100%" }}>
          <AlertTitle sx={{ fontWeight: 700, color: "#12233b", fontSize: "0.95rem", mb: 0.5 }}>
            {title}
          </AlertTitle>
          <Typography variant="body2" color="text.secondary" sx={{ fontSize: "0.875rem", lineHeight: 1.5 }}>
            {description}
          </Typography>
        </Box>

        {(status || createdAt) && (
          <Stack
            direction="row"
            spacing={1.5}
            sx={{
              alignItems: "center",
              flexWrap: "wrap",
              mt: 1,
            }}
          >
            {status && (
              <Chip
                size="small"
                label={status}
                sx={{
                  fontWeight: 700,
                  fontSize: "0.7rem",
                  backgroundColor: status.toUpperCase() === "OPEN" ? "rgba(239, 68, 68, 0.1)" : "rgba(100, 116, 139, 0.08)",
                  color: status.toUpperCase() === "OPEN" ? "#ef4444" : "#64748b",
                  borderRadius: 1.5,
                  height: 20,
                  textTransform: "uppercase",
                  letterSpacing: "0.5px",
                }}
              />
            )}

            {createdAt && (
              <Typography
                variant="caption"
                color="text.secondary"
                sx={{ fontWeight: 500 }}
              >
                {new Date(createdAt).toLocaleString([], { hour: '2-digit', minute: '2-digit', month: 'short', day: 'numeric' })}
              </Typography>
            )}
          </Stack>
        )}

        {action && (
          <Box sx={{ mt: 1.5, display: "flex", justifyContent: "flex-end", width: "100%", gap: 1 }}>
            {action}
          </Box>
        )}
      </Stack>
    </Alert>
  );
};

export default AlertCard;
