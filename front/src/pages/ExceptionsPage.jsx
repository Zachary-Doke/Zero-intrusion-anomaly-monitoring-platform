import { useDeferredValue, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";
import { formatDateTime, formatNumber, severityLabel, statusLabel } from "../lib/format";
import { SeverityPill, StatusPill } from "../components/StatusPill";

const DEFAULT_PAGE_SIZE = 12;

const DEFAULT_FILTERS = {
  severity: "",
  status: "",
  serviceName: "",
  keyword: "",
  days: "7"
};

const SEVERITY_OPTIONS = [
  { value: "", label: "全部" },
  { value: "CRITICAL", label: severityLabel("CRITICAL") },
  { value: "HIGH", label: severityLabel("HIGH") },
  { value: "MEDIUM", label: severityLabel("MEDIUM") }
];

const STATUS_OPTIONS = [
  { value: "", label: "全部" },
  { value: "OPEN", label: statusLabel("OPEN") },
  { value: "INVESTIGATING", label: statusLabel("INVESTIGATING") },
  { value: "RESOLVED", label: statusLabel("RESOLVED") }
];

const DAYS_OPTIONS = [
  { value: "1", label: "最近 1 天" },
  { value: "7", label: "最近 7 天" },
  { value: "30", label: "最近 30 天" }
];

function FilterSelect({ label, value, options, onChange }) {
  const [open, setOpen] = useState(false);
  const wrapRef = useRef(null);
  const selected = options.find((option) => option.value === value) || options[0];

  useEffect(() => {
    if (!open) {
      return undefined;
    }

    function handleOutsideClick(event) {
      if (wrapRef.current && !wrapRef.current.contains(event.target)) {
        setOpen(false);
      }
    }

    function handleEscape(event) {
      if (event.key === "Escape") {
        setOpen(false);
      }
    }

    document.addEventListener("mousedown", handleOutsideClick);
    document.addEventListener("keydown", handleEscape);

    return () => {
      document.removeEventListener("mousedown", handleOutsideClick);
      document.removeEventListener("keydown", handleEscape);
    };
  }, [open]);

  return (
    <div className="field field--compact field--select" ref={wrapRef}>
      <span>{label}</span>
      <button
        type="button"
        className={`select-trigger ${open ? "select-trigger--open" : ""}`}
        onClick={() => setOpen((current) => !current)}
        aria-haspopup="listbox"
        aria-expanded={open}
      >
        <span>{selected.label}</span>
        <span className="select-trigger__caret">▾</span>
      </button>
      {open ? (
        <div className="select-popover" role="listbox" aria-label={label}>
          {options.map((option) => (
            <button
              key={option.value || "all"}
              type="button"
              className={`select-option ${value === option.value ? "select-option--active" : ""}`}
              onClick={() => {
                onChange(option.value);
                setOpen(false);
              }}
            >
              {option.label}
            </button>
          ))}
        </div>
      ) : null}
    </div>
  );
}

export function ExceptionsPage() {
  const [filters, setFilters] = useState(DEFAULT_FILTERS);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [pageSizeInput, setPageSizeInput] = useState(String(DEFAULT_PAGE_SIZE));
  const [jumpPage, setJumpPage] = useState("1");
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
          size: pageSize
        });
        setPayload(response);
      } catch (loadError) {
        setError(loadError.message || "加载异常列表失败");
      } finally {
        setLoading(false);
      }
    }

    loadExceptions();
  }, [filters.severity, filters.status, filters.serviceName, filters.days, deferredKeyword, page, pageSize]);

  function updateFilter(name, value) {
    setPage(0);
    setFilters((current) => ({
      ...current,
      [name]: value
    }));
  }

  useEffect(() => {
    setJumpPage(String(page + 1));
  }, [page]);

  function handleJumpToPage(event) {
    event.preventDefault();
    const totalPages = Math.max(payload?.totalPages || 1, 1);
    const parsed = Number.parseInt(jumpPage, 10);
    const targetPage = Number.isFinite(parsed) ? Math.min(Math.max(parsed, 1), totalPages) : page + 1;

    setJumpPage(String(targetPage));
    setPage(targetPage - 1);
  }

  useEffect(() => {
    setPageSizeInput(String(pageSize));
  }, [pageSize]);

  function applyPageSize(rawValue) {
    const parsed = Number.parseInt(rawValue, 10);
    if (!Number.isFinite(parsed)) {
      setPageSizeInput(String(pageSize));
      return;
    }

    const nextSize = Math.min(Math.max(parsed, 1), 200);
    setPageSizeInput(String(nextSize));

    if (nextSize !== pageSize) {
      setPage(0);
      setPageSize(nextSize);
    }
  }

  const content = payload?.content ?? [];
  const openCount = content.filter((item) => item.status === "OPEN").length;
  const criticalCount = content.filter((item) => item.severity === "CRITICAL").length;
  const serviceCount = new Set(content.map((item) => item.serviceName).filter(Boolean)).size;

  return (
    <div className="page-stack">
      <header className="page-header">
        <div>
          <span className="label-overline">异常流</span>
          <h2>异常列表</h2>
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
          <p className="metric-card__note">当前页未处理数量</p>
        </article>
        <article className="metric-card metric-card--critical">
          <span className="label-overline">严重级别</span>
          <div className="metric-card__value">{formatNumber(criticalCount)}</div>
          <p className="metric-card__note">当前页严重异常数量</p>
        </article>
        <article className="metric-card metric-card--secondary">
          <span className="label-overline">服务分布</span>
          <div className="metric-card__value">{formatNumber(serviceCount)}</div>
          <p className="metric-card__note">当前页涉及服务</p>
        </article>
      </section>

      <section className="surface-panel filters-panel">
        <div className="filters-panel__head">
          <span className="label-overline">筛选器</span>
          <p>多条件组合筛选异常事件</p>
        </div>
        <div className="filters-bar">
          <FilterSelect
            label="严重级别"
            value={filters.severity}
            options={SEVERITY_OPTIONS}
            onChange={(value) => updateFilter("severity", value)}
          />
          <FilterSelect
            label="状态"
            value={filters.status}
            options={STATUS_OPTIONS}
            onChange={(value) => updateFilter("status", value)}
          />
          <label className="field field--compact">
            <span>服务名</span>
            <input
              value={filters.serviceName}
              onChange={(event) => updateFilter("serviceName", event.target.value)}
              placeholder="请输入服务名"
            />
          </label>
          <FilterSelect
            label="时间范围"
            value={filters.days}
            options={DAYS_OPTIONS}
            onChange={(value) => updateFilter("days", value)}
          />
          <label className="field field--compact">
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
            <span className="label-overline">实时列表</span>
            <h3>异常事件矩阵</h3>
          </div>
          <span className="panel-footnote">
            {loading ? "加载中..." : `共 ${formatNumber(payload?.totalElements ?? 0)} 条，第 ${page + 1} / ${payload?.totalPages || 1} 页`}
          </span>
        </div>

        <div className="table-wrap">
          <table className="data-table">
            <colgroup>
              <col className="data-col data-col--severity" />
              <col className="data-col data-col--time" />
              <col className="data-col data-col--status" />
              <col className="data-col data-col--action" />
            </colgroup>
            <thead>
              <tr>
                <th>严重程度</th>
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
                    <td className="cell-time">{formatDateTime(item.occurrenceTime)}</td>
                    <td className="cell-status">
                      <StatusPill value={item.status} />
                    </td>
                    <td className="cell-action">
                      <Link to={`/exceptions/${item.id}`} className="table-link">
                        查看详情
                      </Link>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="4">
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
          <div className="pagination__center">
            <span className="pagination__meta">
              共 {formatNumber(payload?.totalElements ?? 0)} 条，第 {page + 1} / {payload?.totalPages || 1} 页
            </span>
            <form
              className="pagination-size"
              onSubmit={(event) => {
                event.preventDefault();
                applyPageSize(pageSizeInput);
              }}
            >
              <label className="pagination-size__label" htmlFor="page-size-input">每页显示</label>
              <input
                id="page-size-input"
                inputMode="numeric"
                pattern="[0-9]*"
                value={pageSizeInput}
                onChange={(event) => setPageSizeInput(event.target.value.replace(/\D/g, ""))}
                onBlur={() => applyPageSize(pageSizeInput)}
                placeholder="条数"
              />
              <span className="pagination-size__unit">条</span>
              <button type="submit" className="button button--ghost" disabled={loading || !payload}>
                应用
              </button>
            </form>
            <form className="pagination-jump" onSubmit={handleJumpToPage}>
              <label htmlFor="jump-to-page">跳转到</label>
              <input
                id="jump-to-page"
                inputMode="numeric"
                pattern="[0-9]*"
                value={jumpPage}
                onChange={(event) => setJumpPage(event.target.value.replace(/\D/g, ""))}
                placeholder="页码"
              />
              <button type="submit" className="button button--ghost" disabled={loading || !payload}>
                跳转
              </button>
            </form>
          </div>
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
