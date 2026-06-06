import { Box, Toolbar } from "@mui/material";
import Sidebar from "../../components/navigation/Sidebar";
import Topbar from "../../components/navigation/Topbar";

interface Props {
  children: React.ReactNode;
}

const DashboardLayout = ({ children }: Props) => {
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
        {children}
      </Box>
    </Box>
  );
};

export default DashboardLayout;