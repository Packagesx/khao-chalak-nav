# Khao Chalak Outdoor Navigation

GIS-first, offline-first 2.5D/3D outdoor navigation platform for **Khao
Chalak, Chonburi, Thailand** — trail running, hiking, and cycling. See
[ARCHITECTURE.md](./ARCHITECTURE.md) for the full architecture decision and
[GIS_DATA.md](./GIS_DATA.md) for the status of every spatial dataset (this
project never fabricates trail, POI, or elevation data — see that file's
rules before adding any).

This repository is being built **milestone by milestone** (see
[ROADMAP.md](./ROADMAP.md) — added as milestones land). This is
**Milestone 1 — Map MVP**: a mobile-responsive 2D map (pan/zoom/compass/
current-location). No trail, routing, terrain, or offline features exist
yet.

## Stack

| Layer | Choice |
|---|---|
| Frontend | React + TypeScript + Vite, MapLibre GL JS |
| Backend | FastAPI (Python) |
| Spatial database | PostgreSQL + PostGIS (wired in Milestone 5) |
| Local/offline storage | IndexedDB (browser) |
| GIS processing | GDAL / QGIS (added in Milestone 6) |
| Dev orchestration | Docker Compose |
| Deployment (live) | Cloudflare Pages (frontend) + Render (backend) — both free tier, see [DEPLOYMENT.md](./DEPLOYMENT.md) |

## Repository structure

```
khao-chalak-nav/
├── frontend/          React + TypeScript + Vite app (MapLibre GL JS), PWA-installable
├── landing/           Standalone APK/download page — deployed separately from
│                      the nav app itself, so the shared download link is a
│                      distinct, more trustworthy URL. See DEPLOYMENT.md.
├── backend/           FastAPI app
├── docker-compose.yml Frontend + backend + PostGIS, for local dev
├── .env.example       Copy to .env — no secrets are committed
├── GIS_DATA.md        Provenance/metadata for every spatial dataset
├── DEPLOYMENT.md      How to put this online (Cloudflare + Render, free) + APK build
├── INSTALL.md         Detailed local install walkthrough (Thai)
└── README.md          You are here
```

## Quickstart

### Option A — Docker Compose (recommended)

```bash
cp .env.example .env
docker compose up --build
```

- Frontend: http://localhost:3000
- Backend docs: http://localhost:8000/docs
- Backend health: http://localhost:8000/health

### Option B — Run services directly

**Backend:**
```bash
cd backend
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

**Frontend:**
```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

To try the map controls, open the dev URL on your phone (or shrink your
browser window) — pan by dragging, zoom with the +/- buttons or
pinch/scroll, tap the compass button to reset bearing to north, and tap the
location-arrow button to center on your current position (requires
HTTPS or `localhost`, and browser location permission).

## Milestone 0 acceptance criteria

- [x] `npm install` succeeds in `frontend/`
- [x] Frontend runs (`npm run dev` / Docker) and builds (`npm run build`)
- [x] Backend runs (`uvicorn` / Docker)
- [x] `GET /health` responds `200`
- [x] `docker compose up` brings up frontend, backend, and PostGIS
- [x] No secrets committed to the repository (`.env` is git-ignored; only
      `.env.example` with non-sensitive local-dev defaults is tracked)

## Milestone 1 acceptance criteria

- [x] Map pans by drag (mouse and touch) — MapLibre's default `dragPan`,
      unchanged from Milestone 0.
- [x] Map zooms via on-screen +/- buttons and pinch/scroll/double-click —
      MapLibre's `NavigationControl` + default touch-zoom handlers.
- [x] Compass control visible and resets bearing to north on click
      (`NavigationControl({ showCompass: true })`).
- [x] "Current location" control requests the browser Geolocation API,
      centers the map on the result, and shows a live position dot
      (`GeolocateControl({ trackUserLocation: true })`).
- [x] Geolocation failure (permission denied / position unavailable /
      timeout) shows a human-readable on-map message instead of failing
      silently — verified for the permission-denied path.
- [x] Map is strictly 2D for this milestone (`maxPitch: 0`) — 3D terrain is
      Milestone 10, not pulled in early.
- [x] Layout and controls verified responsive at both a desktop viewport
      (1280px) and a small mobile viewport (390×844, iPhone-sized),
      including safe-area padding for notched phones (`env(safe-area-inset-*)`)
      and ≥44px touch targets on the map's zoom/compass/locate buttons
      (MapLibre's default control buttons are 29px, enlarged in `App.css`).
- [x] `npm run build` (`tsc -b && vite build`) passes with no type errors.
- [x] Verified visually via automated screenshots (desktop, zoomed, mobile,
      geolocated, and permission-denied states) before shipping.

## Data accuracy

No real Khao Chalak trail, POI, elevation, or danger-zone data exists in this
repository yet. Anything that looks like a coordinate today (e.g. the map's
default camera position) is an **unverified placeholder** — see
[GIS_DATA.md](./GIS_DATA.md) for exactly what is and isn't verified, and the
project's data rules before adding new spatial data.

## Known limitations (Milestone 1)

- No trail, routing, terrain, GPS *tracking* (recording a route), GPX, or
  offline functionality yet — this milestone is the base map only.
- The map basemap is MapLibre's public demo style (`demotiles.maplibre.org`)
  — sparse vector data (mostly country outlines), no real Khao Chalak
  terrain or features, and not licensed for production use. It's
  intentionally a placeholder until a proper basemap/terrain source is
  wired in (Milestone 6/10).
- The default camera position remains the Milestone 0 **unverified
  placeholder** coordinate (see `GIS_DATA.md`) — not a surveyed trailhead.
- Geolocation only works over HTTPS or `localhost` (a browser security
  requirement, not a bug) — this is already satisfied by the live
  Cloudflare deployment and local dev.
- No authentication, no database migrations yet (PostGIS container starts
  but nothing reads/writes it until Milestone 5).
- One pre-existing lint nit inherited from Milestone 0 (`oxlint`
  `react(set-state-in-effect)` on the map-init `catch` block) — a
  synchronous `setState` on the rare path where the MapLibre constructor
  itself throws; harmless (runs at most once, no cascading renders) and
  left as-is rather than restructured mid-milestone.

## Deploying so others can use it

See [DEPLOYMENT.md](./DEPLOYMENT.md) for the full step-by-step (push to
GitHub → Render for the backend → Cloudflare for the frontend, all
free tier). Note the free-tier caveats documented there — Render's free
web service sleeps after 15 minutes idle, and its free Postgres expires
after 30 days, which is why no database is deployed yet (none is needed
until Milestone 5).

The frontend is PWA-installable ("Add to Home Screen" on Android/iOS).
DEPLOYMENT.md also covers packaging that PWA as a real Android `.apk`
(via PWABuilder — no Mac or Google Play account required just to build
the file) and publishing it from the separate `landing/` download page
rather than the nav app's own URL, so a shared download link looks
trustworthy.

## Next milestone

**Milestone 2 — Trail Data MVP**: render mock trail GeoJSON on the map,
clearly labeled as mock/placeholder data (no real Khao Chalak trail data
exists yet — see `GIS_DATA.md`).
