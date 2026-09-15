"""
Khao Chalak Outdoor Navigation — API entrypoint.

Milestone 0 scope: application skeleton + /health only.
Spatial endpoints (trails, routes, elevation, activities, offline) are added
in later milestones per ROADMAP.md — do not add them ahead of their
milestone's real implementation.
"""
from datetime import datetime, timezone

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import get_settings

settings = get_settings()

app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description="GIS-first, offline-first outdoor navigation platform for Khao Chalak, Chonburi.",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health", tags=["system"])
def health() -> dict:
    """Liveness/readiness probe. No external dependencies at Milestone 0."""
    return {
        "status": "ok",
        "service": settings.app_name,
        "version": settings.app_version,
        "environment": settings.environment,
        "time_utc": datetime.now(timezone.utc).isoformat(),
    }


@app.get("/", tags=["system"])
def root() -> dict:
    return {
        "message": "Khao Chalak Outdoor Navigation API",
        "docs": "/docs",
        "health": "/health",
    }
