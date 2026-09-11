# Dia-Smart AI Service — Deployment & Operations Guide

This directory contains production deployment templates, scripts, and configuration guidelines for running the **Dia-Smart AI Clinical Summary Microservice (FastAPI)** on AWS EC2.

---

## 1. Architecture & Security Invariants

* **Host Co-Location**: The FastAPI AI microservice runs alongside the Spring Boot backend on the same AWS EC2 instance.
* **Loopback Isolation**: The Uvicorn server is bound strictly to `127.0.0.1:8000`. Port 8000 **must never** have public AWS Security Group ingress or be forwarded by Nginx.
* **Network Call Graph**:
  $$\text{Browser} \longrightarrow \text{Spring Boot (Port 8080)} \longrightarrow \text{FastAPI (Port 8000)} \longrightarrow \text{Google Gemini API}$$
  * The Browser communicates exclusively with Spring Boot (`https://api.diasmart.xyz`).
  * Spring Boot queries PostgreSQL RDS, extracts and pseudonymizes clinical context, and calls FastAPI over localhost.
  * FastAPI alone invokes the external Google Gemini API over outbound HTTPS (Port 443).
  * Neither the browser nor Spring Boot calls Google Gemini directly.
* **Zero PHI & PII to AI**: Direct identifiers, database keys, patient names, emails, and account records are stripped by Spring Boot before reaching FastAPI.
* **Credential Isolation**: `GEMINI_API_KEY` exists exclusively in the FastAPI environment; it never reaches Spring Boot, PostgreSQL, or the frontend dashboard.

---

## 2. Directory Contents

| File | Purpose |
| :--- | :--- |
| `diasmart-ai.service` | Systemd service unit for managing the FastAPI microservice on Ubuntu EC2. |
| `install.sh` | Deployment script to provision `/opt/diasmart-ai`, create Python venv, and register systemd. |
| `check-config.sh` | Preflight configuration validator that verifies required variables without printing secrets. |
| `README.md` | This deployment and operations guide. |

---

## 3. Production Environment Variables

Production environment variables must be stored in `/etc/diasmart/ai-service.env` with permissions `600` owned by `root:root`:

```bash
# /etc/diasmart/ai-service.env
AI_ENVIRONMENT=production
AI_PROVIDER=gemini
GEMINI_MODEL=gemini-2.5-flash
GEMINI_TIMEOUT_SECONDS=30.0
GEMINI_TEMPERATURE=0.2

# Secrets (retrieved securely from AWS Secrets Manager / Parameter Store)
GEMINI_API_KEY=<your-production-gemini-api-key>
AI_INTERNAL_SERVICE_TOKEN=<minimum-32-char-cryptographically-secure-token>

# Service & Logging limits
AI_LOG_LEVEL=INFO
AI_MAX_REQUEST_BODY_BYTES=1048576
```

> [!WARNING]
> In production (`AI_ENVIRONMENT=production`), the application enforces that `AI_PROVIDER` must be `gemini`. Configuring `AI_PROVIDER=mock` causes an immediate startup validation failure.

---

## 4. Quick Installation (Ubuntu EC2)

```bash
# 1. Run the installation script
sudo bash deploy/ai-service/install.sh

# 2. Populate /etc/diasmart/ai-service.env with real production credentials
sudo nano /etc/diasmart/ai-service.env
sudo chmod 600 /etc/diasmart/ai-service.env

# 3. Validate configuration preflight (no secrets are printed)
bash deploy/ai-service/check-config.sh /etc/diasmart/ai-service.env

# 4. Start and enable the systemd service
sudo systemctl daemon-reload
sudo systemctl enable diasmart-ai
sudo systemctl start diasmart-ai

# 5. Check health
curl -s http://127.0.0.1:8000/health
```

---

## 5. Operations & Maintenance

### Service Control Commands
```bash
# Check status
sudo systemctl status diasmart-ai

# Restart service
sudo systemctl restart diasmart-ai

# Stop service
sudo systemctl stop diasmart-ai

# View live streaming logs
journalctl -u diasmart-ai -f
```

### Health Check Diagnostic
FastAPI provides a lightweight, local health check endpoint at `/health` that does not consume Gemini quota:
```bash
curl -i http://127.0.0.1:8000/health
```
Expected HTTP 200 response:
```json
{
  "status": "ok",
  "service": "Dia-Smart AI Service",
  "version": "0.1.0",
  "provider": "gemini",
  "prompt_version": "clinical-summary-v1"
}
```

### Disabling the AI Subsystem (Emergency Kill-Switch)
If an external Gemini outage or incident occurs, the AI subsystem can be disabled at the Spring Boot level without restarting or stopping the core application:
```bash
# In Spring Boot environment or application configuration:
AI_ENABLED=false
```
When `AI_ENABLED=false`, Spring Boot immediately returns an informational unavailable state for AI summary requests while all other features (authentication, glucose logging, insulin delivery, inventory, alerts) remain 100% operational.
