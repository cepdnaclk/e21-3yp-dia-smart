import {
  AppBar,
  Toolbar,
  Typography,
  Box,
  IconButton,
  Avatar,
} from "@mui/material";

import NotificationsIcon from "@mui/icons-material/Notifications";

import logo from "../../assets/logo/diasmart-logo.png";

import { useAuth } from "../../context/AuthContext";

const Topbar = () => {
  const { role } = useAuth();

  return (
    <AppBar
      position="fixed"
      sx={{
        backgroundColor: "#3B567C",
      }}
    >
      <Toolbar>
        <Box
          component="img"
          src={logo}
          alt="Dia-Smart Logo"
          sx={{
            width: 40,
            height: 40,
            mr: 2,
            borderRadius: 1,
          }}
        />

        <Typography
          variant="h6"
          sx={{
            fontWeight: 700,
            flexGrow: 1,
          }}
        >
          Dia-Smart
        </Typography>

        <IconButton color="inherit">
          <NotificationsIcon />
        </IconButton>

        <Avatar>
          {role.charAt(0)}
        </Avatar>
      </Toolbar>
    </AppBar>
  );
};

export default Topbar;