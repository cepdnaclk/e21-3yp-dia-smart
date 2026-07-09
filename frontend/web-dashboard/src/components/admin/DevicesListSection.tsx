import React, { useState } from "react";
import { 
  Card, CardContent, Typography, Button, Box
} from "@mui/material";
import DeviceKitRegistrationModal from "./DeviceKitRegistrationModal";

const DevicesListSection = () => {
  const [modalOpen, setModalOpen] = useState(false);

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 2 }}>
          <Typography variant="h6" sx={{ fontWeight: "medium" }}>
            Registered Device Kits
          </Typography>
          <Button variant="contained" onClick={() => setModalOpen(true)}>
            Register New Device Kit
          </Button>
        </Box>
        
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Use this section to provision and register a new hardware device kit into the system.
        </Typography>

        <DeviceKitRegistrationModal 
          open={modalOpen} 
          onClose={() => setModalOpen(false)} 
        />
      </CardContent>
    </Card>
  );
};

export default DevicesListSection;
