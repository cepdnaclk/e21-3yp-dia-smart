import DashboardIcon from "@mui/icons-material/Dashboard";
import WarningIcon from "@mui/icons-material/Warning";
import AnalyticsIcon from "@mui/icons-material/Analytics";
import MedicationIcon from "@mui/icons-material/Medication";
import DevicesIcon from "@mui/icons-material/Devices";
import GroupsIcon from "@mui/icons-material/Groups";

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
    id: "devices",
    label: "Devices",
    icon: DevicesIcon,
    route: "/devices",
  },
  {
    id: "care-team",
    label: "Care Team",
    icon: GroupsIcon,
    route: "/care-team",
  },
];
