import { useState } from "react";
import {
  Box,
  Button,
  Card,
  CardContent,
  Container,
  Grid,
  Typography,
  Chip,
  Paper,
  IconButton,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Divider,
} from "@mui/material";

import {
  Favorite,
  Thermostat,
  Medication,
  CloudDone,
  Security,
  Devices,
  CheckCircle,
  Menu as MenuIcon,
  Close as CloseIcon,
  ArrowForward,
  Inventory,
  Notifications,
  Lock,
  People,
  Person,
  LocalHospital,
  Speed,
} from "@mui/icons-material";

import { Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { DEFAULT_ROLE_ROUTES } from "../../config/routes/roleRoutes";
import { BRAND_ICON_SRC } from "../../components/common/BrandLogo";


const LandingPage = () => {
  const navigate = useNavigate();
  const { isAuthenticated, role } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);

  if (isAuthenticated) {
    const targetRoute = DEFAULT_ROLE_ROUTES[role] || "/dashboard";
    return <Navigate to={targetRoute} replace />;
  }

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const scrollToSection = (id: string) => {
    const element = document.getElementById(id);
    if (element) {
      element.scrollIntoView({ behavior: "smooth" });
    }
    if (mobileOpen) {
      setMobileOpen(false);
    }
  };

  const navLinks = [
    { label: "Product", id: "product" },
    { label: "How It Works", id: "how-it-works" },
    { label: "Features", id: "features" },
    { label: "Care Team", id: "care-team" },
    { label: "Security", id: "security" },
  ];

  const valueStripItems = [
    { title: "Connected Device Ecosystem", subtitle: "Integrated IoT hardware", icon: <Devices sx={{ color: "#3ec1fa" }} /> },
    { title: "Real-Time Monitoring", subtitle: "Instant status updates", icon: <Speed sx={{ color: "#3ec1fa" }} /> },
    { title: "Offline Data Protection", subtitle: "Local event buffering", icon: <Security sx={{ color: "#3ec1fa" }} /> },
    { title: "Patient, Caregiver and Doctor Access", subtitle: "Role-tailored views", icon: <People sx={{ color: "#3ec1fa" }} /> },
  ];

  const ecosystemProducts = [
    {
      title: "Inner Unit — Smart Insulin Vault",
      description: "Monitors refrigerator temperature, remaining insulin inventory, and storage activity.",
      tag: "Storage Safety",
    },
    {
      title: "Outer Unit — Care Gateway",
      description: "Coordinates device data, provides on-device guidance, supports dose confirmation, and securely connects the ecosystem to the cloud.",
      tag: "Central Controller",
    },
    {
      title: "Pen Unit — Dose Detection",
      description: "Uses magnetic sensing to monitor insulin-pen dial movement and estimate selected dose events.",
      tag: "Dose Sensing",
    },
    {
      title: "Connected Care Platform",
      description: "Combines glucose, dose, storage, inventory, prescription, and alert records in one secure patient timeline.",
      tag: "Cloud & Web",
    },
  ];

  const steps = [
    {
      num: "01",
      title: "Capture",
      text: "Sensors and connected devices collect glucose, dose, storage, and inventory information.",
    },
    {
      num: "02",
      title: "Verify",
      text: "The Outer Unit combines and validates local events while allowing manual confirmation when needed.",
    },
    {
      num: "03",
      title: "Synchronize",
      text: "Verified records are securely uploaded to the cloud, with offline buffering during connectivity interruptions.",
    },
    {
      num: "04",
      title: "Support",
      text: "Patients, caregivers, and doctors access relevant records, history, and alerts through role-based views.",
    },
  ];

  const coreFeatures = [
    {
      title: "AUTOMATED DOSE TRACKING",
      description: "Uses magnetic pen-dial sensing to record estimated insulin-dose events.",
      icon: <Medication sx={{ fontSize: 32, color: "#3ec1fa" }} />,
    },
    {
      title: "GLUCOSE INTEGRATION",
      description: "Retrieves readings from supported Bluetooth Low Energy glucometers.",
      icon: <Favorite sx={{ fontSize: 32, color: "#3ec1fa" }} />,
    },
    {
      title: "STORAGE SAFETY",
      description: "Monitors refrigerator temperature and storage activity.",
      icon: <Thermostat sx={{ fontSize: 32, color: "#3ec1fa" }} />,
    },
    {
      title: "INVENTORY AWARENESS",
      description: "Estimates the remaining insulin quantity and highlights low inventory.",
      icon: <Inventory sx={{ fontSize: 32, color: "#3ec1fa" }} />,
    },
    {
      title: "ALERTS AND CAREGIVER VISIBILITY",
      description: "Provides relevant notifications and remote access to patient information.",
      icon: <Notifications sx={{ fontSize: 32, color: "#3ec1fa" }} />,
    },
    {
      title: "RELIABLE OFFLINE OPERATION",
      description: "Buffers important records locally and synchronizes them after connectivity returns.",
      icon: <CloudDone sx={{ fontSize: 32, color: "#3ec1fa" }} />,
    },
  ];

  const careRoles = [
    {
      role: "PATIENTS",
      description: "Clear daily guidance, glucose and dose history, storage status, reminders, and inventory awareness.",
      icon: <Person sx={{ fontSize: 36, color: "#3ec1fa" }} />,
    },
    {
      role: "CAREGIVERS",
      description: "Remote visibility into adherence-related activity, storage conditions, inventory, and important alerts.",
      icon: <People sx={{ fontSize: 36, color: "#3ec1fa" }} />,
    },
    {
      role: "DOCTORS",
      description: "Structured access to glucose trends, insulin-dose history, prescriptions, and patient records.",
      icon: <LocalHospital sx={{ fontSize: 36, color: "#3ec1fa" }} />,
    },
  ];

  const securityBlocks = [
    {
      title: "ROLE-BASED ACCESS",
      description: "Patients, caregivers, doctors, and administrators receive access based on their responsibilities.",
      icon: <Lock sx={{ color: "#3ec1fa" }} />,
    },
    {
      title: "SECURE COMMUNICATION",
      description: "Encrypted device and web communication protects data during transmission.",
      icon: <Security sx={{ color: "#3ec1fa" }} />,
    },
    {
      title: "OFFLINE RESILIENCE",
      description: "Events are buffered locally when connectivity is unavailable.",
      icon: <CloudDone sx={{ color: "#3ec1fa" }} />,
    },
    {
      title: "CONSISTENT RECORDS",
      description: "Automatic synchronization, event persistence, and duplicate prevention support reliable patient histories.",
      icon: <CheckCircle sx={{ color: "#3ec1fa" }} />,
    },
  ];

  const dashboardFeatures = [
    "Patient Overview",
    "Glucose Trends",
    "Dose History",
    "Storage Monitoring",
    "Inventory Status",
    "Alerts and Notifications",
    "Prescription Tracking",
  ];

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "#f8f9fa", color: "#1e293b" }}>
      {/* 1. STICKY HEADER */}
      <Box
        component="header"
        sx={{
          position: "sticky",
          top: 0,
          zIndex: 1100,
          bgcolor: "#12233b",
          borderBottom: "1px solid rgba(255, 255, 255, 0.08)",
          backdropFilter: "blur(10px)",
          paddingTop: "env(safe-area-inset-top, 0px)",
        }}
      >
        <Container maxWidth="lg">
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              height: 72,
            }}
          >
            {/* Left: Logo & Name */}
            <Box
              sx={{
                display: "flex",
                alignItems: "center",
                gap: 1.5,
                cursor: "pointer",
              }}
              onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
            >
              <Box
                component="img"
                src={BRAND_ICON_SRC}
                alt="Dia-Smart Logo"
                sx={{
                  height: 38,
                  width: "auto",
                  objectFit: "contain",
                  borderRadius: "4px",
                }}
              />
              <Typography
                variant="h6"
                sx={{
                  fontWeight: 700,
                  color: "#ffffff",
                  letterSpacing: "-0.3px",
                  whiteSpace: "nowrap",
                }}
              >
                Dia-Smart
              </Typography>
            </Box>

            {/* Desktop Navigation Links */}
            <Box
              sx={{
                display: { xs: "none", md: "flex" },
                alignItems: "center",
                gap: 3,
              }}
            >
              {navLinks.map((link) => (
                <Typography
                  key={link.id}
                  onClick={() => scrollToSection(link.id)}
                  sx={{
                    color: "rgba(255, 255, 255, 0.8)",
                    fontSize: "0.95rem",
                    fontWeight: 500,
                    cursor: "pointer",
                    transition: "color 0.2s",
                    "&:hover": { color: "#3ec1fa" },
                  }}
                >
                  {link.label}
                </Typography>
              ))}
            </Box>

            {/* Actions */}
            <Box
              sx={{
                display: { xs: "none", md: "flex" },
                alignItems: "center",
                gap: 2,
              }}
            >
              <Button
                variant="text"
                onClick={() => navigate("/login")}
                sx={{
                  color: "#ffffff",
                  fontWeight: 600,
                  textTransform: "none",
                  px: 2,
                  "&:hover": { color: "#3ec1fa", bgcolor: "rgba(255,255,255,0.05)" },
                }}
              >
                Login
              </Button>
              <Button
                variant="contained"
                onClick={() => navigate("/register")}
                sx={{
                  bgcolor: "#3ec1fa",
                  color: "#12233b",
                  fontWeight: 700,
                  textTransform: "none",
                  borderRadius: "8px",
                  px: 2.5,
                  py: 1,
                  boxShadow: "0 4px 14px rgba(62, 193, 250, 0.35)",
                  "&:hover": {
                    bgcolor: "#28b4ef",
                    boxShadow: "0 6px 18px rgba(62, 193, 250, 0.45)",
                  },
                }}
              >
                Get Started
              </Button>
            </Box>

            {/* Mobile Menu Icon */}
            <IconButton
              onClick={handleDrawerToggle}
              sx={{
                display: { xs: "flex", md: "none" },
                color: "#ffffff",
              }}
            >
              <MenuIcon />
            </IconButton>
          </Box>
        </Container>
      </Box>

      {/* Mobile Drawer */}
      <Drawer
        anchor="right"
        open={mobileOpen}
        onClose={handleDrawerToggle}
        slotProps={{
          paper: {
            sx: {
              width: 280,
              bgcolor: "#12233b",
              color: "#ffffff",
              px: 2,
              pt: "calc(24px + env(safe-area-inset-top, 0px))",
              pb: "calc(24px + env(safe-area-inset-bottom, 0px))",
            },
          },
        }}
      >
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 3 }}>
          <Typography variant="h6" sx={{ fontWeight: 700, color: "#ffffff" }}>
            Dia-Smart Navigation
          </Typography>
          <IconButton onClick={handleDrawerToggle} sx={{ color: "#ffffff" }}>
            <CloseIcon />
          </IconButton>
        </Box>
        <Divider sx={{ borderColor: "rgba(255,255,255,0.1)", mb: 2 }} />
        <List>
          {navLinks.map((link) => (
            <ListItem disablePadding key={link.id}>
              <ListItemButton onClick={() => scrollToSection(link.id)}>
                <ListItemText
                  primary={<Typography sx={{ color: "rgba(255,255,255,0.9)", fontWeight: 500 }}>{link.label}</Typography>}
                />
              </ListItemButton>
            </ListItem>
          ))}
        </List>
        <Box sx={{ mt: 4, display: "flex", flexDirection: "column", gap: 2 }}>
          <Button
            variant="outlined"
            fullWidth
            onClick={() => {
              navigate("/login");
              setMobileOpen(false);
            }}
            sx={{ color: "#3ec1fa", borderColor: "#3ec1fa" }}
          >
            Login
          </Button>
          <Button
            variant="contained"
            fullWidth
            onClick={() => {
              navigate("/register");
              setMobileOpen(false);
            }}
            sx={{ bgcolor: "#3ec1fa", color: "#12233b", fontWeight: 700 }}
          >
            Get Started
          </Button>
        </Box>
      </Drawer>

      {/* 2. HERO SECTION */}
      <Box
        sx={{
          bgcolor: "#12233b",
          color: "#ffffff",
          pt: { xs: 8, md: 12 },
          pb: { xs: 10, md: 14 },
          position: "relative",
          overflow: "hidden",
          backgroundImage:
            "radial-gradient(circle at 80% 20%, rgba(62, 193, 250, 0.15) 0%, transparent 55%), linear-gradient(180deg, #12233b 0%, #0d1726 100%)",
        }}
      >
        {/* Static CSS Grid Background Pattern */}
        <Box
          sx={{
            position: "absolute",
            inset: 0,
            opacity: 0.04,
            backgroundImage:
              "linear-gradient(#ffffff 1px, transparent 1px), linear-gradient(90deg, #ffffff 1px, transparent 1px)",
            backgroundSize: "40px 40px",
            pointerEvents: "none",
          }}
        />

        <Container maxWidth="lg" sx={{ position: "relative", zIndex: 1 }}>
          <Grid container spacing={5} sx={{ alignItems: "center" }}>
            {/* Left Side */}
            <Grid size={{ xs: 12, md: 6 }}>
              <Chip
                label="SMART DIABETES CARE ECOSYSTEM"
                size="small"
                sx={{
                  bgcolor: "rgba(62, 193, 250, 0.12)",
                  color: "#3ec1fa",
                  border: "1px solid rgba(62, 193, 250, 0.3)",
                  fontWeight: 700,
                  letterSpacing: "1.2px",
                  fontSize: "0.75rem",
                  mb: 2.5,
                  px: 1,
                }}
              />

              <Typography
                variant="h2"
                component="h1"
                sx={{
                  fontWeight: 800,
                  fontSize: { xs: "2.25rem", sm: "3rem", md: "3.5rem" },
                  lineHeight: 1.15,
                  letterSpacing: "-1px",
                  mb: 3,
                }}
              >
                Smarter insulin management for{" "}
                <Box
                  component="span"
                  sx={{
                    background: "linear-gradient(135deg, #3ec1fa 0%, #00b894 100%)",
                    WebkitBackgroundClip: "text",
                    WebkitTextFillColor: "transparent",
                    display: "inline-block",
                  }}
                >
                  safer daily diabetes care
                </Box>
              </Typography>

              <Typography
                sx={{
                  color: "rgba(255, 255, 255, 0.82)",
                  fontSize: { xs: "1rem", md: "1.125rem" },
                  lineHeight: 1.65,
                  mb: 4,
                  maxWidth: 540,
                }}
              >
                Dia-Smart connects insulin-dose tracking, glucose readings, storage safety, inventory monitoring, and caregiver visibility through one integrated IoT platform.
              </Typography>

              <Box sx={{ display: "flex", flexDirection: { xs: "column", sm: "row" }, gap: 2, alignItems: { xs: "stretch", sm: "center" } }}>
                <Button
                  variant="contained"
                  size="large"
                  onClick={() => navigate("/register")}
                  sx={{
                    bgcolor: "#3ec1fa",
                    color: "#12233b",
                    fontWeight: 700,
                    fontSize: "1rem",
                    px: 3.5,
                    py: 1.5,
                    borderRadius: "10px",
                    boxShadow: "0 4px 20px rgba(62, 193, 250, 0.4)",
                    textTransform: "none",
                    "&:hover": { bgcolor: "#28b4ef" },
                  }}
                >
                  Get Started
                </Button>

                <Button
                  variant="outlined"
                  size="large"
                  onClick={() => navigate("/login")}
                  sx={{
                    color: "#ffffff",
                    borderColor: "rgba(255, 255, 255, 0.35)",
                    fontWeight: 600,
                    fontSize: "1rem",
                    px: 3.5,
                    py: 1.5,
                    borderRadius: "10px",
                    textTransform: "none",
                    "&:hover": {
                      borderColor: "#ffffff",
                      bgcolor: "rgba(255, 255, 255, 0.08)",
                    },
                  }}
                >
                  Login
                </Button>
              </Box>

              <Box sx={{ mt: 3 }}>
                <Button
                  variant="text"
                  onClick={() => scrollToSection("product")}
                  endIcon={<ArrowForward sx={{ color: "#3ec1fa" }} />}
                  sx={{
                    color: "rgba(255, 255, 255, 0.75)",
                    fontSize: "0.9rem",
                    textTransform: "none",
                    p: 0,
                    "&:hover": { color: "#3ec1fa", bgcolor: "transparent" },
                  }}
                >
                  Explore the Product
                </Button>
              </Box>
            </Grid>

            {/* Right Side: Stylized Dia-Smart Product Ecosystem Illustration */}
            <Grid size={{ xs: 12, md: 6 }}>
              <Box
                sx={{
                  position: "relative",
                  width: "100%",
                  minHeight: { xs: 380, sm: 460, md: 480 },
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                }}
              >
                {/* Product Family Container */}
                <Paper
                  elevation={0}
                  sx={{
                    width: "100%",
                    height: "100%",
                    bgcolor: "rgba(18, 35, 59, 0.75)",
                    border: "1px solid rgba(62, 193, 250, 0.25)",
                    borderRadius: "20px",
                    p: 3,
                    backdropFilter: "blur(12px)",
                    position: "relative",
                    boxShadow: "0 20px 50px rgba(0,0,0,0.5)",
                  }}
                >
                  {/* Subtle Connection Lines SVG */}
                  <Box
                    component="svg"
                    sx={{
                      position: "absolute",
                      inset: 0,
                      width: "100%",
                      height: "100%",
                      pointerEvents: "none",
                    }}
                  >
                    {/* Line from Glucometer to Outer Unit */}
                    <line x1="25%" y1="68%" x2="48%" y2="50%" stroke="#3ec1fa" strokeWidth="1.5" strokeDasharray="4 3" opacity="0.6" />
                    {/* Line from Pen Unit to Outer Unit */}
                    <line x1="48%" y1="82%" x2="48%" y2="60%" stroke="#3ec1fa" strokeWidth="1.5" strokeDasharray="4 3" opacity="0.6" />
                    {/* Line from Inner Unit to Outer Unit */}
                    <line x1="78%" y1="45%" x2="56%" y2="50%" stroke="#3ec1fa" strokeWidth="1.5" strokeDasharray="4 3" opacity="0.6" />
                    {/* Line from Outer Unit to Laptop Dashboard */}
                    <line x1="48%" y1="40%" x2="48%" y2="22%" stroke="#00b894" strokeWidth="1.5" opacity="0.7" />
                  </Box>

                  {/* 1. Laptop / Dashboard (Positioned Behind / Top Center) */}
                  <Box
                    sx={{
                      position: "absolute",
                      top: 20,
                      left: "50%",
                      transform: "translateX(-50%)",
                      width: { xs: "85%", sm: "70%" },
                      bgcolor: "#0f172a",
                      borderRadius: "12px",
                      border: "1px solid rgba(255, 255, 255, 0.15)",
                      boxShadow: "0 8px 30px rgba(0,0,0,0.6)",
                      overflow: "hidden",
                      zIndex: 1,
                    }}
                  >
                    {/* Laptop Header Bar */}
                    <Box sx={{ bgcolor: "#1e293b", px: 1.5, py: 0.75, display: "flex", alignItems: "center", gap: 0.75 }}>
                      <Box sx={{ width: 8, height: 8, borderRadius: "50%", bgcolor: "#ef4444" }} />
                      <Box sx={{ width: 8, height: 8, borderRadius: "50%", bgcolor: "#eab308" }} />
                      <Box sx={{ width: 8, height: 8, borderRadius: "50%", bgcolor: "#22c55e" }} />
                      <Typography variant="caption" sx={{ color: "rgba(255,255,255,0.6)", fontSize: "0.65rem", ml: 1, fontWeight: 600 }}>
                        Dia-Smart Cloud Dashboard
                      </Typography>
                    </Box>
                    {/* Laptop Screen Content */}
                    <Box sx={{ p: 1.5, bgcolor: "#0b1320" }}>
                      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 1 }}>
                        <Typography variant="caption" sx={{ color: "#3ec1fa", fontSize: "0.7rem", fontWeight: 700 }}>
                          LIVE PATIENT TIMELINE
                        </Typography>
                        <Chip label="CLOUD SYNCED" size="small" sx={{ height: 16, fontSize: "0.6rem", bgcolor: "rgba(0,184,148,0.2)", color: "#00b894", border: "1px solid #00b894" }} />
                      </Box>
                      <Grid container spacing={1}>
                        <Grid size={{ xs: 6 }}>
                          <Box sx={{ bgcolor: "rgba(255,255,255,0.05)", p: 0.75, borderRadius: "6px", border: "1px solid rgba(255,255,255,0.08)" }}>
                            <Typography variant="caption" sx={{ color: "rgba(255,255,255,0.6)", fontSize: "0.6rem", display: "block" }}>GLUCOSE</Typography>
                            <Typography variant="caption" sx={{ color: "#ffffff", fontWeight: 700, fontSize: "0.8rem" }}>118 mg/dL</Typography>
                          </Box>
                        </Grid>
                        <Grid size={{ xs: 6 }}>
                          <Box sx={{ bgcolor: "rgba(255,255,255,0.05)", p: 0.75, borderRadius: "6px", border: "1px solid rgba(255,255,255,0.08)" }}>
                            <Typography variant="caption" sx={{ color: "rgba(255,255,255,0.6)", fontSize: "0.6rem", display: "block" }}>DOSE RECORDED</Typography>
                            <Typography variant="caption" sx={{ color: "#3ec1fa", fontWeight: 700, fontSize: "0.8rem" }}>12 U</Typography>
                          </Box>
                        </Grid>
                      </Grid>
                    </Box>
                    <Box sx={{ textAlign: "center", py: 0.25, bgcolor: "#1e293b" }}>
                      <Typography variant="caption" sx={{ color: "#3ec1fa", fontSize: "0.6rem", fontWeight: 600 }}>Laptop / Dashboard</Typography>
                    </Box>
                  </Box>

                  {/* 2. Outer Unit (Central Foreground Device) */}
                  <Box
                    sx={{
                      position: "absolute",
                      top: "44%",
                      left: "48%",
                      transform: "translate(-50%, -40%)",
                      width: { xs: 150, sm: 170 },
                      bgcolor: "#1a2b42",
                      borderRadius: "14px",
                      border: "2px solid #3ec1fa",
                      boxShadow: "0 10px 30px rgba(0,0,0,0.7), 0 0 20px rgba(62, 193, 250, 0.2)",
                      p: 1.5,
                      zIndex: 3,
                    }}
                  >
                    <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 1 }}>
                      <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
                        <Box sx={{ width: 6, height: 6, borderRadius: "50%", bgcolor: "#00b894" }} />
                        <Typography variant="caption" sx={{ color: "#ffffff", fontSize: "0.65rem", fontWeight: 700 }}>
                          OUTER UNIT
                        </Typography>
                      </Box>
                      <Chip label="GATEWAY" size="small" sx={{ height: 14, fontSize: "0.55rem", bgcolor: "#3ec1fa", color: "#12233b", fontWeight: 800 }} />
                    </Box>
                    {/* On-device Screen */}
                    <Box
                      sx={{
                        bgcolor: "#060d17",
                        borderRadius: "6px",
                        p: 1,
                        border: "1px solid rgba(62,193,250,0.4)",
                        mb: 1.2,
                        textAlign: "center",
                      }}
                    >
                      <Typography variant="caption" sx={{ color: "#3ec1fa", fontSize: "0.65rem", fontWeight: 700, display: "block" }}>
                        DOSE CONFIRMED
                      </Typography>
                      <Typography variant="caption" sx={{ color: "#ffffff", fontSize: "0.75rem", fontWeight: 800 }}>
                        12 U • 08:15 AM
                      </Typography>
                    </Box>
                    {/* Numeric Keypad Illustration */}
                    <Grid container spacing={0.5} sx={{ mb: 0.5 }}>
                      {[1, 2, 3, 4, 5, 6, 7, 8, 9].map((num) => (
                        <Grid size={{ xs: 4 }} key={num}>
                          <Box
                            sx={{
                              bgcolor: "rgba(255,255,255,0.08)",
                              borderRadius: "3px",
                              py: 0.2,
                              textAlign: "center",
                            }}
                          >
                            <Typography variant="caption" sx={{ color: "rgba(255,255,255,0.7)", fontSize: "0.55rem", fontWeight: 600 }}>
                              {num}
                            </Typography>
                          </Box>
                        </Grid>
                      ))}
                    </Grid>
                    <Box sx={{ display: "flex", gap: 0.5, justifyContent: "center" }}>
                      <Box sx={{ bgcolor: "#00b894", borderRadius: "3px", px: 1, py: 0.2 }}>
                        <Typography variant="caption" sx={{ color: "#ffffff", fontSize: "0.55rem", fontWeight: 700 }}>ENT</Typography>
                      </Box>
                      <Box sx={{ bgcolor: "#ef4444", borderRadius: "3px", px: 1, py: 0.2 }}>
                        <Typography variant="caption" sx={{ color: "#ffffff", fontSize: "0.55rem", fontWeight: 700 }}>CAN</Typography>
                      </Box>
                    </Box>
                    <Typography variant="caption" sx={{ color: "#3ec1fa", fontSize: "0.55rem", textAlign: "center", display: "block", mt: 0.5, fontWeight: 600 }}>
                      Outer Unit (Gateway)
                    </Typography>
                  </Box>

                  {/* 3. Glucometer (Beside Outer Unit on Left) */}
                  <Box
                    sx={{
                      position: "absolute",
                      bottom: 70,
                      left: 15,
                      width: { xs: 110, sm: 130 },
                      bgcolor: "#ffffff",
                      color: "#1e293b",
                      borderRadius: "12px",
                      border: "1px solid #cbd5e1",
                      boxShadow: "0 8px 24px rgba(0,0,0,0.3)",
                      p: 1.2,
                      zIndex: 2,
                    }}
                  >
                    <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, mb: 0.5 }}>
                      <Favorite sx={{ fontSize: 14, color: "#ef4444" }} />
                      <Typography variant="caption" sx={{ fontWeight: 800, fontSize: "0.65rem", color: "#0f172a" }}>
                        GLUCOMETER
                      </Typography>
                    </Box>
                    <Box sx={{ bgcolor: "#f1f5f9", p: 0.75, borderRadius: "6px", textAlign: "center", mb: 0.5, border: "1px solid #e2e8f0" }}>
                      <Typography variant="caption" sx={{ fontSize: "0.55rem", color: "#64748b", display: "block" }}>BLE READING</Typography>
                      <Typography variant="caption" sx={{ fontSize: "0.85rem", fontWeight: 800, color: "#0284c7" }}>118 mg/dL</Typography>
                    </Box>
                    <Chip label="BLE CONNECTED" size="small" sx={{ height: 14, fontSize: "0.5rem", bgcolor: "rgba(62,193,250,0.15)", color: "#0284c7", fontWeight: 700, width: "100%" }} />
                  </Box>

                  {/* 4. Pen Unit (Horizontal Foreground) */}
                  <Box
                    sx={{
                      position: "absolute",
                      bottom: 15,
                      left: "50%",
                      transform: "translateX(-50%)",
                      width: { xs: "85%", sm: "75%" },
                      bgcolor: "#1e293b",
                      color: "#ffffff",
                      borderRadius: "10px",
                      border: "1px solid #3ec1fa",
                      boxShadow: "0 10px 25px rgba(0,0,0,0.5)",
                      px: 1.5,
                      py: 1,
                      zIndex: 4,
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                    }}
                  >
                    <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                      <Box sx={{ width: 12, height: 12, borderRadius: "50%", bgcolor: "#3ec1fa", display: "flex", alignItems: "center", justifyContent: "center" }}>
                        <Box sx={{ width: 6, height: 6, borderRadius: "50%", bgcolor: "#12233b" }} />
                      </Box>
                      <Typography variant="caption" sx={{ fontWeight: 700, fontSize: "0.7rem", color: "#ffffff" }}>
                        PEN UNIT — MAGNETIC DOSE SENSING
                      </Typography>
                    </Box>
                    <Box sx={{ display: "flex", gap: 1, alignItems: "center" }}>
                      <Box sx={{ bgcolor: "rgba(62,193,250,0.2)", px: 1, py: 0.2, borderRadius: "4px", border: "1px solid #3ec1fa" }}>
                        <Typography variant="caption" sx={{ color: "#3ec1fa", fontWeight: 800, fontSize: "0.7rem" }}>
                          DIAL: 12 U
                        </Typography>
                      </Box>
                    </Box>
                  </Box>

                  {/* 5. Inner Unit (Smart Insulin Vault on Right Side) */}
                  <Box
                    sx={{
                      position: "absolute",
                      top: 75,
                      right: 15,
                      width: { xs: 115, sm: 135 },
                      bgcolor: "#ffffff",
                      color: "#1e293b",
                      borderRadius: "14px",
                      border: "1px solid #cbd5e1",
                      boxShadow: "0 10px 25px rgba(0,0,0,0.3)",
                      p: 1.2,
                      zIndex: 2,
                    }}
                  >
                    <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, mb: 0.75 }}>
                      <Thermostat sx={{ fontSize: 16, color: "#0284c7" }} />
                      <Typography variant="caption" sx={{ fontWeight: 800, fontSize: "0.65rem", color: "#0f172a" }}>
                        INNER UNIT
                      </Typography>
                    </Box>
                    <Box sx={{ bgcolor: "#f8fafc", p: 0.75, borderRadius: "6px", border: "1px solid #e2e8f0", mb: 0.75 }}>
                      <Typography variant="caption" sx={{ fontSize: "0.55rem", color: "#64748b", display: "block" }}>STORAGE TEMP</Typography>
                      <Typography variant="caption" sx={{ fontSize: "0.85rem", fontWeight: 800, color: "#00b894" }}>4.2 °C</Typography>
                    </Box>
                    <Box sx={{ bgcolor: "#f8fafc", p: 0.75, borderRadius: "6px", border: "1px solid #e2e8f0", mb: 0.5 }}>
                      <Typography variant="caption" sx={{ fontSize: "0.55rem", color: "#64748b", display: "block" }}>INVENTORY</Typography>
                      <Typography variant="caption" sx={{ fontSize: "0.75rem", fontWeight: 700, color: "#1e293b" }}>4 Vials Safe</Typography>
                    </Box>
                    <Typography variant="caption" sx={{ color: "#0284c7", fontSize: "0.55rem", textAlign: "center", display: "block", fontWeight: 600 }}>
                      Smart Insulin Vault
                    </Typography>
                  </Box>
                </Paper>
              </Box>
            </Grid>
          </Grid>
        </Container>
      </Box>

      {/* 3. PRODUCT VALUE STRIP */}
      <Box
        sx={{
          bgcolor: "#ffffff",
          borderBottom: "1px solid #e2e8f0",
          py: 3,
          boxShadow: "0 4px 20px rgba(0,0,0,0.03)",
        }}
      >
        <Container maxWidth="lg">
          <Grid container spacing={2}>
            {valueStripItems.map((item, idx) => (
              <Grid size={{ xs: 12, sm: 6, md: 3 }} key={idx}>
                <Box
                  sx={{
                    display: "flex",
                    alignItems: "center",
                    gap: 1.5,
                    p: 1.5,
                    borderRadius: "12px",
                    bgcolor: "#f8fafc",
                    border: "1px solid #f1f5f9",
                    height: "100%",
                  }}
                >
                  <Box
                    sx={{
                      width: 44,
                      height: 44,
                      borderRadius: "10px",
                      bgcolor: "rgba(62, 193, 250, 0.1)",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      flexShrink: 0,
                    }}
                  >
                    {item.icon}
                  </Box>
                  <Box>
                    <Typography variant="subtitle2" sx={{ fontWeight: 700, fontSize: "0.875rem", color: "#0f172a", lineHeight: 1.2 }}>
                      {item.title}
                    </Typography>
                    <Typography variant="caption" sx={{ color: "#64748b", fontSize: "0.75rem" }}>
                      {item.subtitle}
                    </Typography>
                  </Box>
                </Box>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* 4. DIA-SMART ECOSYSTEM SECTION */}
      <Box id="product" sx={{ py: { xs: 8, md: 12 }, bgcolor: "#f8f9fa" }}>
        <Container maxWidth="lg">
          <Box sx={{ textAlign: "center", maxWidth: 700, mx: "auto", mb: 7 }}>
            <Typography
              variant="caption"
              sx={{
                color: "#0284c7",
                fontWeight: 800,
                letterSpacing: "1.5px",
                textTransform: "uppercase",
                display: "block",
                mb: 1,
              }}
            >
              THE DIA-SMART ECOSYSTEM
            </Typography>
            <Typography
              variant="h3"
              component="h2"
              sx={{ fontWeight: 800, color: "#0f172a", fontSize: { xs: "1.875rem", md: "2.5rem" }, mb: 2 }}
            >
              Everything needed for connected insulin monitoring
            </Typography>
            <Typography variant="body1" sx={{ color: "#64748b", fontSize: "1.05rem", lineHeight: 1.6 }}>
              Dia-Smart combines physical monitoring devices with a secure cloud platform to create one connected experience for patients, caregivers, and healthcare professionals.
            </Typography>
          </Box>

          <Grid container spacing={3}>
            {ecosystemProducts.map((prod, index) => (
              <Grid size={{ xs: 12, sm: 6, md: 3 }} key={index}>
                <Card
                  elevation={0}
                  sx={{
                    height: "100%",
                    borderRadius: "16px",
                    border: "1px solid #e2e8f0",
                    bgcolor: "#ffffff",
                    transition: "all 0.3s ease",
                    display: "flex",
                    flexDirection: "column",
                    "&:hover": {
                      transform: "translateY(-6px)",
                      boxShadow: "0 12px 30px rgba(0,0,0,0.08)",
                      borderColor: "#3ec1fa",
                    },
                  }}
                >
                  <CardContent sx={{ p: 3, display: "flex", flexDirection: "column", flexGrow: 1 }}>
                    <Box sx={{ mb: 2 }}>
                      <Chip
                        label={prod.tag}
                        size="small"
                        sx={{
                          bgcolor: "rgba(62,193,250,0.1)",
                          color: "#0284c7",
                          fontWeight: 700,
                          fontSize: "0.7rem",
                        }}
                      />
                    </Box>
                    <Typography variant="h6" sx={{ fontWeight: 700, color: "#0f172a", mb: 1.5, fontSize: "1.1rem", lineHeight: 1.3 }}>
                      {prod.title}
                    </Typography>
                    <Typography variant="body2" sx={{ color: "#64748b", lineHeight: 1.6, flexGrow: 1 }}>
                      {prod.description}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* 5. HOW IT WORKS SECTION */}
      <Box id="how-it-works" sx={{ py: { xs: 8, md: 12 }, bgcolor: "#ffffff", borderTop: "1px solid #e2e8f0" }}>
        <Container maxWidth="lg">
          <Box sx={{ textAlign: "center", maxWidth: 700, mx: "auto", mb: 8 }}>
            <Typography
              variant="caption"
              sx={{
                color: "#0284c7",
                fontWeight: 800,
                letterSpacing: "1.5px",
                textTransform: "uppercase",
                display: "block",
                mb: 1,
              }}
            >
              HOW IT WORKS
            </Typography>
            <Typography
              variant="h3"
              component="h2"
              sx={{ fontWeight: 800, color: "#0f172a", fontSize: { xs: "1.875rem", md: "2.5rem" } }}
            >
              From device activity to connected care
            </Typography>
          </Box>

          <Grid container spacing={3}>
            {steps.map((st, i) => (
              <Grid size={{ xs: 12, sm: 6, md: 3 }} key={i}>
                <Paper
                  elevation={0}
                  sx={{
                    p: 3.5,
                    height: "100%",
                    borderRadius: "16px",
                    bgcolor: "#f8fafc",
                    border: "1px solid #e2e8f0",
                    position: "relative",
                    overflow: "hidden",
                  }}
                >
                  <Typography
                    variant="h2"
                    sx={{
                      fontWeight: 900,
                      color: "rgba(62, 193, 250, 0.25)",
                      fontSize: "3.5rem",
                      lineHeight: 1,
                      mb: 1,
                    }}
                  >
                    {st.num}
                  </Typography>
                  <Typography variant="h6" sx={{ fontWeight: 700, color: "#0f172a", mb: 1.5 }}>
                    {st.title}
                  </Typography>
                  <Typography variant="body2" sx={{ color: "#64748b", lineHeight: 1.6 }}>
                    {st.text}
                  </Typography>
                </Paper>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* 6. CORE FEATURES SECTION */}
      <Box id="features" sx={{ py: { xs: 8, md: 12 }, bgcolor: "#f8f9fa" }}>
        <Container maxWidth="lg">
          <Box sx={{ textAlign: "center", maxWidth: 700, mx: "auto", mb: 7 }}>
            <Typography
              variant="caption"
              sx={{
                color: "#0284c7",
                fontWeight: 800,
                letterSpacing: "1.5px",
                textTransform: "uppercase",
                display: "block",
                mb: 1,
              }}
            >
              CORE FEATURES
            </Typography>
            <Typography
              variant="h3"
              component="h2"
              sx={{ fontWeight: 800, color: "#0f172a", fontSize: { xs: "1.875rem", md: "2.5rem" } }}
            >
              Essential monitoring features in one connected platform
            </Typography>
          </Box>

          <Grid container spacing={3}>
            {coreFeatures.map((feat, idx) => (
              <Grid size={{ xs: 12, sm: 6, md: 4 }} key={idx}>
                <Card
                  elevation={0}
                  sx={{
                    p: 3,
                    height: "100%",
                    borderRadius: "16px",
                    border: "1px solid #e2e8f0",
                    bgcolor: "#ffffff",
                    transition: "all 0.2s ease-in-out",
                    "&:hover": {
                      borderColor: "#3ec1fa",
                      boxShadow: "0 8px 25px rgba(0,0,0,0.06)",
                    },
                  }}
                >
                  <Box
                    sx={{
                      width: 56,
                      height: 56,
                      borderRadius: "12px",
                      bgcolor: "rgba(62, 193, 250, 0.1)",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      mb: 2.5,
                    }}
                  >
                    {feat.icon}
                  </Box>
                  <Typography variant="h6" sx={{ fontWeight: 700, color: "#0f172a", mb: 1, fontSize: "1.05rem" }}>
                    {feat.title}
                  </Typography>
                  <Typography variant="body2" sx={{ color: "#64748b", lineHeight: 1.6 }}>
                    {feat.description}
                  </Typography>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* 7. DESIGNED FOR CONNECTED CARE */}
      <Box id="care-team" sx={{ py: { xs: 8, md: 12 }, bgcolor: "#ffffff", borderTop: "1px solid #e2e8f0" }}>
        <Container maxWidth="lg">
          <Box sx={{ textAlign: "center", maxWidth: 700, mx: "auto", mb: 7 }}>
            <Typography
              variant="caption"
              sx={{
                color: "#0284c7",
                fontWeight: 800,
                letterSpacing: "1.5px",
                textTransform: "uppercase",
                display: "block",
                mb: 1,
              }}
            >
              CONNECTED CARE
            </Typography>
            <Typography
              variant="h3"
              component="h2"
              sx={{ fontWeight: 800, color: "#0f172a", fontSize: { xs: "1.875rem", md: "2.5rem" } }}
            >
              Designed around patients and the people who support them
            </Typography>
          </Box>

          <Grid container spacing={4}>
            {careRoles.map((cRole, idx) => (
              <Grid size={{ xs: 12, md: 4 }} key={idx}>
                <Paper
                  elevation={0}
                  sx={{
                    p: 4,
                    height: "100%",
                    borderRadius: "18px",
                    bgcolor: "#f8fafc",
                    border: "1px solid #e2e8f0",
                    display: "flex",
                    flexDirection: "column",
                  }}
                >
                  <Box
                    sx={{
                      width: 60,
                      height: 60,
                      borderRadius: "14px",
                      bgcolor: "rgba(62, 193, 250, 0.12)",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      mb: 3,
                    }}
                  >
                    {cRole.icon}
                  </Box>
                  <Typography variant="h5" sx={{ fontWeight: 800, color: "#0f172a", mb: 2 }}>
                    {cRole.role}
                  </Typography>
                  <Typography variant="body1" sx={{ color: "#64748b", lineHeight: 1.65 }}>
                    {cRole.description}
                  </Typography>
                </Paper>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* 8. DASHBOARD PREVIEW SECTION */}
      <Box sx={{ py: { xs: 8, md: 12 }, bgcolor: "#f8f9fa" }}>
        <Container maxWidth="lg">
          <Box sx={{ textAlign: "center", maxWidth: 700, mx: "auto", mb: 7 }}>
            <Typography
              variant="caption"
              sx={{
                color: "#0284c7",
                fontWeight: 800,
                letterSpacing: "1.5px",
                textTransform: "uppercase",
                display: "block",
                mb: 1,
              }}
            >
              CONNECTED DASHBOARD
            </Typography>
            <Typography
              variant="h3"
              component="h2"
              sx={{ fontWeight: 800, color: "#0f172a", fontSize: { xs: "1.875rem", md: "2.5rem" } }}
            >
              One view of the patient’s diabetes-care activity
            </Typography>
          </Box>

          <Grid container spacing={4} sx={{ alignItems: "center" }}>
            {/* Dashboard Mockup */}
            <Grid size={{ xs: 12, lg: 8 }}>
              <Paper
                elevation={0}
                sx={{
                  borderRadius: "18px",
                  border: "1px solid #cbd5e1",
                  boxShadow: "0 20px 40px rgba(0,0,0,0.08)",
                  overflow: "hidden",
                  bgcolor: "#ffffff",
                }}
              >
                {/* Browser Mockup Top Bar */}
                <Box
                  sx={{
                    bgcolor: "#0f172a",
                    color: "#ffffff",
                    px: 2,
                    py: 1.25,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                  }}
                >
                  <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                    <Box sx={{ width: 10, height: 10, borderRadius: "50%", bgcolor: "#ef4444" }} />
                    <Box sx={{ width: 10, height: 10, borderRadius: "50%", bgcolor: "#eab308" }} />
                    <Box sx={{ width: 10, height: 10, borderRadius: "50%", bgcolor: "#22c55e" }} />
                    <Typography variant="caption" sx={{ ml: 1, color: "rgba(255,255,255,0.7)", fontWeight: 600 }}>
                      Dia-Smart Patient Portal
                    </Typography>
                  </Box>
                  <Chip
                    label="Demo Data"
                    size="small"
                    sx={{
                      bgcolor: "rgba(239, 68, 68, 0.2)",
                      color: "#f87171",
                      border: "1px solid #f87171",
                      fontWeight: 700,
                      height: 20,
                      fontSize: "0.65rem",
                    }}
                  />
                </Box>

                {/* Dashboard Body Preview */}
                <Box sx={{ p: { xs: 2, sm: 3 }, bgcolor: "#f8fafc" }}>
                  {/* Top Stats Cards */}
                  <Grid container spacing={2} sx={{ mb: 3 }}>
                    <Grid size={{ xs: 6, sm: 3 }}>
                      <Paper elevation={0} sx={{ p: 1.5, borderRadius: "12px", border: "1px solid #e2e8f0", bgcolor: "#ffffff" }}>
                        <Typography variant="caption" sx={{ color: "#64748b", display: "block", fontWeight: 600 }}>
                          Latest Glucose
                        </Typography>
                        <Typography variant="h6" sx={{ fontWeight: 800, color: "#0284c7", my: 0.5 }}>
                          118 mg/dL
                        </Typography>
                        <Chip label="Normal • 10:30 AM" size="small" sx={{ height: 18, fontSize: "0.6rem", bgcolor: "#e0f2fe", color: "#0369a1" }} />
                      </Paper>
                    </Grid>
                    <Grid size={{ xs: 6, sm: 3 }}>
                      <Paper elevation={0} sx={{ p: 1.5, borderRadius: "12px", border: "1px solid #e2e8f0", bgcolor: "#ffffff" }}>
                        <Typography variant="caption" sx={{ color: "#64748b", display: "block", fontWeight: 600 }}>
                          Recent Dose
                        </Typography>
                        <Typography variant="h6" sx={{ fontWeight: 800, color: "#0f172a", my: 0.5 }}>
                          12 U
                        </Typography>
                        <Chip label="Rapid-Acting • 08:15 AM" size="small" sx={{ height: 18, fontSize: "0.6rem", bgcolor: "#f1f5f9", color: "#475569" }} />
                      </Paper>
                    </Grid>
                    <Grid size={{ xs: 6, sm: 3 }}>
                      <Paper elevation={0} sx={{ p: 1.5, borderRadius: "12px", border: "1px solid #e2e8f0", bgcolor: "#ffffff" }}>
                        <Typography variant="caption" sx={{ color: "#64748b", display: "block", fontWeight: 600 }}>
                          Storage Temp
                        </Typography>
                        <Typography variant="h6" sx={{ fontWeight: 800, color: "#00b894", my: 0.5 }}>
                          4.2 °C
                        </Typography>
                        <Chip label="Safe (2°C - 8°C)" size="small" sx={{ height: 18, fontSize: "0.6rem", bgcolor: "#e6f4ea", color: "#137333" }} />
                      </Paper>
                    </Grid>
                    <Grid size={{ xs: 6, sm: 3 }}>
                      <Paper elevation={0} sx={{ p: 1.5, borderRadius: "12px", border: "1px solid #e2e8f0", bgcolor: "#ffffff" }}>
                        <Typography variant="caption" sx={{ color: "#64748b", display: "block", fontWeight: 600 }}>
                          Inventory
                        </Typography>
                        <Typography variant="h6" sx={{ fontWeight: 800, color: "#0f172a", my: 0.5 }}>
                          4 Vials
                        </Typography>
                        <Chip label="Sufficient" size="small" sx={{ height: 18, fontSize: "0.6rem", bgcolor: "#f1f5f9", color: "#475569" }} />
                      </Paper>
                    </Grid>
                  </Grid>

                  {/* Trend Placeholder Chart */}
                  <Paper elevation={0} sx={{ p: 2, borderRadius: "12px", border: "1px solid #e2e8f0", bgcolor: "#ffffff", mb: 2 }}>
                    <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 1.5 }}>
                      <Typography variant="subtitle2" sx={{ fontWeight: 700, color: "#0f172a" }}>
                        24-Hour Glucose Trend (mg/dL)
                      </Typography>
                      <Chip label="Target: 70 - 140" size="small" sx={{ height: 18, fontSize: "0.65rem", bgcolor: "#f8fafc" }} />
                    </Box>
                    <Box sx={{ width: "100%", height: 120 }}>
                      <svg width="100%" height="100%" viewBox="0 0 500 120" preserveAspectRatio="none">
                        {/* Target Range Band */}
                        <rect x="0" y="30" width="500" height="60" fill="rgba(62,193,250,0.08)" />
                        {/* Grid lines */}
                        <line x1="0" y1="30" x2="500" y2="30" stroke="#e2e8f0" strokeDasharray="3 3" />
                        <line x1="0" y1="90" x2="500" y2="90" stroke="#e2e8f0" strokeDasharray="3 3" />
                        {/* Wave curve */}
                        <path
                          d="M 0,65 Q 60,40 120,55 T 240,45 T 360,70 T 500,50"
                          fill="none"
                          stroke="#0284c7"
                          strokeWidth="3"
                        />
                      </svg>
                    </Box>
                  </Paper>
                </Box>
              </Paper>
            </Grid>

            {/* Feature List Beside Preview */}
            <Grid size={{ xs: 12, lg: 4 }}>
              <Box sx={{ pl: { lg: 2 } }}>
                <Typography variant="h5" sx={{ fontWeight: 800, color: "#0f172a", mb: 3 }}>
                  Complete Patient Care Interface
                </Typography>

                <List disablePadding>
                  {dashboardFeatures.map((item, index) => (
                    <ListItem key={index} disableGutters sx={{ py: 1.25 }}>
                      <Box
                        sx={{
                          width: 24,
                          height: 24,
                          borderRadius: "50%",
                          bgcolor: "rgba(62, 193, 250, 0.15)",
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          mr: 2,
                          flexShrink: 0,
                        }}
                      >
                        <CheckCircle sx={{ fontSize: 16, color: "#0284c7" }} />
                      </Box>
                      <ListItemText
                        primary={<Typography sx={{ fontWeight: 600, color: "#1e293b", fontSize: "1rem" }}>{item}</Typography>}
                      />
                    </ListItem>
                  ))}
                </List>
              </Box>
            </Grid>
          </Grid>
        </Container>
      </Box>

      {/* 9. SECURITY AND RELIABILITY SECTION */}
      <Box id="security" sx={{ py: { xs: 8, md: 12 }, bgcolor: "#ffffff", borderTop: "1px solid #e2e8f0" }}>
        <Container maxWidth="lg">
          <Box sx={{ textAlign: "center", maxWidth: 700, mx: "auto", mb: 7 }}>
            <Typography
              variant="caption"
              sx={{
                color: "#0284c7",
                fontWeight: 800,
                letterSpacing: "1.5px",
                textTransform: "uppercase",
                display: "block",
                mb: 1,
              }}
            >
              SECURITY AND RELIABILITY
            </Typography>
            <Typography
              variant="h3"
              component="h2"
              sx={{ fontWeight: 800, color: "#0f172a", fontSize: { xs: "1.875rem", md: "2.5rem" } }}
            >
              Protected data and dependable synchronization
            </Typography>
          </Box>

          <Grid container spacing={3}>
            {securityBlocks.map((blk, idx) => (
              <Grid size={{ xs: 12, sm: 6, md: 3 }} key={idx}>
                <Paper
                  elevation={0}
                  sx={{
                    p: 3.5,
                    height: "100%",
                    borderRadius: "16px",
                    bgcolor: "#f8fafc",
                    border: "1px solid #e2e8f0",
                  }}
                >
                  <Box
                    sx={{
                      width: 48,
                      height: 48,
                      borderRadius: "12px",
                      bgcolor: "rgba(62, 193, 250, 0.12)",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      mb: 2.5,
                    }}
                  >
                    {blk.icon}
                  </Box>
                  <Typography variant="h6" sx={{ fontWeight: 700, color: "#0f172a", mb: 1, fontSize: "1.05rem" }}>
                    {blk.title}
                  </Typography>
                  <Typography variant="body2" sx={{ color: "#64748b", lineHeight: 1.6 }}>
                    {blk.description}
                  </Typography>
                </Paper>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* 10. FINAL CALL TO ACTION */}
      <Box
        sx={{
          bgcolor: "#12233b",
          color: "#ffffff",
          py: { xs: 10, md: 14 },
          textAlign: "center",
          position: "relative",
          overflow: "hidden",
          backgroundImage:
            "radial-gradient(circle at 50% 50%, rgba(62, 193, 250, 0.15) 0%, transparent 70%), linear-gradient(180deg, #12233b 0%, #0d1726 100%)",
        }}
      >
        <Container maxWidth="md" sx={{ position: "relative", zIndex: 1 }}>
          <Typography
            variant="h3"
            component="h2"
            sx={{
              fontWeight: 800,
              fontSize: { xs: "2rem", sm: "2.5rem", md: "3rem" },
              lineHeight: 1.25,
              mb: 3,
            }}
          >
            Bring insulin monitoring, storage safety, and caregiver visibility into one connected experience.
          </Typography>

          <Typography
            variant="h6"
            sx={{
              color: "rgba(255, 255, 255, 0.85)",
              fontWeight: 400,
              mb: 5,
              maxWidth: 640,
              mx: "auto",
            }}
          >
            Create a Dia-Smart account to access the connected diabetes-monitoring platform.
          </Typography>

          <Box sx={{ display: "flex", flexDirection: { xs: "column", sm: "row" }, gap: 2.5, justifyContent: "center", mb: 4 }}>
            <Button
              variant="contained"
              size="large"
              onClick={() => navigate("/register")}
              sx={{
                bgcolor: "#3ec1fa",
                color: "#12233b",
                fontWeight: 700,
                fontSize: "1.05rem",
                px: 4,
                py: 1.75,
                borderRadius: "10px",
                boxShadow: "0 4px 20px rgba(62, 193, 250, 0.4)",
                textTransform: "none",
                "&:hover": { bgcolor: "#28b4ef" },
              }}
            >
              Get Started
            </Button>

            <Button
              variant="outlined"
              size="large"
              onClick={() => navigate("/login")}
              sx={{
                color: "#ffffff",
                borderColor: "rgba(255, 255, 255, 0.35)",
                fontWeight: 600,
                fontSize: "1.05rem",
                px: 4,
                py: 1.75,
                borderRadius: "10px",
                textTransform: "none",
                "&:hover": {
                  borderColor: "#ffffff",
                  bgcolor: "rgba(255, 255, 255, 0.08)",
                },
              }}
            >
              Login
            </Button>
          </Box>

          <Typography variant="caption" sx={{ color: "rgba(255, 255, 255, 0.6)", fontSize: "0.8rem", display: "block" }}>
            Dia-Smart is a monitoring and support platform and does not replace professional medical advice.
          </Typography>
        </Container>
      </Box>

      {/* 11. FOOTER */}
      <Box component="footer" sx={{ bgcolor: "#0d1726", color: "#ffffff", py: 8, borderTop: "1px solid rgba(255,255,255,0.08)" }}>
        <Container maxWidth="lg">
          <Grid container spacing={4} sx={{ mb: 6 }}>
            {/* Column 1: Logo & Info */}
            <Grid size={{ xs: 12, md: 5 }}>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, mb: 2 }}>
                <Box
                  component="img"
                  src={BRAND_ICON_SRC}
                  alt="Dia-Smart Logo"
                  sx={{ height: 36, width: "auto", objectFit: "contain" }}
                />
                <Typography variant="h6" sx={{ fontWeight: 700, color: "#ffffff" }}>
                  Dia-Smart
                </Typography>
              </Box>
              <Typography variant="body2" sx={{ color: "rgba(255, 255, 255, 0.65)", maxWidth: 360, lineHeight: 1.6 }}>
                Connected insulin monitoring and diabetes-care support.
              </Typography>
            </Grid>

            {/* Column 2: Navigation Links */}
            <Grid size={{ xs: 6, md: 3.5 }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 700, color: "#ffffff", mb: 2, textTransform: "uppercase", letterSpacing: "1px" }}>
                Platform
              </Typography>
              <Box sx={{ display: "flex", flexDirection: "column", gap: 1.25 }}>
                {navLinks.map((link) => (
                  <Typography
                    key={link.id}
                    onClick={() => scrollToSection(link.id)}
                    sx={{
                      color: "rgba(255, 255, 255, 0.65)",
                      fontSize: "0.9rem",
                      cursor: "pointer",
                      "&:hover": { color: "#3ec1fa" },
                    }}
                  >
                    {link.label}
                  </Typography>
                ))}
              </Box>
            </Grid>

            {/* Column 3: Account Links */}
            <Grid size={{ xs: 6, md: 3.5 }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 700, color: "#ffffff", mb: 2, textTransform: "uppercase", letterSpacing: "1px" }}>
                Account
              </Typography>
              <Box sx={{ display: "flex", flexDirection: "column", gap: 1.25 }}>
                <Typography
                  onClick={() => navigate("/login")}
                  sx={{
                    color: "rgba(255, 255, 255, 0.65)",
                    fontSize: "0.9rem",
                    cursor: "pointer",
                    "&:hover": { color: "#3ec1fa" },
                  }}
                >
                  Login
                </Typography>
                <Typography
                  onClick={() => navigate("/register")}
                  sx={{
                    color: "rgba(255, 255, 255, 0.65)",
                    fontSize: "0.9rem",
                    cursor: "pointer",
                    "&:hover": { color: "#3ec1fa" },
                  }}
                >
                  Register / Get Started
                </Typography>
              </Box>
            </Grid>
          </Grid>

          <Divider sx={{ borderColor: "rgba(255, 255, 255, 0.08)", mb: 4 }} />

          <Typography variant="body2" align="center" sx={{ color: "rgba(255, 255, 255, 0.5)", fontSize: "0.85rem" }}>
            © 2026 Dia-Smart. All rights reserved.
          </Typography>
        </Container>
      </Box>
    </Box>
  );
};

export default LandingPage;
