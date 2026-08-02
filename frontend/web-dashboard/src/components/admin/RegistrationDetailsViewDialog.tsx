import React from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Grid,
  Typography,
  Card,
  CardContent,
  Box,
  Divider
} from "@mui/material";

export interface FlattenedKit {
  buyerName: string;
  nic: string;
  contactNumber: string;
  address: string;
  purchaseDate: string;
  buyerId: number;
  numDevices: number;
  outerGatewayId: string;
  innerUnitId: string;
  penUnitId: string;
  glucoseMeterId: string;
  registrationDate: string;
  assignedPatientName: string | null;
}

interface RegistrationDetailsViewDialogProps {
  open: boolean;
  onClose: () => void;
  kit: FlattenedKit | null;
}

const RegistrationDetailsViewDialog: React.FC<RegistrationDetailsViewDialogProps> = ({ open, onClose, kit }) => {
  if (!kit) return null;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle sx={{ fontWeight: 600 }}>Registration Details</DialogTitle>
      <DialogContent dividers>
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 6 }}>
            <Card variant="outlined" sx={{ height: "100%" }}>
              <CardContent>
                <Typography variant="h6" gutterBottom color="primary.main" sx={{ fontWeight: 600 }}>
                  Buyer Information
                </Typography>
                <Divider sx={{ mb: 2 }} />
                <Box sx={{ display: "flex", flexDirection: "column", gap: 1.5 }}>
                  <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                    <Typography variant="body2" color="text.secondary">Buyer Name</Typography>
                    <Typography variant="body1" sx={{ fontWeight: 500 }}>{kit.buyerName}</Typography>
                  </Box>
                  <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                    <Typography variant="body2" color="text.secondary">NIC / Passport</Typography>
                    <Typography variant="body1" sx={{ fontWeight: 500 }}>{kit.nic}</Typography>
                  </Box>
                  <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                    <Typography variant="body2" color="text.secondary">Contact Number</Typography>
                    <Typography variant="body1" sx={{ fontWeight: 500 }}>{kit.contactNumber}</Typography>
                  </Box>
                  <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                    <Typography variant="body2" color="text.secondary">Address</Typography>
                    <Typography variant="body1" sx={{ fontWeight: 500 }}>{kit.address || "N/A"}</Typography>
                  </Box>
                  <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                    <Typography variant="body2" color="text.secondary">Purchase Date</Typography>
                    <Typography variant="body1" sx={{ fontWeight: 500 }}>{kit.purchaseDate}</Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          
          <Grid size={{ xs: 12, md: 6 }}>
            <Card variant="outlined" sx={{ height: "100%" }}>
              <CardContent>
                <Typography variant="h6" gutterBottom color="primary.main" sx={{ fontWeight: 600 }}>
                  Registered Devices
                </Typography>
                <Divider sx={{ mb: 2 }} />
                <Box sx={{ display: "flex", flexDirection: "column", gap: 1.5 }}>
                  <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                    <Typography variant="body2" color="text.secondary">Outer Gateway</Typography>
                    <Typography variant="body1" sx={{ fontWeight: 500 }}>{kit.outerGatewayId || "N/A"}</Typography>
                  </Box>
                  <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                    <Typography variant="body2" color="text.secondary">Inner Unit</Typography>
                    <Typography variant="body1" sx={{ fontWeight: 500 }}>{kit.innerUnitId || "N/A"}</Typography>
                  </Box>
                  <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                    <Typography variant="body2" color="text.secondary">Pen Unit</Typography>
                    <Typography variant="body1" sx={{ fontWeight: 500 }}>{kit.penUnitId || "N/A"}</Typography>
                  </Box>
                  <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                    <Typography variant="body2" color="text.secondary">Glucose Meter</Typography>
                    <Typography variant="body1" sx={{ fontWeight: 500 }}>{kit.glucoseMeterId || "N/A"}</Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>

          <Grid size={{ xs: 12 }}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="h6" gutterBottom color="primary.main" sx={{ fontWeight: 600 }}>
                  Registration Information
                </Typography>
                <Divider sx={{ mb: 2 }} />
                <Grid container spacing={2}>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <Box sx={{ display: "flex", flexDirection: "column", gap: 1.5 }}>
                      <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                        <Typography variant="body2" color="text.secondary">Buyer ID</Typography>
                        <Typography variant="body1" sx={{ fontWeight: 500 }}>#{kit.buyerId}</Typography>
                      </Box>
                      <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                        <Typography variant="body2" color="text.secondary">Registration Date</Typography>
                        <Typography variant="body1" sx={{ fontWeight: 500 }}>{kit.registrationDate}</Typography>
                      </Box>
                    </Box>
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <Box sx={{ display: "flex", flexDirection: "column", gap: 1.5 }}>
                      <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                        <Typography variant="body2" color="text.secondary">Active Status</Typography>
                        <Typography variant="body1" sx={{ fontWeight: 500 }}>Active</Typography>
                      </Box>
                      <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                        <Typography variant="body2" color="text.secondary">Assigned Patient</Typography>
                        <Typography variant="body1" sx={{ fontWeight: 500 }}>{kit.assignedPatientName || "Not Assigned"}</Typography>
                      </Box>
                    </Box>
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ p: 2, bgcolor: "background.default" }}>
        <Button onClick={onClose} variant="contained" disableElevation>
          Close
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default RegistrationDetailsViewDialog;
