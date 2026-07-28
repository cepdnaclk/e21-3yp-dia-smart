import { useState, useEffect } from "react";
import { Card, CardContent, Typography, Box, CircularProgress, LinearProgress, Chip } from "@mui/material";
import ScaleIcon from "@mui/icons-material/Scale";
import MedicationIcon from "@mui/icons-material/Medication";

import { caregiverService } from "../../services/caregiverService";

interface InventoryMonitoringCardProps {
  patientId: number;
  refreshTrigger?: number;
}

const InventoryMonitoringCard = ({ patientId, refreshTrigger }: InventoryMonitoringCardProps) => {
  const [loading, setLoading] = useState(true);
  const [reading, setReading] = useState<any>(null);

  useEffect(() => {
    if (!patientId) return;
    const fetchInventory = async (silent = false) => {
      try {
        if (!silent) setLoading(true);
        const data = await caregiverService.getLatestInventoryReading(patientId);
        setReading(data);
      } catch (err) {
        console.error("Failed to load inventory readings", err);
      } finally {
        if (!silent) setLoading(false);
      }
    };

    const isInitial = !refreshTrigger || refreshTrigger === 0;
    fetchInventory(!isInitial);
  }, [patientId, refreshTrigger]);

  const percent = reading ? reading.estimatedRemainingPercent || 0 : 0;
  const isLow = reading ? reading.inventoryStatus === "LOW_INVENTORY" || reading.inventoryStatus === "CRITICAL_INVENTORY" : false;

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Insulin Cartridge Inventory
        </Typography>

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 4, flexGrow: 1, alignItems: "center" }}>
            <CircularProgress size={32} />
          </Box>
        ) : !reading ? (
          <Typography color="text.secondary" variant="body2" sx={{ py: 2 }}>
            No cartridge inventory readings received from patient.
          </Typography>
        ) : (
          <Box sx={{ flexGrow: 1, display: "flex", flexDirection: "column", justifyContent: "center", gap: 2 }}>
            <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
                <MedicationIcon color={isLow ? "error" : "primary"} fontSize="large" />
                <Box>
                  <Typography variant="h5" sx={{ fontWeight: "bold" }}>
                    {reading.estimatedUnitsRemaining.toFixed(0)} Units
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Remaining Stock ({percent.toFixed(0)}%)
                  </Typography>
                </Box>
              </Box>
              <Chip
                label={reading.inventoryStatus || "NORMAL"}
                color={isLow ? "error" : "success"}
                size="small"
                variant="outlined"
              />
            </Box>

            <Box sx={{ width: "100%", mr: 1 }}>
              <LinearProgress
                variant="determinate"
                value={percent}
                color={isLow ? "error" : "success"}
                sx={{ height: 10, borderRadius: 5 }}
              />
            </Box>

            <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 2, mt: 1, bgcolor: "action.hover", p: 1.5, borderRadius: 2 }}>
              <Box>
                <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>Cartridge State</Typography>
                <Typography variant="body2" sx={{ fontWeight: "medium" }}>
                  {reading.cartridgePresent ? "Inserted" : "Missing"}
                </Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>Pen State</Typography>
                <Typography variant="body2" sx={{ fontWeight: "medium" }}>
                  {reading.penPresent ? "Docked" : "Undocked"}
                </Typography>
              </Box>
              <Box sx={{ gridColumn: "span 2", display: "flex", alignItems: "center", gap: 0.5, borderTop: "1px solid", borderColor: "divider", pt: 1 }}>
                <ScaleIcon fontSize="inherit" color="action" />
                <Typography variant="caption" color="text.secondary">
                  Current Cartridge Weight: <strong>{reading.weightG.toFixed(1)} g</strong>
                </Typography>
              </Box>
            </Box>
          </Box>
        )}
      </CardContent>
    </Card>
  );
};

export default InventoryMonitoringCard;
