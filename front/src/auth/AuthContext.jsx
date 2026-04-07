import { createContext, useContext, useEffect, useState } from "react";
import {
  clearStoredAuth,
  loginRequest,
  readStoredAuth,
  writeStoredAuth
} from "../lib/api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => readStoredAuth());

  useEffect(() => {
    function handleStorage() {
      setAuth(readStoredAuth());
    }

    window.addEventListener("storage", handleStorage);
    return () => window.removeEventListener("storage", handleStorage);
  }, []);

  async function login(credentials) {
    const response = await loginRequest(credentials);
    const nextAuth = {
      token: response.token,
      user: {
        username: response.username,
        displayName: response.displayName,
        role: response.role,
        expiresAt: response.expiresAt
      }
    };
    writeStoredAuth(nextAuth);
    setAuth(nextAuth);
    return nextAuth;
  }

  function logout() {
    clearStoredAuth();
    setAuth({ token: "", user: null });
  }

  const value = {
    token: auth.token,
    user: auth.user,
    isAuthenticated: Boolean(auth.token),
    isAdmin: auth.user?.role === "ADMIN",
    login,
    logout
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}
