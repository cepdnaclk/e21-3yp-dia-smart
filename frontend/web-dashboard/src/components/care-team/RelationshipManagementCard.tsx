import {
  Card,
  CardContent,
  Typography,
} from "@mui/material";

const RelationshipManagementCard = () => {
  return (
    <Card>
      <CardContent>
        <Typography
          variant="h6"
          sx={{ mb: 1 }}
        >
          Relationship Management
        </Typography>

        {/* TODO: Add relationship management actions after care team APIs are available. */}
        <Typography color="text.secondary">
          Care team relationship management actions will appear here.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default RelationshipManagementCard;
