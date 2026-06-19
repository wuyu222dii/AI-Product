export function ErrorBanner({ message, onClose }) {
  return (
    <div className="error-banner">
      <span>{message}</span>
      <button className="ghost-btn" onClick={onClose}>
        关闭
      </button>
    </div>
  );
}
