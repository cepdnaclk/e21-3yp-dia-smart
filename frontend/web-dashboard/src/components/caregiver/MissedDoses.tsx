import { Card, CardContent, Typography, Box, List, ListItem, Chip } from "@mui/material";
import { alpha } from "@mui/material/styles";
import EventBusyOutlinedIcon from "@mui/icons-material/EventBusyOutlined";
import ScheduleRoundedIcon from "@mui/icons-material/ScheduleRounded";
import TaskAltIcon from "@mui/icons-material/TaskAlt";

import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";
import type { MissedDoseRecord } from "../../pages/caregiver/DashboardPage";

interface MissedDosesProps {
  missedDoses: MissedDoseRecord[];
}

const ACCENT = "#f59e0b";

const MissedDoses: React.FC<MissedDosesProps> = ({ missedDoses }) => {
  return (
    <Card sx={{ height: "100%" }}>
      <CardContent sx={{ display: "flex", flexDirection: "column" }}>
        <CardHeading title="Missed Doses Tracker" icon={<EventBusyOutlinedIcon />} color={ACCENT} />

        <List sx={{ display: "flex", flexDirection: "column", gap: 1.25, p: 0 }}>
          {missedDoses.map((dose, idx) => (
            <ListItem
              key={idx}
              sx={{
                p: 1.5,
                bgcolor: alpha(ACCENT, 0.06),
                border: "1px solid",
                borderColor: alpha(ACCENT, 0.22),
                borderLeft: `3px solid ${ACCENT}`,
                borderRadius: 2,
                alignItems: "flex-start",
                gap: 1.25,
              }}
            >
              <ScheduleRoundedIcon sx={{ color: "#d97706", fontSize: 20, mt: 0.25, flexShrink: 0 }} />
              <Box sx={{ display: "flex", flexDirection: "column", gap: 0.5, minWidth: 0, flexGrow: 1 }}>
                <Box sx={{ display: "flex", justifyContent: "space-between", width: "100%", alignItems: "center", gap: 1, flexWrap: "wrap" }}>
                  <Typography variant="subtitle2" sx={{ fontWeight: 700, color: "text.primary", overflowWrap: "anywhere" }}>
                    {dose.patientName}
                  </Typography>
                  <Chip
                    label={`${dose.doseUnits} Units`}
                    size="small"
                    color="warning"
                    sx={{ fontSize: 10, fontWeight: 700, height: 20 }}
                  />
                </Box>
                <Typography variant="caption" sx={{ color: "#b45309", fontWeight: 500, overflowWrap: "anywhere" }}>
                  {dose.scheduleLabel} (Scheduled: {dose.scheduledTime})
                </Typography>
              </Box>
            </ListItem>
          ))}

          {missedDoses.length === 0 && (
            <EmptyState
              message="All insulin doses have been taken on schedule today!"
              icon={<TaskAltIcon sx={{ color: "#10b981" }} />}
              minHeight={140}
            />
          )}
        </List>
      </CardContent>
    </Card>
  );
};

export default MissedDoses;
