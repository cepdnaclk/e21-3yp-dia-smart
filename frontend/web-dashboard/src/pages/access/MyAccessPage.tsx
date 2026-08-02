import { Box, Typography, Paper } from "@mui/material";

export default function MyAccessPage() {
    return (
        <Box sx={{ p: 3 }}>
            <Paper sx={{ p: 3 }}>
                <Typography
                    variant="h5"
                    component="h1"
                    sx={{ fontWeight: 700 }}
                    gutterBottom
                >
                    My Access
                </Typography>

                <Typography variant="body1" color="text.secondary">
                    This page is currently under development.
                </Typography>
            </Paper>
        </Box>
    );
}