import { createClient } from "@supabase/supabase-js";

const configuredUrl = import.meta.env.VITE_SUPABASE_URL;
const configuredKey = import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY;

export const isSupabaseConfigured = Boolean(configuredUrl && configuredKey);

export const supabase = createClient(
  configuredUrl || "http://127.0.0.1:54321",
  configuredKey || "supabase-publishable-key-not-configured",
  {
    auth: {
      flowType: "pkce",
      persistSession: true,
      autoRefreshToken: true,
      // Dedicated /auth/callback page calls exchangeCodeForSession once.
      // Leaving detectSessionInUrl true races that exchange and burns the one-time PKCE code.
      detectSessionInUrl: false
    }
  }
);
