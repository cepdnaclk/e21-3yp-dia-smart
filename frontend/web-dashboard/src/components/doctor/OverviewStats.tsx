import { Grid } from "@mui/material";
import PeopleAltOutlinedIcon from "@mui/icons-material/PeopleAltOutlined";
import NotificationImportantOutlinedIcon from "@mui/icons-material/NotificationImportantOutlined";
import TaskAltIcon from "@mui/icons-material/TaskAlt";
import SensorsIcon from "@mui/icons-material/Sensors";

import MetricCard from "../common/MetricCard";
import type { MetricTone } from "../common/MetricCard";

interface OverviewStatsProps {
  patientsCount: number;
  openAlertsCount: number;
}

const OverviewStats: React.FC<OverviewStatsProps> = ({ patientsCount, openAlertsCount }) => {
  const statItems = [
    {
      title: "Total Patients",
      value: patientsCount,
      desc: "Total number of patients assigned to this doctor.",
      icon: <PeopleAltOutlinedIcon />,
      tone: "primary" as MetricTone,
    },
    {
      title: "Critical Alerts",
      value: openAlertsCount,
      desc: "Number of active critical alerts requiring immediate action.",
      icon: <NotificationImportantOutlinedIcon />,
      tone: (openAlertsCount > 0 ? "error" : "success") as MetricTone,
    },
    {
      title: "Average Adherence",
      value: "88.4%",
      desc: "Average dose adherence rate across all assigned patients.",
      icon: <TaskAltIcon />,
      tone: "success" as MetricTone,
    },
    {
      title: "Active Devices",
      value: patientsCount > 0 ? patientsCount * 2 : 0,
      desc: "Number of currently active monitoring/dosing devices.",
      icon: <SensorsIcon />,
      tone: "info" as MetricTone,
    }
  ];

  return (
    <Grid container spacing={3}>
      {statItems.map((item, idx) => (
        <Grid key={idx} size={{ xs: 12, sm: 6, md: 3 }}>
          <MetricCard
            label={item.title}
            value={item.value}
            description={item.desc}
            icon={item.icon}
            tone={item.tone}
          />
        </Grid>
      ))}
    </Grid>
  );
};

export default OverviewStats;
