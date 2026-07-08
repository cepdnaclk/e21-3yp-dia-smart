import { useState, useEffect } from "react";
import { Grid, Snackbar, Alert } from "@mui/material";

import CaregiverCard from "../../components/care-team/CaregiverCard";
import DoctorCard from "../../components/care-team/DoctorCard";
import RelationshipManagementCard from "../../components/care-team/RelationshipManagementCard";
import RelationshipRequestsCard from "../../components/care-team/RelationshipRequestsCard";
import PageError from "../../components/common/PageError";
import PageLoading from "../../components/common/PageLoading";
import PageTitle from "../../components/common/PageTitle";
import { careTeamService } from "../../services/careTeamService";
import type {
  RelationshipRequestDto,
  RelationshipSummaryDto,
} from "../../types/careTeam";

const CareTeamPage = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [relationships, setRelationships] = useState<RelationshipSummaryDto[]>([]);
  const [sentRequests, setSentRequests] = useState<RelationshipRequestDto[]>([]);
  const [alert, setAlert] = useState<{ message: string; severity: "success" | "error" } | null>(null);

  const fetchData = async () => {
    try {
      const [rels, reqs] = await Promise.all([
        careTeamService.getMyRelationships(),
        careTeamService.getSentRequests(),
      ]);
      setRelationships(rels);
      setSentRequests(reqs);
      setError("");
    } catch (err: any) {
      setError("Failed to load care team information. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleRevoke = async (requestId: number) => {
    try {
      await careTeamService.revokeRelationship(requestId);
      setAlert({ message: "Relationship revoked successfully", severity: "success" });
      await fetchData();
    } catch (err: any) {
      setAlert({ message: "Failed to revoke relationship", severity: "error" });
    }
  };

  const handleShowAlert = (message: string, severity: "success" | "error") => {
    setAlert({ message, severity });
  };

  if (loading) {
    return <PageLoading />;
  }

  if (error) {
    return <PageError message={error} />;
  }

  const doctors = relationships.filter((r) => r.relationshipRole === "DOCTOR");
  const caregivers = relationships.filter((r) => r.relationshipRole === "CAREGIVER");

  return (
    <>
      <PageTitle>Care Team</PageTitle>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <DoctorCard doctors={doctors} onRevoke={handleRevoke} />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <CaregiverCard caregivers={caregivers} onRevoke={handleRevoke} />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <RelationshipRequestsCard requests={sentRequests} />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <RelationshipManagementCard onShowAlert={handleShowAlert} onRefresh={fetchData} />
        </Grid>
      </Grid>

      <Snackbar
        open={alert !== null}
        autoHideDuration={6000}
        onClose={() => setAlert(null)}
        anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
      >
        {alert ? (
          <Alert onClose={() => setAlert(null)} severity={alert.severity} sx={{ width: "100%" }}>
            {alert.message}
          </Alert>
        ) : undefined}
      </Snackbar>
    </>
  );
};

export default CareTeamPage;
