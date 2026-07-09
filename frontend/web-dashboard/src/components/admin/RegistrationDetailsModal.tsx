import React, { useState, useEffect } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  CircularProgress,
  Typography,
  Alert,
  Box
} from "@mui/material";
import api from "../../services/api";
import RegistrationDetailsViewDialog, { type FlattenedKit } from "./RegistrationDetailsViewDialog";

interface RegistrationDetailsModalProps {
  open: boolean;
  onClose: () => void;
}

const RegistrationDetailsModal: React.FC<RegistrationDetailsModalProps> = ({ open, onClose }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [kits, setKits] = useState<FlattenedKit[]>([]);
  const [selectedKit, setSelectedKit] = useState<FlattenedKit | null>(null);
  const [viewDialogOpen, setViewDialogOpen] = useState(false);

  useEffect(() => {
    if (open) {
      fetchKits();
    }
  }, [open]);

  const fetchKits = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get("/admin/devices/device-kits");
      const rawData = response.data.data || [];
      
      const flattened: FlattenedKit[] = [];
      
      rawData.forEach((item: any) => {
        const buyer = item.buyer;
        if (item.kits && item.kits.length > 0) {
          item.kits.forEach((kit: any) => {
            const numDevices = kit.devices ? kit.devices.length : 0;
            
            let outerId = "";
            let innerId = "";
            let penId = "";
            let gluId = "";
            let assignedPatientName = null;

            if (kit.devices) {
              kit.devices.forEach((dev: any) => {
                if (dev.deviceType === "OUTER_GATEWAY") outerId = dev.deviceUid;
                if (dev.deviceType === "INNER_UNIT") innerId = dev.deviceUid;
                if (dev.deviceType === "DOSE_CAP") penId = dev.deviceUid;
                if (dev.deviceType === "GLUCOMETER") gluId = dev.deviceUid;
                
                if (dev.patientDisplayName) {
                  assignedPatientName = dev.patientDisplayName;
                }
              });
            }

            flattened.push({
              buyerName: buyer.fullName,
              nic: buyer.nic,
              contactNumber: buyer.contactNumber,
              address: buyer.address,
              purchaseDate: kit.purchaseDate,
              buyerId: buyer.buyerId,
              numDevices,
              outerGatewayId: outerId,
              innerUnitId: innerId,
              penUnitId: penId,
              glucoseMeterId: gluId,
              registrationDate: kit.purchaseDate,
              assignedPatientName: assignedPatientName
            });
          });
        }
      });
      
      // Sort by purchaseDate descending (newest first)
      flattened.sort((a, b) => new Date(b.purchaseDate).getTime() - new Date(a.purchaseDate).getTime());
      
      setKits(flattened);
    } catch (err: any) {
      setError("Failed to load device kits. Please try again.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleViewDetails = (kit: FlattenedKit) => {
    setSelectedKit(kit);
    setViewDialogOpen(true);
  };

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="xl" fullWidth>
        <DialogTitle sx={{ fontWeight: 600 }}>Registration Details Registry</DialogTitle>
        <DialogContent dividers>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          
          {loading ? (
            <Box sx={{ display: "flex", justifyContent: "center", p: 5 }}>
              <CircularProgress />
            </Box>
          ) : kits.length === 0 && !error ? (
            <Typography variant="body1" color="text.secondary" align="center" sx={{ py: 5 }}>
              No registered device kits found in the system.
            </Typography>
          ) : (
            <TableContainer component={Paper} variant="outlined">
              <Table size="small">
                <TableHead sx={{ bgcolor: "background.default" }}>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 600 }}>Buyer Name</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>NIC / Passport</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Purchase Date</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Total Devices</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Outer ID</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Inner ID</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Pen ID</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Glucometer ID</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Status</TableCell>
                    <TableCell sx={{ fontWeight: 600 }} align="center">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {kits.map((kit, idx) => (
                    <TableRow key={idx} hover>
                      <TableCell>{kit.buyerName}</TableCell>
                      <TableCell>{kit.nic}</TableCell>
                      <TableCell>{kit.purchaseDate}</TableCell>
                      <TableCell>{kit.numDevices}</TableCell>
                      <TableCell>{kit.outerGatewayId || "-"}</TableCell>
                      <TableCell>{kit.innerUnitId || "-"}</TableCell>
                      <TableCell>{kit.penUnitId || "-"}</TableCell>
                      <TableCell>{kit.glucoseMeterId || "-"}</TableCell>
                      <TableCell>{kit.assignedPatientName ? "Assigned" : "Available"}</TableCell>
                      <TableCell align="center">
                        <Button 
                          size="small" 
                          variant="outlined" 
                          onClick={() => handleViewDetails(kit)}
                        >
                          View Details
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={onClose} variant="contained" disableElevation>
            Close
          </Button>
        </DialogActions>
      </Dialog>

      <RegistrationDetailsViewDialog 
        open={viewDialogOpen}
        onClose={() => setViewDialogOpen(false)}
        kit={selectedKit}
      />
    </>
  );
};

export default RegistrationDetailsModal;
