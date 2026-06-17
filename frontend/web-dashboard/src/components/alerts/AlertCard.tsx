import {
  Alert,
  AlertTitle,
  Box,
  Chip,
  Stack,
  Typography,
} from "@mui/material";
import type { ReactNode } from "react";

interface AlertCardProps {
  severity: "error" | "warning" | "info" | "success";
  title: string;
  description: string;
  status?: string;
  createdAt?: string;
  action?: ReactNode;
}

const AlertCard = ({
  severity,
  title,
  description,
  status,
  createdAt,
  action,
}: AlertCardProps) => {
  return (
    <Alert
      severity={severity}
      action={action}
    >
      <Stack spacing={1}>
        <Box>
          <AlertTitle>{title}</AlertTitle>
          {description}
        </Box>

        {(status || createdAt) && (
          <Stack
            direction="row"
            spacing={1}
            sx={{
              alignItems: "center",
              flexWrap: "wrap",
            }}
          >
            {status && (
              <Chip
                size="small"
                label={status}
              />
            )}

            {createdAt && (
              <Typography
                variant="caption"
                color="text.secondary"
              >
                {new Date(createdAt)
                  .toLocaleString()}
              </Typography>
            )}
          </Stack>
        )}
      </Stack>
    </Alert>
  );
};

export default AlertCard;
