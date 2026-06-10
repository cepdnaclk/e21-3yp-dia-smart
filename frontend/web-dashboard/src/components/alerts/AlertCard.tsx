import {
  Alert,
  AlertTitle,
} from "@mui/material";

interface AlertCardProps {
  severity: "error" | "warning" | "info" | "success";
  title: string;
  description: string;
}

const AlertCard = ({
  severity,
  title,
  description,
}: AlertCardProps) => {
  return (
    <Alert severity={severity}>
      <AlertTitle>{title}</AlertTitle>
      {description}
    </Alert>
  );
};

export default AlertCard;