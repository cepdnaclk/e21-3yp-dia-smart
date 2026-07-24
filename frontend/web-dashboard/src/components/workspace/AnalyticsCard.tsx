import AdvancedAnalyticsView from "./AdvancedAnalyticsView";

interface AnalyticsCardProps {
  patientId: number;
}

const AnalyticsCard = ({ patientId }: AnalyticsCardProps) => {
  return <AdvancedAnalyticsView patientId={patientId} />;
};

export default AnalyticsCard;
