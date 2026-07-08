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

interface DoctorCardProps {
  doctors: RelationshipSummaryDto[];
  onRevoke: (requestId: number) => void;
}

const DoctorCard = ({ doctors, onRevoke }: DoctorCardProps) => {
  return (
    <Card sx={{ height: "100%" }}>
      <CardContent>
        <Typography
          variant="h6"
          sx={{ mb: 2, fontWeight: 600 }}
        >
          Connected Doctor
        </Typography>

        {doctors.length === 0 ? (
          <Typography color="text.secondary" sx={{ py: 2 }}>
            No doctor connected
          </Typography>
        ) : (
          <List disablePadding>
            {doctors.map((doc, idx) => (
              <Box key={doc.requestId}>
                {idx > 0 && <Divider sx={{ my: 1 }} />}
                <ListItem
                  disableGutters
                  secondaryAction={
                    <Button
                      variant="outlined"
                      color="error"
                      size="small"
                      onClick={() => onRevoke(doc.requestId)}
                    >
                      Disconnect
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
        )}
      </CardContent>
    </Card>
  );
};

export default DoctorCard;
