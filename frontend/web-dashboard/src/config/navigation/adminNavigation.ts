import DashboardIcon from "@mui/icons-material/Dashboard";
import PeopleIcon from "@mui/icons-material/People";
import DevicesIcon from "@mui/icons-material/Devices";
import AssignmentIndIcon from "@mui/icons-material/AssignmentInd";
import HistoryIcon from "@mui/icons-material/History";
import HealthAndSafetyIcon from "@mui/icons-material/HealthAndSafety";
import AssessmentIcon from "@mui/icons-material/Assessment";

import type { NavigationItem } from "./navigationTypes";

export const adminNavigation: NavigationItem[] = [
  {
    id: "dashboard",
    label: "Dashboard",
    icon: DashboardIcon,
    route: "/dashboard",
  },
  {
    id: "users",
    label: "Users",
    icon: PeopleIcon,
    route: "/users",
  },
  {
    id: "devices",
    label: "Devices",
    icon: DevicesIcon,
    route: "/devices",
  },
  {
    id: "patient-assignments",
    label: "Patient Assignments",
    icon: AssignmentIndIcon,
    route: "/patient-assignments",
  },
  {
    id: "audit-logs",
    label: "Audit Logs",
    icon: HistoryIcon,
    route: "/audit-logs",
  },
  {
    id: "system-health",
    label: "System Health",
    icon: HealthAndSafetyIcon,
    route: "/system-health",
  },
  {
    id: "reports",
    label: "Reports",
    icon: AssessmentIcon,
    route: "/reports",
  },
];
