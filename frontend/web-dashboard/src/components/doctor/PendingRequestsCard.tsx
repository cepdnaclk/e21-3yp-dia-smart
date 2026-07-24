import { useState, useEffect } from "react";
import {
  Card,
  CardContent,
  Typography,
  List,
  ListItem,
  ListItemText,
  Button,
  Box,
  Divider,
  CircularProgress,
} from "@mui/material";
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
          <CircularProgress size={24} />
        </CardContent>
      </Card>
    );
  }

  if (requests.length === 0) {
    return null;
  }

  return (
    <Card
      elevation={2}
      sx={{
        mb: 3,
        borderRadius: 2,
        borderLeft: 5,
        borderColor: "warning.main",
      }}
    >
      <CardContent>
        <Typography
          variant="h6"
          sx={{ mb: 2, fontWeight: 600 }}
        >
          Pending Connection Requests
        </Typography>

        <List disablePadding>
          {requests.map((req, idx) => (
            <Box key={req.requestId}>
              {idx > 0 && <Divider sx={{ my: 1 }} />}
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
                <ListItemText
                  primary={req.requesterName}
                  secondary={`Patient ID: ${req.patientId} • Requested: ${new Date(req.createdAt).toLocaleDateString()}`}
                  sx={{ m: 0, width: "100%" }}
                />
                <Box sx={{ display: "flex", gap: 1, mt: { xs: 1, sm: 0 }, alignSelf: { xs: "flex-end", sm: "auto" } }}>
                  <Button
                    variant="contained"
                    color="success"
                    size="small"
                    disabled={actionId !== null}
                    onClick={() => handleAccept(req.requestId)}
                  >
                    Accept
                  </Button>
                  <Button
                    variant="outlined"
                    color="error"
                    size="small"
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
