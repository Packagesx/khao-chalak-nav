/**
 * App-wide configuration constants.
 *
 * DATA ACCURACY NOTE (see /GIS_DATA.md and the project master prompt §5):
 * KHAO_CHALAK_MAP_CENTER below is an *unverified, approximate* location used
 * only to point the demo basemap at the right part of Chonburi during local
 * development. It is NOT a surveyed trailhead, summit, or POI coordinate and
 * MUST NOT be used for routing, distance, elevation, or any spatial analysis.
 * Replace with a verified coordinate (with source + accuracy_m recorded in
 * GIS_DATA.md) before any real trail/POI work begins.
 */
export const KHAO_CHALAK_MAP_CENTER = {
  // PLACEHOLDER — approximate Bang Phra / Sri Racha area, Chonburi.
  // Source: general web search of trail-running blogs describing Khao
  // Chalak's location (see project delivery notes) — NOT a surveyed point.
  lng: 100.98,
  lat: 13.15,
  zoom: 12,
} as const;

// Free, no-API-key vector style used only as an MVP placeholder basemap.
// Replace with a proper self-hosted / licensed style + terrain source in
// later milestones (see ROADMAP.md, Milestone 1 and Milestone 10).
export const PLACEHOLDER_MAP_STYLE_URL = "https://demotiles.maplibre.org/style.json";

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8000";
