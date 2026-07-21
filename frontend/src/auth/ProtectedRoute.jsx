import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "./AuthProvider.jsx";
import { safeReturnTo } from "./returnTo.js";

export function ProtectedRoute({ children }) {
  const { session, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return <div className="route-loading" role="status" aria-live="polite">正在恢复登录状态...</div>;
  }
  if (!session) {
    const returnTo = safeReturnTo(`${location.pathname}${location.search}`);
    return <Navigate to={`/sign-in?returnTo=${encodeURIComponent(returnTo)}`} replace />;
  }
  return children;
}
