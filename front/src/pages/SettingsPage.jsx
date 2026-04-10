import { useEffect, useState } from "react";
import { api } from "../lib/api";
import { formatDateTime } from "../lib/format";

function numericValue(value) {
  return value === "" ? "" : Number(value);
}

const RESTART_REQUIRED_FIELDS = [
  { key: "packagePatterns", label: "包名过滤器" },
  { key: "queueCapacity", label: "队列容量" }
];

function normalizeComparableValue(key, value) {
  if (key === "packagePatterns") {
    return String(value || "")
      .split(",")
      .map((item) => item.trim())
      .filter(Boolean)
      .join(",");
  }
  if (value === null || value === undefined || value === "") {
    return "";
  }
  if (typeof value === "number") {
    return Number(value);
  }
  if (typeof value === "boolean") {
    return value;
  }
  return String(value).trim();
}

function changedRestartFieldLabels(before, after) {
  return RESTART_REQUIRED_FIELDS
    .filter((item) => normalizeComparableValue(item.key, before?.[item.key]) !== normalizeComparableValue(item.key, after?.[item.key]))
    .map((item) => item.label);
}

export function SettingsPage() {
  const [settings, setSettings] = useState(null);
  const [statuses, setStatuses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  async function loadAll() {
    setLoading(true);
    setError("");
    try {
      const [settingsResponse, statusesResponse] = await Promise.all([
        api.getSettings(),
        api.getAgentSyncStatuses()
      ]);
      setSettings(settingsResponse);
      setStatuses(statusesResponse || []);
    } catch (loadError) {
      setError(loadError.message || "加载配置失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAll();
  }, []);

  function updateField(name, value) {
    setSettings((current) => ({
      ...current,
      [name]: value
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setMessage("");
    try {
      const saved = await api.saveSettings(settings);
      setSettings(saved);
      const changedRestartFields = changedRestartFieldLabels(settings, saved);
      if (changedRestartFields.length) {
        setMessage(
          `配置已保存。${changedRestartFields.join("、")}已变更，需重启挂载 Agent 的应用后生效；其余项将在下一次拉取时生效（默认30秒）。`
        );
      } else {
        setMessage("配置已保存，采集端将在下一次拉取时生效（默认30秒）。");
      }
      const latestStatuses = await api.getAgentSyncStatuses();
      setStatuses(latestStatuses || []);
    } catch (submitError) {
      setError(submitError.message || "保存配置失败");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="page-stack">
      <header className="page-header">
        <div>
          <span className="label-overline">配置中心</span>
          <h2>采集规则与智能分析接口</h2>
        </div>
      </header>

      {error ? <div className="error-banner">{error}</div> : null}
      {message ? <div className="success-banner">{message}</div> : null}
      {loading ? <div className="loading-state">正在加载配置...</div> : null}

      {settings ? (
        <form className="page-stack" onSubmit={handleSubmit}>
          <section className="settings-grid">
            <div>
              <span className="label-overline">采集规则</span>
              <h3>采集端捕获规则</h3>
              <p className="section-copy">远程配置为主，本地参数只保留接入兜底。这里控制包名、采样和快照深度。</p>
              <div className="sync-rule-panel">
                <p>
                  <strong>热更新项：</strong> 保存后由采集端自动拉取生效，默认约 30 秒内。
                </p>
                <p>
                  <strong>需重启项：</strong> 包名过滤器、队列容量。修改后请重启挂载 Agent 的应用。
                </p>
              </div>
            </div>
            <div className="surface-panel form-panel form-panel--rules">
              <label className="field">
                <span className="field-title-row">
                  <span>包名过滤器</span>
                  <span className="pill pill--restart">需重启应用</span>
                </span>
                <input
                  value={settings.packagePatterns || ""}
                  onChange={(event) => updateField("packagePatterns", event.target.value)}
                  placeholder="请输入包名前缀，多个用逗号分隔"
                />
              </label>

              <label className="toggle-row">
                <div>
                  <div className="field-title-row field-title-row--tight">
                    <strong>深度采样</strong>
                    <span className="pill pill--hot">热更新</span>
                  </div>
                  <span>开启后同步扩展 this 快照与更完整的上下文。</span>
                </div>
                <input
                  type="checkbox"
                  checked={Boolean(settings.deepSamplingEnabled)}
                  onChange={(event) => updateField("deepSamplingEnabled", event.target.checked)}
                />
              </label>

              <div className="form-grid settings-rules-grid">
                <label className="field">
                  <span className="field-title-row">
                    <span>快照深度限制</span>
                    <span className="pill pill--hot">热更新</span>
                  </span>
                  <input
                    type="number"
                    value={settings.depthLimit ?? ""}
                    onChange={(event) => updateField("depthLimit", numericValue(event.target.value))}
                  />
                </label>
                <label className="field">
                  <span className="field-title-row">
                    <span>文本长度限制</span>
                    <span className="pill pill--hot">热更新</span>
                  </span>
                  <input
                    type="number"
                    value={settings.lengthLimit ?? ""}
                    onChange={(event) => updateField("lengthLimit", numericValue(event.target.value))}
                  />
                </label>
                <label className="field">
                  <span className="field-title-row">
                    <span>集合数量限制</span>
                    <span className="pill pill--hot">热更新</span>
                  </span>
                  <input
                    type="number"
                    value={settings.collectionLimit ?? ""}
                    onChange={(event) => updateField("collectionLimit", numericValue(event.target.value))}
                  />
                </label>
                <label className="field">
                  <span className="field-title-row">
                    <span>默认采样率</span>
                    <span className="pill pill--hot">热更新</span>
                  </span>
                  <input
                    type="number"
                    step="0.1"
                    value={settings.defaultSampleRate ?? ""}
                    onChange={(event) => updateField("defaultSampleRate", numericValue(event.target.value))}
                  />
                </label>
                <label className="field">
                  <span className="field-title-row">
                    <span>队列容量</span>
                    <span className="pill pill--restart">需重启应用</span>
                  </span>
                  <input
                    type="number"
                    value={settings.queueCapacity ?? ""}
                    onChange={(event) => updateField("queueCapacity", numericValue(event.target.value))}
                  />
                </label>
                <label className="field">
                  <span className="field-title-row">
                    <span>刷新间隔（毫秒）</span>
                    <span className="pill pill--hot">热更新</span>
                  </span>
                  <input
                    type="number"
                    value={settings.flushIntervalMs ?? ""}
                    onChange={(event) => updateField("flushIntervalMs", numericValue(event.target.value))}
                  />
                </label>
              </div>
            </div>
          </section>

          <section className="settings-grid">
            <div>
              <span className="label-overline">智能分析</span>
              <h3>AI接口配置</h3>
              <p className="section-copy">在这里可统一配置 AI 接口地址、模型、接口密钥和提示词模板，异常详情页会直接按此配置生成处理建议。</p>
            </div>
            <div className="surface-panel form-panel">
              <div className="form-grid">
                <label className="field">
                  <span>接口地址</span>
                  <input
                    value={settings.aiBaseUrl || ""}
                    onChange={(event) => updateField("aiBaseUrl", event.target.value)}
                    placeholder="请输入接口地址"
                  />
                </label>
                <label className="field">
                  <span>模型名称</span>
                  <input
                    value={settings.aiModel || ""}
                    onChange={(event) => updateField("aiModel", event.target.value)}
                    placeholder="请输入模型名称"
                  />
                </label>
              </div>

              <label className="field">
                <span className="field-title-row">
                  <span>接口密钥</span>
                  <span className={`pill ${settings.aiApiKeyConfigured ? "pill--resolved" : "pill--neutral"}`}>
                    {settings.aiApiKeyConfigured ? "已配置" : "未配置"}
                  </span>
                </span>
                <input
                  type="password"
                  value={settings.aiApiKey || ""}
                  onChange={(event) => updateField("aiApiKey", event.target.value)}
                  autoComplete="new-password"
                  placeholder={settings.aiApiKeyConfigured ? "已配置，留空则保持不变" : "请输入模型接口密钥"}
                />
                <small className="field-helper">
                  {settings.aiApiKeyConfigured
                    ? "当前已配置密钥：留空提交不会覆盖；输入新密钥后保存将更新。"
                    : "当前未配置密钥：请输入后保存。"}
                </small>
              </label>

              <label className="field">
                <span>提示词模板</span>
                <textarea
                  rows="8"
                  value={settings.aiPromptTemplate || ""}
                  onChange={(event) => updateField("aiPromptTemplate", event.target.value)}
                />
              </label>
            </div>
          </section>

          <section className="settings-grid">
            <div>
              <span className="label-overline">同步状态</span>
              <h3>采集端生效状态</h3>
              <p className="section-copy">只保留配置是否被服务成功拉取并生效的最小状态面，不再展示运维级追踪细节。</p>
            </div>
            <div className="status-board">
              {statuses.length ? (
                statuses.map((item) => (
                  <article key={`${item.appName}-${item.serviceName}`} className={`sync-card ${item.effective ? "sync-card--effective" : ""}`}>
                    <div className="sync-card__head">
                      <strong>{item.serviceName || "--"}</strong>
                      <span>{item.appName || "--"}</span>
                    </div>
                    <dl>
                      <div>
                        <dt>目标版本</dt>
                        <dd>{item.targetConfigVersion ?? "--"}</dd>
                      </div>
                      <div>
                        <dt>已生效版本</dt>
                        <dd>{item.lastSuccessfulConfigVersion ?? "--"}</dd>
                      </div>
                      <div>
                        <dt>最近拉取</dt>
                        <dd>{formatDateTime(item.lastPulledAt)}</dd>
                      </div>
                    </dl>
                    <div className={`sync-status ${item.effective ? "sync-status--effective" : "sync-status--pending"}`}>
                      {item.effective ? "已生效" : "待同步"}
                    </div>
                  </article>
                ))
              ) : (
                <div className="empty-state">暂无采集端同步记录</div>
              )}
            </div>
          </section>

          <footer className="settings-footer">
            <div className="version-note">当前配置版本：{settings.version ?? "--"}</div>
            <button type="submit" className="button button--primary" disabled={saving}>
              <span className="material-symbols-outlined">save</span>
              {saving ? "保存中..." : "保存配置"}
            </button>
          </footer>
        </form>
      ) : null}
    </div>
  );
}
