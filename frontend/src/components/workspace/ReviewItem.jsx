import { StatusBadge } from "../StatusBadge.jsx";
import { formatImpactLabel, formatReviewStatusLabel, formatReviewType, normalizeImpactLevel } from "./workspaceUtils.js";

export function ReviewItem({ review, appealResult, onOpenReview, onOpenAppeal, onFixReview, onUpdateReviewStatus }) {
  const level = normalizeImpactLevel(review.reviewImpactLevel);
  const isClosed = review.reviewStatus === "RESOLVED" || review.reviewStatus === "IGNORED";

  return (
    <article className={`review-item review-item--${level} ${isClosed ? "review-item--closed" : ""}`}>
      <div className="review-head">
        <strong>{formatReviewType(review.reviewType)}</strong>
        <div className="review-badge-row">
          <StatusBadge level={level}>{formatImpactLabel(review.reviewImpactLevel)}</StatusBadge>
          <span className={`review-status-pill review-status-pill--${String(review.reviewStatus || "OPEN").toLowerCase()}`}>
            {formatReviewStatusLabel(review.reviewStatus)}
          </span>
        </div>
      </div>
      <p className="review-message">{review.message}</p>
      {review.suggestedFix && <p className="review-fix">建议：{review.suggestedFix}</p>}
      {review.targetRange && (review.targetRange.start !== undefined || review.targetRange.end !== undefined) && (
        <p className="review-range">
          影响范围：字符 {review.targetRange.start ?? "-"} – {review.targetRange.end ?? "-"}
        </p>
      )}
      {review.resolutionNote && <p className="review-resolution">处理说明：{review.resolutionNote}</p>}
      {appealResult && <p className="review-appeal-result">复审结果：{appealResult}</p>}
      <div className="review-actions">
        <button type="button" className="secondary-btn" onClick={() => onOpenReview(review)}>
          查看详情
        </button>
        <button type="button" className="primary-btn" onClick={() => onFixReview(review)} disabled={isClosed}>
          按建议修改
        </button>
        <button type="button" className="ghost-btn" onClick={() => onOpenAppeal(review)} disabled={isClosed}>
          发起申诉
        </button>
        {isClosed ? (
          <button type="button" className="ghost-btn" onClick={() => onUpdateReviewStatus(review, "OPEN", "重新打开继续处理")}>
            重新打开
          </button>
        ) : (
          <>
            <button type="button" className="ghost-btn" onClick={() => onUpdateReviewStatus(review, "RESOLVED", "用户确认已处理")}>
              标记已解决
            </button>
            {review.canBypass && (
              <button type="button" className="ghost-btn" onClick={() => onUpdateReviewStatus(review, "IGNORED", "用户确认暂不处理")}>
                忽略此项
              </button>
            )}
          </>
        )}
      </div>
    </article>
  );
}
