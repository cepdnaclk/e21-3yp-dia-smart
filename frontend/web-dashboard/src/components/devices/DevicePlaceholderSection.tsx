import {
  Card,
  CardContent,
  Typography,
} from "@mui/material";
import DevicesOtherRoundedIcon from "@mui/icons-material/DevicesOtherRounded";

import CardHeading from "../common/CardHeading";

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
        <CardHeading
          title={title}
          icon={<DevicesOtherRoundedIcon />}
          sx={{ mb: 1.5 }}
        />

        {/* TODO: Replace this placeholder with real device data UI in Milestone 4. */}
        <Typography color="text.secondary">
          {description}
        </Typography>
      </CardContent>
    </Card>
  );
};

export default DevicePlaceholderSection;
