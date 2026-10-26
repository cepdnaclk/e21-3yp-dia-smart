import type { ReactNode } from "react";
import { Box, Card, CardContent, Typography } from "@mui/material";
import { alpha, useTheme } from "@mui/material/styles";

export type MetricTone = "primary" | "info" | "success" | "warning" | "error";

interface MetricCardProps {
  label: string;
  value: ReactNode;
  description?: ReactNode;
  icon: ReactNode;
  tone?: MetricTone;
}

/**
 * Purely presentational KPI tile used by dashboard summary rows.
 * It renders the values it is given and holds no data logic.
 */
const MetricCard = ({ label, value, description, icon, tone = "info" }: MetricCardProps) => {
  const theme = useTheme();
  const accent = tone === "primary" ? theme.palette.primary.main : theme.palette[tone].main;
  const iconColor = tone === "primary" || tone === "info" ? accent : theme.palette[tone].dark;

  return (
    <Card
      sx={{
        height: "100%",
        position: "relative",
        overflow: "hidden",
        transition: "transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease",
        "&:hover": {
          transform: "translateY(-2px)",
          borderColor: alpha(accent, 0.3),
          boxShadow: `0 12px 28px ${alpha(accent, 0.1)}`,
        },
      }}
    >
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 1.5, height: "100%" }}>
        <Box sx={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", gap: 2 }}>
          <Typography
            variant="subtitle2"
            sx={{ color: "text.secondary", fontWeight: 600, lineHeight: 1.35, pt: 0.5 }}
          >
            {label}
          </Typography>
          <Box
            aria-hidden
            sx={{
              width: 42,
              height: 42,
              flexShrink: 0,
              borderRadius: 2.5,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              color: iconColor,
              backgroundColor: alpha(accent, 0.1),
              boxShadow: `inset 0 0 0 1px ${alpha(accent, 0.14)}`,
              "& .MuiSvgIcon-root": { fontSize: 22 },
            }}
          >
            {icon}
          </Box>
        </Box>

        <Box sx={{ fontSize: { xs: "1.75rem", sm: "2rem" }, fontWeight: 800, color: "primary.main", lineHeight: 1.15, letterSpacing: "-0.02em" }}>
          {value}
        </Box>

        {description && (
          <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.5 }}>
            {description}
          </Typography>
        )}
      </CardContent>
    </Card>
  );
};

export default MetricCard;
