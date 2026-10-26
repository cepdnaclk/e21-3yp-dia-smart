import { alpha, createTheme } from "@mui/material/styles";

/**
 * Dia-Smart design tokens.
 * Navy / sky / slate brand palette with restrained clinical state colours.
 * Shared by the theme below and by presentational components (shell, charts).
 */
export const brand = {
  navy: "#12233b",
  navyLight: "#1b3559",
  navyDark: "#0b1626",
  sky: "#3ec1fa",
  skyStrong: "#0284c7", // accessible sky for text / icons on light surfaces
  skySoft: "#e0f2fe",
  slate50: "#f8fafc",
  slate100: "#f1f5f9",
  slate200: "#e2e8f0",
  slate300: "#cbd5e1",
  slate400: "#94a3b8",
  slate500: "#64748b",
  slate600: "#475569",
  slate700: "#334155",
  slate800: "#1e293b",
  canvas: "#f4f7fb",
} as const;

export const SIDEBAR_WIDTH = 264;

const focusRing = `0 0 0 3px ${alpha(brand.sky, 0.35)}`;

const theme = createTheme({
  shape: {
    borderRadius: 6,
  },
  palette: {
    primary: {
      main: brand.navy,
      light: brand.navyLight,
      dark: brand.navyDark,
      contrastText: "#ffffff",
    },
    secondary: {
      main: brand.sky,
      dark: brand.skyStrong,
      contrastText: "#ffffff",
    },
    info: {
      main: brand.skyStrong,
      light: brand.skySoft,
    },
    success: {
      main: "#10b981",
      light: "#d1fae5",
      dark: "#047857",
    },
    warning: {
      main: "#f59e0b",
      light: "#fef3c7",
      dark: "#b45309",
    },
    error: {
      main: "#ef4444",
      light: "#fee2e2",
      dark: "#b91c1c",
    },
    background: {
      default: brand.canvas,
      paper: "#ffffff",
    },
    text: {
      primary: brand.slate800,
      secondary: brand.slate500,
    },
    divider: brand.slate200,
    action: {
      active: brand.slate600,
      hover: alpha(brand.sky, 0.08),
      selected: alpha(brand.sky, 0.14),
    },
  },
  typography: {
    fontFamily: [
      "Plus Jakarta Sans",
      "Inter",
      "-apple-system",
      "BlinkMacSystemFont",
      '"Segoe UI"',
      "Roboto",
      '"Helvetica Neue"',
      "Arial",
      "sans-serif",
    ].join(","),
    h1: { fontWeight: 800, letterSpacing: "-0.02em" },
    h2: { fontWeight: 800, letterSpacing: "-0.02em" },
    h3: { fontWeight: 700, letterSpacing: "-0.015em" },
    h4: { fontWeight: 800, letterSpacing: "-0.02em", fontSize: "1.75rem", lineHeight: 1.25 },
    h5: { fontWeight: 700, letterSpacing: "-0.01em", fontSize: "1.375rem" },
    h6: { fontWeight: 700, letterSpacing: "-0.005em", fontSize: "1.0625rem", lineHeight: 1.4 },
    subtitle1: { fontWeight: 600 },
    subtitle2: { fontWeight: 600 },
    body1: { fontWeight: 400 },
    body2: { fontWeight: 400 },
    overline: { fontWeight: 700, letterSpacing: "0.08em" },
    caption: { letterSpacing: "0.01em" },
    button: {
      fontWeight: 600,
      textTransform: "none",
      letterSpacing: "0.01em",
    },
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          backgroundColor: brand.canvas,
        },
        "::selection": {
          backgroundColor: alpha(brand.sky, 0.28),
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: "none",
        },
        outlined: {
          borderColor: brand.slate200,
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 14,
          border: `1px solid ${brand.slate200}`,
          boxShadow: "0 1px 2px rgba(15, 23, 42, 0.04), 0 4px 16px rgba(15, 23, 42, 0.04)",
          backgroundImage: "none",
          transition: "box-shadow 0.2s ease, border-color 0.2s ease",
        },
      },
    },
    MuiCardContent: {
      styleOverrides: {
        root: {
          padding: 20,
          "&:last-child": {
            paddingBottom: 20,
          },
          "@media (min-width:600px)": {
            padding: 24,
            "&:last-child": {
              paddingBottom: 24,
            },
          },
        },
      },
    },
    MuiCardHeader: {
      styleOverrides: {
        root: {
          padding: "18px 24px",
          borderBottom: `1px solid ${brand.slate100}`,
        },
        title: {
          fontWeight: 700,
          fontSize: "1.0625rem",
          color: brand.navy,
        },
        subheader: {
          fontSize: "0.8125rem",
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          boxShadow: "none",
          fontWeight: 600,
          "&:hover": {
            boxShadow: "none",
          },
          "&.Mui-focusVisible": {
            boxShadow: focusRing,
          },
        },
        sizeLarge: {
          padding: "10px 22px",
        },
        contained: {
          "&.MuiButton-colorPrimary:hover": {
            backgroundColor: brand.navyLight,
          },
        },
        outlined: {
          "&.MuiButton-colorPrimary": {
            borderColor: brand.slate300,
          },
          "&.MuiButton-colorPrimary:hover": {
            borderColor: brand.slate400,
            backgroundColor: brand.slate50,
          },
          "&.MuiButton-colorError": {
            borderColor: alpha("#ef4444", 0.5),
          },
          "&.MuiButton-colorError:hover": {
            borderColor: "#ef4444",
            backgroundColor: alpha("#ef4444", 0.04),
          },
        },
      },
    },
    MuiIconButton: {
      styleOverrides: {
        root: {
          "&.Mui-focusVisible": {
            boxShadow: focusRing,
          },
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          fontWeight: 600,
          letterSpacing: "0.01em",
        },
        sizeSmall: {
          fontSize: "0.72rem",
          height: 24,
        },
        outlined: {
          borderColor: brand.slate300,
        },
      },
    },
    MuiAlert: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          alignItems: "center",
          fontWeight: 500,
        },
        standard: {
          "&.MuiAlert-colorSuccess": {
            border: `1px solid ${alpha("#10b981", 0.25)}`,
          },
          "&.MuiAlert-colorInfo": {
            border: `1px solid ${alpha(brand.skyStrong, 0.2)}`,
          },
          "&.MuiAlert-colorWarning": {
            border: `1px solid ${alpha("#f59e0b", 0.3)}`,
          },
          "&.MuiAlert-colorError": {
            border: `1px solid ${alpha("#ef4444", 0.25)}`,
          },
        },
      },
    },
    MuiAlertTitle: {
      styleOverrides: {
        root: {
          fontWeight: 700,
        },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          backgroundColor: "#ffffff",
          transition: "box-shadow 0.15s ease",
          "& .MuiOutlinedInput-notchedOutline": {
            borderColor: brand.slate300,
          },
          "&:hover:not(.Mui-disabled):not(.Mui-error) .MuiOutlinedInput-notchedOutline": {
            borderColor: brand.slate400,
          },
          "&.Mui-focused:not(.Mui-error)": {
            boxShadow: focusRing,
          },
          "&.Mui-focused:not(.Mui-error) .MuiOutlinedInput-notchedOutline": {
            borderColor: brand.skyStrong,
            borderWidth: 1.5,
          },
          "&.Mui-disabled": {
            backgroundColor: brand.slate50,
          },
        },
      },
    },
    MuiInputLabel: {
      styleOverrides: {
        root: {
          fontWeight: 500,
          "&.Mui-focused:not(.Mui-error)": {
            color: brand.skyStrong,
          },
        },
      },
    },
    MuiFormHelperText: {
      styleOverrides: {
        root: {
          marginLeft: 2,
          fontWeight: 500,
        },
      },
    },
    MuiTableContainer: {
      styleOverrides: {
        root: {
          borderRadius: 12,
        },
      },
    },
    MuiTableHead: {
      styleOverrides: {
        root: {
          "& .MuiTableCell-head": {
            backgroundColor: brand.slate50,
            color: brand.slate600,
            fontWeight: 700,
            fontSize: "0.75rem",
            textTransform: "uppercase",
            letterSpacing: "0.05em",
            borderBottom: `1px solid ${brand.slate200}`,
            whiteSpace: "nowrap",
          },
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          borderBottom: `1px solid ${brand.slate100}`,
          paddingTop: 12,
          paddingBottom: 12,
        },
        sizeSmall: {
          paddingTop: 8,
          paddingBottom: 8,
        },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: {
          transition: "background-color 0.15s ease",
          "&.MuiTableRow-hover:hover": {
            backgroundColor: alpha(brand.sky, 0.05),
          },
          "&:last-child td": {
            borderBottom: 0,
          },
        },
      },
    },
    MuiDialog: {
      styleOverrides: {
        paper: {
          borderRadius: 16,
          boxShadow: "0 24px 64px rgba(15, 23, 42, 0.22)",
        },
      },
    },
    MuiDialogTitle: {
      styleOverrides: {
        root: {
          fontWeight: 700,
          fontSize: "1.15rem",
          color: brand.navy,
          padding: "20px 24px 12px",
        },
      },
    },
    MuiDialogContent: {
      styleOverrides: {
        root: {
          padding: "8px 24px 16px",
        },
      },
    },
    MuiDialogActions: {
      styleOverrides: {
        root: {
          padding: "12px 24px 20px",
          gap: 8,
        },
      },
    },
    MuiBackdrop: {
      styleOverrides: {
        root: {
          "&:not(.MuiBackdrop-invisible)": {
            backgroundColor: alpha(brand.navyDark, 0.55),
          },
        },
      },
    },
    MuiTabs: {
      styleOverrides: {
        root: {
          minHeight: 44,
        },
        indicator: {
          height: 3,
          borderRadius: "3px 3px 0 0",
          backgroundColor: brand.skyStrong,
        },
      },
    },
    MuiTab: {
      styleOverrides: {
        root: {
          minHeight: 44,
          fontWeight: 600,
          textTransform: "none",
          color: brand.slate500,
          "&.Mui-selected": {
            color: brand.navy,
          },
        },
      },
    },
    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          backgroundColor: brand.navy,
          fontSize: "0.75rem",
          fontWeight: 500,
          borderRadius: 8,
          padding: "6px 10px",
        },
        arrow: {
          color: brand.navy,
        },
      },
    },
    MuiLinearProgress: {
      styleOverrides: {
        root: {
          borderRadius: 999,
          backgroundColor: brand.slate100,
        },
        bar: {
          borderRadius: 999,
        },
      },
    },
    MuiDivider: {
      styleOverrides: {
        root: {
          borderColor: brand.slate200,
        },
      },
    },
    MuiMenu: {
      styleOverrides: {
        paper: {
          borderRadius: 12,
          border: `1px solid ${brand.slate200}`,
          boxShadow: "0 12px 32px rgba(15, 23, 42, 0.12)",
        },
      },
    },
    MuiAccordion: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          border: `1px solid ${brand.slate200}`,
          boxShadow: "none",
          "&:before": {
            display: "none",
          },
        },
      },
    },
    MuiSkeleton: {
      styleOverrides: {
        root: {
          backgroundColor: brand.slate100,
        },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          boxShadow: "none",
        },
      },
    },
    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          "&.Mui-focusVisible": {
            boxShadow: `inset ${focusRing}`,
          },
          "&.Mui-selected": {
            backgroundColor: alpha(brand.sky, 0.14),
            "&:hover": {
              backgroundColor: alpha(brand.sky, 0.2),
            },
          },
        },
      },
    },
  },
});

export default theme;
