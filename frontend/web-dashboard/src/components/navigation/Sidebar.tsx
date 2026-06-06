import {
  Drawer,
  List,
  ListItemButton,
  ListItemText,
} from "@mui/material";
import { useNavigate, useLocation } from "react-router-dom";
import DashboardIcon from "@mui/icons-material/Dashboard";
import WarningIcon from "@mui/icons-material/Warning";
import AnalyticsIcon from "@mui/icons-material/Analytics";
import PeopleIcon from "@mui/icons-material/People";
import SettingsIcon from "@mui/icons-material/Settings";
import MedicationIcon from "@mui/icons-material/Medication";

const drawerWidth = 240;

const Sidebar = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const menuItems = [
  {
    text: "Dashboard",
    path: "/dashboard",
    icon: <DashboardIcon />,
  },
  {
    text: "Alerts",
    path: "/alerts",
    icon: <WarningIcon />,
  },
  {
    text: "Analytics",
    path: "/analytics",
    icon: <AnalyticsIcon />,
  },
  {
  text: "Prescriptions",
  path: "/prescriptions",
  icon: <MedicationIcon />,
},
  // {
  //   text: "Patients",
  //   path: "/patients",
  //   icon: <PeopleIcon />,
  // },
  {
    text: "Settings",
    path: "/settings",
    icon: <SettingsIcon />,
  },
  
];

  return (
    <Drawer
      variant="permanent"
      sx={{
        width: drawerWidth,
        flexShrink: 0,
        "& .MuiDrawer-paper": {
          width: drawerWidth,
          boxSizing: "border-box",
        },
      }}
    >
      <List>
        {menuItems.map((item) => (
          <ListItemButton
            key={item.path}
            selected={location.pathname === item.path}
            onClick={() => navigate(item.path)}
          >
            {item.icon}
          <ListItemText
            primary={item.text}
            sx={{ ml: 2 }}
          />
          </ListItemButton>
        ))}
      </List>
    </Drawer>
  );
};

export default Sidebar;