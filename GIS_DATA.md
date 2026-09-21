# GIS_DATA.md — Khao Chalak Outdoor Navigation

This file tracks the provenance of every spatial dataset used by the platform.
Per the project's data-accuracy rule: **no trail, POI, elevation, or danger-zone
coordinate may be treated as real until it has a completed metadata record
below, with a real `source` and `source_date`.** Until then, it is mock or
placeholder data and must be labeled as such everywhere it appears (map
layers, fixtures, code comments).

## Dataset registry

Each dataset gets one entry, using this schema:

```json
{
  "dataset_name": "",
  "source": "",
  "source_date": "",
  "resolution_m": null,
  "horizontal_crs": "",
  "vertical_datum": "",
  "accuracy_m": null,
  "license": ""
}
```

### 1. Demo basemap style (frontend placeholder)

```json
{
  "dataset_name": "MapLibre demo vector style",
  "source": "https://demotiles.maplibre.org/style.json (MapLibre project demo tiles)",
  "source_date": "TODO — accessed 2026-09-15, upstream update date unknown",
  "resolution_m": null,
  "horizontal_crs": "EPSG:3857 (Web Mercator, as served)",
  "vertical_datum": "N/A (2D vector basemap, no elevation)",
  "accuracy_m": null,
  "license": "TODO — verify MapLibre demo tiles license before any non-dev use"
}
```
Status: **PLACEHOLDER — dev/demo only.** Not licensed for production; replace
before any public deployment (Milestone 10+ / DEPLOYMENT.md).

### 2. Khao Chalak map center (frontend `appConfig.ts`)

```json
{
  "dataset_name": "Khao Chalak approximate area center",
  "source": "DATA REQUIRED — currently an approximate point in the Bang Phra / Sri Racha area, Chonburi, inferred only from general web search of trail-running blog descriptions, not a surveyed coordinate",
  "source_date": "TODO",
  "resolution_m": null,
  "horizontal_crs": "EPSG:4326",
  "vertical_datum": "N/A",
  "accuracy_m": null,
  "license": "N/A"
}
```
Status: **PLACEHOLDER.** Used only to point the dev map camera at roughly the
right area. Must not be used for any trail, distance, or elevation
calculation. Replace with a verified trailhead/summit coordinate (GPS survey,
official park data, or verified community GPX) before Milestone 2.

### 3. Trail network (Khao Chalak)

```json
{
  "dataset_name": "TODO",
  "source": "DATA REQUIRED",
  "source_date": "DATA REQUIRED",
  "resolution_m": null,
  "horizontal_crs": "DATA REQUIRED",
  "vertical_datum": "DATA REQUIRED",
  "accuracy_m": null,
  "license": "DATA REQUIRED"
}
```
Status: **MOCK DATA ONLY — no real trail geometry exists in this repository
yet.** Milestone 2 (native Android) added
`android/app/src/main/assets/mock/trails.geojson` — two fabricated
`LineString` features (an arbitrary loop + spur) drawn near the Milestone
0/1 placeholder map center, used only to build and test the
GeoJsonSource/LineLayer trail-rendering pipeline. Every feature carries a
`"mock": true` property and a `source: "DATA REQUIRED..."` note, the file's
top-level `_comment` says it's fabricated, and the app additionally shows a
permanent on-screen "MOCK TRAIL DATA" banner whenever it's loaded (see
`README.md`'s Milestone 2 acceptance criteria) — labeled in the data, the
render style, and the UI, not just one of the three. This file must be
swapped for real data (candidates to evaluate: field GPS survey,
OpenStreetMap extract with manual verification, or a local trail-running
club's shared GPX) before any accuracy claim is made about it.

### 4. DEM (Digital Elevation Model)

```json
{
  "dataset_name": "TODO",
  "source": "DATA REQUIRED",
  "source_date": "DATA REQUIRED",
  "resolution_m": null,
  "horizontal_crs": "DATA REQUIRED",
  "vertical_datum": "DATA REQUIRED",
  "accuracy_m": null,
  "license": "DATA REQUIRED"
}
```
Status: **NOT YET COLLECTED.** Milestone 6 will use a synthetic/test DEM
first (clearly labeled as test data) to validate the processing pipeline,
before any real DEM (e.g. a free global source, evaluated for license and
resolution) is brought in.

### 5. POIs, checkpoints, danger zones, water points

Status: **NOT YET COLLECTED.** No entries exist. These must never be
fabricated — see Milestone 8/13 data models, which are ready to receive real
records but contain none yet.

## Rule reminder

If you are adding a new spatial dataset to this project, add its metadata
record here in the same commit/PR that adds the data. A dataset without a
completed record here is, by definition, not verified and must be treated
(and labeled) as mock/placeholder data throughout the codebase.
