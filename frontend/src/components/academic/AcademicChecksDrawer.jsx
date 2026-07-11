import { useState } from "react";
import { BookOpen, FileSearch, RefreshCw, ShieldCheck, Sparkles, X } from "lucide-react";

const TABS = [
  { key: "evidence", label: "可信链" },
  { key: "risks", label: "原创补强" },
  { key: "reviews", label: "审查项" }
];

export function AcademicChecksDrawer({
  open,
  onClose,
  evidence,
  risks,
  reviews,
  loading,
  onRebuildEvidence,
  onRefreshReviews,
  onUpdateEvidence,
  onPreviewMaterial,
  onInsertCitation,
  onLocate,
  onFixRisk,
  onOpenReview
}) {
  const [tab, setTab] = useState("evidence");
  if (!open) return null;

  return (
    <div className="overlay-shell" onClick={onClose}>
      <aside className="drawer-panel drawer-panel--wide academic-checks-drawer" onClick={(event) => event.stopPropagation()}>
        <header className="drawer-header">
          <div><h3>本章质量检查</h3><p className="muted">检查结果是写作辅助，不替代导师、评审或作者判断。</p></div>
          <button className="ghost-btn" type="button" onClick={onClose}><X size={17} /></button>
        </header>
        <div className="academic-drawer-tabs">
          {TABS.map((item) => <button type="button" className={tab === item.key ? "is-active" : ""} onClick={() => setTab(item.key)} key={item.key}>{item.label}</button>)}
        </div>

        <div className="drawer-body academic-checks-body">
          {loading && <p className="muted">正在读取当前章节检查结果...</p>}
          {tab === "evidence" && (
            <section>
              <div className="academic-check-summary-line">
                <div><ShieldCheck size={18} /><strong>来源覆盖 {evidence?.coverage?.coverageRatio ?? 0}%</strong></div>
                <button className="secondary-btn icon-text-btn" type="button" onClick={onRebuildEvidence}><RefreshCw size={15} /> 重建可信链</button>
              </div>
              {(evidence?.paragraphs ?? []).map((paragraph) => (
                <article className="academic-evidence-row" key={paragraph.paragraphId}>
                  <header><strong>{paragraph.paragraphId}</strong><span className={`quality-state quality-state--${paragraph.bindingStatus?.toLowerCase()}`}>{bindingLabel(paragraph.bindingStatus)}</span></header>
                  <p>{paragraph.paragraphText || "空段落"}</p>
                  {(paragraph.bindings ?? []).map((binding) => (
                    <div className="academic-evidence-source" key={binding.id}>
                      <div><BookOpen size={15} /><strong>{binding.materialTitle || "尚未绑定材料"}</strong></div>
                      <p>{binding.sourceExcerpt || "当前没有可确认的证据片段。"}</p>
                      <div className="button-row">
                        <button className="ghost-btn" type="button" onClick={() => onLocate(binding.targetRange)}>定位正文</button>
                        {binding.materialId && <button className="ghost-btn" type="button" onClick={() => onPreviewMaterial(binding)}>打开材料</button>}
                        {binding.citationText && <button className="ghost-btn" type="button" onClick={() => onInsertCitation(binding.citationText)}>插入引用</button>}
                        {binding.bindingStatus === "WEAK" && <button className="secondary-btn" type="button" onClick={() => onUpdateEvidence(binding, "USER_CONFIRMED")}>确认可信</button>}
                        {binding.bindingStatus !== "MISSING" && <button className="ghost-btn" type="button" onClick={() => onUpdateEvidence(binding, "WEAK")}>标记需补充</button>}
                      </div>
                    </div>
                  ))}
                </article>
              ))}
            </section>
          )}

          {tab === "risks" && (
            <section>
              <div className="academic-check-summary-line"><div><Sparkles size={18} /><strong>原创实证得分 {risks?.overallScore ?? 100}</strong></div></div>
              {(risks?.items ?? []).length === 0 ? <p className="muted">当前没有发现明显空泛无据段落，仍建议人工通读。</p> : (risks?.items ?? []).map((risk) => (
                <article className="academic-risk-row" key={`${risk.paragraphId}-${risk.riskType}`}>
                  <header><strong>{riskLabel(risk.riskType)}</strong><span>{risk.level === "LOCAL_FIX" ? "建议处理" : "提醒"}</span></header>
                  <p>{risk.paragraphExcerpt}</p>
                  <small>{risk.suggestedAction}</small>
                  <div className="button-row">
                    <button className="ghost-btn" type="button" onClick={() => onLocate(risk.targetRange)}>定位正文</button>
                    <button className="secondary-btn" type="button" onClick={() => onFixRisk(risk)}>用已有材料补强</button>
                  </div>
                </article>
              ))}
            </section>
          )}

          {tab === "reviews" && (
            <section>
              <div className="academic-check-summary-line">
                <div><FileSearch size={18} /><strong>{reviews?.length ?? 0} 条当前审查项</strong></div>
                <button className="secondary-btn icon-text-btn" type="button" onClick={onRefreshReviews}><RefreshCw size={15} /> AI 审查本章</button>
              </div>
              {(reviews ?? []).length === 0 ? <p className="muted">尚未执行本章 AI 审查，或当前没有待处理项。</p> : reviews.map((review) => (
                <button className="academic-review-row" type="button" onClick={() => onOpenReview(review)} key={review.id}>
                  <span><strong>{reviewLabel(review.reviewType)}</strong><small>{review.message}</small></span>
                  <span className={`quality-state quality-state--${review.analysisState === "STALE" ? "weak" : "confirmed"}`}>{review.analysisState === "STALE" ? "结果已过期" : reviewStatus(review.reviewStatus)}</span>
                </button>
              ))}
            </section>
          )}
        </div>
      </aside>
    </div>
  );
}

function bindingLabel(value) {
  return { CONFIRMED: "强证据", USER_CONFIRMED: "用户确认", WEAK: "弱绑定", MISSING: "缺来源" }[value] ?? value;
}

function riskLabel(value) {
  return { aigc_style_risk: "AI 写作味风险", generic_unsupported_claim: "空泛论证", original_evidence_missing: "原创实证不足" }[value] ?? value;
}

function reviewLabel(value) {
  return String(value || "审查问题").replaceAll("_", " ");
}

function reviewStatus(value) {
  return { OPEN: "待处理", MODIFIED_PENDING_RECHECK: "已修改待复查", RESOLVED: "已解决", IGNORED: "已忽略" }[value] ?? value;
}
