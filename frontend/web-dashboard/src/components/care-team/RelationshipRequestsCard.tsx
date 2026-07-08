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
import type { RelationshipRequestDto } from "../../types/careTeam";

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
        <Typography
          variant="h6"
          sx={{ mb: 2, fontWeight: 600 }}
        >
          Sent Requests
        </Typography>

        {requests.length === 0 ? (
          <Typography color="text.secondary" sx={{ py: 2 }}>
            No sent relationship requests
          </Typography>
        ) : (
          <List disablePadding>
            {requests.map((req, idx) => (
              <Box key={req.requestId}>
                {idx > 0 && <Divider sx={{ my: 1 }} />}
                <ListItem
                  disableGutters
                  secondaryAction={
                    <Chip
                      label={req.status}
                      color={getStatusColor(req.status)}
                      size="small"
                      variant="outlined"
                    />
                  }
                >
                  <ListItemText
                    primary={`${req.targetName} (${req.relationshipRole})`}
                    secondary={
                      req.message
                        ? `Message: "${req.message}" • Sent: ${new Date(req.createdAt).toLocaleDateString()}`
                        : `Sent: ${new Date(req.createdAt).toLocaleDateString()}`
                    }
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
