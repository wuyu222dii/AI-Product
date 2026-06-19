import { StatusBadge } from "../StatusBadge.jsx";
import {
  formatImpactLabel,
  formatReviewStatusLabel,
  formatReviewType,
  normalizeImpactLevel,
  reviewEvidenceChecklist,
  reviewRecheckActionLabel,
  summarizeReviewBasis,
  suggestReviewAction
} from "./workspaceUtils.js";

export function WorkspaceReviewDrawer({
  review,
  onClose,
  onOpenAppeal,
  onFixReview,
  onUpdateReviewStatus,
  onRecheckReview,
  rechecking = false
}) {
  if (!review) return null;
  const isClosed = review.reviewStatus === "RESOLVED" || review.reviewStatus === "IGNORED";
  const evidenceChecklist = reviewEvidenceChecklist(review);
  const recheckActionLabel = reviewRecheckActionLabel(review);
  const recheckHistory = review.recheckHistory ?? [];

  return (
    <div className="overlay-shell" onClick={onClose}>
      <aside className="drawer-panel" onClick={(event) => event.stopPropagation()}>
        <div className="drawer-header">
          <div>
            <h3>审查详情</h3>
            <p className="muted">查看该条审查的影响范围、修正建议与后续动作。</p>
          </div>
          <button className="ghost-btn" onClick={onClose}>
            关闭
          </button>
        </div>

        <div className="drawer-body">
          <div className="detail-card">
            <strong>审查类型</strong>
            <div className="detail-row">
              <span>{formatReviewType(review.reviewType)}</span>
              <StatusBadge level={normalizeImpactLevel(review.reviewImpactLevel)}>
                {formatImpactLabel(review.reviewImpactLevel)}
              </StatusBadge>
              <span className={`review-status-pill review-status-pill--${String(review.reviewStatus || "OPEN").toLowerCase()}`}>
                {formatReviewStatusLabel(review.reviewStatus)}
              </span>
            </div>
          </div>

          {review.resolutionNote && (
            <div className="detail-card">
              <strong>处理状态</strong>
              <p className="muted">{review.resolutionNote}</p>
            </div>
          )}

          {(review.lastRecheckedAt || review.recheckNote) && (
            <div className="detail-card detail-card--basis">
              <strong>最近复查</strong>
              {review.lastRecheckedAt && (
                <p className="muted">时间：{new Date(review.lastRecheckedAt).toLocaleString()}</p>
              )}
              {review.recheckNote && <p className="muted">结论：{review.recheckNote}</p>}
              <p className="muted">建议：{recheckActionLabel}</p>
            </div>
          )}

          {recheckHistory.length > 0 && (
            <div className="detail-card">
              <strong>复查历史</strong>
              <div className="recheck-history-list">
                {recheckHistory.map((item) => (
                  <article className="recheck-history-item" key={item.id}>
                    <div className="detail-row">
                      <span className={`review-status-pill review-status-pill--${String(item.nextStatus || "OPEN").toLowerCase()}`}>
                        {item.outcome || "UNKNOWN"}
                      </span>
                      <span className="muted">{item.createdAt ? new Date(item.createdAt).toLocaleString() : "未知时间"}</span>
                    </div>
                    <p className="muted">
                      状态：{item.previousStatus || "-"} {"->"} {item.nextStatus || "-"} ｜ 影响级别：{item.previousImpactLevel || "-"} {"->"} {item.nextImpactLevel || "-"}
                    </p>
                    {item.note && <p className="muted">说明：{item.note}</p>}
                    {item.basis?.message && <p className="muted">依据快照：{item.basis.message}</p>}
                  </article>
                ))}
              </div>
            </div>
          )}

          <div className="detail-card detail-card--basis">
            <strong>判断依据</strong>
            <p className="muted">{summarizeReviewBasis(review)}</p>
          </div>

          <div className="detail-card">
            <strong>复查依据清单</strong>
            <ul className="detail-list review-evidence-checklist">
              {evidenceChecklist.map((item, index) => (
                <li key={`${item}-${index}`}>{item}</li>
              ))}
            </ul>
          </div>

          <div className="detail-card">
            <strong>问题说明</strong>
            <p className="muted">{review.message}</p>
          </div>

          {review.suggestedFix && (
            <div className="detail-card">
              <strong>建议修正</strong>
              <p className="muted">{review.suggestedFix}</p>
            </div>
          )}

          {review.targetRange && (
            <div className="detail-card">
              <strong>影响范围</strong>
              <p className="muted">
                开始位置：{review.targetRange.start ?? "-"} ｜ 结束位置：{review.targetRange.end ?? "-"}
              </p>
              {review.targetRange.selectedText && (
                <blockquote className="range-quote">{review.targetRange.selectedText}</blockquote>
              )}
            </div>
          )}

          <div className="detail-card">
            <strong>处理建议</strong>
            <p className="muted">{suggestReviewAction(review)}</p>
            <p className="muted">{recheckActionLabel}</p>
          </div>

          <div className="detail-card">
            <strong>可申诉边界</strong>
            <ul className="detail-list">
              <li>老师要求、材料证据或结构性重复可以作为申诉理由。</li>
              <li>如果只是想保留个人表达，也可以说明保留原因并请求降级。</li>
              <li>涉及事实、数据或引用真实性时，建议补充来源后再复审。</li>
            </ul>
          </div>
        </div>

        <div className="drawer-footer">
          <button className="secondary-btn" onClick={() => onOpenAppeal(review)} disabled={isClosed}>
            发起申诉
          </button>
          <button className="primary-btn" onClick={() => onFixReview(review)} disabled={isClosed}>
            按建议修改
          </button>
          <button className="secondary-btn" onClick={() => onRecheckReview(review)} disabled={rechecking}>
            {rechecking ? "复查中..." : "复查此项"}
          </button>
          {isClosed ? (
            <button className="ghost-btn" onClick={() => onUpdateReviewStatus(review, "OPEN", "重新打开继续处理")}>
              重新打开
            </button>
          ) : (
            <>
              <button className="ghost-btn" onClick={() => onUpdateReviewStatus(review, "RESOLVED", "用户确认已处理")}>
                标记已解决
              </button>
              {review.canBypass && (
                <button className="ghost-btn" onClick={() => onUpdateReviewStatus(review, "IGNORED", "用户确认暂不处理")}>
                  忽略此项
                </button>
              )}
            </>
          )}
          <button className="ghost-btn" onClick={onClose}>
            我知道了
          </button>
        </div>
      </aside>
    </div>
  );
}
