import { useEffect, useState } from "react";
import { APPEAL_REASON_PRESETS } from "./constants.js";
import { formatImpactLabel, formatReviewType, normalizeImpactLevel, summarizeReviewBasis } from "./workspaceUtils.js";

export function WorkspaceAppealModal({ review, onClose, onSubmit, loading }) {
  const [reason, setReason] = useState("我认为这条判断不准确，请结合正文和来源重新复审。");
  const [note, setNote] = useState("");

  useEffect(() => {
    setReason("我认为这条判断不准确，请结合正文和来源重新复审。");
    setNote("");
  }, [review?.id]);

  if (!review) return null;

  return (
    <div className="overlay-shell" onClick={onClose}>
      <div className="modal-panel" onClick={(event) => event.stopPropagation()}>
        <div className="drawer-header">
          <div>
            <h3>发起申诉</h3>
            <p className="muted">如果你认为当前审查不准确，可以补充理由并请求复审。</p>
          </div>
          <button className="ghost-btn" onClick={onClose}>
            关闭
          </button>
        </div>

        <div className="drawer-body">
          <div className="detail-card">
            <strong>当前审查项</strong>
            <p className="muted">
              {formatReviewType(review.reviewType)} ｜ {formatImpactLabel(normalizeImpactLevel(review.reviewImpactLevel))}
            </p>
            <p className="muted">{review.message}</p>
          </div>

          <div className="detail-card detail-card--basis">
            <strong>原判断依据</strong>
            <p className="muted">{summarizeReviewBasis(review)}</p>
          </div>

          <div className="preset-row">
            {APPEAL_REASON_PRESETS.map((preset) => (
              <button
                type="button"
                className="preset-chip"
                key={preset}
                onClick={() => setReason(preset)}
              >
                {preset}
              </button>
            ))}
          </div>

          <div className="field">
            <label>申诉理由</label>
            <textarea value={reason} onChange={(event) => setReason(event.target.value)} />
          </div>

          <div className="field">
            <label>补充说明 / 证据</label>
            <textarea
              value={note}
              onChange={(event) => setNote(event.target.value)}
              placeholder="可补充你对正文、引用或学校、导师、课程、期刊要求的说明"
            />
          </div>
        </div>

        <div className="drawer-footer">
          <button className="ghost-btn" onClick={onClose}>
            取消
          </button>
          <button
            className="primary-btn"
            disabled={loading}
            onClick={() =>
              onSubmit({
                userReason: reason,
                evidence: {
                  note
                }
              })
            }
          >
            {loading ? "复审中..." : "提交申诉"}
          </button>
        </div>
      </div>
    </div>
  );
}
