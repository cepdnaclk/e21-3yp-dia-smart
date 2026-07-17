import { createTheme } from "@mui/material/styles";

const theme = createTheme({
  palette: {
    primary: {
      main: "#12233b",
      contrastText: "#ffffff",
    },
    secondary: {
      main: "#3ec1fa",
      contrastText: "#ffffff",
    },
    success: {
      main: "#10b981", // modern emerald green
    },
    warning: {
      main: "#f59e0b", // warm amber
    },
    error: {
      main: "#ef4444", // vibrant red
    },
    background: {
      default: "#f8f9fa",
      paper: "#ffffff",
    },
    text: {
      primary: "#1e293b",
      secondary: "#64748b",
    },
    action: {
      active: "#3ec1fa",
      hover: "rgba(62, 193, 250, 0.08)",
      selected: "rgba(62, 193, 250, 0.16)",
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
    h1: { fontWeight: 800 },
    h2: { fontWeight: 800 },
    h3: { fontWeight: 700 },
    h4: { fontWeight: 700 },
    h5: { fontWeight: 600 },
    h6: { fontWeight: 600 },
    subtitle1: { fontWeight: 500 },
    subtitle2: { fontWeight: 500 },
    body1: { fontWeight: 400 },
    body2: { fontWeight: 400 },
    button: {
      fontWeight: 600,
      textTransform: "none",
    },
  },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 16,
          boxShadow: "0px 4px 20px rgba(0, 0, 0, 0.03)",
          border: "1px solid #e2e8f0",
          backgroundImage: "none",
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: "none",
        },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 8,
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          boxShadow: "none",
          "&:hover": {
            boxShadow: "none",
          },
        },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundColor: "#12233b",
          boxShadow: "none",
          borderBottom: "1px solid rgba(255, 255, 255, 0.08)",
        },
      },
    },
    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          margin: "4px 8px",
          "&.Mui-selected": {
            backgroundColor: "rgba(62, 193, 250, 0.15)",
            color: "#3ec1fa",
            "& .MuiListItemIcon-root": {
              color: "#3ec1fa",
            },
            "&:hover": {
              backgroundColor: "rgba(62, 193, 250, 0.2)",
            },
          },
        },
      },
    },
  },
});

export default theme;