import { useState } from "react";
import {
  Card, CardContent, Button, Box
} from "@mui/material";
import DevicesOtherOutlinedIcon from "@mui/icons-material/DevicesOtherOutlined";
import AddIcon from "@mui/icons-material/Add";
import ListAltOutlinedIcon from "@mui/icons-material/ListAltOutlined";
import DeviceKitRegistrationModal from "./DeviceKitRegistrationModal";
import RegistrationDetailsModal from "./RegistrationDetailsModal";
import CardHeading from "../common/CardHeading";

const DevicesListSection = () => {
  const [modalOpen, setModalOpen] = useState(false);
  const [detailsModalOpen, setDetailsModalOpen] = useState(false);

  return (
    <Card sx={{ height: "100%" }}>
      <CardContent>
        <CardHeading
          title="Registered Device Kits"
          subtitle="Use this section to provision and register a new hardware device kit into the system."
          icon={<DevicesOtherOutlinedIcon />}
          color="#0ea5e9"
          sx={{ mb: 2, alignItems: "flex-start" }}
        />

        <Box sx={{ display: "flex", gap: 1.5, flexWrap: "wrap", justifyContent: { xs: "stretch", sm: "flex-end" } }}>
          <Button
            variant="outlined"
            startIcon={<ListAltOutlinedIcon />}
            onClick={() => setDetailsModalOpen(true)}
            sx={{ flex: { xs: "1 1 100%", sm: "0 0 auto" } }}
          >
            View Registration Details
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setModalOpen(true)}
            sx={{ flex: { xs: "1 1 100%", sm: "0 0 auto" } }}
          >
            Register New Device Kit
          </Button>
        </Box>

        <DeviceKitRegistrationModal
          open={modalOpen}
          onClose={() => setModalOpen(false)}
        />

        <RegistrationDetailsModal
          open={detailsModalOpen}
          onClose={() => setDetailsModalOpen(false)}
        />
      </CardContent>
    </Card>
  );
};

export default DevicesListSection;
