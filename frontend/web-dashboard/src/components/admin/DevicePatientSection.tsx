import { Card, CardContent, Typography } from "@mui/material";
import LinkOutlinedIcon from "@mui/icons-material/LinkOutlined";
import CardHeading from "../common/CardHeading";

const DevicePatientSection = () => {
  return (
    <Card sx={{ height: "100%" }}>
      <CardContent>
        <CardHeading
          title="Device ↔ Patient Associations"
          icon={<LinkOutlinedIcon />}
          color="#0ea5e9"
        />

        {/* TODO: Integrate device hardware profiling and patient configuration APIs */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Verify sensor calibrations, review telemetry streams, and manage inventory scale association keys for each patient account.
        </Typography>
        <Typography
          variant="caption"
          color="text.disabled"
          sx={{
            display: "block",
            px: 1.5,
            py: 1,
            borderRadius: 2,
            border: "1px dashed",
            borderColor: "divider",
            bgcolor: "#f8fafc",
          }}
        >
          [Placeholder Registry: Device Patient Mappings]
        </Typography>
      </CardContent>
    </Card>
  );
};

export default DevicePatientSection;
