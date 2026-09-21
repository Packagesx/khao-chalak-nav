import { MapView } from "./components/MapView";
import { useApiHealth } from "./hooks/useApiHealth";
import "./App.css";

function ApiStatusBadge() {
  const health = useApiHealth();

  if (health.status === "loading") {
    return <span className="badge badge-loading">API: checking…</span>;
  }
  if (health.status === "ok") {
    return <span className="badge badge-ok">API: online</span>;
  }
  return <span className="badge badge-error" title={health.message}>API: offline</span>;
}

export default function App() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <span className="app-title">KHAO CHALAK</span>
        <ApiStatusBadge />
      </header>

      <main className="app-main">
        <MapView />
      </main>

      <div className="stat-bar">
        <div className="stat">
          <span className="stat-label">Distance</span>
          <span className="stat-value">—</span>
        </div>
        <div className="stat">
          <span className="stat-label">Elevation</span>
          <span className="stat-value">—</span>
        </div>
        <div className="stat">
          <span className="stat-label">Slope</span>
          <span className="stat-value">—</span>
        </div>
        <div className="stat">
          <span className="stat-label">Battery</span>
          <span className="stat-value">—</span>
        </div>
      </div>

      <nav className="bottom-nav">
        <button type="button" disabled title="Milestone 8">Routes</button>
        <button type="button" disabled title="Milestone 3">Record</button>
        <button type="button" disabled title="Milestone 11">Offline</button>
        <button type="button" disabled title="Later milestone">Profile</button>
      </nav>
    </div>
  );
}
