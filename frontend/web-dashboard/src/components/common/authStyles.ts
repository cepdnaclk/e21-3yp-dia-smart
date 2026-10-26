// Shared presentation for the public authentication screens (login, register, reset).
export const authBackdropSx = {
  minHeight: "100vh",
  display: "flex",
  justifyContent: "center",
  alignItems: "center",
  px: 2,
  py: 4,
  backgroundColor: "#f4f7fb",
  backgroundImage: [
    "radial-gradient(900px circle at 0% 0%, rgba(62, 193, 250, 0.16), transparent 55%)",
    "radial-gradient(800px circle at 100% 100%, rgba(18, 35, 59, 0.12), transparent 55%)",
  ].join(", "),
};

export const authCardSx = {
  width: "100%",
  borderRadius: 5,
  p: { xs: 0.5, sm: 1.5 },
  border: "1px solid #e2e8f0",
  boxShadow: "0 1px 2px rgba(15, 23, 42, 0.04), 0 24px 48px -12px rgba(15, 23, 42, 0.14)",
};

export const authPrimaryButtonSx = {
  py: 1.4,
  borderRadius: 2.5,
  backgroundColor: "#12233b",
  fontWeight: 700,
  fontSize: "1rem",
  textTransform: "none",
  boxShadow: "0 8px 20px -8px rgba(18, 35, 59, 0.6)",
  "&:hover": {
    backgroundColor: "#1b3559",
    boxShadow: "0 10px 24px -8px rgba(18, 35, 59, 0.7)",
  },
};

export const authLinkButtonSx = {
  color: "secondary.dark",
  fontWeight: 700,
};
