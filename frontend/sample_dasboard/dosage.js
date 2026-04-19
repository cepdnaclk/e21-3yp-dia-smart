const API_BASE =
  window.location.hostname === "localhost" ||
  window.location.hostname === "127.0.0.1"
    ? "http://localhost:3000"
    : "https://YOUR-BACKEND-URL";

function safeGet(id) {
  return document.getElementById(id);
}

function formatDose(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return "-";
  }
  return `${Number(value).toFixed(1)} U`;
}

function toValidDate(raw) {
  if (!raw) return null;
  const d = new Date(raw);
  return Number.isNaN(d.getTime()) ? null : d;
}

function normalizeRecords(rows) {
  return (Array.isArray(rows) ? rows : [])
    .map((r) => ({
      id: r?.id ?? r?.dosage_id ?? r?.record_id ?? null,
      doseAmount: r?.dose_amount,
      injectionTime: toValidDate(r?.injection_time),
      createdAt: toValidDate(r?.created_at)
    }))
    .map((r) => ({
      ...r,
      displayTime: r.injectionTime || r.createdAt
    }))
    .filter((r) => r.displayTime)
    .sort((a, b) => b.displayTime - a.displayTime);
}

function setStats(records) {
  const totalEl = safeGet("stat-total");
  const latestDoseEl = safeGet("stat-latest-dose");
  const latestTimeEl = safeGet("stat-latest-time");
  const avgDoseEl = safeGet("stat-average-dose");

  if (totalEl) totalEl.textContent = String(records.length);

  const latest = records[0] || null;
  if (latestDoseEl) latestDoseEl.textContent = latest ? formatDose(latest.doseAmount) : "-";
  if (latestTimeEl) {
    latestTimeEl.textContent = latest
      ? latest.displayTime.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
      : "-";
  }

  const doses = records
    .map((r) => Number(r.doseAmount))
    .filter((v) => Number.isFinite(v));

  if (avgDoseEl) {
    if (doses.length === 0) {
      avgDoseEl.textContent = "-";
    } else {
      const avg = doses.reduce((sum, v) => sum + v, 0) / doses.length;
      avgDoseEl.textContent = `${avg.toFixed(1)} U`;
    }
  }
}

function renderTable(records) {
  const body = safeGet("dose-table-body");
  if (!body) return;

  if (records.length === 0) {
    body.innerHTML = '<tr><td colspan="5">No dosage entries found.</td></tr>';
    return;
  }

  const rows = records.map((r, idx) => {
    const dateText = r.displayTime.toLocaleDateString();
    const timeText = r.displayTime.toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit"
    });
    const doseText = formatDose(r.doseAmount);
    const idText = r.id === null || r.id === undefined ? "-" : String(r.id);

    return `
      <tr>
        <td>${idx + 1}</td>
        <td>${dateText}</td>
        <td>${timeText}</td>
        <td>${doseText}</td>
        <td>${idText}</td>
      </tr>
    `;
  });

  body.innerHTML = rows.join("");
}

async function loadDosageLog() {
  try {
    const res = await fetch(`${API_BASE}/api/dosage`);
    if (!res.ok) throw new Error("Failed to load dosage timeline");

    const data = await res.json();
    const records = normalizeRecords(data);

    setStats(records);
    renderTable(records);
  } catch (err) {
    console.error(err);
    const body = safeGet("dose-table-body");
    if (body) {
      body.innerHTML = '<tr><td colspan="5">Failed to load dosage data from backend.</td></tr>';
    }
  }
}

loadDosageLog();
setInterval(loadDosageLog, 10000);
