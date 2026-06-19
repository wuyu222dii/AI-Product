import { countMustConfirm } from "./workspaceUtils.js";

export function WorkspaceHeader({ workspace, draft, reviews, onGoExport, onOpenReviews }) {
  const mustConfirmCount = countMustConfirm(reviews);

  return (
    <header className="workspace-header">
      <div className="workspace-header-main">
        <div className="workspace-header-title">
          <h2>{draft?.titleSuggestion || workspace?.title || "共写工作台"}</h2>
          <div className="workspace-header-meta">
            <span className="meta-chip meta-chip--accent">v{draft?.versionNo ?? "-"}</span>
            <span className="meta-chip">生成状态：{draft?.generationStatus ?? "unknown"}</span>
            {mustConfirmCount > 0 && (
              <span className="meta-chip meta-chip--warn">{mustConfirmCount} 项必须确认</span>
            )}
          </div>
        </div>
        <div className="workspace-header-snapshot">
          <span className="snapshot-label">Requirement Snapshot</span>
          <p className="snapshot-summary">字数、引用格式与截止时间已建立，可在材料检查页查看完整基准。</p>
        </div>
      </div>
      <div className="workspace-header-actions">
        <button type="button" className="ghost-btn" onClick={onOpenReviews}>
          审查列表
        </button>
        <button type="button" className="primary-btn" onClick={onGoExport}>
          导出定稿
        </button>
      </div>
    </header>
  );
}
