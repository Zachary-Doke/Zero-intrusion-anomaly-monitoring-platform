import { formatNumber } from "../lib/format";

export function MetricCard({ icon, label, value, note, accent = "primary" }) {
  return (
    <article className={`metric-card metric-card--${accent}`}>
      <div className="metric-card__head">
        <span className="label-overline">{label}</span>
        <span className="material-symbols-outlined">{icon}</span>
      </div>
      <div className="metric-card__value">{formatNumber(value)}</div>
      <p className="metric-card__note">{note}</p>
    </article>
  );
}
