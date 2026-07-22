import { useEffect, useRef, useState } from "react";
import { ArrowLeft, Chrome, LoaderCircle, Mail, ShieldCheck } from "lucide-react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../auth/AuthProvider.jsx";
import { safeReturnTo } from "../auth/returnTo.js";
import { isSupabaseConfigured, supabase } from "../auth/supabaseClient.js";

const RESEND_SECONDS = 60;

function getOtpSendErrorMessage(sendError) {
  if (!sendError) return "验证码暂时无法发送，请稍后重试。";

  const message = sendError.message || "";
  const status = sendError.status;

  if (message.includes("Error sending magic link email")) {
    return "验证码邮件发送失败。请检查 Supabase 的 SMTP / Email OTP 配置后再试。";
  }

  if (status === 429) {
    return "发送过于频繁，请稍后再试。";
  }

  return message ? `验证码暂时无法发送：${message}` : "验证码暂时无法发送，请稍后重试。";
}

export function SignInPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { session } = useAuth();
  const returnTo = safeReturnTo(searchParams.get("returnTo"));
  const [email, setEmail] = useState("");
  const [token, setToken] = useState("");
  const [step, setStep] = useState("email");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [countdown, setCountdown] = useState(0);
  const tokenRef = useRef(null);

  useEffect(() => {
    if (session) navigate(returnTo, { replace: true });
  }, [session, navigate, returnTo]);

  useEffect(() => {
    if (countdown <= 0) return undefined;
    const timer = window.setInterval(() => setCountdown((value) => Math.max(0, value - 1)), 1000);
    return () => window.clearInterval(timer);
  }, [countdown]);

  async function sendOtp(event) {
    event?.preventDefault();
    if (!email.trim() || busy) return;
    setBusy(true);
    setError("");
    try {
      const { error: sendError } = await supabase.auth.signInWithOtp({
        email: email.trim(),
        options: { shouldCreateUser: true }
      });
      if (sendError) throw sendError;
      setStep("otp");
      setCountdown(RESEND_SECONDS);
      window.setTimeout(() => tokenRef.current?.focus(), 0);
    } catch (sendError) {
      setError(getOtpSendErrorMessage(sendError));
    } finally {
      setBusy(false);
    }
  }

  async function verifyOtp(event) {
    event.preventDefault();
    if (token.length !== 6 || busy) return;
    setBusy(true);
    setError("");
    try {
      const { error: verifyError } = await supabase.auth.verifyOtp({
        email: email.trim(),
        token,
        type: "email"
      });
      if (verifyError) throw verifyError;
    } catch {
      setError("验证码无效或已过期，请重新获取。");
    } finally {
      setBusy(false);
    }
  }

  async function signInWithGoogle() {
    if (busy) return;
    setBusy(true);
    setError("");
    window.sessionStorage.setItem("auth:returnTo", returnTo);
    try {
      const { error: oauthError } = await supabase.auth.signInWithOAuth({
        provider: "google",
        options: {
          redirectTo: `${window.location.origin}/auth/callback`,
          queryParams: {
            access_type: "online",
            prompt: "select_account"
          }
        }
      });
      if (oauthError) throw oauthError;
    } catch (oauthFailure) {
      setBusy(false);
      const detail = oauthFailure?.message ? `（${oauthFailure.message}）` : "";
      setError(`Google 登录暂时不可用，请改用邮箱验证码。${detail}`);
    }
  }

  return (
    <main className="auth-page">
      <aside className="auth-narrative" aria-hidden="false">
        <div className="auth-narrative-gradient" aria-hidden="true" />
        <div className="auth-narrative-noise" aria-hidden="true" />
        <div className="auth-narrative-inner reveal-up">
          <p className="eyebrow">Private research space</p>
          <h2>你的材料与章节，只对当前账号可见</h2>
          <p>登录后创建独立研究空间。项目、上传材料、章节草稿与导出记录按账号隔离，便于在学校或期刊规范下可追溯地使用 AI 辅助写作。</p>
          <ul className="auth-narrative-points">
            <li>每个研究项目拥有独立材料库与知识底座</li>
            <li>章节修改保留预览与人工确认记录</li>
            <li>不承诺规避检测，优先保证证据链完整</li>
          </ul>
        </div>
      </aside>

      <div className="auth-panel-wrap">
        <section className="auth-panel" aria-labelledby="sign-in-title">
          <Link className="auth-back" to="/"><ArrowLeft size={16} /> 返回首页</Link>
          <div className="auth-brand">AI 论文共写工作台</div>
          <h1 id="sign-in-title">登录你的研究空间</h1>
          <p>项目、材料、章节和导出文件只对当前账号可见。</p>

          {!isSupabaseConfigured && (
            <div className="auth-config-warning" role="alert">当前环境尚未配置 Supabase 登录参数。</div>
          )}
          {error && <div className="auth-error" role="alert">{error}</div>}

          <button className="oauth-button" type="button" onClick={signInWithGoogle} disabled={busy || !isSupabaseConfigured}>
            <Chrome size={18} /> 使用 Google 登录
          </button>
          <div className="auth-divider"><span>或使用邮箱验证码</span></div>

          {step === "email" ? (
            <form onSubmit={sendOtp} className="auth-form">
              <label htmlFor="sign-in-email">邮箱地址</label>
              <div className="auth-input-wrap"><Mail size={17} /><input id="sign-in-email" type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="name@university.edu" required /></div>
              <button className="primary-btn" disabled={busy || !isSupabaseConfigured}>{busy ? <LoaderCircle className="spin" size={18} /> : null}发送 6 位验证码</button>
            </form>
          ) : (
            <form onSubmit={verifyOtp} className="auth-form">
              <div className="auth-otp-head"><span>验证码已发送至</span><strong>{email}</strong></div>
              <label htmlFor="email-otp">6 位验证码</label>
              <input ref={tokenRef} id="email-otp" className="otp-input" inputMode="numeric" autoComplete="one-time-code" maxLength={6} value={token} onChange={(event) => setToken(event.target.value.replace(/\D/g, "").slice(0, 6))} pattern="[0-9]{6}" aria-describedby="otp-help" required />
              <span id="otp-help" className="auth-help">可直接粘贴邮件中的 6 位数字。</span>
              <button className="primary-btn" disabled={busy || token.length !== 6}>{busy ? <LoaderCircle className="spin" size={18} /> : null}验证并登录</button>
              <div className="auth-secondary-actions">
                <button type="button" className="text-button" onClick={() => { setStep("email"); setToken(""); }}>更换邮箱</button>
                <button type="button" className="text-button" onClick={sendOtp} disabled={countdown > 0 || busy}>{countdown > 0 ? `${countdown}s 后可重发` : "重新发送"}</button>
              </div>
            </form>
          )}
          <p className="auth-trust"><ShieldCheck size={15} /> AI 输出保留材料依据与人工确认记录。</p>
        </section>
      </div>
    </main>
  );
}
