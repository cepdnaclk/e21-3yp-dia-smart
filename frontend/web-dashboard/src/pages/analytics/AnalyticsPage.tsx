import {
  Typography,
  Card,
  CardContent,
} from "@mui/material";

import GlucoseChart from "../../components/charts/GlucoseChart";

const AnalyticsPage = () => {
  return (
    <>
      <Typography
        variant="h4"
        sx={{ mb: 3 }}
      >
        Analytics
      </Typography>

      <Card>
        <CardContent>
          <Typography
            variant="h6"
            sx={{ mb: 2 }}
          >
            Weekly Glucose Trend
          </Typography>

          <GlucoseChart />
        </CardContent>
      </Card>
    </>
  );
};

export default AnalyticsPage;