import DashboardIcon from "@mui/icons-material/Dashboard";
import WarningIcon from "@mui/icons-material/Warning";
import AnalyticsIcon from "@mui/icons-material/Analytics";
import MedicationIcon from "@mui/icons-material/Medication";
import PersonIcon from "@mui/icons-material/Person";
import SettingsIcon from "@mui/icons-material/Settings";

import type { NavigationItem } from "./navigationTypes";

export const patientNavigation: NavigationItem[] = [
  {
    id: "dashboard",
    label: "Dashboard",
    icon: DashboardIcon,
    route: "/dashboard",
  },
  {
    id: "alerts",
    label: "Alerts",
    icon: WarningIcon,
    route: "/alerts",
  },
  {
    id: "analytics",
    label: "Analytics",
    icon: AnalyticsIcon,
    route: "/analytics",
  },
  {
    id: "prescriptions",
    label: "Prescriptions",
    icon: MedicationIcon,
    route: "/prescriptions",
  },
  {
    id: "profile",
    label: "Profile",
    icon: PersonIcon,
    route: "/profile",
  },
  {
    id: "settings",
    label: "Settings",
    icon: SettingsIcon,
    route: "/settings",
  },
];
