import { buildTextDiffSummary, inferVersionTags, summarizeVersionDiff } from "./workspaceUtils.js";

export function WorkspaceVersionPanel({
  versions,
  currentDraftId,
  restoring,
  onAcceptCurrent,
  onRestoreVersion
}) {
  const currentIndex = versions.findIndex((item) => item.id === currentDraftId);
  const current = currentIndex >= 0 ? versions[currentIndex] : versions[0];
  const previous = currentIndex >= 0 ? versions[currentIndex + 1] : versions[1];
  const currentTags = inferVersionTags(current, previous);
  const currentLength = current?.draftText?.length ?? 0;
  const previousLength = previous?.draftText?.length ?? 0;
  const delta = previous ? currentLength - previousLength : currentLength;
  const diffSummary = buildTextDiffSummary(current?.draftText, previous?.draftText);

  return (
    <section className="workspace-version-panel">
      <div className="panel-head panel-head-compact">
        <div>
          <h3>版本记录</h3>
          <p>对比当前版与上一版，确认后保留，或一键撤回到上一版。</p>
        </div>
      </div>
      <div className="version-layout">
        <div className="version-column">
          {versions.length === 0 ? (
            <div className="empty-state">
              <p className="muted">暂无历史版本。</p>
            </div>
          ) : (
            versions.map((item, index) => {
              const itemTags = inferVersionTags(item, versions[index + 1]);
              return (
                <article
                  className={`version-item ${item.id === currentDraftId ? "current" : ""}`}
                  key={item.id}
                >
                  <div className="version-item-head">
                    <strong>v{item.versionNo}</strong>
                    {item.id === currentDraftId && <span className="version-current-tag">当前</span>}
                  </div>
                  <p className="muted">来源：{item.createdBy}</p>
                  {item.titleSuggestion && <p className="muted version-title">{item.titleSuggestion}</p>}
                  <div className="version-tag-row">
                    {itemTags.slice(0, 3).map((tag) => (
                      <span className="version-tag" key={`${item.id}-${tag}`}>
                        {tag}
                      </span>
                    ))}
                  </div>
                </article>
              );
            })
          )}
        </div>
        <div className="version-diff-card">
          <div className="version-diff-head">
            <div>
              <strong>改前 / 改后对照</strong>
              <p className="muted">{summarizeVersionDiff(current, previous)}</p>
            </div>
            <div className="version-action-row">
              <button type="button" className="secondary-btn" disabled={!current} onClick={onAcceptCurrent}>
                保留当前版
              </button>
              <button
                type="button"
                className="ghost-btn"
                disabled={!previous || restoring}
                onClick={() => onRestoreVersion(previous)}
              >
                {restoring ? "撤回中..." : "撤回到上一版"}
              </button>
            </div>
          </div>
          <div className="version-stat-grid">
            <div>
              <span className="stat-label">当前字数</span>
              <strong>{currentLength}</strong>
            </div>
            <div>
              <span className="stat-label">相对变化</span>
              <strong>{delta >= 0 ? `+${delta}` : delta}</strong>
            </div>
          </div>
          <div className="diff-compare-grid">
            <div className="diff-column diff-column--before">
              <span className="stat-label">改前 v{previous?.versionNo ?? "-"}</span>
              <p>{previewText(previous?.draftText, "暂无上一版内容。")}</p>
            </div>
            <div className="diff-column diff-column--after">
              <span className="stat-label">改后 v{current?.versionNo ?? "-"}</span>
              <p>{previewText(current?.draftText, "暂无当前版本内容。")}</p>
            </div>
          </div>
          <div className="diff-change-list">
            <strong>主要变化</strong>
            {diffSummary.added.length === 0 && diffSummary.removed.length === 0 ? (
              <p className="muted">当前版与上一版没有明显句段差异，可能是局部措辞或格式调整。</p>
            ) : (
              <>
                {diffSummary.added.length > 0 && (
                  <div className="diff-change-group">
                    <span className="diff-label diff-label--add">新增或改写</span>
                    {diffSummary.added.map((item) => (
                      <p key={`add-${item}`}>{item}</p>
                    ))}
                  </div>
                )}
                {diffSummary.removed.length > 0 && (
                  <div className="diff-change-group">
                    <span className="diff-label diff-label--remove">删除或替换</span>
                    {diffSummary.removed.map((item) => (
                      <p key={`remove-${item}`}>{item}</p>
                    ))}
                  </div>
                )}
              </>
            )}
          </div>
          <div className="version-tag-row version-tag-row--large">
            {currentTags.map((tag) => (
              <span className="version-tag" key={tag}>
                {tag}
              </span>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

function previewText(text, fallback) {
  const normalized = String(text || "").trim();
  if (!normalized) return fallback;
  return normalized.length > 360 ? `${normalized.slice(0, 360)}...` : normalized;
}
