# Khao Chalak Outdoor Navigation

GIS-first, offline-first 2.5D/3D outdoor navigation platform for **Khao
Chalak, Chonburi, Thailand** — trail running, hiking, and cycling. See
[ARCHITECTURE.md](./ARCHITECTURE.md) for the full architecture decision and
[GIS_DATA.md](./GIS_DATA.md) for the status of every spatial dataset (this
project never fabricates trail, POI, or elevation data — see that file's
rules before adding any).

This repository is being built **milestone by milestone** (see
[ROADMAP.md](./ROADMAP.md) — added as milestones land). This is
**Milestone 1 — Map MVP**: a mobile 2D map (pan/zoom/compass/
current-location). No trail, routing, terrain, or offline features exist
yet.

## Native pivot (post-Milestone 1)

The shipping app changed from a web PWA to a **native Android app**
(Kotlin + MapLibre Native Android SDK). The original React/Vite/MapLibre
GL JS web frontend is archived at
[`frontend-web-archived/`](./frontend-web-archived/ARCHIVE_NOTICE.md) —
see that file for the full rationale. Short version: two upcoming
milestones need real native platform APIs a WebView-wrapped PWA can't
reliably provide —

- **Milestone 3 (GPS Tracking)** — recording a route while the phone is
  locked/backgrounded needs a real foreground service with continuous
  location updates; web/WebView background geolocation gets killed by
  Android's battery optimization.
- **Milestone 11 (Offline Mode)** — storing offline map tiles robustly
  needs native file storage (MBTiles/SQLite), not IndexedDB's more limited
  quota and API.

Milestone 10 (3D Terrain) also renders noticeably better through
MapLibre's native SDK than through WebGL inside a browser/WebView.

The **backend** (FastAPI + PostGIS) is unaffected by this decision — it's
a REST API either client can call, and Milestones 5-9 (spatial DB, DEM,
elevation, route engine) proceed exactly as planned.

## Stack

| Layer | Choice |
|---|---|
| Mobile app | Kotlin + MapLibre Native Android SDK (`org.maplibre.gl:android-sdk`) |
| Backend | FastAPI (Python) |
| Spatial database | PostgreSQL + PostGIS (wired in Milestone 5) |
| Offline storage (planned) | Native file storage — MBTiles/SQLite (Milestone 11) |
| GIS processing | GDAL / QGIS (added in Milestone 6) |
| Dev orchestration | Docker Compose (backend + PostGIS) |
| Web frontend | Archived — see [`frontend-web-archived/`](./frontend-web-archived/ARCHIVE_NOTICE.md) |
| Deployment (backend) | Render — see [DEPLOYMENT.md](./DEPLOYMENT.md) |

## Repository structure

```
khao-chalak-nav/
├── android/                    Native Android app (Kotlin + MapLibre Native) — the shipping app
│   └── app/src/main/
│       ├── java/com/khaochalak/nav/MainActivity.kt
│       └── res/                Layouts, drawables, mipmap icons, strings
├── frontend-web-archived/      Archived React + Vite + MapLibre GL JS web app (Milestones 0-1)
│                                — see ARCHIVE_NOTICE.md, do not build on top of this
├── landing/                    Standalone download page — copy still says "install this PWA",
│                                needs updating to point at the native APK/Play listing (not done yet)
├── backend/                    FastAPI app
├── docker-compose.yml          Backend + PostGIS, for local dev
├── .env.example                Copy to .env — no secrets are committed
├── GIS_DATA.md                 Provenance/metadata for every spatial dataset
├── DEPLOYMENT.md                How to put the backend online (Render, free)
├── INSTALL.md                  Detailed local install walkthrough (Thai)
└── README.md                   You are here
```

## Quickstart

### Android app

Requires **Android Studio** (not yet installed on the dev machine as of
this milestone — this project's build/verification so far was done from
a headless Gradle/Android-SDK toolchain in the cloud sandbox, without
Android Studio or an emulator).

1. Install Android Studio (https://developer.android.com/studio).
2. Open the `android/` folder as a project — Android Studio will
   regenerate `local.properties` with your local SDK path automatically.
3. Run on a connected device or emulator (▶ button), or
   `Build > Build Bundle(s)/APK(s) > Build APK(s)` to produce a sideloadable
   `.apk`.
4. Grant location permission when prompted, to use the current-location
   button.

A pre-built debug APK for Milestone 1 (arm64-v8a only, ~20MB) has already
been shared for hands-on sideload testing while Android Studio isn't set
up locally yet.

### Backend (for later milestones)

```bash
cd backend
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

Or via Docker Compose (backend + PostGIS):

```bash
cp .env.example .env
docker compose up --build
```

- Backend docs: http://localhost:8000/docs
- Backend health: http://localhost:8000/health

The backend has no Android-app caller wired up yet — that starts at
Milestone 5 (spatial DB) onward.

## Milestone 0 acceptance criteria (web track, archived)

- [x] `npm install` succeeded in the archived `frontend-web-archived/`
- [x] Frontend ran (`npm run dev` / Docker) and built (`npm run build`)
- [x] Backend runs (`uvicorn` / Docker)
- [x] `GET /health` responds `200`
- [x] `docker compose up` brings up backend + PostGIS
- [x] No secrets committed to the repository (`.env` is git-ignored; only
      `.env.example` with non-sensitive local-dev defaults is tracked)

## Milestone 1 acceptance criteria (native Android)

- [x] Map pans via MapLibre Native's default drag/fling gestures.
- [x] Map zooms via two on-screen FAB buttons (+/-) using
      `map.easeCamera(CameraUpdateFactory.zoomIn/zoomOut())`, plus default
      pinch-zoom and double-tap gestures.
- [x] Compass is shown natively via MapLibre's built-in compass control
      (`uiSettings.isCompassEnabled = true`) and heading-aware camera
      tracking is available via `CameraMode.TRACKING_COMPASS`.
- [x] "Current location" FAB requests runtime location permission
      (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` via
      `ActivityResultContracts.RequestMultiplePermissions`), then activates
      MapLibre's `LocationComponent` with the SDK's own bundled location
      engine (no Google Play Services dependency required).
- [x] Location failures (permission denied, position unavailable, or an
      engine exception) surface as a human-readable on-screen banner
      instead of failing silently, via an explicit
      `LocationEngineCallback.onFailure` handler — the native equivalent
      of the archived web app's `geolocate.on("error", ...)` handling.
- [x] Map is strictly 2D for this milestone
      (`uiSettings.isTiltGesturesEnabled = false`) — 3D terrain is
      Milestone 10, not pulled in early.
- [x] `./gradlew assembleDebug` builds successfully (`BUILD SUCCESSFUL`).
- [x] `./gradlew lintDebug` passes (only non-blocking nits — see Known
      limitations below).
- [x] App icon set (adaptive + legacy) generated from the existing PWA
      icon artwork so the app isn't shipped with the default icon.
- [ ] **Not yet verified**: interactive on-device confirmation of
      pan/zoom/compass/current-location by the user. Verified so far only
      via a real compiled build + lint (no emulator/display is available
      in the build sandbox) — a debug APK was shared for hands-on sideload
      testing, and this checkbox flips once that's confirmed.

## Data accuracy

No real Khao Chalak trail, POI, elevation, or danger-zone data exists in this
repository yet. Anything that looks like a coordinate today (e.g. the map's
default camera position) is an **unverified placeholder** — see
[GIS_DATA.md](./GIS_DATA.md) for exactly what is and isn't verified, and the
project's data rules before adding new spatial data.

## Known limitations (Milestone 1, native)

- No trail, routing, terrain, GPS *tracking* (recording a route), GPX, or
  offline functionality yet — this milestone is the base map only.
- The map basemap is MapLibre's public demo style (`demotiles.maplibre.org`)
  — sparse vector data (mostly country outlines), no real Khao Chalak
  terrain or features, and not licensed for production use. It's
  intentionally a placeholder until a proper basemap/terrain source is
  wired in (Milestone 6/10).
- The default camera position remains the Milestone 0 **unverified
  placeholder** coordinate (see `GIS_DATA.md`) — not a surveyed trailhead.
- Not verified in an emulator or by me directly — the build sandbox has no
  Android emulator/display, so verification so far is a real
  `assembleDebug`/`lintDebug` build plus API introspection (`javap` against
  the actual downloaded MapLibre AAR) rather than a visual check. A debug
  APK was shared for the user to sideload and confirm on a real device.
- Debug build only covers the `arm64-v8a` ABI (kept the sideload APK under
  the file-delivery size limit); a real release would build an `.aab` so
  Play Store serves each device its own ABI slice.
- `minSdk 24` (Android 7.0+, ~2016 onward) — no support for older devices.
- A few non-blocking `lintDebug` nits: legacy (pre-API 26) launcher icon
  isn't circular-masked (only the API 26+ adaptive icon is), no monochrome
  icon variant declared, and a couple of dependency-version-bump
  suggestions (androidx.core/appcompat/material) — none block the build.
- No authentication, no database migrations yet, and the Android app makes
  no backend calls yet (PostGIS container starts but nothing reads/writes
  it, and the app doesn't talk to FastAPI until later milestones).
- `landing/` (the promo/download page) still describes PWA install
  instructions and has not yet been updated to point at the native APK or
  a future Play Store listing.

## Deploying so others can use it

See [DEPLOYMENT.md](./DEPLOYMENT.md) for the backend deployment steps
(Render, free tier — note the free-tier caveats documented there: the
free web service sleeps after 15 minutes idle, and free Postgres expires
after 30 days, which is why no database is deployed yet). Android app
distribution (signed release build / Play Store listing) hasn't been set
up yet — Milestone 1's native app has only been shared as an unsigned
debug APK for direct sideload testing.

## Next milestone

**Milestone 2 — Trail Data MVP**: render mock trail GeoJSON on the native
map, clearly labeled as mock/placeholder data (no real Khao Chalak trail
data exists yet — see `GIS_DATA.md`).
