import { Box, Chip, Paper, Stack, Typography } from "@mui/material";
import { alpha } from "@mui/material/styles";
import type { ReactNode } from "react";
import ErrorOutlineRoundedIcon from "@mui/icons-material/ErrorOutlineRounded";
import WarningAmberRoundedIcon from "@mui/icons-material/WarningAmberRounded";
import InfoOutlinedIcon from "@mui/icons-material/InfoOutlined";
import CheckCircleOutlineRoundedIcon from "@mui/icons-material/CheckCircleOutlineRounded";
import ScheduleRoundedIcon from "@mui/icons-material/ScheduleRounded";

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
  // Determine severity accent colours and icon
  const getSeverityStyles = () => {
    switch (severity) {
      case "error":
        return { border: "#ef4444", iconColor: "#dc2626", icon: <ErrorOutlineRoundedIcon /> };
      case "warning":
        return { border: "#f59e0b", iconColor: "#b45309", icon: <WarningAmberRoundedIcon /> };
      case "success":
        return { border: "#10b981", iconColor: "#047857", icon: <CheckCircleOutlineRoundedIcon /> };
      case "info":
      default:
        return { border: "#3ec1fa", iconColor: "#0284c7", icon: <InfoOutlinedIcon /> };
    }
  };

  const styleConfig = getSeverityStyles();

  const getStatusStyles = (value: string) => {
    switch (value.toUpperCase()) {
      case "OPEN":
        return { bg: alpha("#ef4444", 0.1), color: "#b91c1c", border: alpha("#ef4444", 0.25) };
      case "ACKNOWLEDGED":
        return { bg: alpha("#f59e0b", 0.12), color: "#b45309", border: alpha("#f59e0b", 0.3) };
      case "RESOLVED":
        return { bg: alpha("#10b981", 0.1), color: "#047857", border: alpha("#10b981", 0.25) };
      default:
        return { bg: alpha("#64748b", 0.08), color: "#475569", border: alpha("#64748b", 0.2) };
    }
  };

  const statusStyles = status ? getStatusStyles(status) : null;

  return (
    <Paper
      role="alert"
      variant="outlined"
      sx={{
        position: "relative",
        overflow: "hidden",
        borderRadius: 3,
        borderColor: "divider",
        backgroundColor: "#ffffff",
        boxShadow: "0 1px 2px rgba(15, 23, 42, 0.04)",
        transition: "box-shadow 0.2s ease, border-color 0.2s ease",
        "&:hover": {
          borderColor: alpha(styleConfig.border, 0.4),
          boxShadow: `0 8px 20px ${alpha(styleConfig.border, 0.08)}`,
        },
        "&::before": {
          content: '""',
          position: "absolute",
          left: 0,
          top: 0,
          bottom: 0,
          width: 4,
          backgroundColor: styleConfig.border,
        },
      }}
    >
      <Box
        sx={{
          display: "flex",
          gap: { xs: 1.5, sm: 2 },
          alignItems: "flex-start",
          p: { xs: 2, sm: 2.5 },
          pl: { xs: 2.5, sm: 3 },
        }}
      >
        <Box
          role="img"
          aria-label={`${severity} severity`}
          sx={{
            width: 40,
            height: 40,
            flexShrink: 0,
            borderRadius: 2,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            color: styleConfig.iconColor,
            backgroundColor: alpha(styleConfig.border, 0.12),
            "& .MuiSvgIcon-root": { fontSize: 22 },
          }}
        >
          {styleConfig.icon}
        </Box>

        <Stack spacing={1} sx={{ flex: 1, minWidth: 0 }}>
          <Box sx={{ minWidth: 0 }}>
            <Typography
              component="div"
              sx={{
                fontWeight: 700,
                color: "primary.main",
                fontSize: "0.975rem",
                lineHeight: 1.35,
                mb: 0.5,
                overflowWrap: "anywhere",
              }}
            >
              {title}
            </Typography>
            <Typography
              variant="body2"
              color="text.secondary"
              sx={{ fontSize: "0.875rem", lineHeight: 1.55, overflowWrap: "anywhere" }}
            >
              {description}
            </Typography>
          </Box>

          {(status || createdAt || action) && (
            <Box
              sx={{
                display: "flex",
                alignItems: { xs: "flex-start", sm: "center" },
                justifyContent: "space-between",
                flexDirection: { xs: "column", sm: "row" },
                gap: 1.5,
                pt: 0.5,
              }}
            >
              <Stack
                direction="row"
                sx={{
                  alignItems: "center",
                  flexWrap: "wrap",
                  columnGap: 1.5,
                  rowGap: 1,
                }}
              >
                {status && statusStyles && (
                  <Chip
                    size="small"
                    label={status}
                    sx={{
                      fontWeight: 700,
                      fontSize: "0.68rem",
                      backgroundColor: statusStyles.bg,
                      color: statusStyles.color,
                      border: `1px solid ${statusStyles.border}`,
                      borderRadius: 999,
                      height: 22,
                      textTransform: "uppercase",
                      letterSpacing: "0.5px",
                    }}
                  />
                )}

                {createdAt && (
                  <Box
                    sx={{
                      display: "inline-flex",
                      alignItems: "center",
                      gap: 0.5,
                      color: "text.secondary",
                    }}
                  >
                    <ScheduleRoundedIcon aria-hidden sx={{ fontSize: 15 }} />
                    <Typography
                      variant="caption"
                      color="text.secondary"
                      sx={{ fontWeight: 500 }}
                    >
                      {new Date(createdAt).toLocaleString([], { hour: '2-digit', minute: '2-digit', month: 'short', day: 'numeric' })}
                    </Typography>
                  </Box>
                )}
              </Stack>

              {action && (
                <Box
                  sx={{
                    display: "flex",
                    flexWrap: "wrap",
                    justifyContent: "flex-end",
                    gap: 1,
                    alignSelf: { xs: "stretch", sm: "auto" },
                  }}
                >
                  {action}
                </Box>
              )}
            </Box>
          )}
        </Stack>
      </Box>
    </Paper>
  );
};

export default AlertCard;
