# --- STAGE 1: Builder ---
# Fixed: Changed 'as' to 'AS' to match keyword casing
FROM python:3.10-slim AS builder

WORKDIR /app

# Install compilation tools required for CVXPY and Scipy 
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    g++ \
    python3-dev \
    libssl-dev \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
# Install to a local directory to copy over later 
RUN pip install --user --no-cache-dir -r requirements.txt

# --- STAGE 2: Runner ---
# Fixed: Changed 'as' to 'AS' to match keyword casing
FROM python:3.10-slim AS runner

WORKDIR /app

# Environment optimizations [cite: 23]
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PORT=8080

# Copy only the compiled site-packages from the builder 
COPY --from=builder /root/.local /root/.local
COPY code/ .

# Ensure the local bin is in PATH
ENV PATH=/root/.local/bin:$PATH

# JSON array format to handle OS signals gracefully [cite: 25]
# Updated CMD for stability and performance
CMD ["gunicorn", "api:app", \
     "--workers", "3", \
     "--worker-class", "uvicorn.workers.UvicornWorker", \
     "--bind", "0.0.0.0:8080", \
     "--timeout", "300", \
     "--keep-alive", "65", \
     "--graceful-timeout", "30"]