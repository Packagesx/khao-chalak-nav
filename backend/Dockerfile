FROM python:3.11-slim

WORKDIR /app

# System deps kept minimal for Milestone 0. GDAL/GEOS libs are added in the
# Milestone 6 (DEM pipeline) image, not here, to keep this image small and
# fast for everyday API development.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app

EXPOSE 8000

# Shell form (not exec-form JSON array) so $PORT expands — required by
# Render and most PaaS hosts, which inject PORT at runtime and expect the
# process to bind to it. Defaults to 8000 for local `docker run`.
CMD uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000}
