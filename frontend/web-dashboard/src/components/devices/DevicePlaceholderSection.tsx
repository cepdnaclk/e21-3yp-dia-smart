import {
  Card,
  CardContent,
  Typography,
} from "@mui/material";

interface DevicePlaceholderSectionProps {
  title: string;
  description: string;
}

const DevicePlaceholderSection = ({
  title,
  description,
}: DevicePlaceholderSectionProps) => {
  return (
    <Card>
      <CardContent>
        <Typography
          variant="h6"
          sx={{ mb: 1 }}
        >
          {title}
        </Typography>

        {/* TODO: Replace this placeholder with real device data UI in Milestone 4. */}
        <Typography color="text.secondary">
          {description}
        </Typography>
      </CardContent>
    </Card>
  );
};

export default DevicePlaceholderSection;
