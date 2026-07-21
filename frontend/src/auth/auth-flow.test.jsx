import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AuthProvider } from "./AuthProvider.jsx";
import { exchangeOAuthCode, resetOAuthExchangeForTests } from "./exchangeOAuthCode.js";
import { OAuthCodeCatcher } from "./OAuthCodeCatcher.jsx";
import { ProtectedRoute } from "./ProtectedRoute.jsx";
import { AuthCallbackPage } from "../pages/AuthCallbackPage.jsx";
import { SignInPage } from "../pages/SignInPage.jsx";
import { supabase } from "./supabaseClient.js";

vi.mock("./supabaseClient.js", () => ({
  isSupabaseConfigured: true,
  supabase: {
    auth: {
      getSession: vi.fn(),
      onAuthStateChange: vi.fn(),
      signInWithOtp: vi.fn(),
      verifyOtp: vi.fn(),
      signInWithOAuth: vi.fn(),
      exchangeCodeForSession: vi.fn(),
      signOut: vi.fn()
    }
  }
}));

describe("authentication flow", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    resetOAuthExchangeForTests();
    window.sessionStorage.clear();
    supabase.auth.getSession.mockResolvedValue({ data: { session: null } });
    supabase.auth.onAuthStateChange.mockReturnValue({ data: { subscription: { unsubscribe: vi.fn() } } });
    supabase.auth.signInWithOtp.mockResolvedValue({ error: null });
    supabase.auth.verifyOtp.mockResolvedValue({ error: null });
    supabase.auth.signInWithOAuth.mockResolvedValue({ error: null });
    supabase.auth.exchangeCodeForSession.mockResolvedValue({ data: { session: null }, error: null });
  });

  it("redirects an anonymous app route to sign in", async () => {
    render(
      <MemoryRouter initialEntries={["/app/projects"]}>
        <AuthProvider>
          <Routes>
            <Route path="/sign-in" element={<div>登录页面</div>} />
            <Route path="/app/projects" element={<ProtectedRoute><div>项目列表</div></ProtectedRoute>} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    );
    expect(await screen.findByText("登录页面")).toBeInTheDocument();
  });

  it("recovers from a session restore failure instead of loading forever", async () => {
    supabase.auth.getSession.mockRejectedValueOnce(new Error("network unavailable"));
    render(
      <MemoryRouter initialEntries={["/app/projects"]}>
        <AuthProvider>
          <Routes>
            <Route path="/sign-in" element={<div>登录页面</div>} />
            <Route path="/app/projects" element={<ProtectedRoute><div>项目列表</div></ProtectedRoute>} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    );

    expect(await screen.findByText("登录页面")).toBeInTheDocument();
  });

  it("sends and verifies a six-digit email OTP", async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={["/sign-in"]}>
        <AuthProvider><Routes><Route path="/sign-in" element={<SignInPage />} /></Routes></AuthProvider>
      </MemoryRouter>
    );
    await screen.findByLabelText("邮箱地址");
    await user.type(screen.getByLabelText("邮箱地址"), "student@example.edu");
    await user.click(screen.getByRole("button", { name: "发送 6 位验证码" }));
    expect(supabase.auth.signInWithOtp).toHaveBeenCalledWith({
      email: "student@example.edu",
      options: { shouldCreateUser: true }
    });
    const otp = await screen.findByLabelText("6 位验证码");
    await user.type(otp, "12a3456");
    expect(otp).toHaveValue("123456");
    await user.click(screen.getByRole("button", { name: "验证并登录" }));
    await waitFor(() => expect(supabase.auth.verifyOtp).toHaveBeenCalledWith({
      email: "student@example.edu",
      token: "123456",
      type: "email"
    }));
  });

  it("starts Google OAuth with the fixed callback route", async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={["/sign-in?returnTo=/app/projects"]}>
        <AuthProvider><Routes><Route path="/sign-in" element={<SignInPage />} /></Routes></AuthProvider>
      </MemoryRouter>
    );

    await user.click(await screen.findByRole("button", { name: "使用 Google 登录" }));

    expect(window.sessionStorage.getItem("auth:returnTo")).toBe("/app/projects");
    expect(supabase.auth.signInWithOAuth).toHaveBeenCalledWith({
      provider: "google",
      options: {
        redirectTo: `${window.location.origin}/auth/callback`,
        queryParams: {
          access_type: "online",
          prompt: "select_account"
        }
      }
    });
  });

  it("exchanges the Google OAuth code once on the callback route", async () => {
    window.sessionStorage.setItem("auth:returnTo", "/app/projects");
    const session = { access_token: "token", user: { id: "u1" } };
    supabase.auth.exchangeCodeForSession.mockResolvedValue({ data: { session }, error: null });
    supabase.auth.getSession.mockImplementation(async () => ({
      data: {
        session: supabase.auth.exchangeCodeForSession.mock.calls.length > 0 ? session : null
      }
    }));

    render(
      <MemoryRouter initialEntries={["/auth/callback?code=oauth-code"]}>
        <AuthProvider>
          <Routes>
            <Route path="/auth/callback" element={<AuthCallbackPage />} />
            <Route path="/app/projects" element={<div>项目列表</div>} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    );

    expect(await screen.findByText("项目列表")).toBeInTheDocument();
    expect(supabase.auth.exchangeCodeForSession).toHaveBeenCalledTimes(1);
    expect(supabase.auth.exchangeCodeForSession).toHaveBeenCalledWith("oauth-code");
  });

  it("reuses one in-flight PKCE exchange under concurrent calls", async () => {
    let resolveExchange;
    supabase.auth.exchangeCodeForSession.mockImplementation(
      () => new Promise((resolve) => {
        resolveExchange = resolve;
      })
    );

    const first = exchangeOAuthCode("same-code");
    const second = exchangeOAuthCode("same-code");
    expect(first).toBe(second);
    expect(supabase.auth.exchangeCodeForSession).toHaveBeenCalledTimes(1);

    resolveExchange({ data: { session: { access_token: "t" } }, error: null });
    await expect(first).resolves.toMatchObject({ error: null });
    await expect(second).resolves.toMatchObject({ error: null });
  });

  it("forwards OAuth codes that land on the site URL to the callback route", async () => {
    render(
      <MemoryRouter initialEntries={["/?code=root-code"]}>
        <OAuthCodeCatcher />
        <Routes>
          <Route path="/" element={<div>首页</div>} />
          <Route path="/auth/callback" element={<div>回调页</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText("回调页")).toBeInTheDocument();
  });

  it("recovers when the OAuth code was already consumed but a session exists", async () => {
    window.sessionStorage.setItem("auth:returnTo", "/app/projects");
    supabase.auth.exchangeCodeForSession.mockResolvedValue({
      data: { session: null },
      error: new Error("Invalid auth code")
    });
    supabase.auth.getSession.mockResolvedValue({
      data: { session: { access_token: "token", user: { id: "u1" } } }
    });

    render(
      <MemoryRouter initialEntries={["/auth/callback?code=used-code"]}>
        <AuthProvider>
          <Routes>
            <Route path="/auth/callback" element={<AuthCallbackPage />} />
            <Route path="/app/projects" element={<div>项目列表</div>} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    );

    expect(await screen.findByText("项目列表")).toBeInTheDocument();
  });

  it("shows a recoverable error when OAuth returns an error query", async () => {
    render(
      <MemoryRouter initialEntries={["/auth/callback?error=access_denied&error_description=User+cancelled"]}>
        <AuthProvider>
          <Routes>
            <Route path="/auth/callback" element={<AuthCallbackPage />} />
            <Route path="/sign-in" element={<div>登录页面</div>} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    );

    expect(await screen.findByText("User cancelled")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "返回登录" })).toHaveAttribute("href", "/sign-in");
  });
});
