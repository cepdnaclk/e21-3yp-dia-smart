import {
  Card,
  CardContent,
  Typography,
  Box,
} from "@mui/material";
import { alpha } from "@mui/material/styles";
import WaterDropIcon from "@mui/icons-material/WaterDrop";
import ThermostatIcon from "@mui/icons-material/Thermostat";
import InventoryIcon from "@mui/icons-material/Inventory";
import VaccinesIcon from "@mui/icons-material/Vaccines";

// Tolerates non-hex colour values (e.g. palette keys) by falling back to slate.
const tint = (color: string | undefined, opacity: number) => {
  try {
    return alpha(color || "#64748b", opacity);
  } catch {
    return alpha("#64748b", opacity);
  }
};

interface Props {
  title: string;
  value: string;
  statusText?: string;
  statusColor?: string;
}

const StatCard = ({ title, value, statusText, statusColor }: Props) => {
  // Determine icon and theme color based on metric title
  const getCardTheme = () => {
    switch (title.toLowerCase()) {
      case "glucose":
        return {
          icon: <WaterDropIcon sx={{ fontSize: 24 }} />,
          color: "#ef4444", // Red for blood glucose
          bgColor: "#fef2f2",
          statusText: "In Range",
          statusColor: "#10b981",
        };
      case "temperature":
        return {
          icon: <ThermostatIcon sx={{ fontSize: 24 }} />,
          color: "#3ec1fa", // Blue for temperature
          bgColor: "#e0f2fe",
          statusText: "Normal Range",
          statusColor: "#10b981",
        };
      case "inventory":
        return {
          icon: <InventoryIcon sx={{ fontSize: 24 }} />,
          color: "#f59e0b", // Amber for inventory levels
          bgColor: "#fffbeb",
          statusText: "Adequate Level",
          statusColor: "#10b981",
        };
      case "last dose":
      case "lastdose":
        return {
          icon: <VaccinesIcon sx={{ fontSize: 24 }} />,
          color: "#10b981", // Emerald for insulin doses
          bgColor: "#e6f7ed",
          statusText: "Recent",
          statusColor: "#64748b",
        };
      default:
        return {
          icon: <WaterDropIcon sx={{ fontSize: 24 }} />,
          color: "#3ec1fa",
          bgColor: "#f8f9fa",
          statusText: "Updated",
          statusColor: "#64748b",
        };
    }
  };

  const themeConfig = getCardTheme();
  const displayStatusText = statusText !== undefined ? statusText : themeConfig.statusText;
  const displayStatusColor = statusColor !== undefined ? statusColor : themeConfig.statusColor;

  return (
    <Card
      sx={{
        height: "100%",
        overflow: "hidden",
        position: "relative",
        transition: "transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease",
        "&:hover": {
          transform: "translateY(-2px)",
          borderColor: alpha(themeConfig.color, 0.35),
          boxShadow: `0 12px 28px ${alpha(themeConfig.color, 0.12)}`,
        },
      }}
    >
      {/* Soft metric-tinted corner glow */}
      <Box
        aria-hidden
        sx={{
          position: "absolute",
          top: -48,
          right: -48,
          width: 140,
          height: 140,
          borderRadius: "50%",
          background: `radial-gradient(circle, ${alpha(themeConfig.color, 0.12)} 0%, transparent 70%)`,
          pointerEvents: "none",
        }}
      />

      <CardContent sx={{ p: { xs: 2.5, sm: 3 }, position: "relative" }}>
        <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, mb: 2.5 }}>
          {/* Metric-specific styled icon container */}
          <Box
            sx={{
              width: 44,
              height: 44,
              borderRadius: 2.5,
              backgroundColor: themeConfig.bgColor,
              color: themeConfig.color,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              flexShrink: 0,
              boxShadow: `inset 0 0 0 1px ${alpha(themeConfig.color, 0.15)}`,
            }}
          >
            {themeConfig.icon}
          </Box>

          <Typography
            variant="body2"
            sx={{ fontWeight: 700, color: "text.secondary", textTransform: "uppercase", letterSpacing: "0.06em", fontSize: "0.78rem" }}
          >
            {title}
          </Typography>
        </Box>

        <Typography
          variant="h4"
          sx={{ fontWeight: 800, color: "#12233b", mb: 1.5, letterSpacing: "-0.5px", fontSize: { xs: "1.75rem", sm: "2rem" }, lineHeight: 1.15 }}
        >
          {value}
        </Typography>

        {/* Status pill: coloured dot plus explicit text (never colour alone) */}
        {displayStatusText && (
          <Box
            sx={{
              display: "inline-flex",
              alignItems: "center",
              gap: 0.75,
              px: 1.25,
              py: 0.4,
              borderRadius: 999,
              backgroundColor: tint(displayStatusColor, 0.1),
              maxWidth: "100%",
            }}
          >
            <Box
              sx={{
                width: 7,
                height: 7,
                borderRadius: "50%",
                backgroundColor: displayStatusColor,
                flexShrink: 0,
              }}
            />
            <Typography variant="caption" sx={{ color: "text.primary", fontWeight: 600 }} noWrap>
              {displayStatusText}
            </Typography>
          </Box>
        )}
      </CardContent>
    </Card>
  );
};

export default StatCard;
