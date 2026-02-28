const API_BASE = "http://localhost:3000";

let combinedChart;

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

async function loadLatest() {
  try {
    const res = await fetch(`${API_BASE}/api/latest`);
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
      if (data?.created_at) {
        const d = new Date(data.created_at);
        updatedEl.textContent = `Last updated: ${d.toLocaleString()}`;
      } else {
        updatedEl.textContent = "Last updated: —";
      }
    }
  } catch (err) {
    console.error(err);
  }
}

async function loadChart() {
  try {
    const res = await fetch(`${API_BASE}/api/history`);
    if (!res.ok) {
      throw new Error("Failed to load history");
    }
    const data = await res.json();

    if (!Array.isArray(data) || data.length === 0) {
      return;
    }

    const labels = data.map((r) =>
      new Date(r.created_at).toLocaleTimeString([], {
        hour: "2-digit",
        minute: "2-digit"
      })
    );
    const temps = data.map((r) => r.temperature);
    const glucose = data.map((r) => r.glucose_value);
    const inventory = data.map((r) => r.insulin_inventory_weight);

    const rangeEl = safeGet("history-range");
    if (rangeEl) {
      const first = new Date(data[0].created_at);
      const last = new Date(data[data.length - 1].created_at);
      const sameDay = first.toDateString() === last.toDateString();
      const datePart = sameDay
        ? first.toLocaleDateString()
        : `${first.toLocaleDateString()} → ${last.toLocaleDateString()}`;
      rangeEl.textContent = `Showing ${data.length} points • ${datePart}`;
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
  } catch (err) {
    console.error(err);
  }
}

// Initial load
loadLatest();
loadChart();

// Auto refresh
setInterval(loadLatest, 5000);
setInterval(loadChart, 15000);