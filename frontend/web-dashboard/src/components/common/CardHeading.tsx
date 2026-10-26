import type { ReactNode } from "react";
import { Box, Typography } from "@mui/material";
import { alpha } from "@mui/material/styles";
import type { SxProps, Theme } from "@mui/material/styles";

interface CardHeadingProps {
  title: ReactNode;
  subtitle?: ReactNode;
  icon?: ReactNode;
  action?: ReactNode;
  /** Colour used for the icon badge. Accepts any CSS colour. */
  color?: string;
  sx?: SxProps<Theme>;
}

const safeTint = (color: string, opacity: number) => {
  try {
    return alpha(color, opacity);
  } catch {
    return alpha("#0284c7", opacity);
  }
};

/**
 * Consistent heading row for cards: icon badge, title, optional subtitle and action slot.
 * Presentational only.
 */
const CardHeading = ({ title, subtitle, icon, action, color = "#0284c7", sx }: CardHeadingProps) => {
  return (
    <Box
      sx={[
        {
          display: "flex",
          alignItems: "center",
          gap: 1.5,
          mb: 2.5,
          flexWrap: "wrap",
        },
        ...(Array.isArray(sx) ? sx : [sx]),
      ]}
    >
      {icon && (
        <Box
          aria-hidden
          sx={{
            width: 38,
            height: 38,
            borderRadius: 2,
            flexShrink: 0,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            color,
            backgroundColor: safeTint(color, 0.1),
            "& .MuiSvgIcon-root": { fontSize: 20 },
          }}
        >
          {icon}
        </Box>
      )}
      <Box sx={{ flex: "1 1 160px", minWidth: 0 }}>
        <Typography variant="h6" component="h2" sx={{ color: "primary.main", lineHeight: 1.3 }}>
          {title}
        </Typography>
        {subtitle && (
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.25 }}>
            {subtitle}
          </Typography>
        )}
      </Box>
      {action && <Box sx={{ flexShrink: 0 }}>{action}</Box>}
    </Box>
  );
};

export default CardHeading;
