import { Card, CardContent, Typography, Box, List, ListItem, Chip } from "@mui/material";
import { alpha } from "@mui/material/styles";
import AcUnitRoundedIcon from "@mui/icons-material/AcUnitRounded";
import ThermostatRoundedIcon from "@mui/icons-material/ThermostatRounded";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";

import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";
import type { Alert } from "../../types/alert";

interface StorageWarningsProps {
  warnings: Alert[];
}

const CRITICAL = "#ef4444";
const WARNING = "#f59e0b";

const StorageWarnings: React.FC<StorageWarningsProps> = ({ warnings }) => {
  return (
    <Card sx={{ height: "100%" }}>
      <CardContent sx={{ display: "flex", flexDirection: "column" }}>
        <CardHeading title="Storage Warnings" icon={<AcUnitRoundedIcon />} color="#0ea5e9" />

        <List sx={{ display: "flex", flexDirection: "column", gap: 1.25, p: 0 }}>
          {warnings.map((warn) => {
            const accent = warn.severity === "CRITICAL" ? CRITICAL : WARNING;
            return (
              <ListItem
                key={warn.alertId}
                sx={{
                  p: 1.5,
                  bgcolor: alpha(accent, 0.05),
                  border: "1px solid",
                  borderColor: alpha(accent, 0.2),
                  borderLeft: `3px solid ${accent}`,
                  borderRadius: 2,
                  alignItems: "flex-start",
                  gap: 1.25,
                }}
              >
                <ThermostatRoundedIcon sx={{ color: accent, fontSize: 20, mt: 0.25, flexShrink: 0 }} />
                <Box sx={{ display: "flex", flexDirection: "column", gap: 0.5, minWidth: 0, flexGrow: 1 }}>
                  <Box sx={{ display: "flex", justifyContent: "space-between", width: "100%", alignItems: "center", gap: 1, flexWrap: "wrap" }}>
                    <Typography variant="subtitle2" sx={{ fontWeight: 700, color: "text.primary", overflowWrap: "anywhere" }}>
                      {warn.title}
                    </Typography>
                    <Chip
                      label={warn.severity}
                      size="small"
                      color={warn.severity === "CRITICAL" ? "error" : "warning"}
                      sx={{ fontSize: 10, fontWeight: 700, height: 20 }}
                    />
                  </Box>
                  <Typography variant="caption" sx={{ color: "text.secondary", overflowWrap: "anywhere" }}>
                    {warn.message}
                  </Typography>
                </Box>
              </ListItem>
            );
          })}

          {warnings.length === 0 && (
            <EmptyState
              message="All insulin storage containers are within normal temperature and volume thresholds."
              icon={<Inventory2OutlinedIcon sx={{ color: "#0ea5e9" }} />}
              minHeight={140}
            />
          )}
        </List>
      </CardContent>
    </Card>
  );
};

export default StorageWarnings;
