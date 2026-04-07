import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";
import { formatDateTime, formatNumber } from "../lib/format";
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

  return (
    <div className="page-stack">
      <header className="page-header">
        <div>
          <span className="label-overline">Overview</span>
          <h2>异常概览</h2>
          <p>沿用设计稿的编辑式留白和层级，但只保留真实可支撑的异常监控指标。</p>
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
          note="仍处于 OPEN 状态"
          accent="warning"
        />
        <MetricCard
          icon="priority_high"
          label="严重异常"
          value={metrics?.criticalExceptionCount ?? 0}
          note="Critical + Open"
          accent="critical"
        />
        <MetricCard
          icon="sync"
          label="生效 Agent"
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
                <span className="label-overline">Recent Events</span>
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
                    <strong>{item.summary || item.exceptionClass}</strong>
                    <p>{item.topStackFrame || item.methodName}</p>
                    <div className="event-card__meta">
                      <span>{item.serviceName || item.appName}</span>
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
                <span className="label-overline">Focus</span>
                <h3>当前观察重点</h3>
              </div>
            </div>
            <ul className="bullet-list">
              <li>优先处理 OPEN 状态的高频异常。</li>
              <li>配置页只保留采集规则与 AI 接口，降低答辩面。</li>
              <li>详情页内直接生成处理建议，不再跳独立报告流。</li>
            </ul>
          </section>
        </div>
      </section>
    </div>
  );
}
