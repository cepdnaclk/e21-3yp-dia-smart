import { useState, useEffect } from "react";
import { Card, CardContent, Box, CircularProgress } from "@mui/material";
import GroupsOutlinedIcon from "@mui/icons-material/GroupsOutlined";
import PeopleAltOutlinedIcon from "@mui/icons-material/PeopleAltOutlined";

import CardHeading from "../common/CardHeading";
import MetricCard from "../common/MetricCard";
import { caregiverService } from "../../services/caregiverService";

const AssignedPatientsSummary = () => {
  const [loading, setLoading] = useState(true);
  const [count, setCount] = useState<number | null>(null);

  useEffect(() => {
    const fetchCount = async () => {
      try {
        const patients = await caregiverService.getAssignedPatients();
        setCount(patients.length);
      } catch (err) {
        console.error("Failed to load patient count", err);
      } finally {
        setLoading(false);
      }
    };
    fetchCount();
  }, []);

  return (
    <Card sx={{ height: "100%" }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
        <CardHeading
          title="Assigned Patients Summary"
          icon={<GroupsOutlinedIcon />}
          color="#12233b"
        />

        <Box sx={{ flexGrow: 1 }}>
          <MetricCard
            label="Total Assigned Patients"
            value={
              loading ? (
                <CircularProgress size={20} sx={{ color: "secondary.dark" }} />
              ) : (
                count !== null ? count : "N/A"
              )
            }
            icon={<PeopleAltOutlinedIcon />}
            tone="primary"
          />
        </Box>
      </CardContent>
    </Card>
  );
};

export default AssignedPatientsSummary;
