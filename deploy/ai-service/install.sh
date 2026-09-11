#!/usr/bin/env bash
# ==============================================================================
# Dia-Smart AI Service - Deployment & Installation Script for AWS EC2 (Ubuntu)
#
# Provisions the FastAPI microservice environment on localhost, configures
# dedicated virtual environment, installs dependencies, and registers systemd.
#
# Note:
# - Run as user with sudo privileges (e.g. ubuntu).
# - This script does NOT contain or generate secret keys.
# - The environment file (/etc/diasmart/ai-service.env) must be populated
#   from AWS Secrets Manager or Parameter Store before starting the service.
# ==============================================================================

set -euo pipefail

APP_DIR="/opt/diasmart-ai"
SERVICE_NAME="diasmart-ai"
SYSTEMD_DEST="/etc/systemd/system/${SERVICE_NAME}.service"
ENV_DIR="/etc/diasmart"
ENV_FILE="${ENV_DIR}/ai-service.env"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

echo "=================================================================="
echo " Dia-Smart AI Service - Host Installation (AWS EC2)"
echo "=================================================================="

# 1. Check Python version
echo "[1/6] Checking Python 3.11+ prerequisite..."
if ! command -v python3 &>/dev/null; then
    echo "[ERROR] Python 3 is not installed. Run: sudo apt-get update && sudo apt-get install -y python3 python3-venv python3-pip"
    exit 1
fi

PY_VERSION=$(python3 -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}')")
echo "      Found Python version: $PY_VERSION"

# 2. Create target directory
echo "[2/6] Preparing application directory at ${APP_DIR}..."
sudo mkdir -p "${APP_DIR}"
sudo chown -R "$(id -u):$(id -g)" "${APP_DIR}"

# 3. Synchronize code to /opt/diasmart-ai
echo "[3/6] Syncing AI service code to ${APP_DIR}..."
rsync -av --delete \
    --exclude '__pycache__' \
    --exclude '*.pyc' \
    --exclude '.venv' \
    --exclude '.pytest_cache' \
    --exclude '.coverage' \
    --exclude '.mypy_cache' \
    --exclude '.ruff_cache' \
    "${SOURCE_ROOT}/ai-service/" "${APP_DIR}/"

# 4. Create virtual environment & install dependencies
echo "[4/6] Setting up Python virtual environment and dependencies..."
if [ ! -d "${APP_DIR}/.venv" ]; then
    python3 -m venv "${APP_DIR}/.venv"
fi

"${APP_DIR}/.venv/bin/pip" install --upgrade pip setuptools wheel
"${APP_DIR}/.venv/bin/pip" install -e "${APP_DIR}"

# 5. Prepare configuration directory with secure permissions
echo "[5/6] Ensuring secure configuration directory exists at ${ENV_DIR}..."
sudo mkdir -p "${ENV_DIR}"
sudo chmod 700 "${ENV_DIR}"

if [ ! -f "${ENV_FILE}" ]; then
    echo "      [NOTICE] Configuration file ${ENV_FILE} not found."
    echo "      Creating template placeholder with permissions 600."
    sudo touch "${ENV_FILE}"
    sudo chmod 600 "${ENV_FILE}"
    sudo chown root:root "${ENV_FILE}"
fi

# 6. Install and enable systemd unit
echo "[6/6] Installing systemd unit file..."
sudo cp "${SCRIPT_DIR}/diasmart-ai.service" "${SYSTEMD_DEST}"
sudo chmod 644 "${SYSTEMD_DEST}"
sudo systemctl daemon-reload

echo "=================================================================="
echo " Installation Complete!"
echo " Next Steps:"
echo " 1. Populate secrets securely in ${ENV_FILE} (permissions 600):"
echo "      sudo chmod 600 ${ENV_FILE}"
echo " 2. Validate configuration preflight:"
echo "      bash ${SCRIPT_DIR}/check-config.sh ${ENV_FILE}"
echo " 3. Enable and start the systemd service:"
echo "      sudo systemctl enable ${SERVICE_NAME}"
echo "      sudo systemctl start ${SERVICE_NAME}"
echo " 4. Verify local loopback health:"
echo "      curl -s http://127.0.0.1:8000/health"
echo "=================================================================="
