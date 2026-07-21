import { supabase } from "./supabaseClient.js";

/** @type {Map<string, Promise<{ data: unknown, error: Error | null }>>} */
const inflightExchanges = new Map();

/**
 * Exchange a one-time PKCE auth code at most once per tab.
 * React StrictMode remounts / concurrent effects must reuse the same promise,
 * otherwise the second /token call fails with "invalid flow state".
 */
export function exchangeOAuthCode(code) {
  if (!code) {
    return Promise.resolve({ data: { session: null }, error: new Error("缺少 OAuth 授权码") });
  }
  const existing = inflightExchanges.get(code);
  if (existing) return existing;

  const pending = supabase.auth.exchangeCodeForSession(code).finally(() => {
    window.setTimeout(() => inflightExchanges.delete(code), 10_000);
  });
  inflightExchanges.set(code, pending);
  return pending;
}

export function resetOAuthExchangeForTests() {
  inflightExchanges.clear();
}
