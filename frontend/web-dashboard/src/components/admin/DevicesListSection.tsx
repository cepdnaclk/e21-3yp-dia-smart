import React, { useState, useEffect } from "react";
import { Card, CardContent, Typography, Button, Box, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Chip, CircularProgress } from "@mui/material";
import DeviceKitRegistrationModal from "./DeviceKitRegistrationModal";
import api from "../../services/api";

const DevicesListSection = () => {
  const [modalOpen, setModalOpen] = useState(false);
  const [devices, setDevices] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchDevices = async () => {
    setLoading(true);
    try {
      const response = await api.get("/admin/devices");
      setDevices(response.data.data || []);
    } catch (error) {
      console.error("Failed to fetch devices", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDevices();
  }, []);

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 2 }}>
          <Typography variant="h6" sx={{ fontWeight: "medium" }}>
            Registered Devices Registry
          </Typography>
          <Button variant="contained" onClick={() => setModalOpen(true)}>
            Register New Device Kit
          </Button>
        </Box>
        
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Catalog of all hardware devices provisioned in the system.
        </Typography>
        
        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", p: 3 }}>
            <CircularProgress />
          </Box>
        ) : (
          <TableContainer component={Paper} variant="outlined">
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Device ID</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Buyer Name</TableCell>
                  <TableCell>Registration Date</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {devices.map((device) => (
                  <TableRow key={device.deviceId}>
                    <TableCell>{device.deviceUid}</TableCell>
                    <TableCell>{device.deviceType}</TableCell>
                    <TableCell>{device.buyer?.fullName || "Unknown"}</TableCell>
                    <TableCell>{device.buyer?.purchaseDate || "N/A"}</TableCell>
                    <TableCell>
                      <Chip 
                        label={device.active ? "Active" : "Inactive"} 
                        color={device.active ? "success" : "default"} 
                        size="small" 
                      />
                    </TableCell>
                  </TableRow>
                ))}
                {devices.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5} align="center">No devices found.</TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        )}

        <DeviceKitRegistrationModal 
          open={modalOpen} 
          onClose={() => {
            setModalOpen(false);
            fetchDevices();
          }} 
        />
      </CardContent>
    </Card>
  );
};

export default DevicesListSection;
