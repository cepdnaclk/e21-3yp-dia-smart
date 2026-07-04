import { Alert } from "@mui/material";

interface PageErrorProps {
  message: string;
}

const PageError = ({
  message,
}: PageErrorProps) => {
  return (
    <Alert severity="error">
      {message}
    </Alert>
  );
};

export default PageError;
