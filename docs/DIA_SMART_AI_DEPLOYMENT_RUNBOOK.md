# Dia-Smart AI Integration: Production Deployment & Operations Runbook

**Target Service:** `diasmart-ai.service` (FastAPI AI Gateway)  
**Host Environment:** AWS EC2 Ubuntu LTS (Target: co-located with Spring Boot `diasmart.service`)  
**Network Ingress:** Strictly Loopback (`127.0.0.1:8000`)  
**Status:** Approved Standard Operating Procedure (SOP)  
**Target Branch:** `ai-integration`  

---

## 1. Overview & Operational Scope

This runbook details operational procedures for provisioning, maintaining, updating, rotating secrets, and troubleshooting the Dia-Smart FastAPI AI Gateway on an Ubuntu EC2 host.

> [!NOTE]
> **Implementation vs. Deployment Notice:**
> The deployment templates, systemd service unit, preflight checker, installation script, and local loopback hardening have been implemented and verified locally. No actual AWS cloud deployment was executed during Part 7. The AWS infrastructure, security groups, and production environment paths described below represent the target production operational design.

### Host Topology & File System Conventions
- **Application Directory:** `/opt/diasmart-ai`
- **System User:** Unprivileged user with sudo privileges (`ubuntu` or dedicated service user)
- **Environment File:** `/etc/diasmart/ai-service.env` (permissions: `0600`, owned by `root:root`)
- **Systemd Service:** `/etc/systemd/system/diasmart-ai.service`
- **Python Version:** Python 3.11+ / 3.12 (managed in `/opt/diasmart-ai/.venv`)

---

## 2. Pre-Deployment Configuration Checklist

Before provisioning or updating the service on the production host, verify that the following environment variables are securely retrieved from a secure secret store (such as AWS Secrets Manager or Parameter Store) and never committed to source control:

| Variable | Description | Required Production Value |
|---|---|---|
| `AI_ENVIRONMENT` | Runtime operational environment | `production` |
| `AI_PROVIDER` | Active LLM inference provider | `gemini` (must NOT be `mock`) |
| `GEMINI_API_KEY` | Google AI Studio / Gemini API Key | Secure production key |
| `GEMINI_MODEL` | Gemini LLM model identifier | `gemini-2.5-flash` |
| `AI_INTERNAL_SERVICE_TOKEN` | Shared secret between Spring Boot & FastAPI | Cryptographically random string (min 32 chars) |
| `HOST` | Loopback binding address | `127.0.0.1` |
| `PORT` | Local service port | `8000` |
| `AI_LOG_LEVEL` | Application logging verbosity | `INFO` |

---

## 3. Automated Host Installation Procedure

The repository provides automated installation and preflight scripts in [deploy/ai-service/](deploy/ai-service/).

### Installation Steps (Target Ubuntu EC2 Host):

```bash
# 1. SSH into the production EC2 host
ssh -i /path/to/diasmart-key.pem ubuntu@api.diasmart.xyz

# 2. Pull the latest code from origin/ai-integration
cd /opt/diasmart/repo
git fetch origin
git checkout ai-integration
git pull origin ai-integration

# 3. Create the secure environment directory and file
sudo mkdir -p /etc/diasmart
sudo cp deploy/ai-service/diasmart-ai.env.template /etc/diasmart/ai-service.env
sudo chown root:root /etc/diasmart/ai-service.env
sudo chmod 0600 /etc/diasmart/ai-service.env

# 4. Populate /etc/diasmart/ai-service.env with production secrets from AWS Secrets Manager
sudo nano /etc/diasmart/ai-service.env
# Ensure:
# AI_ENVIRONMENT=production
# AI_PROVIDER=gemini
# GEMINI_API_KEY=<key>
# AI_INTERNAL_SERVICE_TOKEN=<min-32-char-token>

# 5. Run the preflight configuration validation check (no secrets are printed)
bash deploy/ai-service/check-config.sh /etc/diasmart/ai-service.env

# Expected output:
# [SUCCESS] Preflight configuration validation PASSED for: /etc/diasmart/ai-service.env

# 6. Execute automated installer
sudo bash deploy/ai-service/install.sh
```

### Verification of Systemd Status:
```bash
sudo systemctl status diasmart-ai.service

# Check local loopback health endpoint
curl -i http://127.0.0.1:8000/health
# Expected output: HTTP/1.1 200 OK -> {"status":"ok","service":"Dia-Smart AI Service","version":"0.1.0","provider":"gemini","prompt_version":"clinical-summary-v1"}
```

---

## 4. Routine Update Procedure (Rollout)

When new updates are merged to `ai-integration`:

```bash
# Step 1: Pull new release
cd /opt/diasmart/repo
git pull origin ai-integration

# Step 2: Re-run installer to sync files and virtualenv
sudo bash deploy/ai-service/install.sh

# Step 3: Run preflight configuration check
bash deploy/ai-service/check-config.sh /etc/diasmart/ai-service.env

# Step 4: Restart the AI service
sudo systemctl restart diasmart-ai.service

# Step 5: Verify health & status
sudo systemctl status diasmart-ai.service
curl -s http://127.0.0.1:8000/health
```

---

## 5. Rollback Procedures

### Scenario A: AI Service Defect Rollback
If a newly deployed AI service build encounters unexpected runtime errors:

```bash
# 1. Roll back code to previous known good commit
cd /opt/diasmart/repo
git checkout <PREVIOUS_STABLE_COMMIT_HASH>

# 2. Re-install from the previous stable code
sudo bash deploy/ai-service/install.sh

# 3. Restart the service
sudo systemctl restart diasmart-ai.service

# 4. Verify loopback health
curl -f http://127.0.0.1:8000/health
```

### Scenario B: Instant Kill-Switch Deactivation
If Google Gemini API experiences an extended outage or a safety review is underway:

```bash
# 1. Edit Spring Boot production environment file
sudo nano /etc/diasmart.env

# 2. Set AI kill-switch:
AI_ENABLED=false

# 3. Restart Spring Boot API:
sudo systemctl restart diasmart.service

# Spring Boot will immediately respond with an informative disabled state:
# HTTP 200 -> {"enabled": false, "message": "AI summary generation is currently deactivated."}
# Zero traffic will be sent to FastAPI or Google Gemini.
```

---

## 6. Secret Management & Rotation Procedures

### Procedure 1: Gemini API Key Rotation
Rotate the Gemini API Key every 90 days or immediately upon suspected compromise. **This procedure has zero downtime and zero impact on Spring Boot or React.**

1. Generate a new API Key in Google AI Studio / Google Cloud Console.
2. Update `/etc/diasmart/ai-service.env`:
   ```bash
   sudo nano /etc/diasmart/ai-service.env
   # Update: GEMINI_API_KEY=<NEW_KEY>
   ```
3. Verify configuration format:
   ```bash
   bash deploy/ai-service/check-config.sh /etc/diasmart/ai-service.env
   ```
4. Restart FastAPI:
   ```bash
   sudo systemctl restart diasmart-ai.service
   ```
5. Perform a verification smoke test via the Spring Boot clinical summary endpoint.
6. Revoke the old API Key in Google Cloud Console.

### Procedure 2: Internal Service Token Rotation (`AI_INTERNAL_SERVICE_TOKEN`)
The internal token protects the private loopback HTTP channel between Spring Boot and FastAPI using internal service-to-service bearer-token authentication.

1. Generate a new cryptographically secure 64-character token:
   ```bash
   openssl rand -hex 32
   ```
2. Update the token in `/etc/diasmart/ai-service.env`:
   ```bash
   sudo nano /etc/diasmart/ai-service.env
   # Update: AI_INTERNAL_SERVICE_TOKEN=<NEW_TOKEN>
   ```
3. Update the token in Spring Boot's `/etc/diasmart.env`:
   ```bash
   sudo nano /etc/diasmart.env
   # Update: AI_INTERNAL_SERVICE_TOKEN=<NEW_TOKEN>
   ```
4. Restart FastAPI first, followed immediately by Spring Boot:
   ```bash
   sudo systemctl restart diasmart-ai.service
   sudo systemctl restart diasmart.service
   ```
5. Verify end-to-end functionality via the Web Dashboard or an authenticated request.

---

## 7. Monitoring, Logging, and Auditing

### Live Journal Logs:
```bash
# Monitor AI service logs in real time
sudo journalctl -u diasmart-ai.service -f

# View last 100 log lines with timestamp
sudo journalctl -u diasmart-ai.service -n 100 --no-pager
```

### Log Sanitization Standards:
- FastAPI's logging formatters sanitize all output.
- `GEMINI_API_KEY` and `AI_INTERNAL_SERVICE_TOKEN` are **never** printed to logs or standard error.
- Patient health records and raw prompts are logged at `DEBUG` level only (disabled in production where `AI_LOG_LEVEL=INFO`).

---

## 8. Incident Response & Troubleshooting Matrix

| Symptom / Error | Root Cause | Remediation Step |
|---|---|---|
| `503 Service Unavailable` on React UI | FastAPI service is stopped or crashed | Run `sudo systemctl status diasmart-ai.service`. Check logs with `journalctl -u diasmart-ai.service -n 50`. Restart with `systemctl restart diasmart-ai.service`. |
| `401 Unauthorized` in Spring Boot logs | `AI_INTERNAL_SERVICE_TOKEN` mismatch between Spring Boot and FastAPI | Compare tokens in `/etc/diasmart.env` and `/etc/diasmart/ai-service.env`. Synchronize them and restart both services. |
| `502 Bad Gateway` / Provider Mismatch | FastAPI returned `mock` while Spring expected `gemini` | Verify `/etc/diasmart/ai-service.env` has `AI_ENVIRONMENT=production` and `AI_PROVIDER=gemini`. Ensure `AI_EXPECTED_PROVIDER=gemini` in Spring Boot environment. |
| `502 Bad Gateway` / Safety Rejection | Generated output contained unauthorized clinical statements | Safety validator rejected unsafe output (fail-closed). Inspect prompt context for unexpected data patterns. |
| `429 Too Many Requests` from Gemini API | Google Gemini rate limits or quota exceeded | Check quota in Google Cloud Console. Consider increasing quota or temporarily setting `AI_ENABLED=false` until quota resets. |
| Port 8000 bind collision (`EADDRINUSE`) | Orphaned process running on port 8000 | Run `sudo lsof -i :8000` or `sudo netstat -tulpn \| grep :8000`. Terminate the process and restart `diasmart-ai.service`. |
| Interactive `/docs` accessible externally | Reverse proxy misconfiguration | Verify Nginx config at `/etc/nginx/sites-available/diasmart` does NOT forward `/internal` or port 8000. Verify FastAPI was started with `AI_ENVIRONMENT=production`. |
