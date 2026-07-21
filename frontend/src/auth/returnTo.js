export function safeReturnTo(value, fallback = "/app/projects") {
  if (typeof value !== "string") return fallback;
  if (!value.startsWith("/app") || value.startsWith("//") || value.includes("\\")) return fallback;
  try {
    const parsed = new URL(value, window.location.origin);
    return parsed.origin === window.location.origin && parsed.pathname.startsWith("/app")
      ? `${parsed.pathname}${parsed.search}${parsed.hash}`
      : fallback;
  } catch {
    return fallback;
  }
}
