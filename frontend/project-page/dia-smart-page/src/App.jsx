import logo from './assets/logo.png'
import './App.css'

const navItems = [
  ['introduction', 'Introduction'],
  ['about', 'About Us'],
  ['architecture', 'Architecture'],
  ['tech-stack', 'Tech Stack'],
  ['implementation', 'Implementation'],
  ['designs', 'Designs'],
  ['demonstration', 'Demonstration'],
  ['team', 'Team'],
]

const techStack = [
  {
    icon: 'web',
    title: 'Dashboard & Mobile UI',
    accent: 'React, React Native and Expo',
    body: 'The caregiver dashboard and mobile app visualize glucose trends, dose history, storage safety, inventory, and alert states from the backend.',
  },
  {
    icon: 'app',
    title: 'Embedded Firmware',
    accent: 'ESP32-C3 and ESP32-S3',
    body: 'The inner fridge node, dose sensing firmware, BLE glucometer sync, and outer gateway are implemented using ESP32-based microcontrollers.',
  },
  {
    icon: 'api',
    title: 'Backend & Storage',
    accent: 'Express API, PostgreSQL and JSONL',
    body: 'The prototype backend stores glucose, dosage, temperature, door, and inventory events using PostgreSQL with JSONL fallback for raw evidence logs.',
  },
  {
    icon: 'cloud',
    title: 'IoT Communication',
    accent: 'ESP-NOW, BLE, REST and MQTT',
    body: 'Local telemetry uses ESP-NOW and BLE, while the gateway uploads records to software services through REST now and MQTT/TLS in the cloud path.',
  },
]

const architectureItems = [
  {
    number: '01',
    title: 'Inner Fridge Unit',
    body: 'The ESP32-C3 sensor node monitors insulin storage temperature, door/access events, and inventory weight inside the refrigerator.',
  },
  {
    number: '02',
    title: 'Outer Hub Gateway',
    body: 'The ESP32-S3 hub receives fridge telemetry, dose events, and BLE glucometer records, then uploads structured records to the backend.',
  },
  {
    number: '03',
    title: 'Dashboard & Alerts',
    body: 'Caregivers and clinicians can review correlated glucose, insulin dosage, storage safety, inventory, and adherence data through dashboards.',
  },
]

const hardwareComponents = [
  'AS5600 magnetic encoder for insulin dose rotation sensing',
  'DS18B20 temperature sensor for 2-8 C insulin storage monitoring',
  'HX711 load cell module for insulin inventory estimation',
  'Reed switch and magnet for door/access event detection',
  'ESP32-C3 inner node and ESP32-S3 outer gateway',
  'BLE glucometer sync, ESP-NOW telemetry, Wi-Fi upload, display and buzzer',
]

const appFeatures = [
  'JWT login with caregiver, patient, and doctor role access',
  'Glucose, insulin dose, storage temperature, and inventory records',
  'Charts for glucose trends and dose adherence history',
  'Status badges for unsafe storage, missed doses, repeated doses, and low inventory',
  'REST API integration with PostgreSQL and raw JSONL fallback logs',
]

const designScreens = [
  { title: 'Overview', value: 'Patient Status', detail: 'Dose, glucose, storage, inventory' },
  { title: 'Storage', value: '4.8 C', detail: 'Safe insulin cold-chain range' },
  { title: 'Dose Log', value: '5 Units', detail: 'AS5600 BLE dose event captured' },
  { title: 'History', value: 'Trend View', detail: 'Glucose and adherence timeline' },
]

const teamMembers = [
  { name: 'Ananthasagaran N.', id: 'E/21/031' },
  { name: 'Arnikan U.', id: 'E/21/036' },
  { name: 'Sanjeevan U.', id: 'E/21/356' },
  { name: 'Sivasuthan J.', id: 'E/21/386' },
]

function Icon({ type }) {
  if (type === 'web') {
    return (
      <svg viewBox="0 0 48 48" aria-hidden="true">
        <circle cx="24" cy="24" r="16" />
        <path d="M8 24h32M24 8c5 5 8 10 8 16s-3 11-8 16M24 8c-5 5-8 10-8 16s3 11 8 16" />
      </svg>
    )
  }

  if (type === 'app') {
    return (
      <svg viewBox="0 0 48 48" aria-hidden="true">
        <rect x="15" y="7" width="18" height="34" rx="4" />
        <path d="M21 12h6M22 36h4" />
      </svg>
    )
  }

  if (type === 'api') {
    return (
      <svg viewBox="0 0 48 48" aria-hidden="true">
        <rect x="8" y="10" width="32" height="8" rx="2" />
        <rect x="8" y="30" width="32" height="8" rx="2" />
        <path d="M15 22h18M24 18v12M15 14h1M15 34h1" />
      </svg>
    )
  }

  return (
    <svg viewBox="0 0 48 48" aria-hidden="true">
      <path d="M15 33h22a8 8 0 0 0 1-16 12 12 0 0 0-23-3 9.5 9.5 0 0 0 0 19Z" />
      <path d="M18 38h12M22 42h12" />
    </svg>
  )
}

function SectionTitle({ kicker, title, intro }) {
  return (
    <div className="section-heading">
      {kicker && <span>{kicker}</span>}
      <h2>{title}</h2>
      <div className="title-line" aria-hidden="true" />
      {intro && <p>{intro}</p>}
    </div>
  )
}

function DeviceIllustration() {
  return (
    <div className="device-illustration" aria-label="DiaSmart device illustration">
      <div className="device-card device-card-top">
        <span>DiaSmart Hub</span>
        <strong>5U</strong>
        <small>Dose logged</small>
      </div>
      <div className="insulin-pen">
        <span />
        <span />
        <span />
      </div>
      <div className="pulse-ring pulse-one" />
      <div className="pulse-ring pulse-two" />
      <div className="reading-chip">BLE</div>
      <div className="chart-mini">
        <i />
        <i />
        <i />
        <i />
      </div>
    </div>
  )
}

function ArchitectureDiagram() {
  return (
    <div className="architecture-diagram" aria-label="DiaSmart solution architecture diagram">
      <div className="node hardware-node">
        <span className="node-icon">IN</span>
        <strong>Inner Unit</strong>
        <small>Temp, door, weight</small>
      </div>
      <div className="connector bluetooth">
        <span>ESP-NOW</span>
      </div>
      <div className="client-column">
        <div className="node client-node">
          <span className="node-icon">OUT</span>
          <strong>Outer Hub</strong>
          <small>ESP32-S3 gateway</small>
        </div>
        <div className="node client-node">
          <span className="node-icon">BLE</span>
          <strong>Dose + Glucometer</strong>
          <small>BLE records</small>
        </div>
      </div>
      <div className="connector request">
        <span>REST / MQTT</span>
      </div>
      <div className="cloud-box">
        <div className="node server-node">
          <span className="node-icon">JS</span>
          <strong>API Server</strong>
          <small>Express + auth</small>
        </div>
        <div className="db-stack">
          <span />
          <span />
          <span />
        </div>
        <strong>PostgreSQL</strong>
      </div>
    </div>
  )
}

function PhoneMockup({ screen }) {
  return (
    <article className="phone">
      <div className="phone-bar">
        <span>10:25</span>
        <span>5G</span>
      </div>
      <div className="phone-screen">
        <small>{screen.title}</small>
        <strong>{screen.value}</strong>
        <p>{screen.detail}</p>
        <div className="phone-visual">
          <span />
          <span />
          <span />
        </div>
      </div>
    </article>
  )
}

function App() {
  return (
    <div className="site-shell">
      <header className="navbar">
        <a className="brand" href="#top" aria-label="DiaSmart home">
          <img src={logo} alt="" className="brand-logo" />
          <span>DiaSmart</span>
        </a>
        <nav aria-label="Main navigation">
          {navItems.map(([id, label]) => (
            <a key={id} href={`#${id}`}>
              {label}
            </a>
          ))}
        </nav>
      </header>

      <main id="top">
        <section className="hero-section" id="introduction">
          <div className="hero-copy">
            <h1>IoT-assisted diabetes care for elderly patients</h1>
            <p>
              DiaSmart helps prevent insulin dosage errors and insulin spoilage
              by combining dose sensing, refrigerator storage monitoring,
              inventory tracking, BLE glucometer sync, and caregiver dashboard
              alerts.
            </p>
            <div className="hero-actions">
              <a className="primary-button" href="#about">
                Get Started
              </a>
              <a className="video-button" href="#demonstration">
                <span aria-hidden="true">&#9658;</span>
                Watch Demo
              </a>
            </div>
          </div>
          <DeviceIllustration />
        </section>

        <section className="content-section soft-section" id="about">
          <SectionTitle title="About Us" />
          <div className="about-grid">
            <div>
              <p>
                DiaSmart is designed for elderly diabetes care, where memory
                decline, reduced vision, reduced dexterity, and daily
                forgetfulness can make insulin routines risky. A missed dose can
                cause hyperglycemia, while a repeated dose can cause dangerous
                hypoglycemia.
              </p>
              <p>
                The system connects the home refrigerator, insulin pen usage,
                glucometer records, backend storage, and dashboard views into
                one evidence-based timeline. This helps caregivers and
                clinicians see what was measured, what was administered, and
                whether the insulin was stored safely.
              </p>
            </div>
            <div className="about-card">
              <span className="about-icon">IoT</span>
              <h3>Project Goal</h3>
              <p>
                Reduce patient interaction complexity while automatically
                collecting glucose, dose, storage, and inventory evidence for
                safer diabetes care.
              </p>
            </div>
          </div>
        </section>

        <section className="content-section architecture-section" id="architecture">
          <div className="architecture-copy">
            <SectionTitle kicker="System Flow" title="Solution Architecture" />
            <div className="accordion-list">
              {architectureItems.map((item, index) => (
                <details key={item.number} open={index === 0}>
                  <summary>
                    <span>{item.number}</span>
                    <strong>{item.title}</strong>
                  </summary>
                  <p>{item.body}</p>
                </details>
              ))}
            </div>
          </div>
          <ArchitectureDiagram />
        </section>

        <section className="content-section soft-section" id="tech-stack">
          <SectionTitle title="Technology Stack" />
          <div className="tech-grid">
            {techStack.map((item) => (
              <article className="tech-card" key={item.title}>
                <Icon type={item.icon} />
                <h3>{item.title}</h3>
                <strong>{item.accent}</strong>
                <p>{item.body}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="content-section implementation-section" id="implementation">
          <SectionTitle title="Hardware Implementation" />
          <div className="implementation-grid">
            <div className="hardware-board">
              <div className="microchip">ESP32-C3 / ESP32-S3</div>
              <span className="component nir">AS5600 Dose</span>
              <span className="component touch">DS18B20 Temp</span>
              <span className="component amp">HX711 Load Cell</span>
              <span className="component filter">Reed Door Sensor</span>
              <span className="component bt">BLE Glucometer</span>
              <span className="component lcd">Display + Buzzer</span>
            </div>
            <ul className="component-list">
              {hardwareComponents.map((component) => (
                <li key={component}>{component}</li>
              ))}
            </ul>
          </div>
        </section>

        <section className="content-section soft-section software-section">
          <SectionTitle
            title="Software Implementation"
            intro="The software layer stores sensor evidence, protects dashboard access, and presents patient history in a way caregivers and clinicians can act on."
          />
          <div className="feature-columns">
            <article>
              <h3>Mobile App</h3>
              <ul>
                {appFeatures.map((feature) => (
                  <li key={`mobile-${feature}`}>{feature}</li>
                ))}
              </ul>
            </article>
            <article>
              <h3>Web Dashboard</h3>
              <ul>
                {appFeatures.map((feature) => (
                  <li key={`web-${feature}`}>{feature}</li>
                ))}
              </ul>
            </article>
          </div>
        </section>

        <section className="content-section designs-section" id="designs">
          <SectionTitle
            title="Designs"
            intro="Prototype views for elderly-friendly guidance, caregiver monitoring, and clinical-style history review."
          />
          <div className="tabs" aria-label="Design categories">
            <span>Gateway Display</span>
            <span>Mobile App</span>
            <span>Web Dashboard</span>
          </div>
          <div className="phone-grid">
            {designScreens.map((screen) => (
              <PhoneMockup key={screen.title} screen={screen} />
            ))}
          </div>
        </section>

        <section className="demo-section" id="demonstration">
          <SectionTitle title="Demonstration" />
          <div className="demo-panel">
            <div>
              <h3>How to use DiaSmart</h3>
              <p>
                Store the insulin inside the monitored fridge unit, connect the
                outer hub, and let DiaSmart collect dose events, temperature,
                door/access status, inventory weight, and BLE glucometer
                records. The backend then shows correlated history and alerts on
                the dashboard.
              </p>
            </div>
            <a href="#designs">Watch this</a>
          </div>
        </section>

        <section className="content-section soft-section budget-section">
          <SectionTitle title="Final Budget" />
          <div className="budget-grid">
            <article>
              <span>Sensing and Boards</span>
              <strong>LKR 6,060</strong>
            </article>
            <article>
              <span>Power and Wiring</span>
              <strong>LKR 5,550</strong>
            </article>
            <article>
              <span>Total Prototype Estimate</span>
              <strong>LKR 23,610</strong>
            </article>
          </div>
        </section>

        <section className="content-section team-section" id="team">
          <SectionTitle title="Team" intro="Project contributors and responsibilities." />
          <div className="team-grid">
            {teamMembers.map((member, index) => (
              <article key={member.id}>
                <span>{String(index + 1).padStart(2, '0')}</span>
                <h3>{member.name}</h3>
                <p>{member.id}</p>
              </article>
            ))}
          </div>
        </section>
      </main>

      <a className="back-to-top" href="#top" aria-label="Back to top">
        &uarr;
      </a>
    </div>
  )
}

export default App
