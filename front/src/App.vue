<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import DetailSection from "./components/DetailSection.vue";
import EventTable from "./components/EventTable.vue";
import FingerprintTable from "./components/FingerprintTable.vue";
import TrendChart from "./components/TrendChart.vue";

const DEFAULT_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";
const STORAGE_KEYS = {
  baseUrl: "zero-monitor-base-url",
  token: "zero-monitor-token",
  profile: "zero-monitor-profile"
};

const baseUrl = ref(loadSavedBaseUrl());
const token = ref(localStorage.getItem(STORAGE_KEYS.token) || "");
const profile = reactive(loadSavedProfile());
const statusText = ref(token.value ? "已恢复登录状态" : "请先登录平台");
const activeView = ref(token.value ? "dashboard" : "login");
const analysisResult = ref(null);

const authForm = reactive({
  username: "admin",
  password: "Admin@123"
});

const overview = ref(defaultOverview());
const exceptions = ref([]);
const alerts = ref([]);
const reports = ref([]);
const selectedExceptionId = ref(null);
const selectedException = ref(null);
const exceptionPage = reactive({
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0
});
const exceptionFilters = reactive({
  severity: "",
  status: "",
  serviceName: "",
  keyword: "",
  days: 7
});
const alertFilters = reactive({
  keyword: "",
  alertStatus: "",
  sendStatus: ""
});
const reportFilters = reactive({
  keyword: "",
  status: ""
});
const settingsForm = reactive(defaultSettings());
const agentStatuses = ref([]);
const loading = reactive({
  login: false,
  overview: false,
  exceptions: false,
  detail: false,
  alerts: false,
  reports: false,
  settings: false,
  status: false,
  analyze: false
});

const canOperate = computed(() => profile.role === "ADMIN" || profile.role === "OPERATOR");
const isAdmin = computed(() => profile.role === "ADMIN");
const navItems = computed(() => {
  const items = [
    { id: "dashboard", label: "总览" },
    { id: "exceptions", label: "异常" },
    { id: "alerts", label: "告警" },
    { id: "reports", label: "报告" }
  ];
  if (isAdmin.value) {
    items.push({ id: "settings", label: "设置" });
  }
  return items;
});

const metricCards = computed(() => [
  { label: "异常总数", value: overview.value.metrics.totalExceptions, trace: "已上报并入库的异常事件" },
  { label: "异常指纹", value: overview.value.metrics.fingerprintCount, trace: "按指纹聚合后的问题簇" },
  { label: "触发告警", value: overview.value.metrics.triggeredAlertCount, trace: "达到阈值的异常指纹" },
  { label: "AI 报告", value: overview.value.metrics.aiReportCount, trace: "已生成的根因分析报告" },
  { label: "生效 Agent", value: `${overview.value.metrics.effectiveAgentCount}/${overview.value.metrics.agentCount}`, trace: "配置生效的运行中 Agent" }
]);

const detailSummary = computed(() => {
  if (!selectedException.value) {
    return [];
  }
  return [
    ["异常指纹", selectedException.value.fingerprint],
    ["异常级别", selectedException.value.severity],
    ["事件状态", selectedException.value.status],
    ["告警状态", selectedException.value.alertStatus || "-"],
    ["触发次数", selectedException.value.alertCount ?? "-"],
    ["发生时间", formatDateTime(selectedException.value.occurrenceTime)]
  ];
});

const detailRuntime = computed(() => {
  if (!selectedException.value) {
    return [];
  }
  return [
    ["应用", selectedException.value.appName || "-"],
    ["服务", selectedException.value.serviceName || "-"],
    ["环境", selectedException.value.environment || "-"],
    ["主机", selectedException.value.host || "-"],
    ["进程", selectedException.value.pid || "-"],
    ["线程", selectedException.value.threadName || "-"],
    ["Agent 版本", selectedException.value.agentVersion || "-"]
  ];
});

const detailMethod = computed(() => {
  if (!selectedException.value) {
    return [];
  }
  return [
    ["类名", selectedException.value.className || "-"],
    ["方法", selectedException.value.methodName || "-"],
    ["签名", selectedException.value.methodSignature || "-"],
    ["异常类型", selectedException.value.exceptionClass || "-"],
    ["栈顶位置", selectedException.value.topStackFrame || "-"],
    ["Trace ID", selectedException.value.traceId || "-"]
  ];
});

const detailSync = computed(() => {
  if (!selectedException.value) {
    return [];
  }
  return [
    ["配置版本", selectedException.value.configVersion ?? "-"],
    ["同步时间", formatDateTime(selectedException.value.lastConfigSyncAt)],
    ["同步状态", selectedException.value.lastConfigSyncStatus || "-"],
    ["同步错误", selectedException.value.lastConfigSyncError || "-"]
  ];
});

onMounted(() => {
  if (token.value) {
    bootstrapWorkspace();
  }
});

async function login() {
  loading.login = true;
  setStatus("正在登录平台...");
  try {
    const response = await requestJson("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(authForm),
      skipAuth: true
    });
    token.value = response.token;
    profile.username = response.username;
    profile.displayName = response.displayName;
    profile.role = response.role;
    profile.expiresAt = response.expiresAt;
    localStorage.setItem(STORAGE_KEYS.token, token.value);
    localStorage.setItem(STORAGE_KEYS.profile, JSON.stringify(profile));
    activeView.value = "dashboard";
    await bootstrapWorkspace();
    setStatus(`欢迎回来，${profile.displayName}`);
  } catch (error) {
    setStatus(`登录失败：${error.message}`);
  } finally {
    loading.login = false;
  }
}

function logout() {
  token.value = "";
  profile.username = "";
  profile.displayName = "";
  profile.role = "";
  profile.expiresAt = "";
  analysisResult.value = null;
  activeView.value = "login";
  localStorage.removeItem(STORAGE_KEYS.token);
  localStorage.removeItem(STORAGE_KEYS.profile);
  setStatus("已退出登录");
}

async function bootstrapWorkspace() {
  setStatus("正在同步平台数据...");
  const tasks = [loadOverview(), loadExceptions(0), loadAlerts(), loadReports()];
  if (isAdmin.value) {
    tasks.push(loadSettings());
  }
  await Promise.all(tasks);
  setStatus("平台数据已同步");
}

async function loadOverview() {
  loading.overview = true;
  try {
    overview.value = await requestJson("/api/dashboard/overview");
  } finally {
    loading.overview = false;
  }
}

async function loadExceptions(page = exceptionPage.page) {
  loading.exceptions = true;
  try {
    const params = new URLSearchParams({
      page: String(page),
      size: String(exceptionPage.size)
    });
    appendQueryParam(params, "severity", exceptionFilters.severity);
    appendQueryParam(params, "status", exceptionFilters.status);
    appendQueryParam(params, "serviceName", exceptionFilters.serviceName);
    appendQueryParam(params, "keyword", exceptionFilters.keyword);
    appendQueryParam(params, "days", exceptionFilters.days);
    const pageData = await requestJson(`/api/exceptions?${params.toString()}`);
    exceptions.value = pageData.content || [];
    exceptionPage.page = pageData.number || 0;
    exceptionPage.size = pageData.size || 20;
    exceptionPage.totalElements = pageData.totalElements || 0;
    exceptionPage.totalPages = pageData.totalPages || 0;
    if (!selectedExceptionId.value || !exceptions.value.some((item) => item.id === selectedExceptionId.value)) {
      selectedExceptionId.value = exceptions.value[0]?.id || null;
    }
    if (selectedExceptionId.value) {
      await loadExceptionDetail(selectedExceptionId.value);
    } else {
      selectedException.value = null;
    }
  } finally {
    loading.exceptions = false;
  }
}

async function loadExceptionDetail(id) {
  if (!id) {
    selectedException.value = null;
    return;
  }
  loading.detail = true;
  try {
    selectedExceptionId.value = id;
    selectedException.value = await requestJson(`/api/exceptions/${id}`);
  } finally {
    loading.detail = false;
  }
}

async function updateStatus(status) {
  if (!selectedExceptionId.value || !canOperate.value) {
    return;
  }
  loading.status = true;
  setStatus(`正在更新异常状态为 ${status}...`);
  try {
    await requestJson(`/api/exceptions/${selectedExceptionId.value}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status })
    });
    await Promise.all([loadOverview(), loadExceptions(exceptionPage.page), loadAlerts()]);
    setStatus(`状态已更新为 ${status}`);
  } finally {
    loading.status = false;
  }
}

async function loadAlerts() {
  loading.alerts = true;
  try {
    const params = new URLSearchParams();
    appendQueryParam(params, "keyword", alertFilters.keyword);
    appendQueryParam(params, "alertStatus", alertFilters.alertStatus);
    appendQueryParam(params, "sendStatus", alertFilters.sendStatus);
    alerts.value = await requestJson(`/api/alerts${params.toString() ? `?${params.toString()}` : ""}`);
  } finally {
    loading.alerts = false;
  }
}

async function loadReports() {
  loading.reports = true;
  try {
    const params = new URLSearchParams();
    appendQueryParam(params, "keyword", reportFilters.keyword);
    appendQueryParam(params, "status", reportFilters.status);
    reports.value = await requestJson(`/api/ai/reports${params.toString() ? `?${params.toString()}` : ""}`);
  } finally {
    loading.reports = false;
  }
}

async function runAnalysis(fingerprint) {
  if (!fingerprint || !canOperate.value) {
    return;
  }
  loading.analyze = true;
  setStatus(`正在为 ${fingerprint} 生成 AI 报告...`);
  try {
    analysisResult.value = await requestJson(`/api/ai/analyze/${fingerprint}`, { method: "POST" });
    await loadReports();
    activeView.value = "reports";
    setStatus("AI 报告已生成");
  } finally {
    loading.analyze = false;
  }
}

async function loadSettings() {
  if (!isAdmin.value) {
    return;
  }
  loading.settings = true;
  try {
    const [settings, statuses] = await Promise.all([
      requestJson("/api/settings"),
      requestJson("/api/settings/agent-sync-status")
    ]);
    Object.assign(settingsForm, settings);
    agentStatuses.value = statuses;
  } finally {
    loading.settings = false;
  }
}

async function saveSettings() {
  if (!isAdmin.value) {
    return;
  }
  loading.settings = true;
  setStatus("正在发布规则配置...");
  try {
    Object.assign(settingsForm, await requestJson("/api/settings", {
      method: "PUT",
      body: JSON.stringify({ ...settingsForm })
    }));
    await loadSettings();
    setStatus("规则配置已保存");
  } finally {
    loading.settings = false;
  }
}

function openExceptionFromDashboard(id) {
  activeView.value = "exceptions";
  loadExceptionDetail(id);
}

function focusFingerprint(fingerprint) {
  activeView.value = "exceptions";
  exceptionFilters.keyword = fingerprint;
  loadExceptions(0);
}

function previousPage() {
  if (exceptionPage.page > 0) {
    loadExceptions(exceptionPage.page - 1);
  }
}

function nextPage() {
  if (exceptionPage.page + 1 < exceptionPage.totalPages) {
    loadExceptions(exceptionPage.page + 1);
  }
}

function persistBaseUrl() {
  localStorage.setItem(STORAGE_KEYS.baseUrl, baseUrl.value.trim());
  if (token.value) {
    bootstrapWorkspace();
  }
}

function setStatus(message) {
  statusText.value = message;
}

function requestJson(path, options = {}) {
  const headers = { Accept: "application/json", ...(options.headers || {}) };
  if (!options.skipAuth && token.value) {
    headers.Authorization = `Bearer ${token.value}`;
  }
  if (options.body) {
    headers["Content-Type"] = "application/json";
  }
  return fetch(buildUrl(path), {
    method: options.method || "GET",
    headers,
    body: options.body
  }).then(async (response) => {
    const payload = await response.json().catch(() => null);
    if (response.status === 401) {
      logout();
      throw new Error(payload?.message || "登录已失效");
    }
    if (!response.ok) {
      throw new Error(payload?.message || `请求失败 (${response.status})`);
    }
    if (payload && payload.code && payload.code !== 200) {
      throw new Error(payload.message || "接口返回异常");
    }
    return payload?.data;
  });
}

function buildUrl(path) {
  const prefix = baseUrl.value.trim().replace(/\/+$/, "");
  return prefix ? `${prefix}${path}` : path;
}

function appendQueryParam(params, key, value) {
  if (value === null || value === undefined || value === "") {
    return;
  }
  params.set(key, String(value));
}

function loadSavedBaseUrl() {
  return localStorage.getItem(STORAGE_KEYS.baseUrl) || DEFAULT_BASE_URL;
}

function loadSavedProfile() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.profile) || "{\"username\":\"\",\"displayName\":\"\",\"role\":\"\",\"expiresAt\":\"\"}");
  } catch (error) {
    return { username: "", displayName: "", role: "", expiresAt: "" };
  }
}

function defaultOverview() {
  return {
    metrics: {
      totalExceptions: 0,
      fingerprintCount: 0,
      openExceptionCount: 0,
      criticalExceptionCount: 0,
      serviceCount: 0,
      triggeredAlertCount: 0,
      aiReportCount: 0,
      averageAnalysisMinutes: 0,
      agentCount: 0,
      effectiveAgentCount: 0
    },
    trends: [],
    recentEvents: [],
    topFingerprints: []
  };
}

function defaultSettings() {
  return {
    packagePatterns: "",
    deepSamplingEnabled: true,
    depthLimit: 8,
    lengthLimit: 1024,
    collectionLimit: 50,
    defaultSampleRate: 1,
    queueCapacity: 10000,
    flushIntervalMs: 5000,
    thresholdCount: 5,
    thresholdWindowMinutes: 1,
    alertRecipients: "",
    aiModel: "deepseek-chat",
    traceKeys: "traceId,requestId",
    sensitiveFields: "password,token,secret",
    version: 0
  };
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  return String(value).replace("T", " ").replace(/\.\d+$/, "");
}
</script>

<template>
  <div class="shell">
    <aside class="sidebar">
      <div>
        <div class="brand">
          <strong>Zero Monitor</strong>
          <span>异常监控平台</span>
        </div>
        <div class="base-url-panel">
          <label>API Base URL</label>
          <div class="inline-form">
            <input v-model="baseUrl" type="text" placeholder="默认走 Vite /api 代理" />
            <button type="button" class="secondary-button" @click="persistBaseUrl">应用</button>
          </div>
        </div>
        <nav v-if="token" class="nav-list">
          <button
            v-for="item in navItems"
            :key="item.id"
            type="button"
            class="nav-button"
            :class="{ active: activeView === item.id }"
            @click="activeView = item.id"
          >
            {{ item.label }}
          </button>
        </nav>
      </div>

      <div class="sidebar-footer">
        <p class="status-text">{{ statusText }}</p>
        <div v-if="token" class="profile-card">
          <strong>{{ profile.displayName }}</strong>
          <span>{{ profile.role }}</span>
          <span>{{ profile.username }}</span>
          <button type="button" class="secondary-button" @click="logout">退出登录</button>
        </div>
      </div>
    </aside>

    <main class="workspace">
      <section v-if="!token" class="login-screen">
        <div class="login-panel">
          <h1>平台登录</h1>
          <p>已撤回为带鉴权的原始控制台流程，访问接口前需要先登录。</p>
          <label>用户名</label>
          <input v-model="authForm.username" type="text" />
          <label>密码</label>
          <input v-model="authForm.password" type="password" />
          <button type="button" class="primary-button" :disabled="loading.login" @click="login">
            {{ loading.login ? "登录中..." : "登录并进入平台" }}
          </button>
        </div>
      </section>

      <template v-else>
        <header class="workspace-header">
          <div>
            <h1>平台总控台</h1>
            <p>异常分析、告警记录、AI 报告和规则配置都已恢复。</p>
          </div>
          <div class="header-actions">
            <button type="button" class="secondary-button" @click="bootstrapWorkspace">刷新全部</button>
          </div>
        </header>

        <section v-if="activeView === 'dashboard'" class="view-panel">
          <div class="metric-grid">
            <article v-for="card in metricCards" :key="card.label" class="metric-card">
              <span>{{ card.label }}</span>
              <strong>{{ card.value }}</strong>
              <small>{{ card.trace }}</small>
            </article>
          </div>

          <div class="dashboard-grid">
            <section class="panel">
              <div class="panel-head">
                <h2>近 7 天趋势</h2>
              </div>
              <TrendChart :trends="overview.trends" />
            </section>

            <section class="panel">
              <div class="panel-head">
                <h2>重点异常指纹</h2>
              </div>
              <FingerprintTable
                :items="overview.topFingerprints"
                :selected-id="selectedException?.fingerprint || null"
                @select="focusFingerprint"
                @analyze="runAnalysis"
              />
            </section>
          </div>

          <section class="panel">
            <div class="panel-head">
              <h2>最近异常事件</h2>
            </div>
            <div class="recent-grid">
              <button
                v-for="item in overview.recentEvents"
                :key="item.id"
                type="button"
                class="recent-card"
                @click="openExceptionFromDashboard(item.id)"
              >
                <strong>{{ item.serviceName }}</strong>
                <span>{{ item.exceptionClass }}</span>
                <small>{{ item.summary }}</small>
              </button>
            </div>
          </section>
        </section>

        <section v-if="activeView === 'exceptions'" class="view-panel">
          <section class="panel filters-panel">
            <div class="filter-grid">
              <input v-model="exceptionFilters.keyword" type="text" placeholder="关键字 / 指纹 / 消息" />
              <select v-model="exceptionFilters.severity">
                <option value="">全部级别</option>
                <option value="CRITICAL">CRITICAL</option>
                <option value="HIGH">HIGH</option>
                <option value="MEDIUM">MEDIUM</option>
              </select>
              <select v-model="exceptionFilters.status">
                <option value="">全部状态</option>
                <option value="OPEN">OPEN</option>
                <option value="INVESTIGATING">INVESTIGATING</option>
                <option value="RESOLVED">RESOLVED</option>
              </select>
              <input v-model="exceptionFilters.serviceName" type="text" placeholder="服务名" />
              <input v-model.number="exceptionFilters.days" type="number" min="1" placeholder="近几天" />
              <button type="button" class="primary-button" @click="loadExceptions(0)">筛选</button>
            </div>
          </section>

          <div class="detail-layout">
            <section class="panel">
              <div class="panel-head">
                <h2>异常事件列表</h2>
                <small>共 {{ exceptionPage.totalElements }} 条</small>
              </div>
              <EventTable
                :items="exceptions"
                :selected-id="selectedExceptionId"
                @select="loadExceptionDetail"
              />
              <div class="pagination-bar">
                <button type="button" class="secondary-button" :disabled="exceptionPage.page === 0" @click="previousPage">上一页</button>
                <span>第 {{ exceptionPage.page + 1 }} / {{ Math.max(exceptionPage.totalPages, 1) }} 页</span>
                <button type="button" class="secondary-button" :disabled="exceptionPage.page + 1 >= exceptionPage.totalPages" @click="nextPage">下一页</button>
              </div>
            </section>

            <section class="panel detail-panel">
              <div class="panel-head">
                <h2>异常详情</h2>
                <div class="header-actions">
                  <button type="button" class="secondary-button" :disabled="!canOperate || !selectedExceptionId" @click="updateStatus('INVESTIGATING')">标记排查中</button>
                  <button type="button" class="secondary-button" :disabled="!canOperate || !selectedExceptionId" @click="updateStatus('RESOLVED')">标记已解决</button>
                  <button type="button" class="primary-button" :disabled="!canOperate || !selectedException" @click="runAnalysis(selectedException.fingerprint)">生成 AI 报告</button>
                </div>
              </div>
              <template v-if="selectedException">
                <DetailSection title="事件摘要" :entries="detailSummary" />
                <DetailSection title="运行环境" :entries="detailRuntime" />
                <DetailSection title="方法信息" :entries="detailMethod" />
                <DetailSection title="配置同步" :entries="detailSync" />
                <section class="detail-section">
                  <h4>异常消息</h4>
                  <pre class="detail-block">{{ selectedException.message || "-" }}</pre>
                </section>
                <section class="detail-section">
                  <h4>堆栈信息</h4>
                  <pre class="detail-block">{{ selectedException.stackTrace || "-" }}</pre>
                </section>
              </template>
              <div v-else class="empty-block">请选择一条异常记录查看详情。</div>
            </section>
          </div>
        </section>

        <section v-if="activeView === 'alerts'" class="view-panel">
          <section class="panel filters-panel">
            <div class="filter-grid">
              <input v-model="alertFilters.keyword" type="text" placeholder="指纹 / 服务 / 摘要" />
              <select v-model="alertFilters.alertStatus">
                <option value="">全部告警状态</option>
                <option value="TRIGGERED">TRIGGERED</option>
                <option value="PENDING">PENDING</option>
              </select>
              <select v-model="alertFilters.sendStatus">
                <option value="">全部发送状态</option>
                <option value="SENT">SENT</option>
                <option value="SIMULATED">SIMULATED</option>
                <option value="FAILED">FAILED</option>
              </select>
              <button type="button" class="primary-button" @click="loadAlerts">刷新告警</button>
            </div>
          </section>

          <section class="panel">
            <div class="panel-head">
              <h2>告警记录</h2>
            </div>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>触发时间</th>
                    <th>服务</th>
                    <th>级别</th>
                    <th>状态</th>
                    <th>AI 报告</th>
                    <th>摘要</th>
                  </tr>
                </thead>
                <tbody v-if="alerts.length">
                  <tr v-for="item in alerts" :key="item.id">
                    <td>{{ formatDateTime(item.triggeredAt) }}</td>
                    <td>{{ item.serviceName || "-" }}</td>
                    <td>{{ item.severity || "-" }}</td>
                    <td>{{ item.alertStatus }}</td>
                    <td>{{ item.reportStatus }}</td>
                    <td>{{ item.summary }}</td>
                  </tr>
                </tbody>
                <tbody v-else>
                  <tr>
                    <td colspan="6" class="empty-cell">暂无告警记录</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </section>

        <section v-if="activeView === 'reports'" class="view-panel">
          <section class="panel filters-panel">
            <div class="filter-grid">
              <input v-model="reportFilters.keyword" type="text" placeholder="标题 / 指纹 / 摘要" />
              <select v-model="reportFilters.status">
                <option value="">全部报告状态</option>
                <option value="PENDING">PENDING</option>
                <option value="RUNNING">RUNNING</option>
                <option value="COMPLETED">COMPLETED</option>
                <option value="FAILED">FAILED</option>
              </select>
              <button type="button" class="primary-button" @click="loadReports">刷新报告</button>
            </div>
          </section>

          <div class="dashboard-grid">
            <section class="panel">
              <div class="panel-head">
                <h2>AI 报告列表</h2>
              </div>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>标题</th>
                      <th>状态</th>
                      <th>模型</th>
                      <th>触发来源</th>
                      <th>完成时间</th>
                    </tr>
                  </thead>
                  <tbody v-if="reports.length">
                    <tr v-for="item in reports" :key="item.fingerprint + item.requestedAt">
                      <td>{{ item.title }}</td>
                      <td>{{ item.reportStatus }}</td>
                      <td>{{ item.modelName }}</td>
                      <td>{{ item.triggerSource }}</td>
                      <td>{{ formatDateTime(item.analysisTime) }}</td>
                    </tr>
                  </tbody>
                  <tbody v-else>
                    <tr>
                      <td colspan="5" class="empty-cell">暂无 AI 报告</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>

            <section class="panel">
              <div class="panel-head">
                <h2>最近一次 AI 分析</h2>
              </div>
              <template v-if="analysisResult">
                <section class="detail-section">
                  <h4>可能原因</h4>
                  <p>{{ analysisResult.probableRootCause }}</p>
                </section>
                <section class="detail-section">
                  <h4>影响范围</h4>
                  <p>{{ analysisResult.impactScope }}</p>
                </section>
                <section class="detail-section">
                  <h4>排查步骤</h4>
                  <ul class="plain-list">
                    <li v-for="item in analysisResult.troubleshootingSteps || []" :key="item">{{ item }}</li>
                  </ul>
                </section>
                <section class="detail-section">
                  <h4>修复建议</h4>
                  <p>{{ analysisResult.fixSuggestion }}</p>
                </section>
              </template>
              <div v-else class="empty-block">从总览或异常列表触发一次 AI 分析后，这里会显示详细结论。</div>
            </section>
          </div>
        </section>

        <section v-if="activeView === 'settings' && isAdmin" class="view-panel">
          <div class="dashboard-grid">
            <section class="panel">
              <div class="panel-head">
                <h2>规则配置</h2>
                <small>版本 {{ settingsForm.version }}</small>
              </div>
              <div class="settings-grid">
                <label>监控包</label>
                <input v-model="settingsForm.packagePatterns" type="text" />
                <label>深度限制</label>
                <input v-model.number="settingsForm.depthLimit" type="number" min="1" />
                <label>长度限制</label>
                <input v-model.number="settingsForm.lengthLimit" type="number" min="1" />
                <label>集合限制</label>
                <input v-model.number="settingsForm.collectionLimit" type="number" min="1" />
                <label>默认采样率</label>
                <input v-model.number="settingsForm.defaultSampleRate" type="number" step="0.1" min="0" max="1" />
                <label>队列容量</label>
                <input v-model.number="settingsForm.queueCapacity" type="number" min="1" />
                <label>刷新间隔(ms)</label>
                <input v-model.number="settingsForm.flushIntervalMs" type="number" min="1000" />
                <label>告警阈值</label>
                <input v-model.number="settingsForm.thresholdCount" type="number" min="1" />
                <label>告警窗口(分钟)</label>
                <input v-model.number="settingsForm.thresholdWindowMinutes" type="number" min="1" />
                <label>告警接收人</label>
                <input v-model="settingsForm.alertRecipients" type="text" />
                <label>AI 模型</label>
                <input v-model="settingsForm.aiModel" type="text" />
                <label>Trace Keys</label>
                <input v-model="settingsForm.traceKeys" type="text" />
                <label>敏感字段</label>
                <input v-model="settingsForm.sensitiveFields" type="text" />
                <label>深采样开关</label>
                <select v-model="settingsForm.deepSamplingEnabled">
                  <option :value="true">开启</option>
                  <option :value="false">关闭</option>
                </select>
              </div>
              <div class="panel-actions">
                <button type="button" class="primary-button" :disabled="loading.settings" @click="saveSettings">保存配置</button>
              </div>
            </section>

            <section class="panel">
              <div class="panel-head">
                <h2>Agent 同步状态</h2>
              </div>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>服务</th>
                      <th>环境</th>
                      <th>版本</th>
                      <th>同步状态</th>
                      <th>目标版本</th>
                      <th>最后在线</th>
                    </tr>
                  </thead>
                  <tbody v-if="agentStatuses.length">
                    <tr v-for="item in agentStatuses" :key="`${item.appName}-${item.serviceName}`">
                      <td>{{ item.serviceName }}</td>
                      <td>{{ item.environment || "-" }}</td>
                      <td>{{ item.agentVersion || "-" }}</td>
                      <td>{{ item.lastConfigSyncStatus || (item.effective ? "EFFECTIVE" : "UNKNOWN") }}</td>
                      <td>{{ item.targetConfigVersion ?? "-" }}</td>
                      <td>{{ formatDateTime(item.lastSeenAt) }}</td>
                    </tr>
                  </tbody>
                  <tbody v-else>
                    <tr>
                      <td colspan="6" class="empty-cell">暂无 Agent 同步状态</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
          </div>
        </section>
      </template>
    </main>
  </div>
</template>
