import { useEffect, useRef, useState } from "react";
import { Map as MapLibreMap, NavigationControl, GeolocateControl, type ErrorEvent } from "maplibre-gl";
import "maplibre-gl/dist/maplibre-gl.css";
import { KHAO_CHALAK_MAP_CENTER, PLACEHOLDER_MAP_STYLE_URL } from "../config/appConfig";

/**
 * Milestone 0/1 placeholder map.
 *
 * Renders a basic 2D MapLibre GL map so the toolchain (MapLibre + Vite +
 * React + TS) is proven end-to-end. No trail, POI, or terrain data is
 * loaded here yet — that starts at Milestone 2 (Trail Data MVP) and
 * Milestone 6 (DEM pipeline), using real or clearly-labeled mock data only.
 */
export function MapView() {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<InstanceType<typeof MapLibreMap> | null>(null);
  const [mapError, setMapError] = useState<string | null>(null);

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;

    try {
      const map = new MapLibreMap({
        container: containerRef.current,
        style: PLACEHOLDER_MAP_STYLE_URL,
        center: [KHAO_CHALAK_MAP_CENTER.lng, KHAO_CHALAK_MAP_CENTER.lat],
        zoom: KHAO_CHALAK_MAP_CENTER.zoom,
      });

      map.addControl(new NavigationControl({ visualizePitch: true }), "top-right");
      map.addControl(new GeolocateControl({ trackUserLocation: true }), "top-right");

      map.on("error", (e: ErrorEvent) => {
        // Surfaces basemap/network failures instead of failing silently —
        // required "error state handled" per the Definition of Done.
        setMapError(e.error?.message ?? "Unknown map error");
      });

      mapRef.current = map;
    } catch (err) {
      setMapError(err instanceof Error ? err.message : "Failed to initialize map");
    }

    return () => {
      mapRef.current?.remove();
      mapRef.current = null;
    };
  }, []);

  return (
    <div className="map-shell">
      <div ref={containerRef} className="map-container" role="application" aria-label="Khao Chalak map" />
      {mapError && (
        <div className="map-error-banner" role="alert">
          Map failed to load: {mapError}
        </div>
      )}
    </div>
  );
}
