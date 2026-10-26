import {
  Box,
  CircularProgress,
  Typography,
} from "@mui/material";

interface PageLoadingProps {
  minHeight?: string;
}

const PageLoading = ({
  minHeight = "300px",
}: PageLoadingProps) => {
  return (
    <Box
      role="status"
      aria-live="polite"
      sx={{
        display: "flex",
        flexDirection: "column",
        justifyContent: "center",
        alignItems: "center",
        gap: 2,
        minHeight,
      }}
    >
      <CircularProgress size={36} thickness={4} sx={{ color: "secondary.dark" }} />
      <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 500 }}>
        Loading data…
      </Typography>
    </Box>
  );
};

export default PageLoading;
