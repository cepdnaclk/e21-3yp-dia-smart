import {
  Card,
  CardContent,
  Typography,
  List,
  ListItem,
  ListItemText,
  Button,
  Divider,
  Box,
} from "@mui/material";
import type { RelationshipSummaryDto } from "../../types/careTeam";

interface CaregiverCardProps {
  caregivers: RelationshipSummaryDto[];
  onRevoke: (requestId: number) => void;
}

const CaregiverCard = ({ caregivers, onRevoke }: CaregiverCardProps) => {
  return (
    <Card sx={{ height: "100%" }}>
      <CardContent>
        <Typography
          variant="h6"
          sx={{ mb: 2, fontWeight: 600 }}
        >
          Connected Caregivers
        </Typography>

        {caregivers.length === 0 ? (
          <Typography color="text.secondary" sx={{ py: 2 }}>
            No caregivers connected
          </Typography>
        ) : (
          <List disablePadding>
            {caregivers.map((cg, idx) => (
              <Box key={cg.requestId}>
                {idx > 0 && <Divider sx={{ my: 1 }} />}
                <ListItem
                  disableGutters
                  secondaryAction={
                    <Button
                      variant="outlined"
                      color="error"
                      size="small"
                      onClick={() => onRevoke(cg.requestId)}
                    >
                      Disconnect
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
        )}
      </CardContent>
    </Card>
  );
};

export default CaregiverCard;
