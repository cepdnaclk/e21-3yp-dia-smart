import DashboardIcon from "@mui/icons-material/Dashboard";
import PeopleIcon from "@mui/icons-material/People";
import AssessmentIcon from "@mui/icons-material/Assessment";

import type { NavigationItem } from "./navigationTypes";

export const doctorNavigation: NavigationItem[] = [
  {
    id: "dashboard",
    label: "Dashboard",
    icon: DashboardIcon,
    route: "/doctor/dashboard",
  },
  {
    id: "assigned-patients",
    label: "Assigned Patients",
    icon: PeopleIcon,
    route: "/doctor/patients",
  },
  {
    id: "reports",
    label: "Reports",
    icon: AssessmentIcon,
    route: "/doctor/reports",
  },
];
