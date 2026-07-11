import { Download, FileCheck2, RefreshCw, ShieldCheck } from "lucide-react";

export function AcademicDocumentQualityView({
  document,
  quality,
  assembled,
  busyAction,
  downloadUrl,
  onAssemble,
  onExport,
  onRefreshReview,
  onRefreshEvidence,
  onOpenSection,
  onOpenReview
}) {
  return (
    <section className="academic-document-quality">
      <header className="academic-document-quality-head">
        <div><span className="eyebrow">Whole document check</span><h3>{document?.title}</h3><p>整篇视图只用于检查与交付，正文仍按章节编辑。</p></div>
        <div className="button-row">
          <button className="ghost-btn icon-text-btn" type="button" onClick={onRefreshEvidence}><RefreshCw size={16} /> 刷新全部可信链</button>
          <button className="secondary-btn icon-text-btn" type="button" onClick={onRefreshReview}><FileCheck2 size={16} /> AI 审查整篇</button>
          <button className="ghost-btn" type="button" onClick={onAssemble}>组装预览</button>
          <button className="primary-btn icon-text-btn" type="button" onClick={onExport} disabled={busyAction === "export"}><Download size={16} /> 导出文档</button>
        </div>
      </header>

      <div className="academic-quality-overview">
        <div><span>交付质量</span><strong>{quality?.score ?? 0}</strong><small>{qualityStatus(quality?.status)}</small></div>
        <div><span>来源覆盖</span><strong>{quality?.evidence?.coverage?.coverageRatio ?? 0}%</strong><small>{quality?.evidence?.analysisState === "STALE" ? "部分结果待刷新" : "当前版本"}</small></div>
        <div><span>原创实证</span><strong>{quality?.writingRisks?.overallScore ?? 100}</strong><small>{quality?.writingRisks?.overallStatus ?? "READY"}</small></div>
        <div><span>审查项</span><strong>{quality?.reviewItems?.length ?? 0}</strong><small>点击章节继续处理</small></div>
      </div>

      <div className="academic-quality-layout">
        <section className="academic-quality-sections">
          <h4>章节检查</h4>
          {(quality?.sections ?? []).map((section) => (
            <button type="button" onClick={() => onOpenSection(section.sectionId)} key={section.sectionId}>
              <span><strong>{section.title}</strong><small>v{section.versionNo} · {section.analysisState === "STALE" ? "可信链待刷新" : "检查已同步"}</small></span>
              <span>来源 {section.evidenceCoverage}%</span><span>原创 {section.writingRiskScore}</span><span>{section.openReviewCount} 项待处理</span>
            </button>
          ))}
        </section>

        <aside className="academic-delivery-notes">
          <h4><ShieldCheck size={17} /> 交付前建议</h4>
          {(quality?.recommendations ?? []).map((item) => <p key={item}>{item}</p>)}
          {(quality?.reviewItems ?? []).slice(0, 5).map((review) => <button className="academic-review-row" type="button" onClick={() => onOpenReview(review)} key={review.id}><span><strong>{String(review.reviewType).replaceAll("_", " ")}</strong><small>{review.message}</small></span></button>)}
        </aside>
      </div>

      {assembled && <details open className="academic-assembled-preview"><summary>组装预览 · {assembled.characterCount} 字符</summary><pre>{assembled.content}</pre></details>}
      {downloadUrl && <a className="primary-btn academic-download-link" href={downloadUrl} target="_blank" rel="noreferrer"><Download size={16} /> 下载导出文件</a>}
    </section>
  );
}

function qualityStatus(value) {
  return { READY: "可进入交付确认", NEEDS_REVIEW: "建议继续处理", NEEDS_EVIDENCE: "证据不足", NEEDS_REFRESH: "检查结果待刷新" }[value] ?? value;
}
