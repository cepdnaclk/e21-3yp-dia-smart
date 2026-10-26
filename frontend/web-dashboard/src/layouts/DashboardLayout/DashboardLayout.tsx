import { useState } from "react";
import { Box, Toolbar, useTheme, useMediaQuery } from "@mui/material";
import { Outlet } from "react-router-dom";

import Sidebar from "../../components/navigation/Sidebar";
import Topbar from "../../components/navigation/Topbar";
import BottomNav from "../../components/navigation/BottomNav";
import { SIDEBAR_WIDTH } from "../../theme/theme";

const layoutStyles = {
  display: "flex",
  minHeight: "100vh",
  backgroundColor: "background.default",
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
          pt: { xs: 2, sm: 3, lg: 4 },
          px: { xs: 2, sm: 3, lg: 4 },
          width: { xs: "100%", md: `calc(100% - ${SIDEBAR_WIDTH}px)` }, // accounts for sidebar width on desktop
          minWidth: 0,
          pb: isMobile ? "calc(160px + env(safe-area-inset-bottom, 0px))" : "40px", // adds buffer at the bottom for bottom navigation on mobile with safe area support
          transition: theme.transitions.create(["margin", "width"], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
          }),
        }}
      >
        <Toolbar sx={{ pt: "env(safe-area-inset-top, 0px)", mb: { xs: 1, sm: 0 } }} />

        {/* Centered content column keeps wide screens readable */}
        <Box sx={{ width: "100%", maxWidth: 1480, mx: "auto", minWidth: 0 }}>
          <Outlet />
        </Box>
      </Box>

      {/* Conditionally render BottomNav only on Mobile/Tablet viewports */}
      {isMobile && <BottomNav onDrawerToggle={handleDrawerToggle} />}
    </Box>
  );
};

export default DashboardLayout;
