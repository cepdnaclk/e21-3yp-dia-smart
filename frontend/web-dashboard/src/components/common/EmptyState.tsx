import type { ReactNode } from "react";
import { Box, Typography } from "@mui/material";
import InboxOutlinedIcon from "@mui/icons-material/InboxOutlined";

interface EmptyStateProps {
  message: ReactNode;
  icon?: ReactNode;
  minHeight?: number | string;
}

/** Presentational placeholder shown when a section has no records to display. */
const EmptyState = ({ message, icon, minHeight = 160 }: EmptyStateProps) => {
  return (
    <Box
      sx={{
        minHeight,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        gap: 1.25,
        px: 2,
        py: 3,
        textAlign: "center",
        borderRadius: 3,
        border: "1px dashed",
        borderColor: "divider",
        bgcolor: "#f8fafc",
      }}
    >
      <Box
        aria-hidden
        sx={{
          width: 44,
          height: 44,
          borderRadius: "50%",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          bgcolor: "#ffffff",
          color: "#94a3b8",
          boxShadow: "0 1px 3px rgba(15, 23, 42, 0.08)",
        }}
      >
        {icon ?? <InboxOutlinedIcon />}
      </Box>
      <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 500, maxWidth: 360 }}>
        {message}
      </Typography>
    </Box>
  );
};

export default EmptyState;
