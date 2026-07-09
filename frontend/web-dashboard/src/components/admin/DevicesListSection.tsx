import { useState } from "react";
import { 
  Card, CardContent, Typography, Button, Box
} from "@mui/material";
import DeviceKitRegistrationModal from "./DeviceKitRegistrationModal";
import RegistrationDetailsModal from "./RegistrationDetailsModal";

const DevicesListSection = () => {
  const [modalOpen, setModalOpen] = useState(false);
  const [detailsModalOpen, setDetailsModalOpen] = useState(false);

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 2 }}>
          <Typography variant="h6" sx={{ fontWeight: "medium" }}>
            Registered Device Kits
          </Typography>
          <Box sx={{ display: "flex", gap: 2 }}>
            <Button variant="outlined" onClick={() => setDetailsModalOpen(true)}>
              View Registration Details
            </Button>
            <Button variant="contained" onClick={() => setModalOpen(true)}>
              Register New Device Kit
            </Button>
          </Box>
        </Box>
        
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Use this section to provision and register a new hardware device kit into the system.
        </Typography>

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
