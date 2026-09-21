# Khao Chalak Outdoor Navigation

GIS-first, offline-first 2.5D/3D outdoor navigation platform for **Khao
Chalak, Chonburi, Thailand** — trail running, hiking, and cycling. See
[ARCHITECTURE.md](./ARCHITECTURE.md) for the full architecture decision,
[GIS_DATA.md](./GIS_DATA.md) for the status of every spatial dataset (this
project never fabricates trail, POI, or elevation data — see that file's
rules before adding any), and [DESIGN.md](./DESIGN.md) for UI/UX design
references and which ones are scoped into which milestone.

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
├── DESIGN.md                    UI/UX design references, scoped per milestone
├── design-refs/                 Reference screenshots cited by DESIGN.md
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
- [x] **Verified on a real device** (Xiaomi phone, sideloaded debug APK):
      pan, zoom (+/- buttons), and current-location (GPS fix acquired,
      position dot tracked/moved with the device) all confirmed working
      by the user.

**Milestone 1 is fully DONE** — both the build/lint verification and the
on-device interactive confirmation have passed.

## Milestone 2 acceptance criteria (native Android, Trail Data MVP)

- [x] A fabricated mock trail network (`android/app/src/main/assets/mock/trails.geojson`,
      two `LineString` features) renders on the map as a muted-gray, dashed
      line via a `GeoJsonSource` + `LineLayer` (`PropertyFactory.lineColor`,
      `lineDasharray`, etc.) — the trail-rendering pipeline now works
      end-to-end, ready to swap in real data later without changing the
      rendering code.
- [x] The mock data is **clearly labeled in three places**, per the
      project's data-accuracy rule: (1) the GeoJSON file's own `_comment`
      and per-feature `source` properties say it's fabricated, (2) the
      line style itself (muted gray + dashed) is deliberately unlike how a
      real, verified trail will eventually look, and (3) a permanent
      on-screen banner (`mockDataLabel`, "⚠ MOCK TRAIL DATA — not real Khao
      Chalak trails, placeholder only") is shown whenever the mock layer
      loads — not relying on the line style alone, since a future restyle
      could silently drop the only "this isn't real" signal.
- [x] `GIS_DATA.md` dataset #3 (Trail network) updated to point at the new
      mock file instead of saying "NOT YET COLLECTED".
- [x] `gradle assembleDebug` and `gradle lintDebug` both `BUILD SUCCESSFUL`.
- [x] Missing/unreadable mock asset fails visibly (a `Log.w` warning) rather
      than silently showing an empty map, so a future regression (e.g. a
      renamed asset path) is easy to notice.
- [x] **Verified on a real device** (Xiaomi phone, sideloaded debug APK):
      mock trail line renders correctly, confirmed by the user.

**Milestone 2 is fully DONE** — both the build/lint verification and the
on-device interactive confirmation have passed.

## Milestone 3 acceptance criteria (native Android, GPS Tracking)

- [x] Start/pause/resume/stop controls for recording a route
      (`btnRecord`/`btnStop`), backed by `TrackingService`, a real Android
      **foreground service** — recording keeps running when the phone is
      locked or the app is backgrounded, using
      `ContextCompat.startForegroundService` + a bound-service connection
      for live UI updates while the app is in the foreground.
- [x] Local persistence via **Room** (`RecordingEntity`, `TrackPointEntity`,
      `TrackDao`, `AppDatabase`) — every GPS fix during a recording is
      written to SQLite as it arrives, and the recording row is finalized
      (end time, total distance) when stopped.
- [x] Live UI modeled on `design-refs/live-tracking-ui-reference.png` (see
      [DESIGN.md](./DESIGN.md#reference-1--live-tracking-screen-design-refslive-tracking-ui-referencepng)):
      a floating status card (elapsed time, distance in km, updated every
      second), and the route being recorded rendered live on the map as a
      **solid vivid-red line** — deliberately distinct from Milestone 2's
      muted-gray-dashed *mock* trail, so "what I'm recording right now" is
      never visually confused with "unverified placeholder data".
- [x] The location arrow stays **centered on screen** while recording
      (`CameraMode.TRACKING_COMPASS` forced on at record-start), and the
      existing "current location" button now also re-centers on every tap
      even if the map had been panned away — both per explicit request.
- [x] Distance uses `Location.distanceTo()` (a proper great-circle
      calculation in meters), never a naive degree diff — consistent with
      the project's CRS discipline.
- [x] `gradle assembleDebug` and `gradle lintDebug` both `BUILD SUCCESSFUL`
      (26 non-blocking lint nits, 0 errors — see Known limitations).
- [ ] Not yet verified on-device by the user (build/lint-verified only so far).

## Data accuracy

No real Khao Chalak trail, POI, elevation, or danger-zone data exists in this
repository yet. Anything that looks like a coordinate today (e.g. the map's
default camera position) is an **unverified placeholder** — see
[GIS_DATA.md](./GIS_DATA.md) for exactly what is and isn't verified, and the
project's data rules before adding new spatial data.

## Known limitations (Milestone 1 + 2 + 3, native)

- The Milestone 2 trail is **fabricated placeholder geometry**, not a real
  Khao Chalak trail — see `GIS_DATA.md` dataset #3 and the labeling
  described in the Milestone 2 acceptance criteria above. It's also not
  yet interactive (no tap-to-select, no distance/name popup) — that's
  future work once real trail data and the route engine (Milestone 8)
  exist.
- No routing, GPX import/export, or offline functionality yet. Milestone 3
  adds GPS *tracking* (recording), but recorded routes aren't exportable or
  viewable in a history list yet — that's Milestone 4 (GPX export) and
  Milestone 13 (Activity Analytics).
- Milestone 3's foreground service requests location updates via MapLibre's
  own `LocationEngineDefault` (same engine as Milestone 1's current-location
  button) at a 1-2s interval with 3m displacement filtering — not yet tuned
  for battery use over a multi-hour hike; revisit if real-world testing
  shows it draining the battery too fast.
- Room's annotation processor runs via **KSP, not kapt** — kapt's bundled
  `kotlinx-metadata-jvm` couldn't parse the metadata format our pinned
  Kotlin 2.2.10 compiler writes (`Provided Metadata instance has version
  2.2.0, while maximum supported version is 2.0.0`). KSP processes Kotlin
  symbols directly instead of reading that compiled metadata, so it isn't
  affected, and it's what Room's own docs recommend over kapt now anyway.
- A very small (practically unreachable at the ~1-2s GPS update rate)
  timing race exists between starting a recording (an async Room insert
  assigns its ID) and the first location fix arriving — see the comment in
  `TrackingService.locationCallback` for the exact scenario and why it was
  accepted rather than engineered away for this milestone.
- The map basemap is MapLibre's public demo style (`demotiles.maplibre.org`)
  — sparse vector data (mostly country outlines), no real Khao Chalak
  terrain or features, and not licensed for production use. It's
  intentionally a placeholder until a proper basemap/terrain source is
  wired in (Milestone 6/10).
- The default camera position remains the Milestone 0 **unverified
  placeholder** coordinate (see `GIS_DATA.md`) — not a surveyed trailhead.
- Not verified in an emulator or visually by me directly — the build
  sandbox has no Android emulator/display, so my own verification was a
  real `assembleDebug`/`lintDebug` build plus API introspection (`javap`
  against the actual downloaded MapLibre AAR) rather than a visual check.
  The interactive UI/UX (pan/zoom/current-location) has since been
  confirmed working by the user on a real device (sideloaded debug APK).
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
up yet — the native app has only been shared as unsigned debug APKs for
direct sideload testing.

## Next milestone

**Milestone 4 — GPX**: import/export recorded routes as GPX/GeoJSON, plus a
"Connect with Strava" personal GPX import (own activities/routes only, via
backend-mediated OAuth — see the Claude project doc's Strava section for
the full legal/architecture rationale). First, though, Milestone 3 needs
on-device confirmation from the user (see its acceptance criteria above).
