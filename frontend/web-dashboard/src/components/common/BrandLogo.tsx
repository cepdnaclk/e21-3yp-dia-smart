import { Box } from "@mui/material";
import type { SxProps, Theme } from "@mui/material/styles";

// Official Dia-Smart branding assets served from /public/branding.
const BASE = import.meta.env.BASE_URL;
export const BRAND_ICON_SRC = `${BASE}branding/diasmart-icon.png`;
export const BRAND_LOGO_SRC = `${BASE}branding/diasmart-logo.png`;

interface BrandIconProps {
  size?: number;
  /** Place the icon on a white tile so it reads cleanly on dark surfaces. */
  tile?: boolean;
  sx?: SxProps<Theme>;
}

/** Icon-only Dia-Smart mark for compact branding (sidebar, headers). */
export const BrandIcon = ({ size = 36, tile = false, sx }: BrandIconProps) => (
  <Box
    sx={[
      {
        width: size,
        height: size,
        flexShrink: 0,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        borderRadius: 2,
        overflow: "hidden",
        ...(tile && {
          bgcolor: "#ffffff",
          p: `${Math.round(size * 0.1)}px`,
          boxShadow: "0 2px 8px rgba(2, 6, 23, 0.25)",
        }),
      },
      ...(Array.isArray(sx) ? sx : [sx]),
    ]}
  >
    <Box
      component="img"
      src={BRAND_ICON_SRC}
      alt="Dia-Smart Logo"
      sx={{ width: "100%", height: "100%", objectFit: "contain", display: "block", mixBlendMode: "multiply" }}
    />
  </Box>
);

interface BrandWordmarkProps {
  width?: number | string | Record<string, number | string>;
  sx?: SxProps<Theme>;
}

/** Full Dia-Smart logo (mark + wordmark) for authentication screens. */
export const BrandWordmark = ({ width = 220, sx }: BrandWordmarkProps) => (
  <Box
    component="img"
    src={BRAND_LOGO_SRC}
    alt="Dia-Smart Logo"
    sx={[
      // multiply blends the artwork's off-white canvas into light surfaces without altering its colours
      { width, height: "auto", aspectRatio: "1408 / 768", objectFit: "contain", display: "block", mixBlendMode: "multiply" },
      ...(Array.isArray(sx) ? sx : [sx]),
    ]}
  />
);
