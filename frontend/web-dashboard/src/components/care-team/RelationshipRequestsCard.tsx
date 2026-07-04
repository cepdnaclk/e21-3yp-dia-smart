import {
  Card,
  CardContent,
  Typography,
} from "@mui/material";

const RelationshipRequestsCard = () => {
  return (
    <Card>
      <CardContent>
        <Typography
          variant="h6"
          sx={{ mb: 1 }}
        >
          Relationship Requests
        </Typography>

        {/* TODO: Display pending relationship requests and actions after API integration. */}
        <Typography color="text.secondary">
          Pending care team requests will appear here.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default RelationshipRequestsCard;
