import MetricCard from "../common/MetricCard";
import GroupsIcon from "@mui/icons-material/Groups";

interface TotalUsersCardProps {
  count: number;
}

const TotalUsersCard: React.FC<TotalUsersCardProps> = ({ count }) => {
  return (
    <MetricCard
      label="Total Registered Users"
      value={count}
      description="Total registered users across patients, clinicians, caregivers, and administrators."
      icon={<GroupsIcon />}
      tone="primary"
    />
  );
};

export default TotalUsersCard;
