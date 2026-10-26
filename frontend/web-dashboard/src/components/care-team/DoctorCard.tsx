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
import MedicalServicesRoundedIcon from "@mui/icons-material/MedicalServicesRounded";
import LinkOffRoundedIcon from "@mui/icons-material/LinkOffRounded";
import type { RelationshipSummaryDto } from "../../types/careTeam";
import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";

interface DoctorCardProps {
  doctors: RelationshipSummaryDto[];
  onRevoke: (requestId: number) => void;
}

const DoctorCard = ({ doctors, onRevoke }: DoctorCardProps) => {
  return (
    <Card sx={{ height: "100%" }}>
      <CardContent>
        <CardHeading
          title="Connected Doctor"
          icon={<MedicalServicesRoundedIcon />}
        />

        {doctors.length === 0 ? (
          <EmptyState
            message="No doctor connected"
            icon={<MedicalServicesRoundedIcon />}
            minHeight={140}
          />
        ) : (
          <List disablePadding sx={{ display: "flex", flexDirection: "column", gap: 1.25 }}>
            {doctors.map((doc) => (
              <ListItem
                key={doc.requestId}
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
                  sx={{ width: 40, height: 40, bgcolor: "#e0f2fe", color: "#0284c7" }}
                >
                  <MedicalServicesRoundedIcon sx={{ fontSize: 20 }} />
                </Avatar>
                <Box sx={{ flex: "1 1 160px", minWidth: 0 }}>
                  <Typography sx={{ fontWeight: 700, color: "primary.main", overflowWrap: "anywhere" }}>
                    {doc.displayName}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ overflowWrap: "anywhere" }}>
                    {doc.email}
                  </Typography>
                </Box>
                <Button
                  variant="outlined"
                  color="error"
                  size="small"
                  startIcon={<LinkOffRoundedIcon />}
                  onClick={() => onRevoke(doc.requestId)}
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

export default DoctorCard;
