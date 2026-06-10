import {
  Card,
  CardContent,
  Typography,
} from "@mui/material";

interface Props {
  title: string;
  value: string;
}

const StatCard = ({ title, value }: Props) => {
  return (
    <Card elevation={3}>
      <CardContent>
        <Typography
          color="text.secondary"
          gutterBottom
        >
          {title}
        </Typography>

        <Typography
          variant="h4"
          fontWeight="bold"
        >
          {value}
        </Typography>
      </CardContent>
    </Card>
  );
};

export default StatCard;