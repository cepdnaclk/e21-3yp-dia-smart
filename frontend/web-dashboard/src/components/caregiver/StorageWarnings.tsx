import { Card, CardContent, Typography, Box, List, ListItem, Chip } from "@mui/material";
import type { Alert } from "../../types/alert";

interface StorageWarningsProps {
  warnings: Alert[];
}

const StorageWarnings: React.FC<StorageWarningsProps> = ({ warnings }) => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 3 }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
        <Typography variant="h6" sx={{ fontWeight: "bold" }}>
          Storage Warnings
        </Typography>

        <List sx={{ display: "flex", flexDirection: "column", gap: 1.5, p: 0 }}>
          {warnings.map((warn) => (
            <ListItem
              key={warn.alertId}
              sx={{
                p: 1.5,
                bgcolor: warn.severity === "CRITICAL" ? "error.light" : "warning.light",
                color: warn.severity === "CRITICAL" ? "error.dark" : "warning.dark",
                borderRadius: 2,
                flexDirection: "column",
                alignItems: "flex-start",
                gap: 0.5,
                opacity: 0.95
              }}
            >
              <Box sx={{ display: "flex", justifyContent: "space-between", width: "100%", alignItems: "center" }}>
                <Typography variant="subtitle2" sx={{ fontWeight: "bold" }}>
                  {warn.title}
                </Typography>
                <Chip
                  label={warn.severity}
                  size="small"
                  color={warn.severity === "CRITICAL" ? "error" : "warning"}
                  sx={{ fontSize: 10, fontWeight: "bold", height: 20 }}
                />
              </Box>
              <Typography variant="caption" sx={{ color: warn.severity === "CRITICAL" ? "error.dark" : "warning.dark" }}>
                {warn.message}
              </Typography>
            </ListItem>
          ))}

          {warnings.length === 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ textAlign: "center", py: 3 }}>
              All insulin storage containers are within normal temperature and volume thresholds.
            </Typography>
          )}
        </List>
      </CardContent>
    </Card>
  );
};

export default StorageWarnings;
