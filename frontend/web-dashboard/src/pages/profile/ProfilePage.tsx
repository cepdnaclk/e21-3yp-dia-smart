import {
  Avatar,
  Box,
  Card,
  CardContent,
  CircularProgress,
  Divider,
  Grid,
  Typography,
  Alert,
} from "@mui/material";

import { useEffect, useState } from "react";

import { profileService } from "../../services/profileService";
import type { Profile } from "../../types/profile";

const ProfilePage = () => {
  const [profile, setProfile] =
    useState<Profile | null>(null);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState("");

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const data =
          await profileService.getProfile();

        setProfile(data);
      } catch (error) {
        console.error(error);

        setError(
          "Failed to load profile"
        );
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, []);

  if (loading) {
    return (
      <Box
        sx={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          minHeight: "50vh",
        }}
      >
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error">
        {error}
      </Alert>
    );
  }

  return (
    <>
      <Typography variant="h4" sx={{ mb: 3 }}>
        Profile
      </Typography>

      <Card>
        <CardContent>
          <Box
            sx={{
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              mb: 3
            }}
          >
            <Avatar
              sx={{
                width: 100,
                height: 100,
                fontSize: 36,
                bgcolor: "primary.main",
              }}
            >
              {profile?.displayName
                ?.charAt(0)
                .toUpperCase()}
            </Avatar>

            <Typography variant="h5" sx={{ mt: 2 }}>
              {profile?.displayName}
            </Typography>

            <Typography color="text.secondary">
              {profile?.role}
            </Typography>
          </Box>

          <Divider sx={{ mb: 3 }} />

          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 6 }}>
              <Typography>
                <strong>Email:</strong>{" "}
                {profile?.email}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography>
                <strong>Role:</strong>{" "}
                {profile?.role}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography>
                <strong>Contact Number:</strong>{" "}
                {profile?.contactNumber ||
                  "Not Available"}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography>
                <strong>Status:</strong>{" "}
                {profile?.active
                  ? "Active"
                  : "Inactive"}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography>
                <strong>User ID:</strong>{" "}
                {profile?.userId}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography>
                <strong>Last Login:</strong>{" "}
                {profile?.lastLoginAt ??
                  "Never"}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12 }}>
              <Typography>
                <strong>Email Verified Account:</strong>{" "}
                {profile?.active
                  ? "Yes"
                  : "No"}
              </Typography>
            </Grid>
          </Grid>
        </CardContent>
      </Card>
    </>
  );
};

export default ProfilePage;