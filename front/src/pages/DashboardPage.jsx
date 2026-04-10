import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";
import { chineseOnlyList, chineseOnlyText, formatDateTime, formatNumber } from "../lib/format";
import { FingerprintList } from "../components/FingerprintList";
import { MetricCard } from "../components/MetricCard";
import { SeverityPill, StatusPill } from "../components/StatusPill";
import { TrendPanel } from "../components/TrendPanel";

export function DashboardPage() {
  const [overview, setOverview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function loadOverview() {
    setLoading(true);
    setError("");
    try {
      const response = await api.getOverview();
      setOverview(response);
    } catch (loadError) {
      setError(loadError.message || "加载总览失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadOverview();
  }, []);

  const metrics = overview?.metrics;
  const riskSummary = overview?.riskSummary;
  const riskHighlights = chineseOnlyList(riskSummary?.highlights, "暂无可用的风险摘要，请稍后刷新。");
  const riskSourceLabel = (riskSummary?.source || "").toUpperCase() === "AI" ? "智能生成" : "规则生成";

  return (
    <div className="page-stack">
      <header className="page-header">
        <div>
          <span className="label-overline">总览仪表盘</span>
          <h2>异常概览</h2>
        </div>
        <button type="button" className="button button--primary" onClick={loadOverview} disabled={loading}>
          <span className="material-symbols-outlined">refresh</span>
          {loading ? "刷新中..." : "立即刷新"}
        </button>
      </header>

      {error ? <div className="error-banner">{error}</div> : null}

      <section className="metric-grid">
        <MetricCard
          icon="error"
          label="总异常量"
          value={metrics?.totalExceptions ?? 0}
          note={`${formatNumber(metrics?.fingerprintCount ?? 0)} 类指纹`}
        />
        <MetricCard
          icon="inventory_2"
          label="待处理异常"
          value={metrics?.openExceptionCount ?? 0}
          note="仍处于未处理状态"
          accent="warning"
        />
        <MetricCard
          icon="priority_high"
          label="严重异常"
          value={metrics?.criticalExceptionCount ?? 0}
          note="严重且未处理"
          accent="critical"
        />
        <MetricCard
          icon="hub"
          label="生效采集端"
          value={metrics?.effectiveAgentCount ?? 0}
          note={`${formatNumber(metrics?.serviceCount ?? 0)} 个服务参与采集`}
          accent="secondary"
        />
      </section>

      <section className="dashboard-grid">
        <div className="dashboard-main">
          <TrendPanel trends={overview?.trends ?? []} />

          <section className="surface-panel">
            <div className="panel-head">
              <div>
                <h3>最近异常</h3>
              </div>
              <Link to="/exceptions" className="panel-link">
                查看全部
              </Link>
            </div>

            <div className="event-list">
              {(overview?.recentEvents ?? []).length ? (
                overview.recentEvents.map((item) => (
                  <Link key={item.id} to={`/exceptions/${item.id}`} className="event-card">
                    <div className="event-card__top">
                      <SeverityPill value={item.severity} />
                      <StatusPill value={item.status} />
                    </div>
                    <strong>异常事件</strong>
                    <p>已过滤技术字段</p>
                    <div className="event-card__meta">
                      <span>已过滤服务字段</span>
                      <span>{formatDateTime(item.occurrenceTime)}</span>
                    </div>
                  </Link>
                ))
              ) : (
                <div className="empty-state">暂无异常事件</div>
              )}
            </div>
          </section>
        </div>

        <div className="dashboard-side">
          <FingerprintList items={overview?.topFingerprints ?? []} />

          <section className="surface-panel surface-panel--accent">
            <div className="panel-head">
              <div>
                <h3>本日风险摘要</h3>
              </div>
            </div>
            <div className="event-card__top">
              <SeverityPill value={riskSummary?.riskLevel || "MEDIUM"} />
              <span className="panel-footnote">
                {riskSourceLabel} · {formatDateTime(riskSummary?.updatedAt)}
              </span>
            </div>
            <p className="section-copy">{chineseOnlyText(riskSummary?.summary, "暂无可用的风险摘要，请稍后刷新。")}</p>
            <ul className="bullet-list">
              {riskHighlights.map((item, index) => (
                <li key={`${index}-${item}`}>{item}</li>
              ))}
            </ul>
          </section>
        </div>
      </section>
    </div>
  );
}
