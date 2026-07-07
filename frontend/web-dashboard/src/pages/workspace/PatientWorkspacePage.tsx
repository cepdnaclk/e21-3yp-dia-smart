import { Grid, Box } from "@mui/material";
import { useAuth } from "../../context/AuthContext";
import { workspaceSections } from "../../config/workspace/workspaceSections";

import PageTitle from "../../components/common/PageTitle";

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

// Register all available workspace components by their section ID
const COMPONENT_REGISTRY: Record<string, React.ComponentType> = {
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
  
  // Resolve layout sections configuration for the active user role
  const sections = workspaceSections[role] || [];

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>Patient Workspace</PageTitle>

      <Box sx={{ mb: 4 }}>
        <PatientHeader />
      </Box>

      <Grid container spacing={3}>
        {sections.map((section) => {
          const Component = COMPONENT_REGISTRY[section.id];
          if (!Component) return null;
          return (
            <Grid key={section.id} size={section.gridSize}>
              <Component />
            </Grid>
          );
        })}
      </Grid>
    </Box>
  );
};

export default PatientWorkspacePage;
