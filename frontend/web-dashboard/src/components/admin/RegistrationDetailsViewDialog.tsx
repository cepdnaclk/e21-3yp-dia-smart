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
import ReceiptLongOutlinedIcon from "@mui/icons-material/ReceiptLongOutlined";
import PersonOutlineIcon from "@mui/icons-material/PersonOutlined";
import MemoryOutlinedIcon from "@mui/icons-material/MemoryOutlined";
import FactCheckOutlinedIcon from "@mui/icons-material/FactCheckOutlined";

const rowSx = {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "baseline",
  flexWrap: "wrap",
  columnGap: 2,
  rowGap: 0.25,
} as const;

const valueSx = { fontWeight: 600, color: "text.primary", textAlign: "right", overflowWrap: "anywhere", minWidth: 0 } as const;

const sectionTitleSx = {
  display: "flex",
  alignItems: "center",
  gap: 1,
  fontWeight: 700,
  fontSize: "1rem",
} as const;

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
          <ReceiptLongOutlinedIcon fontSize="small" />
        </Box>
        Registration Details
      </DialogTitle>
      <DialogContent dividers>
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 6 }}>
            <Card variant="outlined" sx={{ height: "100%", boxShadow: "none", bgcolor: "#f8fafc" }}>
              <CardContent>
                <Typography variant="h6" component="h3" gutterBottom color="primary.main" sx={sectionTitleSx}>
                  <PersonOutlineIcon fontSize="small" sx={{ color: "#0284c7" }} />
                  Buyer Information
                </Typography>
                <Divider sx={{ mb: 2 }} />
                <Box sx={{ display: "flex", flexDirection: "column", gap: 1.5 }}>
                  <Box sx={rowSx}>
                    <Typography variant="body2" color="text.secondary">Buyer Name</Typography>
                    <Typography variant="body2" sx={valueSx}>{kit.buyerName}</Typography>
                  </Box>
                  <Box sx={rowSx}>
                    <Typography variant="body2" color="text.secondary">NIC / Passport</Typography>
                    <Typography variant="body2" sx={valueSx}>{kit.nic}</Typography>
                  </Box>
                  <Box sx={rowSx}>
                    <Typography variant="body2" color="text.secondary">Contact Number</Typography>
                    <Typography variant="body2" sx={valueSx}>{kit.contactNumber}</Typography>
                  </Box>
                  <Box sx={rowSx}>
                    <Typography variant="body2" color="text.secondary">Address</Typography>
                    <Typography variant="body2" sx={valueSx}>{kit.address || "N/A"}</Typography>
                  </Box>
                  <Box sx={rowSx}>
                    <Typography variant="body2" color="text.secondary">Purchase Date</Typography>
                    <Typography variant="body2" sx={valueSx}>{kit.purchaseDate}</Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          
          <Grid size={{ xs: 12, md: 6 }}>
            <Card variant="outlined" sx={{ height: "100%", boxShadow: "none", bgcolor: "#f8fafc" }}>
              <CardContent>
                <Typography variant="h6" component="h3" gutterBottom color="primary.main" sx={sectionTitleSx}>
                  <MemoryOutlinedIcon fontSize="small" sx={{ color: "#0284c7" }} />
                  Registered Devices
                </Typography>
                <Divider sx={{ mb: 2 }} />
                <Box sx={{ display: "flex", flexDirection: "column", gap: 1.5 }}>
                  <Box sx={rowSx}>
                    <Typography variant="body2" color="text.secondary">Outer Gateway</Typography>
                    <Typography variant="body2" sx={valueSx}>{kit.outerGatewayId || "N/A"}</Typography>
                  </Box>
                  <Box sx={rowSx}>
                    <Typography variant="body2" color="text.secondary">Inner Unit</Typography>
                    <Typography variant="body2" sx={valueSx}>{kit.innerUnitId || "N/A"}</Typography>
                  </Box>
                  <Box sx={rowSx}>
                    <Typography variant="body2" color="text.secondary">Pen Unit</Typography>
                    <Typography variant="body2" sx={valueSx}>{kit.penUnitId || "N/A"}</Typography>
                  </Box>
                  <Box sx={rowSx}>
                    <Typography variant="body2" color="text.secondary">Glucose Meter</Typography>
                    <Typography variant="body2" sx={valueSx}>{kit.glucoseMeterId || "N/A"}</Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>

          <Grid size={{ xs: 12 }}>
            <Card variant="outlined" sx={{ boxShadow: "none", bgcolor: "#f8fafc" }}>
              <CardContent>
                <Typography variant="h6" component="h3" gutterBottom color="primary.main" sx={sectionTitleSx}>
                  <FactCheckOutlinedIcon fontSize="small" sx={{ color: "#0284c7" }} />
                  Registration Information
                </Typography>
                <Divider sx={{ mb: 2 }} />
                <Grid container spacing={2}>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <Box sx={{ display: "flex", flexDirection: "column", gap: 1.5 }}>
                      <Box sx={rowSx}>
                        <Typography variant="body2" color="text.secondary">Buyer ID</Typography>
                        <Typography variant="body2" sx={valueSx}>#{kit.buyerId}</Typography>
                      </Box>
                      <Box sx={rowSx}>
                        <Typography variant="body2" color="text.secondary">Registration Date</Typography>
                        <Typography variant="body2" sx={valueSx}>{kit.registrationDate}</Typography>
                      </Box>
                    </Box>
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <Box sx={{ display: "flex", flexDirection: "column", gap: 1.5 }}>
                      <Box sx={rowSx}>
                        <Typography variant="body2" color="text.secondary">Active Status</Typography>
                        <Typography variant="body2" sx={valueSx}>Active</Typography>
                      </Box>
                      <Box sx={rowSx}>
                        <Typography variant="body2" color="text.secondary">Assigned Patient</Typography>
                        <Typography variant="body2" sx={valueSx}>{kit.assignedPatientName || "Not Assigned"}</Typography>
                      </Box>
                    </Box>
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} variant="contained" disableElevation>
          Close
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default RegistrationDetailsViewDialog;
