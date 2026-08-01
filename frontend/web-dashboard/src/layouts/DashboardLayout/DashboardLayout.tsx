import { useState } from "react";
import { Box, Toolbar, useTheme, useMediaQuery } from "@mui/material";
import { Outlet } from "react-router-dom";

import Sidebar from "../../components/navigation/Sidebar";
import Topbar from "../../components/navigation/Topbar";
import BottomNav from "../../components/navigation/BottomNav";

const layoutStyles = {
  display: "flex",
  minHeight: "100vh",
  backgroundColor: "#f8f9fa",
};

const DashboardLayout = () => {
  const [mobileOpen, setMobileOpen] = useState(false);
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down("md"));

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleDrawerClose = () => {
    setMobileOpen(false);
  };

  return (
    <Box sx={layoutStyles}>
      <Topbar onDrawerToggle={handleDrawerToggle} />

      <Sidebar mobileOpen={mobileOpen} onClose={handleDrawerClose} />

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: { xs: 2, sm: 3 },
          width: { xs: "100%", md: `calc(100% - 260px)` }, // accounts for sidebar width on desktop
          minWidth: 0,
          pb: isMobile ? "calc(120px + env(safe-area-inset-bottom, 0px))" : "24px", // adds buffer at the bottom for bottom navigation on mobile with safe area support
          transition: theme.transitions.create(["margin", "width"], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
          }),
        }}
      >
        <Toolbar sx={{ pt: "env(safe-area-inset-top, 0px)", mb: { xs: 1, sm: 0 } }} />

        <Outlet />
      </Box>

      {/* Conditionally render BottomNav only on Mobile/Tablet viewports */}
      {isMobile && <BottomNav onDrawerToggle={handleDrawerToggle} />}
    </Box>
  );
};

export default DashboardLayout;

