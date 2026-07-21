import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";

/**
 * Supabase may fall back to Site URL (often "/") when Redirect URLs are incomplete.
 * Forward any OAuth PKCE payload to the dedicated callback route.
 */
export function OAuthCodeCatcher() {
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    if (location.pathname === "/auth/callback") return;
    const params = new URLSearchParams(location.search);
    const hashParams = new URLSearchParams(location.hash.replace(/^#/, ""));
    const hasOAuthPayload = Boolean(
      params.get("code")
      || params.get("error")
      || hashParams.get("code")
      || hashParams.get("error")
      || hashParams.get("access_token")
    );
    if (!hasOAuthPayload) return;
    navigate(`/auth/callback${location.search}${location.hash}`, { replace: true });
  }, [location.hash, location.pathname, location.search, navigate]);

  return null;
}
