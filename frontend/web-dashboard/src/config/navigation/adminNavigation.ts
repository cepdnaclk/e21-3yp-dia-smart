import DashboardIcon from "@mui/icons-material/Dashboard";
import PeopleIcon from "@mui/icons-material/People";
import DevicesIcon from "@mui/icons-material/Devices";
import AssignmentIndIcon from "@mui/icons-material/AssignmentInd";
import HealthAndSafetyIcon from "@mui/icons-material/HealthAndSafety";
import AssessmentIcon from "@mui/icons-material/Assessment";

import type { NavigationItem } from "./navigationTypes";

export const adminNavigation: NavigationItem[] = [
  {
    id: "dashboard",
    label: "Dashboard",
    icon: DashboardIcon,
    route: "/admin/dashboard",
  },
  {
    id: "users",
    label: "Users",
    icon: PeopleIcon,
    route: "/admin/users",
  },
  {
    id: "devices",
    label: "Devices",
    icon: DevicesIcon,
    route: "/admin/devices",
  },
  {
    id: "patient-assignments",
    label: "Assignments",
    icon: AssignmentIndIcon,
    route: "/admin/assignments",
  },
  {
    id: "system-health",
    label: "System",
    icon: HealthAndSafetyIcon,
    route: "/admin/system",
  },
  {
    id: "reports",
    label: "Reports",
    icon: AssessmentIcon,
    route: "/admin/reports",
  },
];
