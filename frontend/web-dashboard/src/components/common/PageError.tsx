import { Alert, AlertTitle } from "@mui/material";

interface PageErrorProps {
  message: string;
}

const PageError = ({
  message,
}: PageErrorProps) => {
  return (
    <Alert severity="error" sx={{ alignItems: "flex-start", py: 1.5 }}>
      <AlertTitle>Unable to load this page</AlertTitle>
      {message}
    </Alert>
  );
};

export default PageError;
