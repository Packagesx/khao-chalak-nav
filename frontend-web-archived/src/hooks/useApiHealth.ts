import { useEffect, useState } from "react";
import { API_BASE_URL } from "../config/appConfig";

export type ApiHealthState =
  | { status: "loading" }
  | { status: "ok"; message: string }
  | { status: "error"; message: string };

/**
 * Polls the backend /health endpoint once on mount.
 * Used only to prove the frontend <-> backend wiring works for
 * Milestone 0 — real data fetching (trails, routes, etc.) is added
 * behind dedicated services in later milestones.
 */
export function useApiHealth(): ApiHealthState {
  const [state, setState] = useState<ApiHealthState>({ status: "loading" });

  useEffect(() => {
    let cancelled = false;

    fetch(`${API_BASE_URL}/health`)
      .then(async (res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        if (!cancelled) setState({ status: "ok", message: `${data.service} v${data.version}` });
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setState({
            status: "error",
            message: err instanceof Error ? err.message : "Unable to reach backend",
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return state;
}
