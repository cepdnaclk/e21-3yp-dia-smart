import React, { useState, useEffect } from "react";
import {
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Grid,
  Divider,
  Chip
} from "@mui/material";

interface Buyer {
  fullName: string;
  nic: string;
  contactNumber: string;
  address: string;
  purchaseDate: string;
}

interface Device {
  deviceId: number;
  patientId: number | null;
  deviceUid: string;
  deviceType: string;
  deviceName: string;
  status: string;
  online: boolean;
  active: boolean;
  buyer: Buyer | null;
}

interface GroupedKits {
  buyer: Buyer;
  devices: Device[];
  patientId: number | null;
  status: string;
}

const DeviceAssignmentSection = () => {
  const [kits, setKits] = useState<GroupedKits[]>([]);
  const [selectedKit, setSelectedKit] = useState<GroupedKits | null>(null);

  useEffect(() => {
    fetch("/api/v1/devices", {
      headers: {
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
    })
      .then((res) => res.json())
      .then((data) => {
        if (data.success && data.data) {
          const allDevices: Device[] = data.data;
          
          // Group devices by buyer NIC
          const grouped: Record<string, GroupedKits> = {};
          
          allDevices.forEach((device) => {
            if (device.buyer) {
              const key = device.buyer.nic;
              if (!grouped[key]) {
                grouped[key] = {
                  buyer: device.buyer,
                  devices: [],
                  patientId: device.patientId,
                  status: device.status,
                };
              }
              grouped[key].devices.push(device);
              
              // If any device is connected, mark kit as connected
              if (device.status === 'CONNECTED' || device.patientId) {
                grouped[key].patientId = device.patientId;
                grouped[key].status = 'CONNECTED';
              }
            }
          });
          
          setKits(Object.values(grouped));
        }
      });
  }, []);

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Device Provisioning & Assignments
        </Typography>
        
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Buyer</TableCell>
                <TableCell>Devices</TableCell>
                <TableCell>Assigned Patient ID</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {kits.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} align="center">No registered device kits found.</TableCell>
                </TableRow>
              ) : (
                kits.map((kit, index) => (
                  <TableRow key={index}>
                    <TableCell>{kit.buyer.fullName}</TableCell>
                    <TableCell>{kit.devices.length}</TableCell>
                    <TableCell>{kit.patientId || "Unassigned"}</TableCell>
                    <TableCell>
                      <Chip 
                        label={kit.status || "AVAILABLE"} 
                        size="small" 
                        color={kit.status === 'CONNECTED' ? 'success' : 'default'} 
                      />
                    </TableCell>
                    <TableCell>
                      <Button size="small" onClick={() => setSelectedKit(kit)}>View</Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>

        <Dialog open={!!selectedKit} onClose={() => setSelectedKit(null)} maxWidth="sm" fullWidth>
          <DialogTitle>Provisioning Details</DialogTitle>
          <DialogContent dividers>
            {selectedKit && (
              <>
                <Typography variant="subtitle1" fontWeight="bold">Buyer Information</Typography>
                <Grid container spacing={1} sx={{ mb: 2 }}>
                  <Grid item xs={6}><Typography variant="body2" color="textSecondary">Name:</Typography> <Typography variant="body2">{selectedKit.buyer.fullName}</Typography></Grid>
                  <Grid item xs={6}><Typography variant="body2" color="textSecondary">NIC:</Typography> <Typography variant="body2">{selectedKit.buyer.nic}</Typography></Grid>
                  <Grid item xs={6}><Typography variant="body2" color="textSecondary">Contact:</Typography> <Typography variant="body2">{selectedKit.buyer.contactNumber}</Typography></Grid>
                  <Grid item xs={6}><Typography variant="body2" color="textSecondary">Purchase Date:</Typography> <Typography variant="body2">{selectedKit.buyer.purchaseDate}</Typography></Grid>
                </Grid>

                <Divider sx={{ my: 2 }} />

                <Typography variant="subtitle1" fontWeight="bold">Devices in Kit</Typography>
                <Grid container spacing={1} sx={{ mb: 2 }}>
                  {selectedKit.devices.map(d => (
                    <Grid item xs={12} sm={6} key={d.deviceUid}>
                      <Typography variant="body2" color="textSecondary">{d.deviceType}:</Typography> 
                      <Typography variant="body2">{d.deviceUid}</Typography>
                    </Grid>
                  ))}
                </Grid>

                <Divider sx={{ my: 2 }} />

                <Typography variant="subtitle1" fontWeight="bold">Current Assignment</Typography>
                <Grid container spacing={1}>
                  <Grid item xs={6}><Typography variant="body2" color="textSecondary">Assigned Patient ID:</Typography> <Typography variant="body2">{selectedKit.patientId || "None"}</Typography></Grid>
                  <Grid item xs={6}><Typography variant="body2" color="textSecondary">Current Status:</Typography> <Typography variant="body2">{selectedKit.status || "AVAILABLE"}</Typography></Grid>
                </Grid>
              </>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setSelectedKit(null)}>Close</Button>
          </DialogActions>
        </Dialog>
      </CardContent>
    </Card>
  );
};

export default DeviceAssignmentSection;
