import { Box, Toolbar } from "@mui/material";
import { Outlet } from "react-router-dom";

import Sidebar from "../../components/navigation/Sidebar";
import Topbar from "../../components/navigation/Topbar";

const layoutStyles = {
  display: "flex",
};

const mainContentStyles = {
  flexGrow: 1,
  p: 3,
};

const DashboardLayout = () => {
  return (
    <Box sx={layoutStyles}>
      <Topbar />

      <Sidebar />

      {/* TODO: Adjust content offset when mobile drawer navigation is introduced. */}
      <Box
        component="main"
        sx={mainContentStyles}
      >
        <Toolbar />

        <Outlet />
      </Box>
    </Box>
  );
};

export default DashboardLayout;
