import { Box } from "@mui/material";
import PersonOffOutlinedIcon from "@mui/icons-material/PersonOffOutlined";
import PageTitle from "../../components/common/PageTitle";
import EmptyState from "../../components/common/EmptyState";
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
        <EmptyState
          message="No patient profile associated with this account."
          icon={<PersonOffOutlinedIcon />}
          minHeight={220}
        />
      )}
    </Box>
  );
};

export default AnalyticsPage;
