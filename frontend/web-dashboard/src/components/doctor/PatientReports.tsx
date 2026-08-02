import { useState } from "react";
import {
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button,
  Box,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  IconButton,
  Divider
} from "@mui/material";
import DownloadIcon from "@mui/icons-material/Download";
import PrintIcon from "@mui/icons-material/Print";
import VisibilityIcon from "@mui/icons-material/Visibility";

import { doctorService } from "../../services/doctorService";
import type { Alert } from "../../types/alert";
import type { AdherenceAnalyticsResponse } from "../../types/analytics";

interface PatientReportsProps {
  patientId: number;
  startDate: string;
  endDate: string;
  patientName: string;
}

const PatientReports = ({ patientId, startDate, endDate, patientName }: PatientReportsProps) => {
  const [loading, setLoading] = useState(false);
  const [viewDialogOpen, setViewDialogOpen] = useState(false);
  const [viewContent, setViewContent] = useState<string>("");
  const [viewTitle, setViewTitle] = useState<string>("");

  const reports = [
    {
      id: "vitals",
      name: "General Vitals & Alerts Log",
      description: "Chronological log of patient behavioral warnings and vitals anomalies."
    },
    {
      id: "adherence",
      name: "Dose Adherence Compliance Breakdown",
      description: "Breakdown of daily scheduled dose compliance (On-Time, Late, Missed)."
    }
  ];

  const fetchReportData = async (reportId: string) => {
    setLoading(true);
    try {
      if (reportId === "vitals") {
        const alerts = await doctorService.getAlerts();
        const patientAlerts = alerts.filter(a => a.patientId === patientId);
        return { type: "vitals" as const, data: patientAlerts };
      } else {
        const analytics = await doctorService.getAdherenceAnalytics(patientId, startDate, endDate);
        return { type: "adherence" as const, data: analytics };
      }
    } catch (err) {
      console.error(err);
      alert("Failed to retrieve report data.");
      return null;
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadCSV = async (reportId: string, name: string) => {
    const res = await fetchReportData(reportId);
    if (!res) return;

    let csvContent = "";
    if (res.type === "vitals") {
      const data = res.data as Alert[];
      csvContent = "Alert ID,Severity,Title,Message,Status,Date\n";
      data.forEach((a) => {
        csvContent += `${a.alertId},"${a.severity}","${a.title}","${a.message}","${a.status}",${a.createdAt}\n`;
      });
    } else {
      const data = res.data as AdherenceAnalyticsResponse;
      csvContent = "Date,Total Scheduled,On Time,Late,Missed,Unscheduled\n";
      data.dailyBreakdown.forEach((d) => {
        const onTime = d.entries.filter((e) => e.status === "ON_TIME").length;
        const late = d.entries.filter((e) => e.status === "LATE").length;
        const missed = d.entries.filter((e) => e.status === "MISSED").length;
        const unscheduled = d.entries.filter((e) => e.status === "UNSCHEDULED").length;
        const totalScheduled = onTime + late + missed;
        csvContent += `${d.date},${totalScheduled},${onTime},${late},${missed},${unscheduled}\n`;
      });
    }

    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", `${name.replace(/\s+/g, "_")}_${patientName}_${startDate}_to_${endDate}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handlePrintReport = async (reportId: string, name: string) => {
    const res = await fetchReportData(reportId);
    if (!res) return;

    let htmlContent = `
      <html>
        <head>
          <title>${name}</title>
          <style>
            body { font-family: sans-serif; padding: 20px; line-height: 1.6; }
            h1 { color: #1976d2; margin-bottom: 5px; }
            .meta { color: #666; margin-bottom: 20px; border-bottom: 1px solid #ccc; padding-bottom: 10px; }
            table { width: 100%; border-collapse: collapse; margin-top: 15px; }
            th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
            th { background-color: #f2f2f2; }
          </style>
        </head>
        <body>
          <h1>${name}</h1>
          <div class="meta">
            <strong>Patient Name:</strong> ${patientName} (ID: ${patientId})<br/>
            <strong>Reporting Window:</strong> ${startDate} to ${endDate}<br/>
            <strong>Generated At:</strong> ${new Date().toLocaleString()}
          </div>
    `;

    if (res.type === "vitals") {
      const data = res.data as Alert[];
      htmlContent += `
        <table>
          <thead>
            <tr>
              <th>Alert ID</th>
              <th>Severity</th>
              <th>Title</th>
              <th>Message</th>
              <th>Status</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            ${data.length === 0 ? "<tr><td colspan='6'>No active vital alerts registered.</td></tr>" : ""}
            ${data.map((a) => `
              <tr>
                <td>${a.alertId}</td>
                <td>${a.severity}</td>
                <td>${a.title}</td>
                <td>${a.message}</td>
                <td>${a.status}</td>
                <td>${new Date(a.createdAt).toLocaleString()}</td>
              </tr>
            `).join("")}
          </tbody>
        </table>
      `;
    } else {
      const data = res.data as AdherenceAnalyticsResponse;
      htmlContent += `
        <h3>Summary Adherence Statistics</h3>
        <p><strong>Overall Adherence Ratio:</strong> ${(data.adherenceRate * 100).toFixed(1)}%</p>
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Total Scheduled</th>
              <th>On Time</th>
              <th>Late</th>
              <th>Missed</th>
              <th>Unscheduled</th>
            </tr>
          </thead>
          <tbody>
            ${data.dailyBreakdown.map((d) => {
              const onTime = d.entries.filter((e) => e.status === "ON_TIME").length;
              const late = d.entries.filter((e) => e.status === "LATE").length;
              const missed = d.entries.filter((e) => e.status === "MISSED").length;
              const unscheduled = d.entries.filter((e) => e.status === "UNSCHEDULED").length;
              const totalScheduled = onTime + late + missed;
              return `
                <tr>
                  <td>${d.date}</td>
                  <td>${totalScheduled}</td>
                  <td>${onTime}</td>
                  <td>${late}</td>
                  <td>${missed}</td>
                  <td>${unscheduled}</td>
                </tr>
              `;
            }).join("")}
          </tbody>
        </table>
      `;
    }

    htmlContent += `
        </body>
      </html>
    `;

    const printWindow = window.open("", "_blank");
    if (printWindow) {
      printWindow.document.open();
      printWindow.document.write(htmlContent);
      printWindow.document.close();
      printWindow.focus();
      setTimeout(() => {
        printWindow.print();
        printWindow.close();
      }, 500);
    }
  };

  const handleViewReport = async (reportId: string, name: string) => {
    const res = await fetchReportData(reportId);
    if (!res) return;

    let rawText = `Patient Name: ${patientName} (ID: ${patientId})\nReporting Window: ${startDate} to ${endDate}\nGenerated: ${new Date().toLocaleString()}\n\n`;

    if (res.type === "vitals") {
      const data = res.data as Alert[];
      rawText += `Vitals Alert Logs:\n`;
      if (data.length === 0) rawText += "No alerts found.\n";
      data.forEach((a) => {
        rawText += `[${a.severity}] ${a.title} - ${a.message} (${new Date(a.createdAt).toLocaleString()})\n`;
      });
    } else {
      const data = res.data as AdherenceAnalyticsResponse;
      rawText += `Compliance Statistics:\n`;
      rawText += `Adherence Ratio: ${(data.adherenceRate * 100).toFixed(1)}%\n\n`;
      rawText += `Daily Logs Breakdown:\n`;
      data.dailyBreakdown.forEach((d) => {
        const onTime = d.entries.filter((e) => e.status === "ON_TIME").length;
        const late = d.entries.filter((e) => e.status === "LATE").length;
        const missed = d.entries.filter((e) => e.status === "MISSED").length;
        const totalScheduled = onTime + late + missed;
        rawText += `${d.date} • Scheduled: ${totalScheduled} • Met: ${onTime} on-time, ${late} late • Missed: ${missed}\n`;
      });
    }

    setViewTitle(name);
    setViewContent(rawText);
    setViewDialogOpen(true);
  };

  return (
    <Card elevation={2} sx={{ borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Patient Reports
        </Typography>

        {loading && (
          <Box sx={{ display: "flex", justifyContent: "center", py: 2 }}>
            <CircularProgress size={24} />
          </Box>
        )}

        {/* Desktop Table View */}
        <TableContainer component={Paper} variant="outlined" sx={{ display: { xs: "none", md: "block" }, borderRadius: 2 }}>
          <Table>
            <TableHead sx={{ bgcolor: "action.hover" }}>
              <TableRow>
                <TableCell sx={{ fontWeight: "bold" }}>Report Name</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Description</TableCell>
                <TableCell align="right" sx={{ fontWeight: "bold" }}>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {reports.map((report) => (
                <TableRow key={report.id} hover>
                  <TableCell sx={{ fontWeight: "medium" }}>{report.name}</TableCell>
                  <TableCell>{report.description}</TableCell>
                  <TableCell align="right">
                    <Box sx={{ display: "flex", justifyContent: "flex-end", gap: 1 }}>
                      <IconButton size="small" color="primary" onClick={() => handleViewReport(report.id, report.name)} title="Preview Report">
                        <VisibilityIcon fontSize="small" />
                      </IconButton>
                      <IconButton size="small" color="secondary" onClick={() => handlePrintReport(report.id, report.name)} title="Print PDF">
                        <PrintIcon fontSize="small" />
                      </IconButton>
                      <IconButton size="small" color="success" onClick={() => handleDownloadCSV(report.id, report.name)} title="Download CSV">
                        <DownloadIcon fontSize="small" />
                      </IconButton>
                    </Box>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>

        {/* Mobile Card List View */}
        <Box sx={{ display: { xs: "flex", md: "none" }, flexDirection: "column", gap: 2 }}>
          {reports.map((report) => (
            <Card key={report.id} variant="outlined" sx={{ borderRadius: 3, border: "1px solid #e2e8f0" }}>
              <Box sx={{ p: 2 }}>
                <Typography variant="subtitle1" sx={{ fontWeight: 800, color: "#12233b", mb: 1 }}>
                  {report.name}
                </Typography>

                <Typography variant="body2" color="text.secondary" sx={{ fontSize: "0.85rem", mb: 2, lineHeight: 1.4 }}>
                  {report.description}
                </Typography>

                <Divider sx={{ mb: 1.5 }} />

                <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
                    Report Actions
                  </Typography>
                  <Box sx={{ display: "flex", gap: 1 }}>
                    <IconButton size="small" color="primary" onClick={() => handleViewReport(report.id, report.name)} title="Preview Report">
                      <VisibilityIcon fontSize="small" />
                    </IconButton>
                    <IconButton size="small" color="secondary" onClick={() => handlePrintReport(report.id, report.name)} title="Print PDF">
                      <PrintIcon fontSize="small" />
                    </IconButton>
                    <IconButton size="small" color="success" onClick={() => handleDownloadCSV(report.id, report.name)} title="Download CSV">
                      <DownloadIcon fontSize="small" />
                    </IconButton>
                  </Box>
                </Box>
              </Box>
            </Card>
          ))}
        </Box>

        <Dialog open={viewDialogOpen} onClose={() => setViewDialogOpen(false)} maxWidth="sm" fullWidth>
          <DialogTitle sx={{ fontWeight: "bold" }}>{viewTitle}</DialogTitle>
          <DialogContent dividers>
            <Typography component="pre" variant="body2" sx={{ whiteSpace: "pre-wrap", fontFamily: "monospace", bgcolor: "action.hover", p: 2, borderRadius: 2 }}>
              {viewContent}
            </Typography>
          </DialogContent>
          <DialogActions sx={{ px: 3, py: 2 }}>
            <Button onClick={() => setViewDialogOpen(false)} variant="contained">
              Close Preview
            </Button>
          </DialogActions>
        </Dialog>
      </CardContent>
    </Card>
  );
};

export default PatientReports;
