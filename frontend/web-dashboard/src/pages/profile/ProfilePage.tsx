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
} from "@mui/material";

import { useEffect, useState } from "react";

import PageError from "../../components/common/PageError";
import PageLoading from "../../components/common/PageLoading";
import PageTitle from "../../components/common/PageTitle";
import { profileService } from "../../services/profileService";
import type { Profile } from "../../types/profile";

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
          <Card sx={{ height: "100%", borderRadius: 4 }}>
            <CardContent sx={{ display: "flex", flexDirection: "column", alignItems: "center", p: 3 }}>
              <Avatar
                sx={{
                  width: 90,
                  height: 90,
                  fontSize: 32,
                  bgcolor: "#12233b",
                  color: "#3ec1fa",
                  fontWeight: "bold",
                  mb: 2,
                  boxShadow: "0px 4px 10px rgba(18, 35, 59, 0.15)",
                }}
              >
                {profile?.displayName?.charAt(0).toUpperCase()}
              </Avatar>

              <Typography variant="h6" sx={{ fontWeight: 800, color: "#12233b", textAlign: "center" }}>
                {profile?.displayName}
              </Typography>

              <Box
                sx={{
                  px: 1.5,
                  py: 0.5,
                  borderRadius: 2,
                  backgroundColor: "rgba(62, 193, 250, 0.1)",
                  color: "#3ec1fa",
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

              <Box sx={{ width: "100%", display: "flex", flexDirection: "column", gap: 2 }}>
                <Box>
                  <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, display: "block" }}>
                    Email Address
                  </Typography>
                  <Typography variant="body2" sx={{ fontWeight: 700, color: "#12233b" }}>
                    {profile?.email}
                  </Typography>
                </Box>

                <Box>
                  <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, display: "block" }}>
                    Contact Number
                  </Typography>
                  <Typography variant="body2" sx={{ fontWeight: 700, color: "#12233b" }}>
                    {profile?.contactNumber || "Not Available"}
                  </Typography>
                </Box>

                <Box>
                  <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, display: "block" }}>
                    Account Status
                  </Typography>
                  <Box sx={{ display: "flex", alignItems: "center", gap: 0.75, mt: 0.5 }}>
                    <Box sx={{ width: 8, height: 8, borderRadius: "50%", bgcolor: profile?.active ? "#10b981" : "#64748b" }} />
                    <Typography variant="body2" sx={{ fontWeight: 700, color: "#12233b" }}>
                      {profile?.active ? "Active & Verified" : "Inactive"}
                    </Typography>
                  </Box>
                </Box>

                <Box>
                  <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, display: "block" }}>
                    Last Login Timestamp
                  </Typography>
                  <Typography variant="body2" sx={{ fontWeight: 700, color: "#12233b" }}>
                    {profile?.lastLoginAt ? new Date(profile.lastLoginAt).toLocaleString() : "Never"}
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Right Column - Tabs Form Controls */}
        <Grid size={{ xs: 12, md: 8 }}>
          <Card sx={{ borderRadius: 4 }}>
            <Box sx={{ borderBottom: 1, borderColor: "divider", px: 2, pt: 1 }}>
              <Tabs
                value={activeTab}
                onChange={handleTabChange}
                aria-label="profile action tabs"
                sx={{
                  "& .MuiTab-root": { fontWeight: 700, textTransform: "none", fontSize: "0.95rem" },
                  "& .MuiTabs-indicator": { backgroundColor: "#3ec1fa" },
                  "& .MuiTab-root.Mui-selected": { color: "#3ec1fa" },
                }}
              >
                <Tab label="Edit Profile Information" />
                <Tab label="Change Password" />
              </Tabs>
            </Box>

            <CardContent sx={{ p: 4 }}>
              {successMsg && (
                <Alert severity="success" sx={{ mb: 3, borderRadius: 2 }}>
                  {successMsg}
                </Alert>
              )}

              {formErrorMsg && (
                <Alert severity="error" sx={{ mb: 3, borderRadius: 2 }}>
                  {formErrorMsg}
                </Alert>
              )}

              {activeTab === 0 ? (
                // Edit Profile Form
                <Box component="form" onSubmit={handleProfileSave} sx={{ display: "flex", flexDirection: "column", gap: 3 }}>
                  <Typography variant="subtitle1" sx={{ fontWeight: 700, color: "#12233b" }}>
                    Personal Information Details
                  </Typography>

                  <TextField
                    label="Display Name (Full Name)"
                    fullWidth
                    value={displayName}
                    onChange={(e) => setDisplayName(e.target.value)}
                  />

                  <TextField
                    label="Contact Phone Number"
                    fullWidth
                    value={contactNumber}
                    onChange={(e) => setContactNumber(e.target.value)}
                  />

                  <Button
                    type="submit"
                    variant="contained"
                    disabled={isSubmitting}
                    sx={{
                      alignSelf: "flex-start",
                      backgroundColor: "#12233b",
                      color: "#ffffff",
                      px: 4,
                      py: 1.25,
                      fontWeight: 700,
                      borderRadius: 2,
                      textTransform: "none",
                      "&:hover": { backgroundColor: "#1b3559" },
                    }}
                  >
                    {isSubmitting ? "Saving Changes..." : "Save Changes"}
                  </Button>
                </Box>
              ) : (
                // Change Password Form
                <Box component="form" onSubmit={handlePasswordSave} sx={{ display: "flex", flexDirection: "column", gap: 3 }}>
                  <Typography variant="subtitle1" sx={{ fontWeight: 700, color: "#12233b" }}>
                    Update Security Password
                  </Typography>

                  <TextField
                    label="Current Password"
                    type="password"
                    fullWidth
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                  />

                  <TextField
                    label="New Password"
                    type="password"
                    fullWidth
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                  />

                  <TextField
                    label="Confirm New Password"
                    type="password"
                    fullWidth
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                  />

                  <Button
                    type="submit"
                    variant="contained"
                    disabled={isSubmitting}
                    sx={{
                      alignSelf: "flex-start",
                      backgroundColor: "#12233b",
                      color: "#ffffff",
                      px: 4,
                      py: 1.25,
                      fontWeight: 700,
                      borderRadius: 2,
                      textTransform: "none",
                      "&:hover": { backgroundColor: "#1b3559" },
                    }}
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
