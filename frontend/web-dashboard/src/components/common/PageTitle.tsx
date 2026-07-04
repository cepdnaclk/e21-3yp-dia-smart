import { Typography } from "@mui/material";

interface PageTitleProps {
  children: string;
  mb?: number;
}

const PageTitle = ({
  children,
  mb = 3,
}: PageTitleProps) => {
  return (
    <Typography
      variant="h4"
      sx={{ mb }}
    >
      {children}
    </Typography>
  );
};

export default PageTitle;
