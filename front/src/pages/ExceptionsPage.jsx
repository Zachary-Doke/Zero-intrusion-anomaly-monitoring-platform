import { useDeferredValue, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";
import { formatDateTime, formatNumber } from "../lib/format";
import { SeverityPill, StatusPill } from "../components/StatusPill";

const PAGE_SIZE = 12;

const DEFAULT_FILTERS = {
  severity: "",
  status: "",
  serviceName: "",
  keyword: "",
  days: "7"
};

export function ExceptionsPage() {
  const [filters, setFilters] = useState(DEFAULT_FILTERS);
  const [page, setPage] = useState(0);
  const [payload, setPayload] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const deferredKeyword = useDeferredValue(filters.keyword);

  useEffect(() => {
    async function loadExceptions() {
      setLoading(true);
      setError("");
      try {
        const response = await api.getExceptions({
          severity: filters.severity,
          status: filters.status,
          serviceName: filters.serviceName,
          keyword: deferredKeyword,
          days: filters.days,
          page,
          size: PAGE_SIZE
        });
        setPayload(response);
      } catch (loadError) {
        setError(loadError.message || "加载异常列表失败");
      } finally {
        setLoading(false);
      }
    }

    loadExceptions();
  }, [filters.severity, filters.status, filters.serviceName, filters.days, deferredKeyword, page]);

  function updateFilter(name, value) {
    setPage(0);
    setFilters((current) => ({
      ...current,
      [name]: value
    }));
  }

  const content = payload?.content ?? [];
  const openCount = content.filter((item) => item.status === "OPEN").length;
  const criticalCount = content.filter((item) => item.severity === "CRITICAL").length;
  const serviceCount = new Set(content.map((item) => item.serviceName).filter(Boolean)).size;

  return (
    <div className="page-stack">
      <header className="page-header">
        <div>
          <span className="label-overline">Exception Stream</span>
          <h2>异常列表</h2>
          <p>按严重级别、状态、服务名和关键词筛选。详情页单独承载根因分析和修复建议。</p>
        </div>
      </header>

      <section className="metric-grid metric-grid--compact">
        <article className="metric-card">
          <span className="label-overline">匹配结果</span>
          <div className="metric-card__value">{formatNumber(payload?.totalElements ?? 0)}</div>
          <p className="metric-card__note">当前查询命中总数</p>
        </article>
        <article className="metric-card metric-card--warning">
          <span className="label-overline">未处理</span>
          <div className="metric-card__value">{formatNumber(openCount)}</div>
          <p className="metric-card__note">当前页 OPEN 数量</p>
        </article>
        <article className="metric-card metric-card--critical">
          <span className="label-overline">严重级别</span>
          <div className="metric-card__value">{formatNumber(criticalCount)}</div>
          <p className="metric-card__note">当前页 CRITICAL 数量</p>
        </article>
        <article className="metric-card metric-card--secondary">
          <span className="label-overline">服务分布</span>
          <div className="metric-card__value">{formatNumber(serviceCount)}</div>
          <p className="metric-card__note">当前页涉及服务</p>
        </article>
      </section>

      <section className="surface-panel filters-panel">
        <div className="filters-bar">
          <label className="field field--compact">
            <span>严重级别</span>
            <select value={filters.severity} onChange={(event) => updateFilter("severity", event.target.value)}>
              <option value="">全部</option>
              <option value="CRITICAL">CRITICAL</option>
              <option value="HIGH">HIGH</option>
              <option value="MEDIUM">MEDIUM</option>
            </select>
          </label>
          <label className="field field--compact">
            <span>状态</span>
            <select value={filters.status} onChange={(event) => updateFilter("status", event.target.value)}>
              <option value="">全部</option>
              <option value="OPEN">OPEN</option>
              <option value="INVESTIGATING">INVESTIGATING</option>
              <option value="RESOLVED">RESOLVED</option>
            </select>
          </label>
          <label className="field field--compact">
            <span>服务名</span>
            <input
              value={filters.serviceName}
              onChange={(event) => updateFilter("serviceName", event.target.value)}
              placeholder="payment-gateway"
            />
          </label>
          <label className="field field--compact">
            <span>时间范围</span>
            <select value={filters.days} onChange={(event) => updateFilter("days", event.target.value)}>
              <option value="1">最近 1 天</option>
              <option value="7">最近 7 天</option>
              <option value="30">最近 30 天</option>
            </select>
          </label>
          <label className="field field--compact field--grow">
            <span>关键词</span>
            <input
              value={filters.keyword}
              onChange={(event) => updateFilter("keyword", event.target.value)}
              placeholder="异常摘要、指纹或类名"
            />
          </label>
          <button type="button" className="button button--ghost" onClick={() => {
            setFilters(DEFAULT_FILTERS);
            setPage(0);
          }}>
            重置筛选
          </button>
        </div>
      </section>

      {error ? <div className="error-banner">{error}</div> : null}

      <section className="surface-panel table-panel">
        <div className="panel-head">
          <div>
            <span className="label-overline">Live Grid</span>
            <h3>异常事件矩阵</h3>
          </div>
          <span className="panel-footnote">{loading ? "加载中..." : `第 ${page + 1} / ${payload?.totalPages || 1} 页`}</span>
        </div>

        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>严重程度</th>
                <th>摘要</th>
                <th>服务 / 方法</th>
                <th>发生时间</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {content.length ? (
                content.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <SeverityPill value={item.severity} />
                    </td>
                    <td>
                      <strong>{item.summary || item.exceptionClass}</strong>
                      <p>{item.traceId || item.fingerprint}</p>
                    </td>
                    <td>
                      <strong>{item.serviceName || item.appName}</strong>
                      <p>{item.topStackFrame || item.methodName}</p>
                    </td>
                    <td>{formatDateTime(item.occurrenceTime)}</td>
                    <td>
                      <StatusPill value={item.status} />
                    </td>
                    <td>
                      <Link to={`/exceptions/${item.id}`} className="table-link">
                        查看详情
                      </Link>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="6">
                    <div className="empty-state">{loading ? "正在加载..." : "当前筛选条件下暂无异常"}</div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="pagination">
          <button type="button" className="button button--ghost" disabled={!payload || payload.first} onClick={() => setPage((current) => Math.max(current - 1, 0))}>
            上一页
          </button>
          <span>
            共 {formatNumber(payload?.totalElements ?? 0)} 条，当前第 {page + 1} 页
          </span>
          <button
            type="button"
            className="button button--ghost"
            disabled={!payload || payload.last}
            onClick={() => setPage((current) => current + 1)}
          >
            下一页
          </button>
        </div>
      </section>
    </div>
  );
}
