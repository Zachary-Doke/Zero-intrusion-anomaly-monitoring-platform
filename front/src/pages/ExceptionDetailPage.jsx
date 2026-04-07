import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../lib/api";
import { formatDateTime, formatFullDateTime, formatJsonText, safeArray } from "../lib/format";
import { SeverityPill, StatusPill } from "../components/StatusPill";

const STATUS_OPTIONS = ["OPEN", "INVESTIGATING", "RESOLVED"];

function MetaRow({ label, value }) {
  return (
    <div className="meta-row">
      <span>{label}</span>
      <strong>{value || "--"}</strong>
    </div>
  );
}

export function ExceptionDetailPage() {
  const { id } = useParams();
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionLoading, setActionLoading] = useState(false);

  async function loadDetail() {
    setLoading(true);
    setError("");
    try {
      const response = await api.getExceptionDetail(id);
      setDetail(response);
    } catch (loadError) {
      setError(loadError.message || "加载异常详情失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadDetail();
  }, [id]);

  async function updateStatus(status) {
    setActionLoading(true);
    try {
      const response = await api.updateExceptionStatus(id, status);
      setDetail(response);
    } catch (actionError) {
      setError(actionError.message || "更新状态失败");
    } finally {
      setActionLoading(false);
    }
  }

  async function generateSuggestion() {
    setActionLoading(true);
    try {
      const suggestion = await api.generateSuggestion(id);
      setDetail((current) => ({
        ...current,
        suggestion
      }));
    } catch (actionError) {
      setError(actionError.message || "生成建议失败");
    } finally {
      setActionLoading(false);
    }
  }

  return (
    <div className="page-stack">
      <header className="page-header page-header--detail">
        <div>
          <Link to="/exceptions" className="back-link">
            <span className="material-symbols-outlined">arrow_back</span>
            返回异常列表
          </Link>
          <span className="label-overline">Exception Detail</span>
          <h2>{detail?.summary || detail?.exceptionClass || "异常详情"}</h2>
          <p>{detail?.topStackFrame || "查看完整上下文、根因分析和处理建议"}</p>
        </div>
        <div className="action-row">
          <SeverityPill value={detail?.severity} />
          <StatusPill value={detail?.status} />
        </div>
      </header>

      {error ? <div className="error-banner">{error}</div> : null}
      {loading ? <div className="loading-state">正在加载异常详情...</div> : null}

      {detail ? (
        <div className="detail-grid">
          <div className="detail-main">
            <section className="suggestion-card">
              <div className="panel-head">
                <div>
                  <span className="label-overline">Root Cause</span>
                  <h3>异常原因分析</h3>
                </div>
                <span className="panel-footnote">状态：{detail.suggestion?.suggestionStatus || "READY"}</span>
              </div>
              <p className="analysis-copy">{detail.suggestion?.rootCauseAnalysis || "暂无根因分析。"}</p>
              {detail.suggestion?.impactScope ? (
                <div className="surface-subpanel">
                  <span className="label-overline">Impact Scope</span>
                  <p>{detail.suggestion.impactScope}</p>
                </div>
              ) : null}
            </section>

            <section className="surface-panel">
              <div className="panel-head">
                <div>
                  <span className="label-overline">Fix Suggestion</span>
                  <h3>处理建议</h3>
                </div>
                <button
                  type="button"
                  className="button button--primary"
                  onClick={generateSuggestion}
                  disabled={actionLoading}
                >
                  <span className="material-symbols-outlined">auto_awesome</span>
                  {detail.suggestion?.fixSuggestion ? "重新生成建议" : "生成处理建议"}
                </button>
              </div>

              <p className="analysis-copy">
                {detail.suggestion?.fixSuggestion || "当前只展示默认根因分析。点击右上角按钮生成可执行处理建议。"}
              </p>

              {safeArray(detail.suggestion?.troubleshootingSteps).length ? (
                <ul className="bullet-list">
                  {detail.suggestion.troubleshootingSteps.map((item) => (
                    <li key={item}>{item}</li>
                  ))}
                </ul>
              ) : null}
            </section>

            <section className="surface-panel">
              <div className="panel-head">
                <div>
                  <span className="label-overline">Stack Trace</span>
                  <h3>堆栈与消息</h3>
                </div>
              </div>
              <div className="stack-console">
                <pre>{detail.stackTrace || detail.message || "--"}</pre>
              </div>
            </section>

            <section className="surface-panel">
              <div className="panel-head">
                <div>
                  <span className="label-overline">Context</span>
                  <h3>上下文快照</h3>
                </div>
              </div>
              <div className="context-grid">
                <div className="surface-subpanel">
                  <span className="label-overline">Arguments Snapshot</span>
                  <pre className="json-block">{formatJsonText(detail.argumentsSnapshot)}</pre>
                </div>
                <div className="surface-subpanel">
                  <span className="label-overline">This Snapshot</span>
                  <pre className="json-block">{formatJsonText(detail.thisSnapshot)}</pre>
                </div>
                <div className="surface-subpanel surface-subpanel--wide">
                  <span className="label-overline">Trace Context</span>
                  <pre className="json-block">{formatJsonText(detail.traceContext)}</pre>
                </div>
              </div>
            </section>
          </div>

          <aside className="detail-side">
            <section className="surface-panel">
              <div className="panel-head">
                <div>
                  <span className="label-overline">Metadata</span>
                  <h3>事件元数据</h3>
                </div>
              </div>
              <div className="meta-list">
                <MetaRow label="事件 ID" value={detail.id} />
                <MetaRow label="指纹" value={detail.fingerprint} />
                <MetaRow label="应用" value={detail.appName} />
                <MetaRow label="服务" value={detail.serviceName} />
                <MetaRow label="环境" value={detail.environment} />
                <MetaRow label="线程" value={detail.threadName} />
                <MetaRow label="类名" value={detail.className} />
                <MetaRow label="方法" value={detail.methodName} />
                <MetaRow label="Trace ID" value={detail.traceId} />
                <MetaRow label="发生时间" value={formatFullDateTime(detail.occurrenceTime)} />
              </div>
            </section>

            <section className="surface-panel">
              <div className="panel-head">
                <div>
                  <span className="label-overline">Workflow</span>
                  <h3>状态流转</h3>
                </div>
              </div>
              <div className="action-column">
                {STATUS_OPTIONS.map((status) => (
                  <button
                    type="button"
                    key={status}
                    className={`button ${detail.status === status ? "button--primary" : "button--ghost"}`}
                    disabled={actionLoading}
                    onClick={() => updateStatus(status)}
                  >
                    {status}
                  </button>
                ))}
              </div>
            </section>

            <section className="surface-panel">
              <div className="panel-head">
                <div>
                  <span className="label-overline">Agent Sync</span>
                  <h3>采集链路状态</h3>
                </div>
              </div>
              <div className="meta-list">
                <MetaRow label="Agent 版本" value={detail.agentVersion} />
                <MetaRow label="配置版本" value={detail.configVersion} />
                <MetaRow label="同步状态" value={detail.lastConfigSyncStatus} />
                <MetaRow label="同步时间" value={formatDateTime(detail.lastConfigSyncAt)} />
                <MetaRow label="队列深度" value={detail.queueSize} />
                <MetaRow label="丢弃数量" value={detail.droppedCount} />
              </div>
              {detail.lastConfigSyncError ? (
                <div className="surface-subpanel">
                  <span className="label-overline">Last Error</span>
                  <p>{detail.lastConfigSyncError}</p>
                </div>
              ) : null}
            </section>
          </aside>
        </div>
      ) : null}
    </div>
  );
}
