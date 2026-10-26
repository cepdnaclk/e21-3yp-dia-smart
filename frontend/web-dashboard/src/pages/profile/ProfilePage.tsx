import {
  Avatar,
  Box,
  Card,
  CardContent,
  Divider,
  Grid,
  Typography,
  Tabs,
  Tab,
  TextField,
  Button,
  Alert,
  InputAdornment,
  IconButton,
} from "@mui/material";

import { useEffect, useState } from "react";
import Visibility from "@mui/icons-material/Visibility";
import VisibilityOff from "@mui/icons-material/VisibilityOff";
import EmailOutlinedIcon from "@mui/icons-material/EmailOutlined";
import PhoneOutlinedIcon from "@mui/icons-material/PhoneOutlined";
import VerifiedUserOutlinedIcon from "@mui/icons-material/VerifiedUserOutlined";
import HistoryRoundedIcon from "@mui/icons-material/HistoryRounded";
import BadgeOutlinedIcon from "@mui/icons-material/BadgeOutlined";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import PersonOutlineRoundedIcon from "@mui/icons-material/PersonOutlineRounded";
import SaveRoundedIcon from "@mui/icons-material/SaveRounded";

import PageError from "../../components/common/PageError";
import PageLoading from "../../components/common/PageLoading";
import PageTitle from "../../components/common/PageTitle";
import { profileService } from "../../services/profileService";
import type { Profile } from "../../types/profile";

/** Presentational styles shared by the profile summary rows and forms. */
const infoRowSx = {
  display: "flex",
  alignItems: "flex-start",
  gap: 1.5,
  minWidth: 0,
} as const;

const infoIconSx = {
  width: 34,
  height: 34,
  flexShrink: 0,
  borderRadius: 2,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  bgcolor: "#f1f5f9",
  color: "#475569",
  "& .MuiSvgIcon-root": { fontSize: 18 },
} as const;

const primaryActionSx = {
  alignSelf: { xs: "stretch", sm: "flex-end" },
  px: 4,
  py: 1.25,
  fontWeight: 700,
  textTransform: "none",
} as const;

const formSectionHeaderSx = {
  display: "flex",
  alignItems: "center",
  gap: 1.5,
  pb: 2,
  borderBottom: "1px solid #f1f5f9",
} as const;

const formSectionIconSx = {
  width: 38,
  height: 38,
  flexShrink: 0,
  borderRadius: 2,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  bgcolor: "#e0f2fe",
  color: "#0284c7",
  "& .MuiSvgIcon-root": { fontSize: 20 },
} as const;

const ProfilePage = () => {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Tab State
  const [activeTab, setActiveTab] = useState(0);

  // Edit Profile Form State
  const [displayName, setDisplayName] = useState("");
  const [contactNumber, setContactNumber] = useState("");

  // Change Password Form State
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  
  // Show/Hide Password States
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // Action status states
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [successMsg, setSuccessMsg] = useState("");
  const [formErrorMsg, setFormErrorMsg] = useState("");

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const data = await profileService.getProfile();
        setProfile(data);
        // Initialize form fields
        setDisplayName(data.displayName || "");
        setContactNumber(data.contactNumber || "");
      } catch (error) {
        console.error(error);
        setError("Failed to load profile");
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, []);

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue);
    setSuccessMsg("");
    setFormErrorMsg("");
  };

  const handleProfileSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!displayName.trim()) {
      setFormErrorMsg("Display name cannot be empty");
      return;
    }

    try {
      setIsSubmitting(true);
      setFormErrorMsg("");
      setSuccessMsg("");
      
      const updatedProfile = await profileService.updateProfile(
        displayName,
        contactNumber
      );
      
      setProfile(updatedProfile);
      setSuccessMsg("Profile updated successfully!");
    } catch (err: any) {
      console.error(err);
      setFormErrorMsg(err.response?.data?.message || "Failed to update profile. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handlePasswordSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentPassword || !newPassword || !confirmPassword) {
      setFormErrorMsg("All password fields are required");
      return;
    }
    if (newPassword.length < 8) {
      setFormErrorMsg("New password must be at least 8 characters");
      return;
    }
    if (newPassword !== confirmPassword) {
      setFormErrorMsg("New passwords do not match");
      return;
    }

    try {
      setIsSubmitting(true);
      setFormErrorMsg("");
      setSuccessMsg("");

      await profileService.updatePassword(currentPassword, newPassword);

      setSuccessMsg("Password changed successfully!");
      // Reset password fields
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err: any) {
      console.error(err);
      setFormErrorMsg(err.response?.data?.message || "Failed to change password. Ensure current password is correct.");
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading) {
    return <PageLoading minHeight="50vh" />;
  }

  if (error) {
    return <PageError message={error} />;
  }

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle mb={3}>Profile Management</PageTitle>

      <Grid container spacing={3}>
        {/* Left Column - User Summary */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Card sx={{ height: "100%", overflow: "hidden" }}>
            <Box
              aria-hidden
              sx={{
                height: 84,
                background: "linear-gradient(135deg, #12233b 0%, #1b3559 55%, #0b5f8a 100%)",
              }}
            />
            <CardContent sx={{ display: "flex", flexDirection: "column", alignItems: "center", p: 3, pt: 0 }}>
              <Avatar
                sx={{
                  width: 90,
                  height: 90,
                  fontSize: 32,
                  bgcolor: "#12233b",
                  color: "#3ec1fa",
                  fontWeight: "bold",
                  mt: "-45px",
                  mb: 2,
                  border: "4px solid #ffffff",
                  boxShadow: "0px 6px 16px rgba(18, 35, 59, 0.2)",
                }}
              >
                {profile?.displayName?.charAt(0).toUpperCase()}
              </Avatar>

              <Typography variant="h6" sx={{ fontWeight: 800, color: "#12233b", textAlign: "center", overflowWrap: "anywhere" }}>
                {profile?.displayName}
              </Typography>

              <Box
                sx={{
                  px: 1.5,
                  py: 0.5,
                  borderRadius: 999,
                  backgroundColor: "rgba(62, 193, 250, 0.12)",
                  border: "1px solid rgba(2, 132, 199, 0.2)",
                  color: "#0284c7",
                  fontSize: "0.75rem",
                  fontWeight: 700,
                  textTransform: "uppercase",
                  letterSpacing: "0.5px",
                  mt: 1,
                  mb: 3,
                }}
              >
                {profile?.role}
              </Box>

              <Divider sx={{ width: "100%", mb: 3 }} />

              <Box sx={{ width: "100%", display: "flex", flexDirection: "column", gap: 2.25 }}>
                <Box sx={infoRowSx}>
                  <Box aria-hidden sx={infoIconSx}>
                    <EmailOutlinedIcon />
                  </Box>
                  <Box sx={{ minWidth: 0 }}>
                    <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, display: "block" }}>
                      Email Address
                    </Typography>
                    <Typography variant="body2" sx={{ fontWeight: 700, color: "#12233b", overflowWrap: "anywhere" }}>
                      {profile?.email}
                    </Typography>
                  </Box>
                </Box>

                <Box sx={infoRowSx}>
                  <Box aria-hidden sx={infoIconSx}>
                    <PhoneOutlinedIcon />
                  </Box>
                  <Box sx={{ minWidth: 0 }}>
                    <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, display: "block" }}>
                      Contact Number
                    </Typography>
                    <Typography variant="body2" sx={{ fontWeight: 700, color: "#12233b", overflowWrap: "anywhere" }}>
                      {profile?.contactNumber || "Not Available"}
                    </Typography>
                  </Box>
                </Box>

                <Box sx={infoRowSx}>
                  <Box aria-hidden sx={infoIconSx}>
                    <VerifiedUserOutlinedIcon />
                  </Box>
                  <Box sx={{ minWidth: 0 }}>
                    <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, display: "block" }}>
                      Account Status
                    </Typography>
                    <Box sx={{ display: "flex", alignItems: "center", gap: 0.75, mt: 0.25 }}>
                      <Box sx={{ width: 8, height: 8, borderRadius: "50%", bgcolor: profile?.active ? "#10b981" : "#64748b", boxShadow: profile?.active ? "0 0 0 3px rgba(16, 185, 129, 0.18)" : "none" }} />
                      <Typography variant="body2" sx={{ fontWeight: 700, color: "#12233b" }}>
                        {profile?.active ? "Active & Verified" : "Inactive"}
                      </Typography>
                    </Box>
                  </Box>
                </Box>

                <Box sx={infoRowSx}>
                  <Box aria-hidden sx={infoIconSx}>
                    <HistoryRoundedIcon />
                  </Box>
                  <Box sx={{ minWidth: 0 }}>
                    <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, display: "block" }}>
                      Last Login Timestamp
                    </Typography>
                    <Typography variant="body2" sx={{ fontWeight: 700, color: "#12233b" }}>
                      {profile?.lastLoginAt ? new Date(profile.lastLoginAt).toLocaleString() : "Never"}
                    </Typography>
                  </Box>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Right Column - Tabs Form Controls */}
        <Grid size={{ xs: 12, md: 8 }}>
          <Card>
            <Box sx={{ borderBottom: 1, borderColor: "divider", px: { xs: 1, sm: 2 }, pt: 1 }}>
              <Tabs
                value={activeTab}
                onChange={handleTabChange}
                aria-label="profile action tabs"
                variant="scrollable"
                scrollButtons="auto"
                allowScrollButtonsMobile
                sx={{
                  "& .MuiTab-root": { fontWeight: 700, textTransform: "none", fontSize: "0.95rem", minHeight: 52 },
                  "& .MuiTabs-indicator": { backgroundColor: "#3ec1fa", height: 3, borderRadius: "3px 3px 0 0" },
                  "& .MuiTab-root.Mui-selected": { color: "#0284c7" },
                }}
              >
                <Tab icon={<PersonOutlineRoundedIcon fontSize="small" />} iconPosition="start" label="Edit Profile Information" />
                <Tab icon={<LockOutlinedIcon fontSize="small" />} iconPosition="start" label="Change Password" />
              </Tabs>
            </Box>

            <CardContent sx={{ p: { xs: 2.5, sm: 4 } }}>
              {successMsg && (
                <Alert severity="success" sx={{ mb: 3 }}>
                  {successMsg}
                </Alert>
              )}

              {formErrorMsg && (
                <Alert severity="error" sx={{ mb: 3 }}>
                  {formErrorMsg}
                </Alert>
              )}

              {activeTab === 0 ? (
                // Edit Profile Form
                <Box component="form" onSubmit={handleProfileSave} sx={{ display: "flex", flexDirection: "column", gap: 3 }}>
                  <Box sx={formSectionHeaderSx}>
                    <Box aria-hidden sx={formSectionIconSx}>
                      <BadgeOutlinedIcon />
                    </Box>
                    <Typography variant="subtitle1" sx={{ fontWeight: 700, color: "#12233b" }}>
                      Personal Information Details
                    </Typography>
                  </Box>

                  <Grid container spacing={2.5}>
                    <Grid size={{ xs: 12, md: 6 }}>
                      <TextField
                        label="Display Name (Full Name)"
                        fullWidth
                        value={displayName}
                        onChange={(e) => setDisplayName(e.target.value)}
                      />
                    </Grid>

                    <Grid size={{ xs: 12, md: 6 }}>
                      <TextField
                        label="Contact Phone Number"
                        fullWidth
                        value={contactNumber}
                        onChange={(e) => setContactNumber(e.target.value)}
                      />
                    </Grid>
                  </Grid>

                  <Divider />

                  <Button
                    type="submit"
                    variant="contained"
                    color="primary"
                    disableElevation
                    disabled={isSubmitting}
                    startIcon={<SaveRoundedIcon />}
                    sx={primaryActionSx}
                  >
                    {isSubmitting ? "Saving Changes..." : "Save Changes"}
                  </Button>
                </Box>
              ) : (
                // Change Password Form
                <Box component="form" onSubmit={handlePasswordSave} sx={{ display: "flex", flexDirection: "column", gap: 3 }}>
                  <Box sx={formSectionHeaderSx}>
                    <Box aria-hidden sx={formSectionIconSx}>
                      <LockOutlinedIcon />
                    </Box>
                    <Typography variant="subtitle1" sx={{ fontWeight: 700, color: "#12233b" }}>
                      Update Security Password
                    </Typography>
                  </Box>

                  <TextField
                    label="Current Password"
                    type={showCurrentPassword ? "text" : "password"}
                    fullWidth
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    slotProps={{
                      input: {
                        endAdornment: (
                          <InputAdornment position="end">
                            <IconButton
                              aria-label="toggle current password visibility"
                              onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                              edge="end"
                            >
                              {showCurrentPassword ? <VisibilityOff /> : <Visibility />}
                            </IconButton>
                          </InputAdornment>
                        ),
                      },
                    }}
                  />

                  <TextField
                    label="New Password"
                    type={showNewPassword ? "text" : "password"}
                    fullWidth
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    slotProps={{
                      input: {
                        endAdornment: (
                          <InputAdornment position="end">
                            <IconButton
                              aria-label="toggle new password visibility"
                              onClick={() => setShowNewPassword(!showNewPassword)}
                              edge="end"
                            >
                              {showNewPassword ? <VisibilityOff /> : <Visibility />}
                            </IconButton>
                          </InputAdornment>
                        ),
                      },
                    }}
                  />

                  <TextField
                    label="Confirm New Password"
                    type={showConfirmPassword ? "text" : "password"}
                    fullWidth
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    slotProps={{
                      input: {
                        endAdornment: (
                          <InputAdornment position="end">
                            <IconButton
                              aria-label="toggle confirm password visibility"
                              onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                              edge="end"
                            >
                              {showConfirmPassword ? <VisibilityOff /> : <Visibility />}
                            </IconButton>
                          </InputAdornment>
                        ),
                      },
                    }}
                  />

                  <Divider />

                  <Button
                    type="submit"
                    variant="contained"
                    color="primary"
                    disableElevation
                    disabled={isSubmitting}
                    startIcon={<LockOutlinedIcon />}
                    sx={primaryActionSx}
                  >
                    {isSubmitting ? "Changing Password..." : "Change Password"}
                  </Button>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default ProfilePage;
