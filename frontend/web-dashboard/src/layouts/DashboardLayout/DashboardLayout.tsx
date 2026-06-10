import { Box, Toolbar } from "@mui/material";
import { Outlet } from "react-router-dom";

import Sidebar from "../../components/navigation/Sidebar";
import Topbar from "../../components/navigation/Topbar";

const DashboardLayout = () => {
  return (
    <Box sx={{ display: "flex" }}>
      <Topbar />

      <Sidebar />

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
        }}
      >
        <Toolbar />

        <Outlet />
      </Box>
    </Box>
  );
};

export default DashboardLayout;