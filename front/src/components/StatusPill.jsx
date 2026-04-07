import { severityVariant, statusLabel, statusVariant } from "../lib/format";

export function SeverityPill({ value }) {
  return <span className={`pill ${severityVariant(value)}`}>{value || "--"}</span>;
}

export function StatusPill({ value }) {
  return <span className={`pill ${statusVariant(value)}`}>{statusLabel(value)}</span>;
}
