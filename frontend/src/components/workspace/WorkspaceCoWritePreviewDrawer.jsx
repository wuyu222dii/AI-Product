import { useEffect, useState } from "react";
import {
  buildDetailedDiffRows,
  buildCoWriteGuardrailChecks,
  buildHighlightedDiffBlocks,
  buildSentenceDiffRows,
  buildTextDiffSummary,
  explainCoWriteChange,
  findRelatedReviewsForPreview,
  formatImpactLabel,
  formatReviewType
} from "./workspaceUtils.js";

export function WorkspaceCoWritePreviewDrawer({
  preview,
  currentDraftText,
  applying = false,
  discarding = false,
  reviews = [],
  onApply,
  onApplySelectedRows,
  onDiscard
}) {
  const [selectedRows, setSelectedRows] = useState([]);

  useEffect(() => {
    if (!preview) {
      setSelectedRows([]);
      return;
    }
    setSelectedRows(buildDetailedDiffRows(preview.candidateDraftText || "", currentDraftText || ""));
  }, [preview?.id, preview?.candidateDraftText, currentDraftText]);

  if (!preview) return null;
  const diff = buildTextDiffSummary(preview.candidateDraftText || "", currentDraftText || "");
  const guardrails = preview.diffSummary?.guardrails || preview.controls || {};
  const guardrailChecks = buildCoWriteGuardrailChecks(preview, currentDraftText);
  const sentenceDiffRows = buildSentenceDiffRows(preview.candidateDraftText || "", currentDraftText || "");
  const currentBlocks = buildHighlightedDiffBlocks(currentDraftText || "", preview.candidateDraftText || "", "current");
  const candidateBlocks = buildHighlightedDiffBlocks(preview.candidateDraftText || "", currentDraftText || "", "candidate");
  const relatedReviews = findRelatedReviewsForPreview(preview, reviews, currentDraftText);
  const lengthDelta = Number(preview.diffSummary?.lengthDelta ?? 0);
  const selectedCount = selectedRows.filter((row) => row.selected).length;

  function toggleRow(rowId) {
    setSelectedRows((rows) => rows.map((row) => (row.id === rowId ? { ...row, selected: !row.selected } : row)));
  }

  return (
    <div className="overlay-shell" onClick={onDiscard}>
      <aside className="drawer-panel drawer-panel--wide" onClick={(event) => event.stopPropagation()}>
        <div className="drawer-header">
          <div>
            <h3>AI 修改预览</h3>
            <p className="muted">本次修改尚未写入正文。确认后才会应用为新版本。</p>
          </div>
          <button className="ghost-btn" onClick={onDiscard} disabled={discarding || applying}>
            关闭
          </button>
        </div>

        <div className="drawer-body">
          <div className="preview-summary-grid">
            <div className="detail-card">
              <strong>修改动作</strong>
              <p className="muted">{preview.action?.replace(/_/g, " ") || "自定义共写"}</p>
            </div>
            <div className="detail-card">
              <strong>长度变化</strong>
              <p className="muted">{lengthDelta >= 0 ? `增加 ${lengthDelta} 字符` : `减少 ${Math.abs(lengthDelta)} 字符`}</p>
            </div>
            <div className="detail-card">
              <strong>处理范围</strong>
              <p className="muted">{preview.targetRange?.mode === "selection" ? `局部 ${preview.targetRange.start}-${preview.targetRange.end}` : "全文"}</p>
            </div>
          </div>

          {preview.instruction && (
            <div className="detail-card">
              <strong>用户指令</strong>
              <p className="muted">{preview.instruction}</p>
            </div>
          )}

          <div className="detail-card">
            <strong>保留约束检查</strong>
            <div className="guardrail-grid guardrail-grid--readonly">
              <span className={guardrails.keepCitations ? "guardrail-chip active" : "guardrail-chip"}>保留引用</span>
              <span className={guardrails.keepData ? "guardrail-chip active" : "guardrail-chip"}>不改数据</span>
              <span className={guardrails.noNewSources ? "guardrail-chip active" : "guardrail-chip"}>不新增文献</span>
              <span className={guardrails.keepStudentVoice ? "guardrail-chip active" : "guardrail-chip"}>保留学生表达</span>
              <span className="guardrail-chip">力度：{guardrails.rewriteDepth || preview.controls?.rewriteDepth || "balanced"}</span>
            </div>
            <div className="guardrail-check-list">
              {guardrailChecks.map((check) => (
                <div className={`guardrail-check guardrail-check--${check.passed ? "pass" : "warn"}`} key={check.key}>
                  <strong>{check.enabled ? check.label : `${check.label}（未启用）`}</strong>
                  <p>{check.detail}</p>
                </div>
              ))}
            </div>
          </div>

          <div className="preview-diff-grid">
            <div className="detail-card preview-text-card">
              <strong>当前正文</strong>
              <DiffBlockPreview blocks={currentBlocks} emptyText="暂无正文" />
            </div>
            <div className="detail-card preview-text-card preview-text-card--candidate">
              <strong>AI 修改后</strong>
              <DiffBlockPreview blocks={candidateBlocks} emptyText="暂无候选正文" />
            </div>
          </div>

          <div className="detail-card">
            <strong>可能关联的审查项</strong>
            {relatedReviews.length === 0 ? (
              <p className="muted">暂未发现与本次预览高度相关的待处理审查项。应用后仍可手动复查需要确认的问题。</p>
            ) : (
              <div className="related-review-list">
                {relatedReviews.map((review) => (
                  <div className="related-review-card" key={review.id}>
                    <div>
                      <strong>{formatReviewType(review.reviewType)}</strong>
                      <p className="muted">{review.message || review.suggestedFix || "该审查项需要人工确认。"}</p>
                      <p className="muted">{review.relationReason}</p>
                    </div>
                    <span className={`status-badge ${review.reviewImpactLevel || "notice"}`}>
                      {formatImpactLabel(review.reviewImpactLevel)}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="detail-card">
            <strong>差异摘要</strong>
            {diff.added.length > 0 && <p className="muted"><strong>新增：</strong>{diff.added.join(" / ")}</p>}
            {diff.removed.length > 0 && <p className="muted"><strong>删除：</strong>{diff.removed.join(" / ")}</p>}
            <p className="muted">文本保留比例约 {Math.round((diff.unchangedRatio || 0) * 100)}%。应用后会自动刷新审查项和材料可信链。</p>
          </div>

          <div className="detail-card">
            <strong>逐句差异预览</strong>
            {sentenceDiffRows.length === 0 ? (
              <p className="muted">没有检测到明显的句级新增或删除，可能主要是局部措辞调整。</p>
            ) : (
              <div className="sentence-diff-list">
                {sentenceDiffRows.map((row, index) => (
                  <div className={`sentence-diff-row sentence-diff-row--${row.type}`} key={`${row.type}-${index}`}>
                    <span>{row.label}</span>
                    <div>
                      <p>{row.text}</p>
                      <small>{explainCoWriteChange(row, preview)}</small>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="detail-card">
            <strong>局部接受</strong>
            <p className="muted">勾选你想保留的差异后，可以先应用到编辑区，再手动保存正文。删除类差异默认不勾选，避免误删。</p>
            {selectedRows.length === 0 ? (
              <p className="muted">当前没有可单独接受的句级差异。</p>
            ) : (
              <div className="selective-diff-list">
                {selectedRows.map((row) => (
                  <label className={`selective-diff-row selective-diff-row--${row.type}`} key={row.id}>
                    <input type="checkbox" checked={row.selected} onChange={() => toggleRow(row.id)} />
                    <span>{row.label}</span>
                    <div>
                      <p>{row.text}</p>
                      <small>{explainCoWriteChange(row, preview)}</small>
                    </div>
                  </label>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="drawer-footer">
          <button className="secondary-btn" onClick={onDiscard} disabled={discarding || applying}>
            {discarding ? "放弃中..." : "放弃预览"}
          </button>
          <button
            className="secondary-btn"
            onClick={() => onApplySelectedRows?.(selectedRows)}
            disabled={applying || discarding || selectedCount === 0}
          >
            应用选中差异（{selectedCount}）
          </button>
          <button className="primary-btn" onClick={onApply} disabled={applying || discarding}>
            {applying ? "应用中..." : "应用为新版本"}
          </button>
        </div>
      </aside>
    </div>
  );
}

function DiffBlockPreview({ blocks, emptyText }) {
  if (!blocks || blocks.length === 0) {
    return <pre>{emptyText}</pre>;
  }
  return (
    <div className="diff-highlight-preview">
      {blocks.map((block) => (
        <span className={`diff-highlight diff-highlight--${block.type}`} key={block.id}>
          {block.text}
        </span>
      ))}
    </div>
  );
}
