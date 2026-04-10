import { severityLabel, severityVariant, statusLabel, statusVariant } from "../lib/format";

export function SeverityPill({ value }) {
  return <span className={`pill ${severityVariant(value)}`}>{severityLabel(value)}</span>;
}

export function StatusPill({ value }) {
  return <span className={`pill ${statusVariant(value)}`}>{statusLabel(value)}</span>;
}
