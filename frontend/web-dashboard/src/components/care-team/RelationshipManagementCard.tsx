import { useState, useEffect } from "react";
import {
  Card,
  CardContent,
  Typography,
  Tabs,
  Tab,
  TextField,
  List,
  ListItem,
  ListItemText,
  Button,
  CircularProgress,
  Box,
  Divider,
  InputAdornment,
} from "@mui/material";
import GroupAddRoundedIcon from "@mui/icons-material/GroupAddRounded";
import SearchRoundedIcon from "@mui/icons-material/SearchRounded";
import MedicalServicesRoundedIcon from "@mui/icons-material/MedicalServicesRounded";
import PersonRoundedIcon from "@mui/icons-material/PersonRounded";
import { careTeamService } from "../../services/careTeamService";
import CardHeading from "../common/CardHeading";

const resultListSx = {
  overflowY: "auto",
  maxHeight: 200,
  "& .MuiListItem-root": {
    px: 1,
    borderRadius: 2,
    transition: "background-color 0.15s ease",
    "&:hover": { bgcolor: "#f8fafc" },
  },
  "& .MuiListItemText-primary": {
    fontWeight: 600,
    color: "#12233b",
  },
  "& .MuiListItemText-root": {
    pr: 11,
    minWidth: 0,
    overflowWrap: "anywhere",
  },
} as const;

const searchAdornment = {
  input: {
    startAdornment: (
      <InputAdornment position="start">
        <SearchRoundedIcon fontSize="small" sx={{ color: "#94a3b8" }} />
      </InputAdornment>
    ),
  },
};

interface RelationshipManagementCardProps {
  onShowAlert: (message: string, severity: "success" | "error") => void;
  onRefresh: () => void;
}

const RelationshipManagementCard = ({
  onShowAlert,
  onRefresh,
}: RelationshipManagementCardProps) => {
  const [tabValue, setTabValue] = useState(0);
  const [doctorQuery, setDoctorQuery] = useState("");
  const [caregiverQuery, setCaregiverQuery] = useState("");
  const [doctorResults, setDoctorResults] = useState<any[]>([]);
  const [caregiverResults, setCaregiverResults] = useState<any[]>([]);
  const [loadingDoctors, setLoadingDoctors] = useState(false);
  const [loadingCaregivers, setLoadingCaregivers] = useState(false);
  const [sendingId, setSendingId] = useState<number | null>(null);

  // Debounced search for Doctors
  useEffect(() => {
    if (!doctorQuery.trim()) {
      setDoctorResults([]);
      return;
    }
    const timer = setTimeout(async () => {
      setLoadingDoctors(true);
      try {
        const results = await careTeamService.searchDoctors(doctorQuery);
        setDoctorResults(results);
      } catch (err: any) {
        onShowAlert("Failed to search doctors", "error");
      } finally {
        setLoadingDoctors(false);
      }
    }, 400);
    return () => clearTimeout(timer);
  }, [doctorQuery]);

  // Debounced search for Caregivers
  useEffect(() => {
    if (!caregiverQuery.trim()) {
      setCaregiverResults([]);
      return;
    }
    const timer = setTimeout(async () => {
      setLoadingCaregivers(true);
      try {
        const results = await careTeamService.searchCaregivers(caregiverQuery);
        setCaregiverResults(results);
      } catch (err: any) {
        onShowAlert("Failed to search caregivers", "error");
      } finally {
        setLoadingCaregivers(false);
      }
    }, 400);
    return () => clearTimeout(timer);
  }, [caregiverQuery]);

  const handleSendRequest = async (userId: number, role: "DOCTOR" | "CAREGIVER") => {
    setSendingId(userId);
    try {
      await careTeamService.sendRequest({
        targetUserId: userId,
        relationshipRole: role,
        message: `Requesting to connect with role ${role}`,
      });
      onShowAlert(`Relationship request sent to ${role.toLowerCase()} successfully`, "success");
      if (role === "DOCTOR") {
        setDoctorQuery("");
        setDoctorResults([]);
      } else {
        setCaregiverQuery("");
        setCaregiverResults([]);
      }
      onRefresh();
    } catch (err: any) {
      const errMsg = err?.response?.data?.message || err?.message || "Failed to send relationship request";
      onShowAlert(errMsg, "error");
    } finally {
      setSendingId(null);
    }
  };

  return (
    <Card sx={{ height: "100%" }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
        <CardHeading
          title="Relationship Management"
          icon={<GroupAddRoundedIcon />}
        />

        <Tabs
          value={tabValue}
          onChange={(_, val) => setTabValue(val)}
          sx={{
            mb: 2.5,
            minHeight: 44,
            p: 0.5,
            borderRadius: 2.5,
            bgcolor: "#f1f5f9",
            "& .MuiTabs-indicator": { display: "none" },
            "& .MuiTab-root": {
              minHeight: 36,
              borderRadius: 2,
              textTransform: "none",
              fontWeight: 700,
              color: "#64748b",
            },
            "& .MuiTab-root.Mui-selected": {
              bgcolor: "#ffffff",
              color: "#12233b",
              boxShadow: "0 1px 3px rgba(15, 23, 42, 0.1)",
            },
          }}
          variant="fullWidth"
        >
          <Tab icon={<MedicalServicesRoundedIcon fontSize="small" />} iconPosition="start" label="Search Doctor" />
          <Tab icon={<PersonRoundedIcon fontSize="small" />} iconPosition="start" label="Search Caregiver" />
        </Tabs>

        {tabValue === 0 && (
          <Box sx={{ display: "flex", flexDirection: "column", flexGrow: 1 }}>
            <TextField
              label="Search doctor by name or email"
              variant="outlined"
              size="small"
              fullWidth
              value={doctorQuery}
              onChange={(e) => setDoctorQuery(e.target.value)}
              sx={{ mb: 2 }}
              slotProps={searchAdornment}
            />
            {loadingDoctors && (
              <Box sx={{ display: "flex", justifyContent: "center", py: 2 }}>
                <CircularProgress size={24} />
              </Box>
            )}
            {!loadingDoctors && doctorQuery && doctorResults.length === 0 && (
              <Typography
                color="text.secondary"
                align="center"
                variant="body2"
                sx={{ py: 2, px: 2, borderRadius: 2, border: "1px dashed", borderColor: "divider", bgcolor: "#f8fafc", mb: 1 }}
              >
                No doctors found
              </Typography>
            )}
            <List
              sx={resultListSx}
              disablePadding
            >
              {doctorResults.map((doc, idx) => (
                <Box key={doc.userId}>
                  {idx > 0 && <Divider />}
                  <ListItem
                    disableGutters
                    secondaryAction={
                      <Button
                        variant="contained"
                        size="small"
                        disableElevation
                        disabled={sendingId !== null}
                        sx={{ minWidth: 84 }}
                        onClick={() => handleSendRequest(doc.userId, "DOCTOR")}
                      >
                        {sendingId === doc.userId ? (
                          <CircularProgress
                            size={16}
                            color="inherit"
                          />
                        ) : (
                          "Connect"
                        )}
                      </Button>
                    }
                  >
                    <ListItemText
                      primary={doc.displayName}
                      secondary={doc.email}
                    />
                  </ListItem>
                </Box>
              ))}
            </List>
          </Box>
        )}

        {tabValue === 1 && (
          <Box sx={{ display: "flex", flexDirection: "column", flexGrow: 1 }}>
            <TextField
              label="Search caregiver by name or email"
              variant="outlined"
              size="small"
              fullWidth
              value={caregiverQuery}
              onChange={(e) => setCaregiverQuery(e.target.value)}
              sx={{ mb: 2 }}
              slotProps={searchAdornment}
            />
            {loadingCaregivers && (
              <Box sx={{ display: "flex", justifyContent: "center", py: 2 }}>
                <CircularProgress size={24} />
              </Box>
            )}
            {!loadingCaregivers && caregiverQuery && caregiverResults.length === 0 && (
              <Typography
                color="text.secondary"
                align="center"
                variant="body2"
                sx={{ py: 2, px: 2, borderRadius: 2, border: "1px dashed", borderColor: "divider", bgcolor: "#f8fafc", mb: 1 }}
              >
                No caregivers found
              </Typography>
            )}
            <List
              sx={resultListSx}
              disablePadding
            >
              {caregiverResults.map((cg, idx) => (
                <Box key={cg.userId}>
                  {idx > 0 && <Divider />}
                  <ListItem
                    disableGutters
                    secondaryAction={
                      <Button
                        variant="contained"
                        size="small"
                        disableElevation
                        disabled={sendingId !== null}
                        sx={{ minWidth: 84 }}
                        onClick={() => handleSendRequest(cg.userId, "CAREGIVER")}
                      >
                        {sendingId === cg.userId ? (
                          <CircularProgress
                            size={16}
                            color="inherit"
                          />
                        ) : (
                          "Connect"
                        )}
                      </Button>
                    }
                  >
                    <ListItemText
                      primary={cg.displayName}
                      secondary={cg.email}
                    />
                  </ListItem>
                </Box>
              ))}
            </List>
          </Box>
        )}
      </CardContent>
    </Card>
  );
};

export default RelationshipManagementCard;
