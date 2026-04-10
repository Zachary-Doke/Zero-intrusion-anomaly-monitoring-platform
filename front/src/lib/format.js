export function formatDateTime(value) {
  if (!value) {
    return "--";
  }
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(value));
}

export function formatFullDateTime(value) {
  if (!value) {
    return "--";
  }
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit"
  }).format(new Date(value));
}

export function formatNumber(value) {
  return new Intl.NumberFormat("zh-CN").format(value ?? 0);
}

function hasLatinLetters(value) {
  return /[A-Za-z]/.test(value || "");
}

export function chineseOnlyText(value, fallback = "--") {
  if (!value && value !== 0) {
    return fallback;
  }
  const text = String(value).trim();
  if (!text) {
    return fallback;
  }
  return hasLatinLetters(text) ? fallback : text;
}

export function chineseOnlyMultilineText(value, fallback = "暂无中文内容") {
  if (!value) {
    return fallback;
  }
  const text = String(value).trim();
  if (!text) {
    return fallback;
  }
  return hasLatinLetters(text) ? fallback : text;
}

export function chineseOnlyList(values, fallbackItem = "暂无中文信息") {
  const items = Array.isArray(values) ? values : [];
  const normalized = items
    .map((item) => chineseOnlyText(item, ""))
    .filter(Boolean);
  return normalized.length ? normalized : [fallbackItem];
}

export function formatJsonText(value) {
  if (!value) {
    return "--";
  }
  if (typeof value !== "string") {
    return JSON.stringify(value, null, 2);
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch (error) {
    return value;
  }
}

export function severityVariant(value) {
  switch ((value || "").toUpperCase()) {
    case "CRITICAL":
      return "pill--critical";
    case "HIGH":
      return "pill--high";
    case "MEDIUM":
    case "LOW":
      return "pill--medium";
    default:
      return "pill--neutral";
  }
}

export function severityLabel(value) {
  switch ((value || "").toUpperCase()) {
    case "CRITICAL":
      return "严重";
    case "HIGH":
      return "高";
    case "MEDIUM":
      return "中";
    case "LOW":
      return "低";
    default:
      return "未知级别";
  }
}

export function statusVariant(value) {
  switch ((value || "").toUpperCase()) {
    case "OPEN":
      return "pill--critical";
    case "INVESTIGATING":
      return "pill--investigating";
    case "RESOLVED":
      return "pill--resolved";
    default:
      return "pill--neutral";
  }
}

export function statusLabel(value) {
  switch ((value || "").toUpperCase()) {
    case "OPEN":
      return "未处理";
    case "INVESTIGATING":
      return "处理中";
    case "RESOLVED":
      return "已解决";
    default:
      return "未知状态";
  }
}

export function suggestionStatusLabel(value) {
  switch ((value || "").toUpperCase()) {
    case "READY":
      return "就绪";
    case "GENERATING":
      return "生成中";
    case "FAILED":
      return "生成失败";
    case "SUCCESS":
    case "COMPLETED":
      return "已生成";
    default:
      return "未知状态";
  }
}

export function syncStatusLabel(value) {
  switch ((value || "").toUpperCase()) {
    case "SUCCESS":
      return "成功";
    case "FAILED":
      return "失败";
    case "PENDING":
      return "待同步";
    default:
      return chineseOnlyText(value, "未知状态");
  }
}

export function safeArray(value) {
  return Array.isArray(value) ? value : [];
}
