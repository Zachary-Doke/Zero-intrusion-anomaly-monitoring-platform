import { BrowserRouter, Navigate, Outlet, Route, Routes, useLocation } from "react-router-dom";
import { AuthProvider, useAuth } from "./auth/AuthContext";
import { AppShell } from "./components/AppShell";
import { DashboardPage } from "./pages/DashboardPage";
import { ExceptionDetailPage } from "./pages/ExceptionDetailPage";
import { ExceptionsPage } from "./pages/ExceptionsPage";
import { LoginPage } from "./pages/LoginPage";
import { SettingsPage } from "./pages/SettingsPage";

function RequireAuth({ children }) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return children;
}

function RequireAdmin({ children }) {
  const { isAdmin } = useAuth();

  if (!isAdmin) {
    return <Navigate to="/" replace />;
  }
  return children;
}

function ProtectedLayout() {
  return (
    <RequireAuth>
      <AppShell>
        <Outlet />
      </AppShell>
    </RequireAuth>
  );
}

function FallbackRoute() {
  const { isAuthenticated } = useAuth();
  return <Navigate to={isAuthenticated ? "/" : "/login"} replace />;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<ProtectedLayout />}>
            <Route index element={<DashboardPage />} />
            <Route path="/exceptions" element={<ExceptionsPage />} />
            <Route path="/exceptions/:id" element={<ExceptionDetailPage />} />
            <Route
              path="/settings"
              element={
                <RequireAdmin>
                  <SettingsPage />
                </RequireAdmin>
              }
            />
          </Route>
          <Route path="*" element={<FallbackRoute />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
