# Multi-stage build for a minimal runtime image
FROM python:3.12-slim AS builder

WORKDIR /build

RUN pip install --no-cache-dir --upgrade pip

# Copy only dependency descriptor
COPY pyproject.toml /build/

# Install dependencies into global site-packages (no editable flag for container packaging)
# We use --only-main or similar to skip dev dependencies, but in standard pip we can install the package directly
RUN pip install --no-cache-dir .

# Final slim stage
FROM python:3.12-slim

WORKDIR /app

# Copy dependencies and bin executables from builder
COPY --from=builder /usr/local/lib/python3.12/site-packages /usr/local/lib/python3.12/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin

# Copy app source code
COPY app /app/app/

# Expose FastAPI default port
EXPOSE 8000

# Set non-root execution permissions
RUN useradd -u 10001 diasmart && \
    chown -R diasmart:diasmart /app

USER diasmart

# Health check using native Python to avoid curl/wget dependency in slim images
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')"

# Execute Uvicorn server in production mode
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
