# ARCHITECTURE.md — Khao Chalak Outdoor Navigation

## 1. Architecture decision

The system is treated as a **spatial data platform**, not a "pretty map
website": Terrain Data + Trail Data + Elevation + Spatial Database + Routing
+ GPS + Offline + Visualization. Two principles drive every decision:

1. **GIS-first** — geometry, CRS correctness, and dataset provenance are
   first-class concerns from Milestone 0 onward (see GIS_DATA.md), not
   bolted on later.
2. **Offline-first** — the mobile client must keep working (map, trail,
   GPS recording) with no connectivity on the mountain. Online services
   (PostGIS, FastAPI) are the source of truth and the place heavy GIS
   processing happens, but the client owns a local copy (IndexedDB) it can
   operate from.

The repository is a **monorepo** (`frontend/`, `backend/`) so the two halves
version together during early, fast-moving milestones. It can be split later
without re-architecting, since the API boundary (`/api/v1/...`) is already
the only contract between them.

Development proceeds in **15 milestones (0–14)**, each independently
runnable/demoable/testable — no big-bang implementation (see ROADMAP.md).
Data itself progresses in stages without re-architecting:
`Mock → Test → Verified Khao Chalak Data → Production`.

## 2. Technology stack

| Concern | Choice | Why |
|---|---|---|
| Map rendering | **MapLibre GL JS** | Open source, vector + terrain/3D capable, no vendor lock-in, mobile-friendly. Evaluated against Mapbox (proprietary/paid), CesiumJS (heavier, globe-scale, overkill for one mountain), Unity (native app weight, not a web/PWA fit), raw WebGL (reinvents too much). Any feature MapLibre can't do well will be called out explicitly with an alternative, not forced. |
| Frontend framework | React + TypeScript, built with Vite | Team default per spec; fast dev server; large ecosystem for map/PWA tooling |
| Backend | FastAPI (Python) | Async, typed, first-class OpenAPI docs (`/docs`), strong GIS/scientific Python ecosystem (GDAL, shapely, rasterio) for later milestones |
| Spatial database | PostgreSQL + PostGIS | Industry-standard OSS spatial DB; GiST indexing; wired starting Milestone 5 |
| GIS processing | GDAL, QGIS | DEM reprojection/clipping/hillshade/slope/contour pipeline (Milestone 6) |
| Offline client storage | IndexedDB | Browser-native, no extra service, works in a PWA |
| Local dev orchestration | Docker Compose | One command brings up frontend + backend + PostGIS consistently |
| Deployment target (later) | GitHub Pages / Cloudflare Pages (frontend) + a free-tier backend host | Keeps MVP cost at $0; revisited in DEPLOYMENT.md once a milestone needs it |

Everything above is open-source or free-tier per the project's cost
constraint; no paid service is required to run or develop the MVP.

## 3. Repository structure

```
khao-chalak-nav/
├── README.md
├── ARCHITECTURE.md
├── GIS_DATA.md
├── docker-compose.yml
├── .env.example
├── .gitignore
├── frontend/
│   ├── src/
│   │   ├── components/MapView.tsx     # MapLibre map (placeholder basemap)
│   │   ├── hooks/useApiHealth.ts      # backend wiring check
│   │   ├── config/appConfig.ts        # map center + style, marked PLACEHOLDER
│   │   ├── App.tsx / App.css
│   │   └── main.tsx
│   ├── package.json, vite.config.ts, tsconfig*.json
│   ├── Dockerfile, .dockerignore, .env.example
│   └── index.html
└── backend/
    ├── app/
    │   ├── main.py          # FastAPI app, /health, /
    │   └── core/config.py   # env-var-driven settings (pydantic-settings)
    ├── requirements.txt
    └── Dockerfile, .dockerignore
```

Future milestones add (not yet present, so as not to imply they're done):
`backend/app/api/`, `backend/app/models/`, `backend/scripts/` (DEM pipeline,
Milestone 6), `mock/` (Milestone 2), `offline/` package structure
(Milestone 11), and the remaining docs listed in the master spec §37
(API.md, DATABASE.md, OFFLINE.md, DEPLOYMENT.md, TESTING.md, ROADMAP.md) —
each added when its milestone actually implements the thing it documents.

## 4. Development milestones (summary)

| # | Milestone | Focus |
|---|---|---|
| 0 | Project Foundation | Runnable skeleton (this milestone) |
| 1 | Map MVP | Mobile 2D map: pan/zoom/compass/current-location |
| 2 | Trail Data MVP | Mock trail GeoJSON on the map, clearly labeled |
| 3 | GPS Tracking | Start/pause/stop recording, local persistence |
| 4 | GPX | Import/export GPX and GeoJSON |
| 5 | Spatial Database | Move trail data into PostGIS, expose via API |
| 6 | DEM Pipeline | Reproject/clip/hillshade/slope/contour (test DEM first) |
| 7 | Elevation Engine | Sample DEM along trail, gain/loss/slope |
| 8 | Route Engine | Trail graph, shortest path, elevation-aware cost |
| 9 | Route Preview | Planning UI: profile, stats, 2.5D preview |
| 10 | 3D Terrain | Terrain rendering, 2D/2.5D/3D toggle, camera controls |
| 11 | Offline Mode | Downloadable area package, works with no connectivity |
| 12 | Live Navigation | Segment detection, map matching, deviation/safety alerts |
| 13 | Activity Analytics | Post-activity summary, elevation profile, GPX export |
| 14 | Polish & Production Prep | Error/loading/empty/offline states, security, docs |

Full task/deliverable/acceptance-criteria detail per milestone lives in the
project's master prompt and is restated in each milestone's delivery report
as it is built (ROADMAP.md will consolidate these once created).

## 5. Design principles carried through every milestone

- **Reliability over features.** **GPS/offline performance over 3D visual
  quality.** **Data accuracy over new features.** (priority order per spec §43)
- No fabricated coordinates, trail geometry, elevation, slope, or danger
  zones — ever. Unverified values are `TODO` / `PLACEHOLDER` / `DATA
  REQUIRED`, tracked in GIS_DATA.md.
- CRS discipline: GPS/API boundaries use EPSG:4326; metric calculations use
  an appropriate projected CRS — never degrees treated as meters.
- A feature is only "done" per the Definition of Done (master spec §40):
  implemented, running, tested, usable UI, error state handled, offline
  behavior defined, docs updated, no fabricated data, limitations documented.
