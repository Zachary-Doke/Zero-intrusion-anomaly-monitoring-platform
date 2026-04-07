import { formatNumber } from "../lib/format";

function buildPath(trends) {
  if (!trends.length) {
    return "";
  }
  const max = Math.max(...trends.map((item) => item.count || 0), 1);
  return trends
    .map((item, index) => {
      const x = trends.length === 1 ? 50 : (index / (trends.length - 1)) * 100;
      const y = 88 - ((item.count || 0) / max) * 70;
      return `${index === 0 ? "M" : "L"} ${x.toFixed(2)} ${y.toFixed(2)}`;
    })
    .join(" ");
}

export function TrendPanel({ trends }) {
  const total = trends.reduce((sum, item) => sum + (item.count || 0), 0);
  const max = Math.max(...trends.map((item) => item.count || 0), 0);

  return (
    <section className="surface-panel trend-panel">
      <div className="panel-head">
        <div>
          <span className="label-overline">异常趋势</span>
          <h3>近 7 天波动</h3>
        </div>
        <div className="trend-summary">
          <strong>{formatNumber(total)}</strong>
          <span>累计事件</span>
        </div>
      </div>

      <div className="trend-chart">
        {trends.length ? (
          <svg viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
            <path d="M 0 88 L 100 88" />
            <path d="M 0 60 L 100 60" />
            <path d="M 0 32 L 100 32" />
            <path className="trend-line" d={buildPath(trends)} />
          </svg>
        ) : (
          <div className="empty-state">暂无趋势数据</div>
        )}
      </div>

      <div className="trend-footer">
        {trends.map((item) => (
          <div key={item.date}>
            <span>{item.date.slice(5)}</span>
            <strong>{formatNumber(item.count)}</strong>
          </div>
        ))}
      </div>
      <p className="panel-footnote">峰值：{formatNumber(max)} 次 / 日</p>
    </section>
  );
}
