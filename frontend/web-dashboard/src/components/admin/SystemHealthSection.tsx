import { Card, CardContent, Typography, Box, List, ListItem, Chip } from "@mui/material";
import DnsOutlinedIcon from "@mui/icons-material/DnsOutlined";
import CardHeading from "../common/CardHeading";

const SystemHealthSection = () => {
  const components = [
    { name: "Web API Gateway", status: "ONLINE", latency: "12 ms", color: "success.main" },
    { name: "PostgreSQL Database Cluster", status: "HEALTHY", latency: "5 ms", color: "success.main" },
    { name: "Device Ingestion Gateway", status: "ACTIVE", latency: "1.1k/s ingest", color: "success.main" },
    { name: "Background Alert Evaluator", status: "RUNNING", latency: "Check cycle 1.5s", color: "success.main" }
  ];

  return (
    <Card sx={{ height: "100%" }}>
      <CardContent sx={{ display: "flex", flexDirection: "column" }}>
        <CardHeading
          title="System Telemetry & Health Monitoring"
          subtitle="Real-time service health check monitoring API nodes, database query execution, and message pipelines."
          icon={<DnsOutlinedIcon />}
          color="#10b981"
          sx={{ alignItems: "flex-start" }}
        />

        <List sx={{ display: "flex", flexDirection: "column", gap: 1.25, p: 0 }}>
          {components.map((c, idx) => (
            <ListItem
              key={idx}
              sx={{
                p: 1.5,
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                flexWrap: "wrap",
                gap: 1,
                borderRadius: 2,
                border: "1px solid",
                borderColor: "divider",
                bgcolor: "#f8fafc",
              }}
            >
              <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, minWidth: 0, flex: "1 1 180px" }}>
                <Box
                  aria-hidden
                  sx={{
                    width: 10,
                    height: 10,
                    borderRadius: "50%",
                    flexShrink: 0,
                    bgcolor: c.color,
                    boxShadow: "0 0 0 4px rgba(16, 185, 129, 0.15)",
                  }}
                />
                <Box sx={{ minWidth: 0 }}>
                  <Typography variant="body2" sx={{ fontWeight: 700, color: "text.primary" }}>
                    {c.name}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Performance: {c.latency}
                  </Typography>
                </Box>
              </Box>
              <Chip label={c.status} size="small" color="success" variant="outlined" />
            </ListItem>
          ))}
        </List>
      </CardContent>
    </Card>
  );
};

export default SystemHealthSection;
