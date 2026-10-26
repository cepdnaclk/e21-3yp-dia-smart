import { useState, useEffect } from "react";
import {
  Card,
  CardContent,
  List,
  ListItem,
  ListItemText,
  Button,
  Box,
  Divider,
  CircularProgress,
  Avatar,
} from "@mui/material";
import PersonAddAlt1OutlinedIcon from "@mui/icons-material/PersonAddAlt1Outlined";
import CheckRoundedIcon from "@mui/icons-material/CheckRounded";
import CloseRoundedIcon from "@mui/icons-material/CloseRounded";

import CardHeading from "../common/CardHeading";
import { careTeamService } from "../../services/careTeamService";
import type { RelationshipRequestDto } from "../../types/careTeam";

interface PendingRequestsCardProps {
  onRefresh: () => void;
}

const PendingRequestsCard = ({ onRefresh }: PendingRequestsCardProps) => {
  const [loading, setLoading] = useState(true);
  const [requests, setRequests] = useState<RelationshipRequestDto[]>([]);
  const [actionId, setActionId] = useState<number | null>(null);

  const fetchIncomingRequests = async () => {
    try {
      const data = await careTeamService.getIncomingRequests();
      setRequests(data);
    } catch (err) {
      console.error("Failed to load incoming requests", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchIncomingRequests();
  }, []);

  const handleAccept = async (requestId: number) => {
    setActionId(requestId);
    try {
      await careTeamService.acceptRequest(requestId);
      await fetchIncomingRequests();
      onRefresh();
    } catch (err) {
      console.error("Failed to accept request", err);
    } finally {
      setActionId(null);
    }
  };

  const handleReject = async (requestId: number) => {
    setActionId(requestId);
    try {
      await careTeamService.rejectRequest(requestId);
      await fetchIncomingRequests();
      onRefresh();
    } catch (err) {
      console.error("Failed to reject request", err);
    } finally {
      setActionId(null);
    }
  };

  if (loading) {
    return (
      <Card sx={{ mb: 3 }}>
        <CardContent sx={{ display: "flex", justifyContent: "center", py: 4 }}>
          <CircularProgress size={24} sx={{ color: "secondary.dark" }} />
        </CardContent>
      </Card>
    );
  }

  if (requests.length === 0) {
    return null;
  }

  return (
    <Card
      sx={{
        mb: 3,
        borderLeft: "4px solid #f59e0b",
      }}
    >
      <CardContent>
        <CardHeading
          title="Pending Connection Requests"
          icon={<PersonAddAlt1OutlinedIcon />}
          color="#f59e0b"
        />

        <List disablePadding>
          {requests.map((req, idx) => (
            <Box key={req.requestId}>
              {idx > 0 && <Divider sx={{ my: 0.5 }} />}
              <ListItem
                disableGutters
                sx={{
                  display: "flex",
                  flexDirection: { xs: "column", sm: "row" },
                  alignItems: { xs: "flex-start", sm: "center" },
                  gap: 1.5,
                  py: 1,
                }}
              >
                <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, width: "100%", minWidth: 0 }}>
                  <Avatar
                    sx={{ width: 36, height: 36, fontSize: "0.85rem", fontWeight: 700, bgcolor: "#fef3c7", color: "#b45309" }}
                  >
                    {(req.requesterName ?? "").trim().charAt(0).toUpperCase() || "?"}
                  </Avatar>
                  <ListItemText
                    primary={req.requesterName}
                    secondary={`Patient ID: ${req.patientId} • Requested: ${new Date(req.createdAt).toLocaleDateString()}`}
                    sx={{ m: 0, minWidth: 0 }}
                    slotProps={{
                      primary: { sx: { fontWeight: 600, color: "text.primary", overflowWrap: "anywhere" } },
                      secondary: { sx: { overflowWrap: "anywhere" } },
                    }}
                  />
                </Box>
                <Box sx={{ display: "flex", gap: 1, mt: { xs: 0.5, sm: 0 }, flexShrink: 0, alignSelf: { xs: "flex-end", sm: "auto" } }}>
                  <Button
                    variant="contained"
                    color="success"
                    size="small"
                    startIcon={<CheckRoundedIcon />}
                    disabled={actionId !== null}
                    onClick={() => handleAccept(req.requestId)}
                  >
                    Accept
                  </Button>
                  <Button
                    variant="outlined"
                    color="error"
                    size="small"
                    startIcon={<CloseRoundedIcon />}
                    disabled={actionId !== null}
                    onClick={() => handleReject(req.requestId)}
                  >
                    Reject
                  </Button>
                </Box>
              </ListItem>
            </Box>
          ))}
        </List>
      </CardContent>
    </Card>
  );
};

export default PendingRequestsCard;
