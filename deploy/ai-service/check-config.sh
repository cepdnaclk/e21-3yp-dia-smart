#!/usr/bin/env bash
# ==============================================================================
# Dia-Smart AI Service - Production Configuration Validator
#
# Validates the presence, length, and policy constraints of required environment
# variables WITHOUT printing, echoing, logging, or exposing secret values.
#
# Usage:
#   bash check-config.sh [/path/to/env/file]
# ==============================================================================

set -euo pipefail

ENV_FILE="${1:-/etc/diasmart/ai-service.env}"
ERRORS=0

echo "=================================================================="
echo " Dia-Smart AI Service - Configuration Preflight Audit"
echo "=================================================================="

# Check if environment file exists (if path provided)
if [ -f "$ENV_FILE" ]; then
    echo "[INFO] Loading configuration from: $ENV_FILE"
    # Check permissions on environment file
    PERMS=$(stat -c "%a" "$ENV_FILE" 2>/dev/null || stat -f "%OLp" "$ENV_FILE" 2>/dev/null || echo "unknown")
    if [ "$PERMS" != "600" ] && [ "$PERMS" != "400" ]; then
        echo "[WARN] $ENV_FILE has permissions $PERMS (recommended: 600 or 400)"
    else
        echo "[OK]   $ENV_FILE permissions are restricted ($PERMS)"
    fi
    # Source the environment file safely
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
else
    echo "[INFO] No file found at $ENV_FILE; validating current process environment."
fi

echo "------------------------------------------------------------------"
echo " Variable Presence & Policy Checks (Values are NEVER displayed)"
echo "------------------------------------------------------------------"

# 1. AI_ENVIRONMENT
if [ -z "${AI_ENVIRONMENT:-}" ]; then
    echo "[FAIL] AI_ENVIRONMENT is missing or empty"
    ERRORS=$((ERRORS + 1))
else
    echo "[OK]   AI_ENVIRONMENT is configured"
fi

# 2. AI_PROVIDER
if [ -z "${AI_PROVIDER:-}" ]; then
    echo "[FAIL] AI_PROVIDER is missing or empty"
    ERRORS=$((ERRORS + 1))
else
    echo "[OK]   AI_PROVIDER is configured"
fi

# 3. Production Provider Policy Guard
CURRENT_ENV=$(echo "${AI_ENVIRONMENT:-}" | tr '[:upper:]' '[:lower:]' | xargs)
CURRENT_PROVIDER=$(echo "${AI_PROVIDER:-}" | tr '[:upper:]' '[:lower:]' | xargs)

if [ "$CURRENT_ENV" = "production" ] || [ "$CURRENT_ENV" = "prod" ]; then
    if [ "$CURRENT_PROVIDER" = "mock" ]; then
        echo "[FAIL] Production policy violation: AI_PROVIDER='mock' is forbidden in production."
        echo "       Production requires AI_PROVIDER='gemini' or AI_ENABLED=false."
        ERRORS=$((ERRORS + 1))
    elif [ "$CURRENT_PROVIDER" = "gemini" ]; then
        echo "[OK]   Production policy satisfied: AI_PROVIDER='gemini' configured for production."
    else
        echo "[FAIL] Unsupported provider '$CURRENT_PROVIDER' for production."
        ERRORS=$((ERRORS + 1))
    fi
fi

# 4. GEMINI_API_KEY
if [ "$CURRENT_PROVIDER" = "gemini" ]; then
    if [ -z "${GEMINI_API_KEY:-}" ]; then
        echo "[FAIL] GEMINI_API_KEY is missing or empty (required when provider is 'gemini')"
        ERRORS=$((ERRORS + 1))
    else
        KEY_LEN=${#GEMINI_API_KEY}
        if [ "$KEY_LEN" -lt 10 ]; then
            echo "[FAIL] GEMINI_API_KEY appears invalid (length: $KEY_LEN < 10)"
            ERRORS=$((ERRORS + 1))
        else
            echo "[OK]   GEMINI_API_KEY is present (length: $KEY_LEN characters, value masked)"
        fi
    fi

    # 5. GEMINI_MODEL
    if [ -z "${GEMINI_MODEL:-}" ]; then
        echo "[FAIL] GEMINI_MODEL is missing or empty"
        ERRORS=$((ERRORS + 1))
    else
        echo "[OK]   GEMINI_MODEL is configured"
    fi
fi

# 6. AI_INTERNAL_SERVICE_TOKEN
if [ -z "${AI_INTERNAL_SERVICE_TOKEN:-}" ]; then
    echo "[FAIL] AI_INTERNAL_SERVICE_TOKEN is missing or empty"
    ERRORS=$((ERRORS + 1))
else
    TOKEN_LEN=${#AI_INTERNAL_SERVICE_TOKEN}
    if [ "$TOKEN_LEN" -lt 32 ]; then
        echo "[FAIL] AI_INTERNAL_SERVICE_TOKEN is too short (length: $TOKEN_LEN < 32 characters)"
        ERRORS=$((ERRORS + 1))
    else
        echo "[OK]   AI_INTERNAL_SERVICE_TOKEN is present and meets length requirement ($TOKEN_LEN >= 32 chars)"
    fi
fi

echo "=================================================================="
if [ "$ERRORS" -eq 0 ]; then
    echo "[RESULT] ALL PRODUCTION CONFIGURATION CHECKS PASSED."
    echo "=================================================================="
    exit 0
else
    echo "[RESULT] $ERRORS CONFIGURATION CHECK(S) FAILED."
    echo "=================================================================="
    exit 1
fi
