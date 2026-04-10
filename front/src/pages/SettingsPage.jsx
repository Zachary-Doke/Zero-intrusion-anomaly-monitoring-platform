import { useEffect, useState } from "react";
import { api } from "../lib/api";
import { formatDateTime } from "../lib/format";

function numericValue(value) {
  return value === "" ? "" : Number(value);
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
      setMessage("配置已保存，采集端将在下一次拉取时生效。");
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
          <p>删掉阈值告警和报告中心后，配置页只保留当前主线真正需要的参数。</p>
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
            </div>
            <div className="surface-panel form-panel">
              <label className="field">
                <span>包名过滤器</span>
                <input
                  value={settings.packagePatterns || ""}
                  onChange={(event) => updateField("packagePatterns", event.target.value)}
                  placeholder="请输入包名前缀，多个用逗号分隔"
                />
              </label>

              <label className="toggle-row">
                <div>
                  <strong>深度采样</strong>
                  <span>开启后同步扩展 this 快照与更完整的上下文。</span>
                </div>
                <input
                  type="checkbox"
                  checked={Boolean(settings.deepSamplingEnabled)}
                  onChange={(event) => updateField("deepSamplingEnabled", event.target.checked)}
                />
              </label>

              <div className="form-grid">
                <label className="field">
                  <span>快照深度限制</span>
                  <input
                    type="number"
                    value={settings.depthLimit ?? ""}
                    onChange={(event) => updateField("depthLimit", numericValue(event.target.value))}
                  />
                </label>
                <label className="field">
                  <span>文本长度限制</span>
                  <input
                    type="number"
                    value={settings.lengthLimit ?? ""}
                    onChange={(event) => updateField("lengthLimit", numericValue(event.target.value))}
                  />
                </label>
                <label className="field">
                  <span>集合数量限制</span>
                  <input
                    type="number"
                    value={settings.collectionLimit ?? ""}
                    onChange={(event) => updateField("collectionLimit", numericValue(event.target.value))}
                  />
                </label>
                <label className="field">
                  <span>默认采样率</span>
                  <input
                    type="number"
                    step="0.1"
                    value={settings.defaultSampleRate ?? ""}
                    onChange={(event) => updateField("defaultSampleRate", numericValue(event.target.value))}
                  />
                </label>
                <label className="field">
                  <span>队列容量</span>
                  <input
                    type="number"
                    value={settings.queueCapacity ?? ""}
                    onChange={(event) => updateField("queueCapacity", numericValue(event.target.value))}
                  />
                </label>
                <label className="field">
                  <span>刷新间隔（毫秒）</span>
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
              <h3>智能分析接口配置</h3>
              <p className="section-copy">异常详情页会读取这里的接口配置，直接在详情内生成处理建议，不再进入独立报告中心。</p>
            </div>
            <div className="surface-panel form-panel">
              <div className="form-grid">
                <label className="field">
                  <span>接口地址</span>
                  <input
                    value={settings.aiBaseUrl || ""}
                    onChange={(event) => updateField("aiBaseUrl", event.target.value)}
                    placeholder="https://api.deepseek.com"
                  />
                </label>
                <label className="field">
                  <span>模型名称</span>
                  <input
                    value={settings.aiModel || ""}
                    onChange={(event) => updateField("aiModel", event.target.value)}
                    placeholder="deepseek-chat"
                  />
                </label>
              </div>

              <label className="field">
                <span>接口密钥</span>
                <input
                  type="password"
                  value={settings.aiApiKey || ""}
                  onChange={(event) => updateField("aiApiKey", event.target.value)}
                  autoComplete="new-password"
                  placeholder={settings.aiApiKeyConfigured ? "已配置，留空则保持不变" : "请输入模型接口密钥"}
                />
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
                      <strong>{item.serviceName}</strong>
                      <span>{item.appName}</span>
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
