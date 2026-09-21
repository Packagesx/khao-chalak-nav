# Archived: this was the web (PWA) track

This folder (`frontend-web-archived/`) is the React + TypeScript + Vite +
MapLibre GL JS web app from Milestones 0-1, kept for reference only. It is
**not** the shipping product anymore.

## Why archived

After Milestone 1 (Map MVP) shipped on the web, the project decided to
build the real app as a **native Android app** instead of a PWA packaged
into an APK (see `../android/`). Two upcoming milestones need real native
platform APIs that a WebView-wrapped PWA can't reliably provide:

- **Milestone 3 (GPS Tracking)** -- recording a route while the phone is
  locked/backgrounded needs a real foreground service with continuous
  location updates. Web/WebView background geolocation gets killed by
  Android's battery optimization.
- **Milestone 11 (Offline Mode)** -- storing offline map tiles robustly
  needs native file storage (MBTiles/SQLite), not IndexedDB's more limited
  quota and API.

Milestone 10 (3D Terrain) also renders noticeably better through
MapLibre's native SDK than through WebGL inside a browser/WebView.

## What's reusable, what isn't

- The **backend** (FastAPI + PostGIS) is untouched by this decision -- it's
  a REST API either client can call. Milestones 5-9 (spatial DB, DEM,
  elevation, route engine) proceed exactly as planned.
- This web frontend itself is not carried forward. It was a thin
  skeleton (Milestone 0) plus a basic map (Milestone 1), so not much
  implementation work is lost -- but do not build on top of this folder
  going forward.
- `landing/` (the promo/download page) still exists and will eventually
  need its copy and download link updated to point at the native APK /
  Play Store listing instead of "install this PWA" language -- not done
  yet as of this archive.

See `../README.md` and the project overview doc for the full pivot
rationale and current status.
