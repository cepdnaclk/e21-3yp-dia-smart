import React, { useState, useEffect } from "react";
import { 
  Card, CardContent, Typography, Button, Box, Table, TableBody, TableCell, 
  TableContainer, TableHead, TableRow, Paper, CircularProgress,
  Dialog, DialogTitle, DialogContent, DialogActions, Divider, Grid
} from "@mui/material";
import DeviceKitRegistrationModal from "./DeviceKitRegistrationModal";
import api from "../../services/api";

const DevicesListSection = () => {
  const [modalOpen, setModalOpen] = useState(false);
  const [deviceKits, setDeviceKits] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedKit, setSelectedKit] = useState<any | null>(null);

  const fetchDeviceKits = async () => {
    setLoading(true);
    try {
      const response = await api.get("/admin/device-kits");
      setDeviceKits(response.data.data || []);
    } catch (error) {
      console.error("Failed to fetch device kits", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDeviceKits();
  }, []);

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
          Catalog of all hardware device kits provisioned in the system grouped by buyer.
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
                  <TableCell>Buyer</TableCell>
                  <TableCell>NIC</TableCell>
                  <TableCell>Contact</TableCell>
                  <TableCell>Purchase Count</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {deviceKits.map((item, index) => (
                  <TableRow key={index}>
                    <TableCell>{item.buyer?.fullName}</TableCell>
                    <TableCell>{item.buyer?.nic}</TableCell>
                    <TableCell>{item.buyer?.contactNumber}</TableCell>
                    <TableCell>{item.purchaseCount} Kits</TableCell>
                    <TableCell align="right">
                      <Button size="small" onClick={() => setSelectedKit(item)}>View</Button>
                    </TableCell>
                  </TableRow>
                ))}
                {deviceKits.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5} align="center">No device kits found.</TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        )}

        {/* View Details Modal */}
        <Dialog open={!!selectedKit} onClose={() => setSelectedKit(null)} maxWidth="md" fullWidth>
          <DialogTitle>Device Kit Details</DialogTitle>
          <DialogContent dividers>
            {selectedKit && (
              <Box>
                <Typography variant="h6" color="primary" gutterBottom>Buyer Information</Typography>
                <Grid container spacing={2}>
                  <Grid item xs={6}>
                    <Typography variant="body2"><strong>Name:</strong> {selectedKit.buyer.fullName}</Typography>
                    <Typography variant="body2"><strong>NIC:</strong> {selectedKit.buyer.nic}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2"><strong>Contact:</strong> {selectedKit.buyer.contactNumber}</Typography>
                    <Typography variant="body2"><strong>Address:</strong> {selectedKit.buyer.address || "N/A"}</Typography>
                  </Grid>
                </Grid>
                
                {selectedKit.kits?.map((kit: any, kitIndex: number) => (
                  <Box key={kitIndex} sx={{ mt: 3 }}>
                    <Divider sx={{ mb: 2 }} />
                    <Typography variant="subtitle1" fontWeight="bold">
                      {kitIndex === 0 ? "Registered Devices" : `Kit ${kitIndex + 1}`}
                      <Typography component="span" variant="body2" color="text.secondary" sx={{ ml: 2 }}>
                        (Purchase Date: {kit.purchaseDate})
                      </Typography>
                    </Typography>
                    
                    <Grid container spacing={2} sx={{ mt: 1 }}>
                      {kit.devices?.map((device: any) => (
                        <Grid item xs={12} sm={6} key={device.deviceId}>
                          <Box sx={{ p: 1.5, border: '1px solid #eee', borderRadius: 1 }}>
                            <Typography variant="body2" color="text.secondary">{device.deviceType}</Typography>
                            <Typography variant="body1">{device.deviceUid}</Typography>
                          </Box>
                        </Grid>
                      ))}
                    </Grid>
                  </Box>
                ))}
              </Box>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setSelectedKit(null)}>Close</Button>
          </DialogActions>
        </Dialog>

        <DeviceKitRegistrationModal 
          open={modalOpen} 
          onClose={() => {
            setModalOpen(false);
            fetchDeviceKits();
          }} 
        />
      </CardContent>
    </Card>
  );
};

export default DevicesListSection;
