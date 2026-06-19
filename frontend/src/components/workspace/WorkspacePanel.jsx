export function WorkspacePanel({ title, subtitle, className = "", children, compact = false }) {
  return (
    <section className={`workspace-panel ${className}`.trim()}>
      {(title || subtitle) && (
        <div className={`panel-head ${compact ? "panel-head-compact" : ""}`}>
          {title && <h3>{title}</h3>}
          {subtitle && <p>{subtitle}</p>}
        </div>
      )}
      {children}
    </section>
  );
}
