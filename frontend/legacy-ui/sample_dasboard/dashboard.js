const API_BASE = "http://localhost:3000";

let combinedChart;
let glucoseDateChart;
let dosageTimelineChart;

function safeGet(elementId) {
  return document.getElementById(elementId);
}

function formatNumber(value, decimals = 1) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "—";
  }
  return Number(value).toFixed(decimals);
}

function setStatusBadge(id, status) {
  const el = safeGet(id);
  if (!el) return;

  el.textContent = status.label;
  el.className = `status-badge ${status.type}`;
}

function evaluateTemperatureStatus(temp) {
  if (temp === null || temp === undefined) {
    return { label: "No data", type: "unknown" };
  }
  const t = Number(temp);
  if (Number.isNaN(t)) {
    return { label: "No data", type: "unknown" };
  }
  if (t < 2 || t > 10) {
    return { label: "Out of safe band", type: "crit" };
  }
  if (t < 3 || t > 8) {
    return { label: "Borderline", type: "warn" };
  }
  return { label: "Within 2–8 °C", type: "ok" };
}

function evaluateDoorStatus(doorStatus) {
  if (!doorStatus) {
    return {
      value: "Unknown",
      status: { label: "No data", type: "unknown" }
    };
  }

  const normalized = String(doorStatus).trim().toUpperCase();

  if (normalized === "OPEN") {
    return {
      value: "OPEN",
      status: { label: "Open – check quickly", type: "crit" }
    };
  }

  if (normalized === "CLOSED") {
    return {
      value: "Closed",
      status: { label: "Closed", type: "ok" }
    };
  }

  return {
    value: doorStatus,
    status: { label: "Unknown state", type: "unknown" }
  };
}

function evaluateInventoryStatus(weight) {
  if (weight === null || weight === undefined) {
    return { label: "No data", type: "unknown" };
  }
  const w = Number(weight);
  if (Number.isNaN(w)) {
    return { label: "No data", type: "unknown" };
  }
  if (w <= 3) {
    return { label: "Very low", type: "crit" };
  }
  if (w <= 12) {
    return { label: "Low", type: "warn" };
  }
  return { label: "OK", type: "ok" };
}

function evaluateInsulinLevelStatus(units) {
  if (units === null || units === undefined) {
    return { label: "No data", type: "unknown" };
  }
  const u = Number(units);
  if (Number.isNaN(u)) {
    return { label: "No data", type: "unknown" };
  }
  if (u <= 5) {
    return { label: "Near empty", type: "crit" };
  }
  if (u <= 20) {
    return { label: "Low", type: "warn" };
  }
  return { label: "Healthy", type: "ok" };
}

function evaluateGlucoseStatus(glucose) {
  if (glucose === null || glucose === undefined) {
    return { label: "No data", type: "unknown" };
  }
  const g = Number(glucose);
  if (Number.isNaN(g)) {
    return { label: "No data", type: "unknown" };
  }

  if (g < 70) {
    return { label: "Hypo risk", type: "crit" };
  }
  if (g > 250) {
    return { label: "Severe hyper", type: "crit" };
  }
  if (g > 180) {
    return { label: "High", type: "warn" };
  }
  if (g > 140) {
    return { label: "Elevated", type: "warn" };
  }
  return { label: "In target", type: "ok" };
}

function bindInteractions() {
  const glucoseCard = safeGet("glucose-card");
  if (glucoseCard) {
    glucoseCard.setAttribute("role", "button");
    glucoseCard.setAttribute("tabindex", "0");

    glucoseCard.addEventListener("click", () => {
      window.location.href = "glucometer.html";
    });

    glucoseCard.addEventListener("keydown", (event) => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        window.location.href = "glucometer.html";
      }
    });
  }

  const dosageCard = safeGet("dosage-card");
  if (dosageCard) {
    dosageCard.setAttribute("role", "button");
    dosageCard.setAttribute("tabindex", "0");

    dosageCard.addEventListener("click", () => {
      window.location.href = "dosage.html";
    });

    dosageCard.addEventListener("keydown", (event) => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        window.location.href = "dosage.html";
      }
    });
  }

  const startCaptureBtn = safeGet("start-dose-capture-btn");
  if (startCaptureBtn) {
    startCaptureBtn.addEventListener("click", triggerDoseCapture);
  }
}

async function triggerDoseCapture() {
  const startCaptureBtn = safeGet("start-dose-capture-btn");
  if (startCaptureBtn) {
    startCaptureBtn.disabled = true;
    startCaptureBtn.textContent = "Starting...";
  }

  try {
    const res = await fetch(`${API_BASE}/api/dosage/capture`, {
      method: "POST"
    });
    const payload = await res.json().catch(() => ({}));
    if (!res.ok) {
      throw new Error(payload.error || "Failed to start capture");
    }

    setStatusBadge("dosage-status", { label: "Capturing", type: "warn" });
  } catch (err) {
    console.error("Error starting dose capture:", err);
    setStatusBadge("dosage-status", { label: "Start failed", type: "crit" });
  } finally {
    if (startCaptureBtn) {
      startCaptureBtn.disabled = false;
      startCaptureBtn.textContent = "Start Dose Capture";
    }
  }
}

async function loadDosage() {
  try {
    const res = await fetch(`${API_BASE}/api/dosage`);
    if (!res.ok) {
      throw new Error("Failed to load dosage timeline");
    }

    const data = await res.json();
    const doseValueEl = safeGet("dosage-value");
    const doseTimeEl = safeGet("dosage-time");

    if (Array.isArray(data) && data.length > 0) {
      const latest = data[0];

      if (doseValueEl) {
        doseValueEl.textContent =
          latest?.dose_amount === null || latest?.dose_amount === undefined
            ? "—"
            : String(latest.dose_amount);
      }

      if (doseTimeEl) {
        if (latest?.injection_time) {
          const d = new Date(latest.injection_time);
          const timeStr = d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
          const dateStr = d.toLocaleDateString([], { month: "short", day: "numeric" });
          doseTimeEl.textContent = `${timeStr} on ${dateStr}`;
        } else {
          doseTimeEl.textContent = "Time: unavailable";
        }
      }

      setStatusBadge("dosage-status", { label: "Logged", type: "ok" });
      return;
    }

    if (doseValueEl) doseValueEl.textContent = "—";
    if (doseTimeEl) doseTimeEl.textContent = "Time: Awaiting detection...";
    setStatusBadge("dosage-status", { label: "No data", type: "unknown" });
  } catch (err) {
    console.error("Error fetching dosage:", err);
    setStatusBadge("dosage-status", { label: "Error", type: "crit" });
  }
}

async function loadLatest() {
  try {
    const res = await fetch(`${API_BASE}/api/latest-summary?source=file`);
    if (!res.ok) {
      throw new Error("Failed to load latest");
    }
    const data = await res.json();

    // Temperature
    const tempValue = data?.temperature ?? null;
    const tempDisplay = formatNumber(tempValue, 1);
    const tempEl = safeGet("temperature-value");
    if (tempEl) tempEl.textContent = tempDisplay;
    setStatusBadge("temperature-status", evaluateTemperatureStatus(tempValue));

    // Door status
    const doorInfo = evaluateDoorStatus(data?.door_status);
    const doorValueEl = safeGet("door-status-value");
    if (doorValueEl) doorValueEl.textContent = doorInfo.value;
    setStatusBadge("door-status-badge", doorInfo.status);

    // Inventory weight
    const invWeight = data?.insulin_inventory_weight ?? null;
    const invEl = safeGet("insulin-inventory-value");
    if (invEl) invEl.textContent = formatNumber(invWeight, 1);
    setStatusBadge("insulin-inventory-status", evaluateInventoryStatus(invWeight));

    // Pen level (estimated units)
    const insulinUnits = data?.insulin_level_value ?? null;
    const insulinLevelEl = safeGet("insulin-level-value");
    if (insulinLevelEl) {
      insulinLevelEl.textContent =
        insulinUnits === null || insulinUnits === undefined
          ? "—"
          : formatNumber(insulinUnits, 0);
    }
    setStatusBadge("insulin-level-status", evaluateInsulinLevelStatus(insulinUnits));

    // Glucose value
    const glucoseValue = data?.glucose_value ?? null;
    const glucoseEl = safeGet("glucose-value");
    if (glucoseEl) {
      glucoseEl.textContent =
        glucoseValue === null || glucoseValue === undefined
          ? "—"
          : formatNumber(glucoseValue, 0);
    }
    setStatusBadge("glucose-status", evaluateGlucoseStatus(glucoseValue));

    // Last updated
    const updatedEl = safeGet("last-updated");
    if (updatedEl) {
      if (data?.latest_event_at) {
        const d = new Date(data.latest_event_at);
        updatedEl.textContent = `Last updated: ${d.toLocaleString()}`;
      } else {
        updatedEl.textContent = "Last updated: —";
      }
    }

    const glucoseSyncedAtEl = safeGet("glucose-synced-at");
    if (glucoseSyncedAtEl) {
      if (data?.glucose_synced_at) {
        const g = new Date(data.glucose_synced_at);
        glucoseSyncedAtEl.textContent = `Last sync: ${g.toLocaleString()}`;
      } else {
        glucoseSyncedAtEl.textContent = "Last sync: Awaiting glucometer upload";
      }
    }
  } catch (err) {
    console.error(err);
  }
}

async function loadChart() {
  try {
    const res = await fetch(`${API_BASE}/api/history?source=file&limit=2000`);
    if (!res.ok) {
      throw new Error("Failed to load history");
    }
    const data = await res.json();

    if (!Array.isArray(data) || data.length === 0) {
      return;
    }

    const parsedRows = data
      .map((r) => ({
        createdAt: r.created_at,
        date: new Date(r.created_at),
        temperature: r.temperature,
        glucose: r.glucose_value,
        inventory: r.insulin_inventory_weight
      }))
      .filter((r) => !Number.isNaN(r.date.getTime()));

    if (parsedRows.length === 0) {
      return;
    }

    const labels = parsedRows.map((r) =>
      r.date.toLocaleTimeString([], {
        hour: "2-digit",
        minute: "2-digit"
      })
    );
    const temps = parsedRows.map((r) => r.temperature);
    const glucose = parsedRows.map((r) => r.glucose);
    const inventory = parsedRows.map((r) => r.inventory);

    const glucoseRows = parsedRows.filter((r) => r.glucose !== null && r.glucose !== undefined);
    const glucoseDateLabels = glucoseRows.map((r) =>
      `${r.date.toLocaleDateString()} ${r.date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`
    );
    const glucoseDateValues = glucoseRows.map((r) => r.glucose);

    const rangeEl = safeGet("history-range");
    if (rangeEl) {
      const first = parsedRows[0].date;
      const last = parsedRows[parsedRows.length - 1].date;
      const sameDay = first.toDateString() === last.toDateString();
      const datePart = sameDay
        ? first.toLocaleDateString()
        : `${first.toLocaleDateString()} - ${last.toLocaleDateString()}`;
      rangeEl.textContent = `Showing ${parsedRows.length} points • ${datePart}`;
    }

    const glucoseRangeEl = safeGet("glucose-history-range");
    if (glucoseRangeEl) {
      if (glucoseRows.length === 0) {
        glucoseRangeEl.textContent = "No glucose points yet";
      } else {
        const first = glucoseRows[0].date;
        const last = glucoseRows[glucoseRows.length - 1].date;
        const datePart = `${first.toLocaleDateString()} - ${last.toLocaleDateString()}`;
        glucoseRangeEl.textContent = `Showing ${glucoseRows.length} glucose points • ${datePart}`;
      }
    }

    const ctx = document.getElementById("combinedChart");
    if (!ctx) return;

    if (!combinedChart) {
      combinedChart = new Chart(ctx, {
        type: "line",
        data: {
          labels,
          datasets: [
            {
              label: "Temperature (°C)",
              data: temps,
              borderColor: "#0ea5e9",
              backgroundColor: "rgba(14, 165, 233, 0.12)",
              fill: false,
              tension: 0.25,
              yAxisID: "yTemp",
              pointRadius: 2,
              pointHoverRadius: 3
            },
            {
              label: "Glucose (mg/dL)",
              data: glucose,
              borderColor: "#22c55e",
              backgroundColor: "rgba(34, 197, 94, 0.08)",
              fill: false,
              tension: 0.25,
              yAxisID: "yGlucose",
              pointRadius: 2,
              pointHoverRadius: 3
            },
            {
              label: "Inventory (g)",
              data: inventory,
              borderColor: "#f97316",
              backgroundColor: "rgba(249, 115, 22, 0.06)",
              fill: false,
              borderDash: [5, 4],
              tension: 0.2,
              yAxisID: "yInventory",
              pointRadius: 1,
              pointHoverRadius: 2
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          interaction: {
            mode: "index",
            intersect: false
          },
          plugins: {
            legend: {
              position: "bottom",
              labels: {
                color: "#e5e7eb",
                usePointStyle: true,
                boxWidth: 8
              }
            },
            tooltip: {
              callbacks: {
                label: (ctx) => {
                  const label = ctx.dataset.label || "";
                  const v = ctx.parsed.y;
                  if (v === null || v === undefined || Number.isNaN(v)) {
                    return `${label}: —`;
                  }
                  return `${label}: ${v.toFixed(1)}`;
                }
              }
            }
          },
          scales: {
            x: {
              grid: {
                color: "rgba(30, 64, 175, 0.35)"
              },
              ticks: {
                color: "#9ca3af",
                maxTicksLimit: 8,
                autoSkip: true,
                maxRotation: 0
              }
            },
            yTemp: {
              type: "linear",
              position: "left",
              title: {
                display: true,
                text: "Temperature (°C)",
                color: "#e5e7eb"
              },
              grid: {
                color: "rgba(55, 65, 81, 0.65)"
              },
              ticks: {
                color: "#9ca3af"
              }
            },
            yGlucose: {
              type: "linear",
              position: "right",
              title: {
                display: true,
                text: "Glucose (mg/dL)",
                color: "#e5e7eb"
              },
              grid: {
                drawOnChartArea: false
              },
              ticks: {
                color: "#9ca3af"
              }
            },
            yInventory: {
              type: "linear",
              position: "right",
              display: false
            }
          }
        }
      });
    } else {
      combinedChart.data.labels = labels;
      combinedChart.data.datasets[0].data = temps;
      combinedChart.data.datasets[1].data = glucose;
      combinedChart.data.datasets[2].data = inventory;
      combinedChart.update();
    }

    const glucoseCtx = document.getElementById("glucoseDateChart");
    if (!glucoseCtx) return;

    if (!glucoseDateChart) {
      glucoseDateChart = new Chart(glucoseCtx, {
        type: "line",
        data: {
          labels: glucoseDateLabels,
          datasets: [
            {
              label: "Glucose (mg/dL)",
              data: glucoseDateValues,
              borderColor: "#22c55e",
              backgroundColor: "rgba(34, 197, 94, 0.15)",
              tension: 0.2,
              pointRadius: 2,
              pointHoverRadius: 3,
              fill: true
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              position: "bottom",
              labels: {
                color: "#e5e7eb",
                usePointStyle: true,
                boxWidth: 8
              }
            }
          },
          scales: {
            x: {
              grid: {
                color: "rgba(30, 64, 175, 0.35)"
              },
              ticks: {
                color: "#9ca3af",
                autoSkip: true,
                maxTicksLimit: 10,
                maxRotation: 40,
                minRotation: 20
              }
            },
            y: {
              title: {
                display: true,
                text: "Glucose (mg/dL)",
                color: "#e5e7eb"
              },
              grid: {
                color: "rgba(55, 65, 81, 0.65)"
              },
              ticks: {
                color: "#9ca3af"
              }
            }
          }
        }
      });
    } else {
      glucoseDateChart.data.labels = glucoseDateLabels;
      glucoseDateChart.data.datasets[0].data = glucoseDateValues;
      glucoseDateChart.update();
    }
  } catch (err) {
    console.error(err);
  }
}

async function loadDosageChart() {
  try {
    const res = await fetch(`${API_BASE}/api/dosage`);
    if (!res.ok) {
      throw new Error("Failed to load dosage timeline");
    }

    const rows = await res.json();
    const parsedRows = (Array.isArray(rows) ? rows : [])
      .map((r) => ({
        date: new Date(r.injection_time || r.created_at),
        dose: r.dose_amount
      }))
      .filter((r) => !Number.isNaN(r.date.getTime()) && r.dose !== null && r.dose !== undefined);

    const dosageRangeEl = safeGet("dosage-history-range");
    if (dosageRangeEl) {
      if (parsedRows.length === 0) {
        dosageRangeEl.textContent = "No dosage points yet";
      } else {
        const first = parsedRows[parsedRows.length - 1].date;
        const last = parsedRows[0].date;
        const datePart = `${first.toLocaleDateString()} - ${last.toLocaleDateString()}`;
        dosageRangeEl.textContent = `Showing ${parsedRows.length} dosage points - ${datePart}`;
      }
    }

    const labels = parsedRows
      .slice()
      .reverse()
      .map((r) => `${r.date.toLocaleDateString()} ${r.date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`);
    const values = parsedRows
      .slice()
      .reverse()
      .map((r) => Number(r.dose));

    const dosageCtx = document.getElementById("dosageTimelineChart");
    if (!dosageCtx) return;

    if (!dosageTimelineChart) {
      dosageTimelineChart = new Chart(dosageCtx, {
        type: "line",
        data: {
          labels,
          datasets: [
            {
              label: "Dosage (Units)",
              data: values,
              borderColor: "#f97316",
              backgroundColor: "rgba(249, 115, 22, 0.16)",
              tension: 0.2,
              pointRadius: 2,
              pointHoverRadius: 3,
              fill: true
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              position: "bottom",
              labels: {
                color: "#e5e7eb",
                usePointStyle: true,
                boxWidth: 8
              }
            }
          },
          scales: {
            x: {
              grid: {
                color: "rgba(30, 64, 175, 0.35)"
              },
              ticks: {
                color: "#9ca3af",
                autoSkip: true,
                maxTicksLimit: 10,
                maxRotation: 40,
                minRotation: 20
              }
            },
            y: {
              title: {
                display: true,
                text: "Dosage (Units)",
                color: "#e5e7eb"
              },
              grid: {
                color: "rgba(55, 65, 81, 0.65)"
              },
              ticks: {
                color: "#9ca3af"
              }
            }
          }
        }
      });
    } else {
      dosageTimelineChart.data.labels = labels;
      dosageTimelineChart.data.datasets[0].data = values;
      dosageTimelineChart.update();
    }
  } catch (err) {
    console.error(err);
  }
}

// Initial load
bindInteractions();
loadLatest();
loadChart();
loadDosage();
loadDosageChart();

// Auto refresh
setInterval(loadLatest, 5000);
setInterval(loadChart, 15000);
setInterval(loadDosage, 3000);
setInterval(loadDosageChart, 15000);
