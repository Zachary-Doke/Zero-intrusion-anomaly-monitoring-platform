import { NavLink, useLocation } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

function NavItem({ to, icon, label, active }) {
  return (
    <NavLink to={to} className={`nav-item ${active ? "nav-item--active" : ""}`}>
      <span className="material-symbols-outlined">{icon}</span>
      <span>{label}</span>
    </NavLink>
  );
}

export function AppShell({ children }) {
  const { user, logout, isAdmin } = useAuth();
  const location = useLocation();
  const exceptionActive = location.pathname.startsWith("/exceptions");

  return (
    <div className="app-layout">
      <aside className="app-sidebar">
        <div className="brand-block">
          <div className="brand-mark">
            <span className="material-symbols-outlined">monitoring</span>
          </div>
          <div>
            <h1>异常监控平台</h1>
            <p>异常链路观察台</p>
          </div>
        </div>

        <nav className="nav-list">
          <NavItem to="/" icon="dashboard" label="总览仪表盘" active={location.pathname === "/"} />
          <NavItem to="/exceptions" icon="list_alt" label="异常列表" active={exceptionActive} />
          {isAdmin ? (
            <NavItem to="/settings" icon="settings_suggest" label="配置中心" active={location.pathname === "/settings"} />
          ) : null}
        </nav>

        <div className="sidebar-footer">
          <div className="user-card">
            <div className="user-avatar">{(user?.displayName || "A").slice(0, 1)}</div>
            <div className="user-meta">
              <strong>{user?.displayName || user?.username}</strong>
              <span>{user?.role === "ADMIN" ? "管理员" : "运维用户"}</span>
            </div>
          </div>
          <button type="button" className="button button--ghost button--block" onClick={logout}>
            <span className="material-symbols-outlined">logout</span>
            退出登录
          </button>
        </div>
      </aside>

      <main className="app-main">
        <div className="page-shell">{children}</div>
      </main>
    </div>
  );
}
