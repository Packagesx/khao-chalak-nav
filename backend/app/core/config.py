"""
Application configuration.

All configuration is read from environment variables (see .env.example at the
repository root). Nothing here is hard-coded to a secret value — local
defaults are safe placeholders for development only and MUST be overridden
via environment variables / a real .env file in any shared or deployed
environment.
"""
from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    app_name: str = "Khao Chalak Outdoor Navigation API"
    app_version: str = "0.0.1"
    environment: str = "development"

    # CORS
    cors_allow_origins: str = "http://localhost:3000,http://localhost:5173"

    # Database (PostGIS) — used from Milestone 5 onward. Not required for
    # Milestone 0 (the /health endpoint has no database dependency), but the
    # setting lives here now so later milestones don't need to re-plumb config.
    database_url: str = "postgresql://khaochalak:khaochalak@localhost:5432/khaochalak"

    @property
    def cors_origins_list(self) -> list[str]:
        return [origin.strip() for origin in self.cors_allow_origins.split(",") if origin.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()
