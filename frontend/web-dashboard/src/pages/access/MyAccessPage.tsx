import { Box, Typography, Card, CardContent } from "@mui/material";
import LockPersonRoundedIcon from "@mui/icons-material/LockPersonRounded";

export default function MyAccessPage() {
    return (
        <Box>
            <Card>
                <CardContent
                    sx={{
                        display: "flex",
                        flexDirection: "column",
                        alignItems: "center",
                        textAlign: "center",
                        gap: 1.5,
                        py: { xs: 5, sm: 7 },
                    }}
                >
                    <Box
                        aria-hidden
                        sx={{
                            width: 56,
                            height: 56,
                            borderRadius: 3,
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            bgcolor: "#e0f2fe",
                            color: "#0284c7",
                            mb: 0.5,
                        }}
                    >
                        <LockPersonRoundedIcon sx={{ fontSize: 28 }} />
                    </Box>
                    <Typography
                        variant="h5"
                        component="h1"
                        sx={{ fontWeight: 700, color: "primary.main" }}
                    >
                        My Access
                    </Typography>

                    <Typography variant="body1" color="text.secondary" sx={{ maxWidth: 420 }}>
                        This page is currently under development.
                    </Typography>
                </CardContent>
            </Card>
        </Box>
    );
}
