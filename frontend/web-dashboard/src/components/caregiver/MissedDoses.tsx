import { Card, CardContent, Typography, Box, List, ListItem, Chip } from "@mui/material";
import type { MissedDoseRecord } from "../../pages/caregiver/DashboardPage";

interface MissedDosesProps {
  missedDoses: MissedDoseRecord[];
}

const MissedDoses: React.FC<MissedDosesProps> = ({ missedDoses }) => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 3 }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
        <Typography variant="h6" sx={{ fontWeight: "bold" }}>
          Missed Doses Tracker
        </Typography>

        <List sx={{ display: "flex", flexDirection: "column", gap: 1.5, p: 0 }}>
          {missedDoses.map((dose, idx) => (
            <ListItem
              key={idx}
              sx={{
                p: 1.5,
                bgcolor: "warning.light",
                color: "warning.dark",
                borderRadius: 2,
                flexDirection: "column",
                alignItems: "flex-start",
                gap: 0.5,
                opacity: 0.95
              }}
            >
              <Box sx={{ display: "flex", justifyContent: "space-between", width: "100%", alignItems: "center" }}>
                <Typography variant="subtitle2" sx={{ fontWeight: "bold" }}>
                  {dose.patientName}
                </Typography>
                <Chip
                  label={`${dose.doseUnits} Units`}
                  size="small"
                  color="warning"
                  sx={{ fontSize: 10, fontWeight: "bold", height: 20 }}
                />
              </Box>
              <Typography variant="caption" sx={{ color: "warning.dark", fontWeight: "medium" }}>
                {dose.scheduleLabel} (Scheduled: {dose.scheduledTime})
              </Typography>
            </ListItem>
          ))}

          {missedDoses.length === 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ textAlign: "center", py: 3 }}>
              All insulin doses have been taken on schedule today!
            </Typography>
          )}
        </List>
      </CardContent>
    </Card>
  );
};

export default MissedDoses;
