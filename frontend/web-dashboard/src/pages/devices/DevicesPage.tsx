import { Grid } from "@mui/material";

import PageError from "../../components/common/PageError";
import PageLoading from "../../components/common/PageLoading";
import PageTitle from "../../components/common/PageTitle";
import DevicePlaceholderSection from "../../components/devices/DevicePlaceholderSection";

const deviceSections = [
  {
    title: "Device Summary",
    description:
      "Device summary information will appear here.",
  },
  {
    title: "Registered Devices",
    description:
      "Registered patient devices will appear here.",
  },
  {
    title: "Device Status",
    description:
      "Live device status details will appear here.",
  },
  {
    title: "Device Information",
    description:
      "Device model, firmware, and configuration details will appear here.",
  },
];

const DevicesPage = () => {
  // TODO: Replace placeholder state with device API loading and error state in Milestone 4.
  const loading = false;
  const error = "";

  // TODO: Fetch patient device data through deviceService after backend endpoints are available.
  if (loading) {
    return <PageLoading />;
  }

  if (error) {
    return <PageError message={error} />;
  }

  return (
    <>
      <PageTitle>Devices</PageTitle>

      <Grid container spacing={3}>
        {deviceSections.map((section) => (
          <Grid
            key={section.title}
            size={{ xs: 12, md: 6 }}
          >
            <DevicePlaceholderSection
              title={section.title}
              description={section.description}
            />
          </Grid>
        ))}
      </Grid>
    </>
  );
};

export default DevicesPage;
