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
  Alert,
  Box,
  Chip
} from "@mui/material";
import AssignmentOutlinedIcon from "@mui/icons-material/AssignmentOutlined";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import EmptyState from "../common/EmptyState";
import api from "../../services/api";
import RegistrationDetailsViewDialog, { type FlattenedKit } from "./RegistrationDetailsViewDialog";

import { useAutoRefresh } from "../../hooks/useAutoRefresh";

const idCellSx = { fontFamily: "monospace", fontSize: "0.8rem", whiteSpace: "nowrap" } as const;

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
      fetchKits(false);
    }
  }, [open]);

  const fetchKits = async (silent = false) => {
    if (!silent) {
      setLoading(true);
      setError(null);
    }
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
      if (!silent) {
        setError("Failed to load device kits. Please try again.");
      }
      console.error(err);
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  };

  useAutoRefresh(() => {
    if (open) {
      fetchKits(true);
    }
  }, 5000);

  const handleViewDetails = (kit: FlattenedKit) => {
    setSelectedKit(kit);
    setViewDialogOpen(true);
  };

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="xl" fullWidth>
        <DialogTitle sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
          <Box
            aria-hidden
            sx={{
              width: 38,
              height: 38,
              borderRadius: 2,
              flexShrink: 0,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              color: "#0ea5e9",
              bgcolor: "#e0f2fe",
            }}
          >
            <AssignmentOutlinedIcon fontSize="small" />
          </Box>
          Registration Details Registry
        </DialogTitle>
        <DialogContent dividers>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          
          {loading ? (
            <Box sx={{ display: "flex", justifyContent: "center", p: 5 }}>
              <CircularProgress size={36} thickness={4} sx={{ color: "secondary.dark" }} />
            </Box>
          ) : kits.length === 0 && !error ? (
            <EmptyState
              message="No registered device kits found in the system."
              icon={<Inventory2OutlinedIcon />}
              minHeight={200}
            />
          ) : (
            <TableContainer component={Paper} variant="outlined" sx={{ overflowX: "auto" }}>
              <Table size="small" sx={{ minWidth: 960 }}>
                <TableHead>
                  <TableRow>
                    <TableCell>Buyer Name</TableCell>
                    <TableCell>NIC / Passport</TableCell>
                    <TableCell>Purchase Date</TableCell>
                    <TableCell>Total Devices</TableCell>
                    <TableCell>Outer ID</TableCell>
                    <TableCell>Inner ID</TableCell>
                    <TableCell>Pen ID</TableCell>
                    <TableCell>Glucometer ID</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="center">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {kits.map((kit, idx) => (
                    <TableRow key={idx} hover>
                      <TableCell sx={{ fontWeight: 600, color: "text.primary" }}>{kit.buyerName}</TableCell>
                      <TableCell>{kit.nic}</TableCell>
                      <TableCell sx={{ whiteSpace: "nowrap" }}>{kit.purchaseDate}</TableCell>
                      <TableCell>{kit.numDevices}</TableCell>
                      <TableCell sx={idCellSx}>{kit.outerGatewayId || "-"}</TableCell>
                      <TableCell sx={idCellSx}>{kit.innerUnitId || "-"}</TableCell>
                      <TableCell sx={idCellSx}>{kit.penUnitId || "-"}</TableCell>
                      <TableCell sx={idCellSx}>{kit.glucoseMeterId || "-"}</TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          label={kit.assignedPatientName ? "Assigned" : "Available"}
                          color={kit.assignedPatientName ? "info" : "success"}
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell align="center">
                        <Button 
                          size="small" 
                          variant="outlined" 
                          onClick={() => handleViewDetails(kit)}
                          sx={{ whiteSpace: "nowrap" }}
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
        <DialogActions>
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
