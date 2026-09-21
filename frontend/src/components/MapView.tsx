import { useEffect, useRef, useState } from "react";
import {
  Map as MapLibreMap,
  NavigationControl,
  GeolocateControl,
  type ErrorEvent,
  type GeolocateErrorEvent,
} from "maplibre-gl";
import "maplibre-gl/dist/maplibre-gl.css";
import { KHAO_CHALAK_MAP_CENTER, PLACEHOLDER_MAP_STYLE_URL } from "../config/appConfig";

/**
 * Milestone 1 — Map MVP.
 *
 * A mobile-responsive 2D MapLibre GL map: pan (drag), zoom (buttons +
 * pinch/scroll), a compass control (also resets bearing to north), and a
 * "current location" control backed by the browser Geolocation API.
 *
 * Kept strictly 2D on purpose — `maxPitch: 0` disables camera tilt so this
 * milestone doesn't reach ahead into Milestone 10 (3D terrain). No trail,
 * POI, or terrain data is loaded here yet — that starts at Milestone 2
 * (Trail Data MVP) and Milestone 6 (DEM pipeline), using real or
 * clearly-labeled mock data only.
 */

// Human-readable text for the standard W3C Geolocation API error codes
// (GeolocationPositionError.code: 1 = PERMISSION_DENIED, 2 = POSITION_UNAVAILABLE,
// 3 = TIMEOUT), so "current location" failures are explained instead of just
// leaving the control in a silent error state.
function describeGeolocateError(code: number, fallback: string): string {
  switch (code) {
    case 1:
      return "Location permission denied. Allow location access in your browser settings to use “current location.”";
    case 2:
      return "Your position is currently unavailable. Check your device's GPS/location service and try again.";
    case 3:
      return "Locating your position timed out. Try again in an area with a clearer GPS/network signal.";
    default:
      return fallback || "Could not get your current location.";
  }
}

export function MapView() {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<InstanceType<typeof MapLibreMap> | null>(null);
  const [mapError, setMapError] = useState<string | null>(null);
  const [locateError, setLocateError] = useState<string | null>(null);

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;

    try {
      const map = new MapLibreMap({
        container: containerRef.current,
        style: PLACEHOLDER_MAP_STYLE_URL,
        center: [KHAO_CHALAK_MAP_CENTER.lng, KHAO_CHALAK_MAP_CENTER.lat],
        zoom: KHAO_CHALAK_MAP_CENTER.zoom,
        // Milestone 1 is a 2D map by definition — 3D terrain/tilt arrives in
        // Milestone 10. Locking pitch to 0 keeps touch-drag gestures
        // unambiguous (pan, never accidental tilt) on mobile.
        maxPitch: 0,
        attributionControl: { compact: true },
      });

      // Zoom buttons + compass (bearing reset). visualizePitch is off since
      // pitch is disabled above — the compass here only ever shows bearing.
      map.addControl(
        new NavigationControl({ showZoom: true, showCompass: true, visualizePitch: false }),
        "top-right",
      );

      const geolocate = new GeolocateControl({
        positionOptions: { enableHighAccuracy: true, timeout: 10_000 },
        trackUserLocation: true,
        showAccuracyCircle: true,
        showUserLocation: true,
        fitBoundsOptions: { maxZoom: 16 },
      });
      map.addControl(geolocate, "top-right");

      geolocate.on("error", (e: GeolocateErrorEvent) => {
        // GeolocateControl already reflects failure in its own button state
        // (spinner -> error icon), but it doesn't explain *why* — surface a
        // human-readable reason instead of a silent dead end.
        setLocateError(describeGeolocateError(e.code, e.message));
      });
      geolocate.on("geolocate", () => setLocateError(null));
      geolocate.on("trackuserlocationstart", () => setLocateError(null));

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
        <div className="map-banner map-banner-error" role="alert">
          Map failed to load: {mapError}
        </div>
      )}
      {!mapError && locateError && (
        <div className="map-banner map-banner-warning" role="alert">
          {locateError}
        </div>
      )}
    </div>
  );
}
