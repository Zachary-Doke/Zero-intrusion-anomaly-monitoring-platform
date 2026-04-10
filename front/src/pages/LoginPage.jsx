import { useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function LoginPage() {
  const { isAuthenticated, login } = useAuth();
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("Admin@123");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setLoading(true);
    setError("");
    try {
      await login({ username, password });
      navigate(location.state?.from || "/", { replace: true });
    } catch (submitError) {
      setError(submitError.message || "登录失败");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-shell">
      <div className="login-card">
        <div className="brand-block brand-block--login">
          <div className="brand-mark">
            <span className="material-symbols-outlined">monitoring</span>
          </div>
          <div>
            <h1>异常监控平台</h1>
            <p>主线收敛版 · 登录后进入监控主台</p>
          </div>
        </div>

        <div className="login-copy">
          <span className="label-overline">系统入口</span>
          <h2>以最小操作面维持异常观察力</h2>
          <p>
            只保留登录、总览、异常列表、异常详情与配置页。去掉告警中心和报告中心，聚焦异常采集闭环。
          </p>
        </div>

        <form className="login-form" onSubmit={handleSubmit}>
          <label className="field">
            <span>账号</span>
            <input value={username} onChange={(event) => setUsername(event.target.value)} placeholder="请输入账号" />
          </label>
          <label className="field">
            <span>密码</span>
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="请输入密码"
            />
          </label>

          {error ? <div className="error-banner">{error}</div> : null}

          <button type="submit" className="button button--primary button--block" disabled={loading}>
            <span className="material-symbols-outlined">login</span>
            {loading ? "登录中..." : "进入监控台"}
          </button>
        </form>
      </div>
    </div>
  );
}
