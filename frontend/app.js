const state = {
  readings: [],
  currentUser: null,
  activeTab: "home"
};

const DEMO_USERS = [
  {
    fullName: "Maria Perera",
    role: "caregiver",
    email: "caregiver@diasmart.com",
    password: "Care1234"
  },
  {
    fullName: "Dr. Silva",
    role: "doctor",
    email: "doctor@diasmart.com",
    password: "Doctor1234"
  },
  {
    fullName: "John Fernando",
    role: "patient",
    email: "patient@diasmart.com",
    password: "Patient1234"
  }
];

const els = {
  authShell: document.getElementById("authShell"),
  dashboardShell: document.getElementById("dashboardShell"),
  loginView: document.getElementById("loginView"),
  signupView: document.getElementById("signupView"),
  loginForm: document.getElementById("loginForm"),
  signupForm: document.getElementById("signupForm"),
  goSignupBtn: document.getElementById("goSignupBtn"),
  goLoginBtn: document.getElementById("goLoginBtn"),
  logoutBtn: document.getElementById("logoutBtn"),
  profileLogoutBtn: document.getElementById("profileLogoutBtn"),
  tabButtons: Array.from(document.querySelectorAll(".tab-btn")),
  tabPanels: Array.from(document.querySelectorAll(".tab-panel")),
  userGreeting: document.getElementById("userGreeting"),
  userRoleBadge: document.getElementById("userRoleBadge"),
  profileUserName: document.getElementById("profileUserName"),
  profileUserEmail: document.getElementById("profileUserEmail"),
  profileUserRole: document.getElementById("profileUserRole"),
  loadSampleBtn: document.getElementById("loadSampleBtn"),
  jsonFileInput: document.getElementById("jsonFileInput"),
  statusText: document.getElementById("statusText"),
  avgValue: document.getElementById("avgValue"),
  latestValue: document.getElementById("latestValue"),
  countValue: document.getElementById("countValue"),
  inRangeValue: document.getElementById("inRangeValue"),
  trendCanvas: document.getElementById("trendCanvas"),
  dailyCanvas: document.getElementById("dailyCanvas"),
  zoneBars: document.getElementById("zoneBars"),
  readingTableBody: document.getElementById("readingTableBody")
};

const ZONES = [
  { key: "low", label: "Low", min: 0, max: 69 },
  { key: "normal", label: "Normal", min: 70, max: 140 },
  { key: "high", label: "High", min: 141, max: 199 },
  { key: "critical", label: "Critical", min: 200, max: Number.POSITIVE_INFINITY }
];

init();

function init() {
  initAuthState();

  els.goSignupBtn.addEventListener("click", () => showAuthView("signup"));
  els.goLoginBtn.addEventListener("click", () => showAuthView("login"));
  els.loginForm.addEventListener("submit", handleLogin);
  els.signupForm.addEventListener("submit", handleSignup);
  els.logoutBtn.addEventListener("click", handleLogout);
  els.profileLogoutBtn.addEventListener("click", handleLogout);

  els.tabButtons.forEach((button) => {
    button.addEventListener("click", () => switchTab(button.dataset.tab));
  });

  els.loadSampleBtn.addEventListener("click", loadSampleData);
  els.jsonFileInput.addEventListener("change", handleFileUpload);
  renderAll([]);
}

function initAuthState() {
  const storedUser = localStorage.getItem("diaSmartCurrentUser");
  if (storedUser) {
    try {
      state.currentUser = JSON.parse(storedUser);
    } catch (error) {
      state.currentUser = null;
      localStorage.removeItem("diaSmartCurrentUser");
    }
  }

  if (state.currentUser) {
    showApp();
  } else {
    showAuth();
    showAuthView("login");
  }
}

function showAuth() {
  els.authShell.classList.remove("hidden");
  els.dashboardShell.classList.add("hidden");
}

function showApp() {
  els.authShell.classList.add("hidden");
  els.dashboardShell.classList.remove("hidden");
  updateUserUI();
  switchTab(state.activeTab || "home");
}

function showAuthView(view) {
  if (view === "signup") {
    els.signupView.classList.remove("hidden");
    els.loginView.classList.add("hidden");
    return;
  }

  els.loginView.classList.remove("hidden");
  els.signupView.classList.add("hidden");
}

function readUsers() {
  try {
    const users = JSON.parse(localStorage.getItem("diaSmartUsers") || "[]");
    const storedUsers = Array.isArray(users) ? users : [];
    return [...DEMO_USERS, ...storedUsers];
  } catch (error) {
    return [...DEMO_USERS];
  }
}

function writeUsers(users) {
  // Persist only user-created accounts. Demo accounts are hardcoded constants.
  const onlyCustomUsers = users.filter((user) => !DEMO_USERS.some((demo) => demo.email === user.email));
  localStorage.setItem("diaSmartUsers", JSON.stringify(onlyCustomUsers));
}

function handleSignup(event) {
  event.preventDefault();

  const password = document.getElementById("signupPassword").value.trim();
  const confirmPassword = document.getElementById("confirmPassword").value.trim();

  if (password !== confirmPassword) {
    alert("Password confirmation does not match.");
    return;
  }

  const newUser = {
    fullName: document.getElementById("fullName").value.trim(),
    dob: document.getElementById("dob").value,
    phone: document.getElementById("phone").value.trim(),
    role: document.getElementById("relationship").value,
    email: document.getElementById("signupEmail").value.trim().toLowerCase(),
    password,
    patientCondition: document.getElementById("patientCondition").value.trim(),
    doctorName: document.getElementById("doctorName").value.trim(),
    deviceId: document.getElementById("deviceId").value.trim()
  };

  const users = readUsers();
  const emailExists = users.some((u) => u.email === newUser.email);

  if (emailExists) {
    alert("An account with this email already exists.");
    return;
  }

  users.push(newUser);
  writeUsers(users);

  alert("Account created successfully. Please login.");
  els.signupForm.reset();
  showAuthView("login");

  document.getElementById("loginRole").value = newUser.role;
  document.getElementById("loginEmail").value = newUser.email;
}

function handleLogin(event) {
  event.preventDefault();

  const email = document.getElementById("loginEmail").value.trim().toLowerCase();
  const password = document.getElementById("loginPassword").value.trim();
  const role = document.getElementById("loginRole").value;

  const users = readUsers();
  const matchingUser = users.find((u) => u.email === email && u.password === password && u.role === role);

  if (!matchingUser) {
    alert("Invalid credentials. Create an account first or check your details.");
    return;
  }

  state.currentUser = {
    fullName: matchingUser.fullName,
    email: matchingUser.email,
    role: matchingUser.role
  };

  localStorage.setItem("diaSmartCurrentUser", JSON.stringify(state.currentUser));
  state.activeTab = "home";
  showApp();
}

function handleLogout() {
  state.currentUser = null;
  localStorage.removeItem("diaSmartCurrentUser");
  showAuth();
  showAuthView("login");
}

function switchTab(tabId) {
  state.activeTab = tabId;

  els.tabButtons.forEach((button) => {
    button.classList.toggle("active", button.dataset.tab === tabId);
  });

  els.tabPanels.forEach((panel) => {
    panel.classList.toggle("active", panel.id === `tab-${tabId}`);
  });
}

function updateUserUI() {
  if (!state.currentUser) {
    return;
  }

  const roleLabel = state.currentUser.role.charAt(0).toUpperCase() + state.currentUser.role.slice(1);
  els.userGreeting.textContent = `${state.currentUser.fullName}`;
  els.userRoleBadge.textContent = `${roleLabel} Dashboard`;
  els.profileUserName.textContent = state.currentUser.fullName;
  els.profileUserEmail.textContent = state.currentUser.email;
  els.profileUserRole.textContent = `Role: ${roleLabel}`;
}

async function loadSampleData() {
  try {
    const response = await fetch("sample-data.json");
    const json = await response.json();
    ingestData(json, "Loaded sample dataset");
  } catch (error) {
    els.statusText.textContent = `Could not load sample data: ${error.message}`;
  }
}

function handleFileUpload(event) {
  const file = event.target.files?.[0];
  if (!file) {
    return;
  }

  const reader = new FileReader();
  reader.onload = () => {
    try {
      const json = JSON.parse(reader.result);
      ingestData(json, `Loaded ${file.name}`);
    } catch (error) {
      els.statusText.textContent = `Invalid JSON: ${error.message}`;
    }
  };

  reader.readAsText(file);
}

function ingestData(raw, message) {
  const list = Array.isArray(raw) ? raw : [raw];

  const normalized = list
    .filter((item) => typeof item === "object" && item !== null)
    .map((item) => ({
      record_id: Number(item.record_id),
      datetime_sl: String(item.datetime_sl || ""),
      glucose_mg_dl: Number(item.glucose_mg_dl),
      device: String(item.device || "Unknown")
    }))
    .filter((item) => Number.isFinite(item.record_id) && Number.isFinite(item.glucose_mg_dl) && item.datetime_sl.length > 0)
    .map((item) => ({ ...item, timestamp: parseDateTime(item.datetime_sl) }))
    .filter((item) => item.timestamp instanceof Date && !Number.isNaN(item.timestamp.getTime()))
    .sort((a, b) => a.timestamp - b.timestamp);

  state.readings = normalized;
  els.statusText.textContent = `${message}. Parsed ${normalized.length} valid readings.`;
  renderAll(normalized);
}

function renderAll(readings) {
  const metrics = buildMetrics(readings);
  renderKpis(metrics);
  renderZones(metrics.zones, readings.length);
  renderTrend(readings);
  renderDailyChart(metrics.dailyAverages);
  renderTable(readings);
}

function buildMetrics(readings) {
  const total = readings.length;
  const avg = total ? readings.reduce((sum, r) => sum + r.glucose_mg_dl, 0) / total : 0;
  const latest = total ? readings[total - 1] : null;
  const inRangeCount = readings.filter((r) => inRange(r.glucose_mg_dl)).length;

  const zones = {
    low: 0,
    normal: 0,
    high: 0,
    critical: 0
  };

  const perDay = {};

  readings.forEach((r) => {
    const zone = zoneFromValue(r.glucose_mg_dl);
    zones[zone] += 1;

    const dayKey = r.datetime_sl.slice(0, 10);
    if (!perDay[dayKey]) {
      perDay[dayKey] = { sum: 0, count: 0 };
    }
    perDay[dayKey].sum += r.glucose_mg_dl;
    perDay[dayKey].count += 1;
  });

  const dailyAverages = Object.entries(perDay)
    .map(([day, values]) => ({ day, avg: values.sum / values.count }))
    .sort((a, b) => (a.day < b.day ? -1 : 1));

  return {
    total,
    avg,
    latest,
    inRangePct: total ? (inRangeCount / total) * 100 : 0,
    zones,
    dailyAverages
  };
}

function renderKpis(metrics) {
  els.avgValue.textContent = metrics.total ? `${metrics.avg.toFixed(1)} mg/dL` : "-";
  els.latestValue.textContent = metrics.latest ? `${metrics.latest.glucose_mg_dl} mg/dL` : "-";
  els.countValue.textContent = String(metrics.total);
  els.inRangeValue.textContent = `${metrics.inRangePct.toFixed(0)}%`;
}

function renderZones(zones, total) {
  els.zoneBars.innerHTML = "";

  ZONES.forEach((zone) => {
    const count = zones[zone.key] || 0;
    const pct = total ? (count / total) * 100 : 0;

    const row = document.createElement("div");
    row.className = "zone-row";
    row.innerHTML = `
      <span>${zone.label}</span>
      <div class="zone-track"><div class="zone-fill ${zone.key}" style="width:${pct}%"></div></div>
      <span>${count}</span>
    `;

    els.zoneBars.appendChild(row);
  });
}

function renderTrend(readings) {
  const ctx = els.trendCanvas.getContext("2d");
  const w = els.trendCanvas.width;
  const h = els.trendCanvas.height;
  ctx.clearRect(0, 0, w, h);

  drawCanvasGrid(ctx, w, h, 5);

  if (readings.length < 2) {
    drawCanvasMessage(ctx, w, h, "Load at least 2 readings");
    return;
  }

  const values = readings.map((r) => r.glucose_mg_dl);
  const min = Math.min(...values, 60);
  const max = Math.max(...values, 220);

  const pad = { top: 20, right: 16, bottom: 24, left: 34 };
  const xScale = (i) => pad.left + (i * (w - pad.left - pad.right)) / (readings.length - 1);
  const yScale = (val) => h - pad.bottom - ((val - min) / (max - min || 1)) * (h - pad.top - pad.bottom);

  // Highlight preferred range band.
  const yTop = yScale(140);
  const yBottom = yScale(70);
  ctx.fillStyle = "rgba(42, 157, 91, 0.12)";
  ctx.fillRect(pad.left, yTop, w - pad.left - pad.right, yBottom - yTop);

  ctx.lineWidth = 3;
  ctx.strokeStyle = "#0e7c86";
  ctx.beginPath();

  readings.forEach((r, i) => {
    const x = xScale(i);
    const y = yScale(r.glucose_mg_dl);
    if (i === 0) {
      ctx.moveTo(x, y);
    } else {
      ctx.lineTo(x, y);
    }
  });

  ctx.stroke();

  readings.forEach((r, i) => {
    const x = xScale(i);
    const y = yScale(r.glucose_mg_dl);

    ctx.beginPath();
    ctx.fillStyle = colorForZone(zoneFromValue(r.glucose_mg_dl));
    ctx.arc(x, y, 3.5, 0, Math.PI * 2);
    ctx.fill();
  });
}

function renderDailyChart(dailyData) {
  const ctx = els.dailyCanvas.getContext("2d");
  const w = els.dailyCanvas.width;
  const h = els.dailyCanvas.height;
  ctx.clearRect(0, 0, w, h);

  drawCanvasGrid(ctx, w, h, 4);

  if (!dailyData.length) {
    drawCanvasMessage(ctx, w, h, "No daily aggregates yet");
    return;
  }

  const pad = { top: 16, right: 12, bottom: 34, left: 28 };
  const maxVal = Math.max(...dailyData.map((d) => d.avg), 200);
  const barW = (w - pad.left - pad.right) / dailyData.length - 10;

  dailyData.forEach((d, i) => {
    const x = pad.left + i * (barW + 10);
    const barH = (d.avg / maxVal) * (h - pad.top - pad.bottom);
    const y = h - pad.bottom - barH;

    ctx.fillStyle = "#4aa8b2";
    ctx.fillRect(x, y, barW, barH);

    ctx.fillStyle = "#36505a";
    ctx.font = "11px 'IBM Plex Mono', monospace";
    ctx.fillText(d.day.slice(5), x, h - 14);
  });
}

function renderTable(readings) {
  els.readingTableBody.innerHTML = "";

  const recent = [...readings].reverse().slice(0, 10);

  recent.forEach((r) => {
    const tr = document.createElement("tr");
    const zone = zoneFromValue(r.glucose_mg_dl);
    tr.innerHTML = `
      <td>${r.record_id}</td>
      <td>${r.datetime_sl}</td>
      <td>${r.glucose_mg_dl}</td>
      <td><span class="badge ${zone}">${zone}</span></td>
    `;
    els.readingTableBody.appendChild(tr);
  });
}

function drawCanvasGrid(ctx, w, h, lines) {
  ctx.strokeStyle = "#e5efeb";
  ctx.lineWidth = 1;

  for (let i = 1; i <= lines; i += 1) {
    const y = (h / (lines + 1)) * i;
    ctx.beginPath();
    ctx.moveTo(0, y);
    ctx.lineTo(w, y);
    ctx.stroke();
  }
}

function drawCanvasMessage(ctx, w, h, text) {
  ctx.fillStyle = "#5b7782";
  ctx.font = "15px 'IBM Plex Mono', monospace";
  ctx.fillText(text, w / 2 - 78, h / 2);
}

function inRange(value) {
  return value >= 70 && value <= 140;
}

function zoneFromValue(value) {
  if (value <= 69) {
    return "low";
  }
  if (value <= 140) {
    return "normal";
  }
  if (value <= 199) {
    return "high";
  }
  return "critical";
}

function colorForZone(zone) {
  if (zone === "low") {
    return "#4895ef";
  }
  if (zone === "normal") {
    return "#2a9d5b";
  }
  if (zone === "high") {
    return "#e66d00";
  }
  return "#cc2936";
}

function parseDateTime(dateTime) {
  const [datePart, timePart] = dateTime.split(" ");
  if (!datePart || !timePart) {
    return new Date("invalid");
  }

  const [year, month, day] = datePart.split("-").map(Number);
  const [hours, minutes, seconds] = timePart.split(":").map(Number);

  return new Date(year, month - 1, day, hours, minutes, seconds);
}
