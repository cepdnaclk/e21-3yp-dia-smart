import { Box, Typography } from "@mui/material";

interface PageTitleProps {
  children: string;
  mb?: number;
}

const PageTitle = ({
  children,
  mb = 3,
}: PageTitleProps) => {
  return (
    <Box
      sx={{
        mb,
        display: "flex",
        alignItems: "center",
        gap: 1.5,
        minWidth: 0,
      }}
    >
      <Box
        aria-hidden
        sx={{
          width: 5,
          alignSelf: "stretch",
          minHeight: 28,
          borderRadius: 999,
          background: "linear-gradient(180deg, #3ec1fa 0%, #0284c7 100%)",
          flexShrink: 0,
        }}
      />
      <Typography
        variant="h4"
        component="h1"
        sx={{
          color: "primary.main",
          fontSize: { xs: "1.45rem", sm: "1.75rem" },
          overflowWrap: "anywhere",
          minWidth: 0,
        }}
      >
        {children}
      </Typography>
    </Box>
  );
};

export default PageTitle;
