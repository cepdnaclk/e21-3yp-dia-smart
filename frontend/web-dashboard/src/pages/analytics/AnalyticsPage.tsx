import { Box } from "@mui/material";
import PageTitle from "../../components/common/PageTitle";
import AdvancedAnalyticsView from "../../components/workspace/AdvancedAnalyticsView";
import { getPatientId } from "../../utils/patient";

const AnalyticsPage = () => {
  const patientId = getPatientId();

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle mb={3}>Analytics Dashboard</PageTitle>
      {patientId ? (
        <AdvancedAnalyticsView patientId={Number(patientId)} />
      ) : (
        <Box sx={{ textAlign: "center", py: 4, color: "text.secondary" }}>
          No patient profile associated with this account.
        </Box>
      )}
    </Box>
  );
};

export default AnalyticsPage;
