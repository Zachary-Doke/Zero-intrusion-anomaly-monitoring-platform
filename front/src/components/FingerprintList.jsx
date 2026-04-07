import { formatDateTime, formatNumber } from "../lib/format";
import { SeverityPill } from "./StatusPill";

export function FingerprintList({ items }) {
  return (
    <section className="surface-panel">
      <div className="panel-head">
        <div>
          <span className="label-overline">Top 指纹</span>
          <h3>高频异常簇</h3>
        </div>
      </div>

      <div className="fingerprint-list">
        {items.length ? (
          items.map((item) => (
            <article key={item.fingerprint} className="fingerprint-row">
              <div className="fingerprint-row__main">
                <SeverityPill value={item.severity} />
                <strong>{item.summary || item.exceptionClass}</strong>
                <span>{item.serviceName || "未知服务"}</span>
              </div>
              <div className="fingerprint-row__meta">
                <strong>{formatNumber(item.occurrenceCount)}</strong>
                <span>{formatDateTime(item.lastSeen)}</span>
              </div>
            </article>
          ))
        ) : (
          <div className="empty-state">暂无指纹聚合结果</div>
        )}
      </div>
    </section>
  );
}
