import {
  Card,
  CardContent,
  Typography,
  Box,
} from "@mui/material";
import WaterDropIcon from "@mui/icons-material/WaterDrop";
import ThermostatIcon from "@mui/icons-material/Thermostat";
import InventoryIcon from "@mui/icons-material/Inventory";
import VaccinesIcon from "@mui/icons-material/Vaccines";

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
        overflow: "hidden",
        position: "relative",
        transition: "transform 0.2s, box-shadow 0.2s",
        "&:hover": {
          transform: "translateY(-4px)",
          boxShadow: "0px 10px 25px rgba(0, 0, 0, 0.06)",
        },
      }}
    >
      {/* Dynamic top boundary accent line */}
      <Box
        sx={{
          height: 4,
          backgroundColor: themeConfig.color,
          width: "100%",
        }}
      />

      <CardContent sx={{ p: 3 }}>
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", mb: 2 }}>
          <Typography
            variant="body2"
            sx={{ fontWeight: 700, color: "text.secondary", textTransform: "uppercase", letterSpacing: "0.5px" }}
          >
            {title}
          </Typography>

          {/* Metric-specific styled icon container */}
          <Box
            sx={{
              width: 42,
              height: 42,
              borderRadius: 3,
              backgroundColor: themeConfig.bgColor,
              color: themeConfig.color,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            {themeConfig.icon}
          </Box>
        </Box>

        <Typography
          variant="h4"
          sx={{ fontWeight: 800, color: "#12233b", mb: 1, letterSpacing: "-0.5px" }}
        >
          {value}
        </Typography>

        {/* Small Adherence status chip at the bottom */}
        {displayStatusText && (
          <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
            <Box
              sx={{
                width: 6,
                height: 6,
                borderRadius: "50%",
                backgroundColor: displayStatusColor,
              }}
            />
            <Typography variant="caption" sx={{ color: "text.secondary", fontWeight: 600 }}>
              {displayStatusText}
            </Typography>
          </Box>
        )}
      </CardContent>
    </Card>
  );
};

export default StatCard;