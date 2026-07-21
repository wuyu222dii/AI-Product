import { useEffect, useState } from "react";
import { LoaderCircle } from "lucide-react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { exchangeOAuthCode } from "../auth/exchangeOAuthCode.js";
import { safeReturnTo } from "../auth/returnTo.js";
import { supabase } from "../auth/supabaseClient.js";

function oauthErrorFromParams(searchParams) {
  const description = searchParams.get("error_description");
  const code = searchParams.get("error_code");
  const error = searchParams.get("error");
  if (!error && !description && !code) {
    const hash = new URLSearchParams(window.location.hash.replace(/^#/, ""));
    return hash.get("error_description") || hash.get("error_code") || hash.get("error") || "";
  }
  return description || code || error || "OAuth 登录被取消或失败";
}

export function AuthCallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    const oauthError = oauthErrorFromParams(searchParams);
    const code = searchParams.get("code");

    async function restore() {
      try {
        if (oauthError) {
          if (active) setError(oauthError);
          return;
        }

        if (code) {
          const { error: exchangeError } = await exchangeOAuthCode(code);
          if (exchangeError) {
            const { data: existing } = await supabase.auth.getSession();
            if (!existing.session) throw exchangeError;
          }
        }

        const { data } = await supabase.auth.getSession();
        if (!active) return;
        if (!data.session) {
          setError("没有恢复到有效登录会话，请返回登录页重试。");
          return;
        }

        const target = safeReturnTo(window.sessionStorage.getItem("auth:returnTo"));
        window.sessionStorage.removeItem("auth:returnTo");
        navigate(target, { replace: true });
      } catch (restoreError) {
        if (!active) return;
        setError(restoreError?.message || "登录回调未能完成，请返回登录页重试。");
      }
    }

    restore();
    return () => {
      active = false;
    };
    // Intentionally depend on the stable query values, not the searchParams object identity.
  }, [navigate, searchParams.toString()]);

  return (
    <main className="auth-page">
      <section className="auth-callback" role="status">
        {error ? (
          <>
            <p>{error}</p>
            <Link className="primary-btn" to="/sign-in">返回登录</Link>
          </>
        ) : (
          <><LoaderCircle className="spin" size={22} /> 正在恢复研究空间...</>
        )}
      </section>
    </main>
  );
}
