import { Card, CardContent, Typography, Box } from "@mui/material";
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from "recharts";

const UserReportsSection = () => {
  const data = [
    { name: "Patients", value: 45, color: "#1976d2" },
    { name: "Clinicians", value: 20, color: "#2e7d32" },
    { name: "Caregivers", value: 25, color: "#ed6c02" },
    { name: "Administrators", value: 10, color: "#9c27b0" }
  ];

  return (
    <Card elevation={2} sx={{ borderRadius: 3, height: "100%", display: "flex", flexDirection: "column" }}>
      <CardContent sx={{ flexGrow: 1, display: "flex", flexDirection: "column", gap: 2 }}>
        <Box>
          <Typography variant="h6" sx={{ fontWeight: "bold" }}>
            User Account Distribution
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Proportion of registered system roles across patients, medical clinicians, caregiver mappings, and administrators.
          </Typography>
        </Box>

        <Box sx={{ width: "100%", height: 260 }}>
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={data}
                cx="50%"
                cy="50%"
                innerRadius={60}
                outerRadius={80}
                paddingAngle={5}
                dataKey="value"
              >
                {data.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip />
              <Legend verticalAlign="bottom" height={36} />
            </PieChart>
          </ResponsiveContainer>
        </Box>
      </CardContent>
    </Card>
  );
};

export default UserReportsSection;
