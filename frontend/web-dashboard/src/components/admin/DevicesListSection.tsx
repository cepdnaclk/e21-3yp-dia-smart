import React, { useState } from "react";
import { Card, CardContent, Typography, Button, Box } from "@mui/material";
import DeviceKitRegistrationModal from "./DeviceKitRegistrationModal";

const DevicesListSection = () => {
  const [modalOpen, setModalOpen] = useState(false);

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Registered Devices Registry
        </Typography>
        
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Catalog of all hardware devices (dosing remind boxes, smart weighing scales, cooling temperature monitors) provisioned in the system.
        </Typography>
        
        <Box sx={{ mt: 3 }}>
          <Button variant="contained" onClick={() => setModalOpen(true)}>
            Register New Device Kit
          </Button>
        </Box>

        <DeviceKitRegistrationModal open={modalOpen} onClose={() => setModalOpen(false)} />
      </CardContent>
    </Card>
  );
};

export default DevicesListSection;
