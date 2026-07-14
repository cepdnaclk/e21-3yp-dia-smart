import { useState } from "react";
import {
  Card,
  CardContent,
  Typography,
  FormGroup,
  FormControlLabel,
  Checkbox,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  CircularProgress,
  Alert
} from "@mui/material";
import DownloadIcon from "@mui/icons-material/Download";

import { doctorService } from "../../services/doctorService";

interface ExportReportsProps {
  patientId: number;
  startDate: string;
  endDate: string;
  patientName: string;
}

const ExportReports = ({ patientId, startDate, endDate, patientName }: ExportReportsProps) => {
  const [includeVitals, setIncludeVitals] = useState(true);
  const [includeAdherence, setIncludeAdherence] = useState(true);
  const [exportFormat, setExportFormat] = useState<"CSV" | "JSON">("CSV");
  const [loading, setLoading] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const handleExport = async () => {
    if (!includeVitals && !includeAdherence) {
      alert("Please select at least one data category to export.");
      return;
    }

    setLoading(true);
    setSuccessMsg(null);

    try {
      let vitalsData: any[] = [];
      let adherenceData: any = null;

      const promises: Promise<any>[] = [];
      if (includeVitals) {
        promises.push(
          doctorService.getAlerts().then((res) => {
            vitalsData = res.filter((a) => a.patientId === patientId);
          })
        );
      }
      if (includeAdherence) {
        promises.push(
          doctorService.getAdherenceAnalytics(patientId, startDate, endDate).then((res) => {
            adherenceData = res;
          })
        );
      }

      await Promise.all(promises);

      let fileContent = "";
      let filename = `export_${patientName.replace(/\s+/g, "_")}_${startDate}_to_${endDate}`;

      if (exportFormat === "JSON") {
        const payload: any = {
          patientName,
          patientId,
          exportDate: new Date().toISOString(),
          range: { startDate, endDate }
        };
        if (includeVitals) payload.vitalsAlerts = vitalsData;
        if (includeAdherence) payload.complianceAnalytics = adherenceData;

        fileContent = JSON.stringify(payload, null, 2);
        filename += ".json";
      } else {
        if (includeVitals && includeAdherence) {
          fileContent += `PATIENT DATA EXPORT\n`;
          fileContent += `Patient Name,${patientName}\n`;
          fileContent += `Patient ID,${patientId}\n`;
          fileContent += `Reporting Range,${startDate} to ${endDate}\n\n`;

          fileContent += `--- SECTION 1: VITALS ALERTS ---\n`;
          fileContent += `Alert ID,Severity,Title,Message,Status,Date\n`;
          vitalsData.forEach((a) => {
            fileContent += `${a.alertId},"${a.severity}","${a.title}","${a.message}","${a.status}",${a.createdAt}\n`;
          });

          fileContent += `\n--- SECTION 2: DOSE COMPLIANCE BREAKDOWN ---\n`;
          fileContent += `Overall Compliance Ratio,${adherenceData ? (adherenceData.adherenceRatio * 100).toFixed(1) : 0}%\n`;
          fileContent += `Date,Total Scheduled,On Time,Late,Missed,Unscheduled\n`;
          if (adherenceData) {
            adherenceData.dailyBreakdown.forEach((d: any) => {
              fileContent += `${d.date},${d.totalScheduled},${d.onTime},${d.late},${d.missed},${d.unscheduled}\n`;
            });
          }
        } else if (includeVitals) {
          fileContent += `Alert ID,Severity,Title,Message,Status,Date\n`;
          vitalsData.forEach((a) => {
            fileContent += `${a.alertId},"${a.severity}","${a.title}","${a.message}","${a.status}",${a.createdAt}\n`;
          });
        } else {
          fileContent += `Overall Compliance Ratio,${adherenceData ? (adherenceData.adherenceRatio * 100).toFixed(1) : 0}%\n`;
          fileContent += `Date,Total Scheduled,On Time,Late,Missed,Unscheduled\n`;
          if (adherenceData) {
            adherenceData.dailyBreakdown.forEach((d: any) => {
              fileContent += `${d.date},${d.totalScheduled},${d.onTime},${d.late},${d.missed},${d.unscheduled}\n`;
            });
          }
        }
        filename += ".csv";
      }

      const mimeType = exportFormat === "JSON" ? "application/json" : "text/csv;charset=utf-8;";
      const blob = new Blob([fileContent], { type: mimeType });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", filename);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      setSuccessMsg(`Successfully exported data to ${exportFormat} file.`);
    } catch (err) {
      console.error(err);
      alert("Failed to compile export files.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card elevation={2} sx={{ borderRadius: 2 }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
        <Typography variant="h6" sx={{ fontWeight: "medium" }}>
          Export Reports
        </Typography>

        <Typography variant="body2" color="text.secondary">
          Configure categories, select format, and download clinical export logs.
        </Typography>

        {successMsg && (
          <Alert severity="success" onClose={() => setSuccessMsg(null)}>
            {successMsg}
          </Alert>
        )}

        <FormGroup sx={{ gap: 1 }}>
          <FormControlLabel
            control={<Checkbox checked={includeVitals} onChange={(e) => setIncludeVitals(e.target.checked)} />}
            label="Include Vitals & Alerts"
          />
          <FormControlLabel
            control={<Checkbox checked={includeAdherence} onChange={(e) => setIncludeAdherence(e.target.checked)} />}
            label="Include Dose Adherence"
          />
        </FormGroup>

        <FormControl fullWidth size="small">
          <InputLabel id="export-format-label">Export File Format</InputLabel>
          <Select
            labelId="export-format-label"
            label="Export File Format"
            value={exportFormat}
            onChange={(e) => setExportFormat(e.target.value as "CSV" | "JSON")}
          >
            <MenuItem value="CSV">Comma Separated Values (.csv)</MenuItem>
            <MenuItem value="JSON">JavaScript Object Notation (.json)</MenuItem>
          </Select>
        </FormControl>

        <Button
          variant="contained"
          startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <DownloadIcon />}
          onClick={handleExport}
          disabled={loading}
          sx={{ textTransform: "none", borderRadius: 2 }}
        >
          {loading ? "Compiling..." : "Export Data"}
        </Button>
      </CardContent>
    </Card>
  );
};

export default ExportReports;
