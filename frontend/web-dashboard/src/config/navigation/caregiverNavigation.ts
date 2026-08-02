import DashboardIcon from "@mui/icons-material/Dashboard";
import PeopleIcon from "@mui/icons-material/People";

import type { NavigationItem } from "./navigationTypes";

export const caregiverNavigation: NavigationItem[] = [
  {
    id: "dashboard",
    label: "Dashboard",
    icon: DashboardIcon,
    route: "/caregiver/dashboard",
  },
  {
    id: "assigned-patients",
    label: "Assigned Patients",
    icon: PeopleIcon,
    route: "/caregiver/patients",
  },
];
