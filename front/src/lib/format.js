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
      return "pill--medium";
    default:
      return "pill--neutral";
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
      return value || "--";
  }
}

export function safeArray(value) {
  return Array.isArray(value) ? value : [];
}
