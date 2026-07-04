import {
  Box,
  CircularProgress,
} from "@mui/material";

interface PageLoadingProps {
  minHeight?: string;
}

const PageLoading = ({
  minHeight = "300px",
}: PageLoadingProps) => {
  return (
    <Box
      sx={{
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        minHeight,
      }}
    >
      <CircularProgress />
    </Box>
  );
};

export default PageLoading;
