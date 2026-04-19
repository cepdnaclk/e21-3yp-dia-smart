const API_BASE =
  window.location.hostname === "localhost" ||
  window.location.hostname === "127.0.0.1"
    ? "http://localhost:3000"
    : "https://YOUR-BACKEND-URL";

function safeGet(id) {
  return document.getElementById(id);
}

function severityFromGlucose(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return { label: "No data", type: "unknown" };
  }

  const g = Number(value);
  if (g < 70) return { label: "Hypo risk", type: "crit" };
  if (g > 250) return { label: "Severe hyper", type: "crit" };
  if (g > 180) return { label: "High", type: "warn" };
  if (g > 140) return { label: "Elevated", type: "warn" };
  return { label: "In target", type: "ok" };
}

function getDisplayDate(raw) {
  const fromDateTimeSL = raw?.datetime_sl ? new Date(raw.datetime_sl.replace(" ", "T") + "+05:30") : null;
  if (fromDateTimeSL && !Number.isNaN(fromDateTimeSL.getTime())) return fromDateTimeSL;

  const fromCreatedAt = raw?.created_at ? new Date(raw.created_at) : null;
  if (fromCreatedAt && !Number.isNaN(fromCreatedAt.getTime())) return fromCreatedAt;

  const fromReceivedAt = raw?.received_at ? new Date(raw.received_at) : null;
  if (fromReceivedAt && !Number.isNaN(fromReceivedAt.getTime())) return fromReceivedAt;

  return null;
}

function setStats(records) {
  const totalEl = safeGet("stat-total");
  const latestEl = safeGet("stat-latest");
  const highEl = safeGet("stat-high");
  const targetEl = safeGet("stat-target");

  if (totalEl) totalEl.textContent = String(records.length);

  const withGlucose = records.filter((r) => r.glucose !== null && r.glucose !== undefined && !Number.isNaN(Number(r.glucose)));
  const latest = withGlucose.length ? withGlucose[0].glucose : null;
  if (latestEl) latestEl.textContent = latest === null ? "—" : `${Number(latest).toFixed(0)} mg/dL`;

  const highCount = withGlucose.filter((r) => {
    const sev = severityFromGlucose(r.glucose);
    return sev.type === "warn" || sev.type === "crit";
  }).length;

  const targetCount = withGlucose.filter((r) => severityFromGlucose(r.glucose).type === "ok").length;

  if (highEl) highEl.textContent = String(highCount);
  if (targetEl) targetEl.textContent = String(targetCount);
}

function renderTable(records) {
  const tableBody = safeGet("log-table-body");
  if (!tableBody) return;

  if (records.length === 0) {
    tableBody.innerHTML = '<tr><td colspan="7">No readings found.</td></tr>';
    return;
  }

  const rows = records.map((r, index) => {
    const severity = severityFromGlucose(r.glucose);
    const reading = r.glucose === null || r.glucose === undefined || Number.isNaN(Number(r.glucose))
      ? "—"
      : Number(r.glucose).toFixed(0);

    const dateText = r.date ? r.date.toLocaleDateString() : "—";
    const timeText = r.date ? r.date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" }) : "—";

    const device = r.device ? String(r.device) : "—";
    const recordId = r.recordId === null || r.recordId === undefined ? "—" : String(r.recordId);

    return `
      <tr>
        <td>${index + 1}</td>
        <td>${dateText}</td>
        <td>${timeText}</td>
        <td>${reading}</td>
        <td><span class="sev ${severity.type}">${severity.label}</span></td>
        <td>${device}</td>
        <td>${recordId}</td>
      </tr>
    `;
  });

  tableBody.innerHTML = rows.join("");
}

async function fetchRecords() {
  // Prefer raw uploaded records so date/time from glucometer is preserved.
  try {
    const rawRes = await fetch(`${API_BASE}/api/glucometer/raw`);
    if (rawRes.ok) {
      const raw = await rawRes.json();
      if (Array.isArray(raw) && raw.length) {
        return raw
          .map((r) => ({
            date: getDisplayDate(r),
            glucose: r.glucose_mg_dl,
            device: r.device,
            recordId: r.record_id
          }))
          .filter((r) => r.date)
          .sort((a, b) => b.date - a.date);
      }
    }
  } catch (err) {
    console.error(err);
  }

  // Fallback to history endpoint.
  const historyRes = await fetch(`${API_BASE}/api/history`);
  if (!historyRes.ok) throw new Error("Failed to load history");
  const rows = await historyRes.json();

  return (Array.isArray(rows) ? rows : [])
    .map((r) => ({
      date: getDisplayDate(r),
      glucose: r.glucose_value,
      device: "Dia-Smart",
      recordId: null
    }))
    .filter((r) => r.date)
    .sort((a, b) => b.date - a.date);
}

async function loadGlucometerLog() {
  try {
    const records = await fetchRecords();
    setStats(records);
    renderTable(records);
  } catch (err) {
    console.error(err);
    const tableBody = safeGet("log-table-body");
    if (tableBody) {
      tableBody.innerHTML = '<tr><td colspan="7">Failed to load data from backend.</td></tr>';
    }
  }
}

loadGlucometerLog();
setInterval(loadGlucometerLog, 15000);
