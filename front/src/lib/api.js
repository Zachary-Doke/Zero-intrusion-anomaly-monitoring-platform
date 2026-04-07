const TOKEN_KEY = "auth_token";
const USER_KEY = "auth_user";

function redirectToLogin() {
  if (window.location.pathname !== "/login") {
    window.location.assign("/login");
  }
}

export function readStoredAuth() {
  try {
    const token = localStorage.getItem(TOKEN_KEY) ?? "";
    const rawUser = localStorage.getItem(USER_KEY);
    return {
      token,
      user: rawUser ? JSON.parse(rawUser) : null
    };
  } catch (error) {
    return {
      token: "",
      user: null
    };
  }
}

export function writeStoredAuth(auth) {
  localStorage.setItem(TOKEN_KEY, auth.token);
  localStorage.setItem(USER_KEY, JSON.stringify(auth.user));
}

export function clearStoredAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

async function request(path, options = {}) {
  const { auth = true, headers, body, ...rest } = options;
  const nextHeaders = new Headers(headers ?? {});
  const { token } = readStoredAuth();

  if (auth && token) {
    nextHeaders.set("Authorization", `Bearer ${token}`);
  }
  if (body !== undefined) {
    nextHeaders.set("Content-Type", "application/json");
  }

  const response = await fetch(path, {
    ...rest,
    headers: nextHeaders,
    body: body === undefined ? undefined : JSON.stringify(body)
  });

  if (response.status === 401) {
    clearStoredAuth();
    redirectToLogin();
    throw new Error("登录状态已失效");
  }

  let payload = null;
  const raw = await response.text();
  if (raw) {
    try {
      payload = JSON.parse(raw);
    } catch (error) {
      payload = {
        message: raw
      };
    }
  }

  if (!response.ok) {
    throw new Error(payload?.message || `请求失败(${response.status})`);
  }

  if (payload && payload.code !== 200) {
    throw new Error(payload.message || "请求失败");
  }

  return payload?.data ?? null;
}

function buildQuery(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      query.set(key, String(value));
    }
  });
  const text = query.toString();
  return text ? `?${text}` : "";
}

export function loginRequest(credentials) {
  return request("/api/auth/login", {
    method: "POST",
    auth: false,
    body: credentials
  });
}

export const api = {
  getOverview() {
    return request("/api/dashboard/overview");
  },
  getExceptions(filters) {
    return request(`/api/exceptions${buildQuery(filters)}`);
  },
  getExceptionDetail(id) {
    return request(`/api/exceptions/${id}`);
  },
  updateExceptionStatus(id, status) {
    return request(`/api/exceptions/${id}/status`, {
      method: "PATCH",
      body: { status }
    });
  },
  generateSuggestion(id) {
    return request(`/api/exceptions/${id}/suggestion`, {
      method: "POST"
    });
  },
  getSettings() {
    return request("/api/settings");
  },
  saveSettings(settings) {
    return request("/api/settings", {
      method: "PUT",
      body: settings
    });
  },
  getAgentSyncStatuses() {
    return request("/api/settings/agent-sync-status");
  }
};
