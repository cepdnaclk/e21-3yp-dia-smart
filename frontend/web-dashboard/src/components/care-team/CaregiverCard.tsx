import {
  Card,
  CardContent,
  Typography,
  List,
  ListItem,
  Button,
  Box,
  Avatar,
} from "@mui/material";
import DiversityRoundedIcon from "@mui/icons-material/Diversity3Rounded";
import PersonRoundedIcon from "@mui/icons-material/PersonRounded";
import LinkOffRoundedIcon from "@mui/icons-material/LinkOffRounded";
import type { RelationshipSummaryDto } from "../../types/careTeam";
import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";

interface CaregiverCardProps {
  caregivers: RelationshipSummaryDto[];
  onRevoke: (requestId: number) => void;
}

const CaregiverCard = ({ caregivers, onRevoke }: CaregiverCardProps) => {
  return (
    <Card sx={{ height: "100%" }}>
      <CardContent>
        <CardHeading
          title="Connected Caregivers"
          icon={<DiversityRoundedIcon />}
          color="#10b981"
        />

        {caregivers.length === 0 ? (
          <EmptyState
            message="No caregivers connected"
            icon={<DiversityRoundedIcon />}
            minHeight={140}
          />
        ) : (
          <List disablePadding sx={{ display: "flex", flexDirection: "column", gap: 1.25 }}>
            {caregivers.map((cg) => (
              <ListItem
                key={cg.requestId}
                disableGutters
                sx={{
                  display: "flex",
                  alignItems: "center",
                  flexWrap: "wrap",
                  gap: 1.5,
                  p: 1.5,
                  borderRadius: 2,
                  border: "1px solid #e2e8f0",
                  bgcolor: "#f8fafc",
                }}
              >
                <Avatar
                  aria-hidden
                  sx={{ width: 40, height: 40, bgcolor: "#d1fae5", color: "#047857" }}
                >
                  <PersonRoundedIcon sx={{ fontSize: 20 }} />
                </Avatar>
                <Box sx={{ flex: "1 1 160px", minWidth: 0 }}>
                  <Typography sx={{ fontWeight: 700, color: "primary.main", overflowWrap: "anywhere" }}>
                    {cg.displayName}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ overflowWrap: "anywhere" }}>
                    {cg.email}
                  </Typography>
                </Box>
                <Button
                  variant="outlined"
                  color="error"
                  size="small"
                  startIcon={<LinkOffRoundedIcon />}
                  onClick={() => onRevoke(cg.requestId)}
                >
                  Disconnect
                </Button>
              </ListItem>
            ))}
          </List>
        )}
      </CardContent>
    </Card>
  );
};

export default CaregiverCard;
