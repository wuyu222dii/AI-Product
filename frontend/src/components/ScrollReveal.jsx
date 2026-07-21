import { useEffect, useRef, useState } from "react";

export function ScrollReveal({
  as: Component = "div",
  className = "",
  children,
  delay,
  direction = "up",
  threshold = 0.2,
  rootMargin = "0px 0px -10% 0px",
}) {
  const ref = useRef(null);
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      setIsVisible(true);
      return undefined;
    }

    const node = ref.current;
    if (!node) return undefined;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry.isIntersecting) return;
        setIsVisible(true);
        observer.disconnect();
      },
      { threshold, rootMargin },
    );

    observer.observe(node);
    return () => observer.disconnect();
  }, [rootMargin, threshold]);

  const classes = [
    "scroll-reveal",
    `scroll-reveal--${direction}`,
    isVisible ? "is-visible" : "",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <Component
      ref={ref}
      className={classes}
      style={delay != null ? { transitionDelay: `${delay}ms` } : undefined}
    >
      {children}
    </Component>
  );
}
