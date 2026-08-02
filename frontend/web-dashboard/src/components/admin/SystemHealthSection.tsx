import { Card, CardContent, Typography, Box, List, ListItem, ListItemText, Divider } from "@mui/material";

const SystemHealthSection = () => {
  const components = [
    { name: "Web API Gateway", status: "ONLINE", latency: "12 ms", color: "success.main" },
    { name: "PostgreSQL Database Cluster", status: "HEALTHY", latency: "5 ms", color: "success.main" },
    { name: "Device Ingestion Gateway", status: "ACTIVE", latency: "1.1k/s ingest", color: "success.main" },
    { name: "Background Alert Evaluator", status: "RUNNING", latency: "Check cycle 1.5s", color: "success.main" }
  ];

  return (
    <Card elevation={2} sx={{ borderRadius: 3, height: "100%" }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
        <Typography variant="h6" sx={{ fontWeight: "bold" }}>
          System Telemetry & Health Monitoring
        </Typography>

        <Typography variant="body2" color="text.secondary">
          Real-time service health check monitoring API nodes, database query execution, and message pipelines.
        </Typography>

        <List sx={{ display: "flex", flexDirection: "column", gap: 2, p: 0, mt: 1 }}>
          {components.map((c, idx) => (
            <Box key={idx}>
              <ListItem sx={{ p: 0, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <ListItemText
                  primary={<Typography variant="body2" sx={{ fontWeight: "bold" }}>{c.name}</Typography>}
                  secondary={<Typography variant="caption" color="text.secondary">Performance: {c.latency}</Typography>}
                />
                <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                  <Box sx={{ width: 8, height: 8, borderRadius: "50%", bgcolor: c.color }} />
                  <Typography variant="caption" sx={{ fontWeight: "bold", color: c.color }}>
                    {c.status}
                  </Typography>
                </Box>
              </ListItem>
              {idx < components.length - 1 && <Divider sx={{ mt: 1.5 }} />}
            </Box>
          ))}
        </List>
      </CardContent>
    </Card>
  );
};

export default SystemHealthSection;
