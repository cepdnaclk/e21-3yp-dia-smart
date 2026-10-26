import {
  Card,
  CardContent,
  Typography,
  List,
  ListItem,
  ListItemText,
  Chip,
  Divider,
  Box,
} from "@mui/material";
import OutboxRoundedIcon from "@mui/icons-material/OutboxRounded";
import HourglassTopRoundedIcon from "@mui/icons-material/HourglassTopRounded";
import CheckCircleRoundedIcon from "@mui/icons-material/CheckCircleRounded";
import CancelRoundedIcon from "@mui/icons-material/CancelRounded";
import type { RelationshipRequestDto } from "../../types/careTeam";
import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";

const getStatusIcon = (status: string) => {
  switch (status) {
    case "PENDING":
      return <HourglassTopRoundedIcon />;
    case "ACCEPTED":
      return <CheckCircleRoundedIcon />;
    case "REJECTED":
      return <CancelRoundedIcon />;
    default:
      return undefined;
  }
};

interface RelationshipRequestsCardProps {
  requests: RelationshipRequestDto[];
}

const getStatusColor = (status: string) => {
  switch (status) {
    case "PENDING":
      return "warning";
    case "ACCEPTED":
      return "success";
    case "REJECTED":
      return "error";
    default:
      return "default";
  }
};

const RelationshipRequestsCard = ({ requests }: RelationshipRequestsCardProps) => {
  return (
    <Card sx={{ height: "100%" }}>
      <CardContent>
        <CardHeading
          title="Sent Requests"
          icon={<OutboxRoundedIcon />}
          color="#f59e0b"
        />

        {requests.length === 0 ? (
          <EmptyState
            message="No sent relationship requests"
            icon={<OutboxRoundedIcon />}
            minHeight={140}
          />
        ) : (
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
                    gap: 1,
                    py: 1,
                    "& .MuiListItemText-secondary": { overflowWrap: "anywhere" },
                  }}
                >
                  <ListItemText
                    primary={
                      <Box sx={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: 1, mb: 0.5 }}>
                        <Typography variant="subtitle2" sx={{ fontWeight: "bold", color: "#12233b", overflowWrap: "anywhere" }}>
                          {req.targetName} ({req.relationshipRole})
                        </Typography>
                        <Chip
                          label={req.status}
                          color={getStatusColor(req.status)}
                          icon={getStatusIcon(req.status)}
                          size="small"
                          variant="outlined"
                          sx={{ textTransform: "uppercase", fontSize: "0.65rem", fontWeight: 700 }}
                        />
                      </Box>
                    }
                    secondary={
                      req.message
                        ? `Message: "${req.message}" • Sent: ${new Date(req.createdAt).toLocaleDateString()}`
                        : `Sent: ${new Date(req.createdAt).toLocaleDateString()}`
                    }
                    sx={{ m: 0, width: "100%" }}
                  />
                </ListItem>
              </Box>
            ))}
          </List>
        )}
      </CardContent>
    </Card>
  );
};

export default RelationshipRequestsCard;
