import MetricCard from "../common/MetricCard";
import PersonPinIcon from "@mui/icons-material/PersonPin";

interface ActivePatientsCardProps {
  count: number;
}

const ActivePatientsCard: React.FC<ActivePatientsCardProps> = ({ count }) => {
  return (
    <MetricCard
      label="Active Patients"
      value={count}
      description="Number of patients actively using monitoring systems, dosing schedules, and caregiver support."
      icon={<PersonPinIcon />}
      tone="success"
    />
  );
};

export default ActivePatientsCard;
