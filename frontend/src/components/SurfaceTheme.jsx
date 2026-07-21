import { useEffect } from "react";
import { useLocation } from "react-router-dom";

export function SurfaceTheme() {
  const { pathname } = useLocation();

  useEffect(() => {
    const isProduct = pathname.startsWith("/app");
    document.documentElement.dataset.surface = isProduct ? "product" : "public";
    return () => {
      delete document.documentElement.dataset.surface;
    };
  }, [pathname]);

  return null;
}
