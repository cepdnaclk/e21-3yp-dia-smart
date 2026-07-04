import { Grid } from "@mui/material";

import CaregiverCard from "../../components/care-team/CaregiverCard";
import DoctorCard from "../../components/care-team/DoctorCard";
import RelationshipManagementCard from "../../components/care-team/RelationshipManagementCard";
import RelationshipRequestsCard from "../../components/care-team/RelationshipRequestsCard";
import PageError from "../../components/common/PageError";
import PageLoading from "../../components/common/PageLoading";
import PageTitle from "../../components/common/PageTitle";

const CareTeamPage = () => {
  // TODO: Replace placeholder state with care team API loading and error state in Milestone 4.
  const loading = false;
  const error = "";

  // TODO: Fetch doctors, caregivers, and relationship requests through careTeamService.
  if (loading) {
    return <PageLoading />;
  }

  if (error) {
    return <PageError message={error} />;
  }

  return (
    <>
      <PageTitle>Care Team</PageTitle>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <DoctorCard />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <CaregiverCard />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <RelationshipRequestsCard />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <RelationshipManagementCard />
        </Grid>
      </Grid>
    </>
  );
};

export default CareTeamPage;
