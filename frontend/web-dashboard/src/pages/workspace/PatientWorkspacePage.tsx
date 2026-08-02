import { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import { Grid, Box } from "@mui/material";
import { useAuth } from "../../context/AuthContext";
import { workspaceSections } from "../../config/workspace/workspaceSections";

import PageTitle from "../../components/common/PageTitle";
import PageLoading from "../../components/common/PageLoading";
import PageError from "../../components/common/PageError";

import PatientHeader from "../../components/workspace/PatientHeader";

// Common details & doctor sections
import PatientDetailsCard from "../../components/workspace/PatientDetailsCard";
import GlucoseTrendsCard from "../../components/workspace/GlucoseTrendsCard";
import DoseHistoryCard from "../../components/workspace/DoseHistoryCard";
import AlertsCard from "../../components/workspace/AlertsCard";
import PrescriptionsCard from "../../components/workspace/PrescriptionsCard";
import DoseScheduleCard from "../../components/workspace/DoseScheduleCard";
import AnalyticsCard from "../../components/workspace/AnalyticsCard";
import ReportsCard from "../../components/workspace/ReportsCard";

// Caregiver specific sections
import TodayDoseCard from "../../components/workspace/TodayDoseCard";
import StorageMonitoringCard from "../../components/workspace/StorageMonitoringCard";
import InventoryMonitoringCard from "../../components/workspace/InventoryMonitoringCard";
import TimelineCard from "../../components/workspace/TimelineCard";
import { patientsService } from "../../services/patientsService";

import { useAutoRefresh } from "../../hooks/useAutoRefresh";

// Register all available workspace components by their section ID
const COMPONENT_REGISTRY: Record<string, React.ComponentType<any>> = {
  "patient-details": PatientDetailsCard,
  "glucose-trends": GlucoseTrendsCard,
  "dose-history": DoseHistoryCard,
  "alerts": AlertsCard,
  "prescriptions": PrescriptionsCard,
  "dose-schedule": DoseScheduleCard,
  "analytics": AnalyticsCard,
  "reports": ReportsCard,
  "today-dose": TodayDoseCard,
  "storage-monitoring": StorageMonitoringCard,
  "inventory-monitoring": InventoryMonitoringCard,
  "timeline": TimelineCard,
};

const PatientWorkspacePage = () => {
  const { role } = useAuth();
  const { patientId } = useParams<{ patientId: string }>();
  const parsedPatientId = Number(patientId);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [patientProfile, setPatientProfile] = useState<any>(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  useEffect(() => {
    if (!parsedPatientId) {
      setError("No patient ID provided.");
      setLoading(false);
      return;
    }
    const fetchProfile = async () => {
      try {
        const profile = await patientsService.getPatientProfile(parsedPatientId);
        setPatientProfile(profile);
        setError("");
      } catch (err: any) {
        setError("Failed to load patient profile.");
      } finally {
        setLoading(false);
      }
    };
    fetchProfile();
  }, [parsedPatientId]);

  // Centralised auto-refresh incrementing refreshTrigger every 5 seconds
  useAutoRefresh(() => {
    setRefreshTrigger((prev) => prev + 1);
  }, 5000);

  // Resolve layout sections configuration for the active user role
  const sections = workspaceSections[role] || [];

  if (loading) {
    return <PageLoading />;
  }

  if (error) {
    return <PageError message={error} />;
  }

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>Patient Workspace</PageTitle>

      <Box sx={{ mb: 4 }}>
        <PatientHeader patientProfile={patientProfile} />
      </Box>

      <Grid container spacing={3}>
        {sections.map((section) => {
          const Component = COMPONENT_REGISTRY[section.id];
          if (!Component) return null;
          return (
            <Grid key={section.id} size={section.gridSize}>
              <Component 
                patientId={parsedPatientId} 
                patientProfile={patientProfile} 
                refreshTrigger={refreshTrigger}
              />
            </Grid>
          );
        })}
      </Grid>
    </Box>
  );
};

export default PatientWorkspacePage;
