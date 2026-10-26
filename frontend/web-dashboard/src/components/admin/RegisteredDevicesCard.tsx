import MetricCard from "../common/MetricCard";
import DevicesIcon from "@mui/icons-material/Devices";

interface RegisteredDevicesCardProps {
  count: number;
}

const RegisteredDevicesCard: React.FC<RegisteredDevicesCardProps> = ({ count }) => {
  return (
    <MetricCard
      label="Registered Devices"
      value={count}
      description="Total registered dosing units, inventory scales, and storage monitoring sensors currently online."
      icon={<DevicesIcon />}
      tone="info"
    />
  );
};

export default RegisteredDevicesCard;
