import { Activity, AlertTriangle, Database, FileCheck2, ShieldCheck } from "lucide-react";

export function AcademicInspector({
  readiness,
  evidence,
  risks,
  reviews,
  materials,
  materialLinks,
  onToggleMaterial,
  materialUpdating,
  aiActions,
  onOpenChecks
}) {
  const linkMap = new Map((materialLinks ?? []).map((item) => [item.materialId, item]));
  const implicitAll = (materialLinks ?? []).length === 0;
  const openReviews = (reviews ?? []).filter((item) => !["RESOLVED", "IGNORED", "SUPERSEDED"].includes(item.reviewStatus));
  return (
    <aside className="academic-inspector academic-inspector--unified">
      <section className="academic-inspector-block">
        <span className="academic-inspector-label"><FileCheck2 size={15} /> 本章状态</span>
        <div className={`academic-readiness-score ${readiness?.generationEligible ? "is-ready" : "is-blocked"}`}>
          <div>{readiness?.generationEligible ? <ShieldCheck size={20} /> : <AlertTriangle size={20} />}<span>{readiness?.generationEligible ? "可继续写作" : "需要补充"}</span></div>
          <strong>{readiness?.score ?? 0}</strong>
        </div>
        <div className="academic-mini-metrics">
          <span>可信链 <strong>{evidence?.coverage?.coverageRatio ?? 0}%</strong></span>
          <span>原创实证 <strong>{risks?.overallScore ?? 100}</strong></span>
          <span>待处理 <strong>{openReviews.length}</strong></span>
        </div>
        <button className="secondary-btn" type="button" onClick={onOpenChecks}>查看本章检查</button>
      </section>

      <details className="academic-inspector-block">
        <summary><Database size={15} /> 当前文档材料</summary>
        <p className="muted">{implicitAll ? "默认使用项目内全部材料。" : "仅勾选材料会进入当前文档上下文。"}</p>
        <div className="academic-material-list">
          {(materials ?? []).map((material) => {
            const checked = implicitAll || Boolean(linkMap.get(material.id)?.included);
            return <label key={material.id}><input type="checkbox" checked={checked} disabled={materialUpdating} onChange={(event) => onToggleMaterial(material, event.target.checked)} /><span><strong>{material.filename}</strong><small>{material.materialRole ?? material.effectiveMaterialCategory ?? "用途待确认"}</small></span></label>;
          })}
        </div>
      </details>

      <details className="academic-inspector-block">
        <summary><Activity size={15} /> AI 使用记录</summary>
        <div className="academic-activity-list">
          {(aiActions ?? []).slice(0, 6).map((item) => <div key={item.id}><strong>{actionLabel(item.actionType)}</strong><span>{item.createdAt ? new Date(item.createdAt).toLocaleString() : ""}</span><p>{item.outputSummary || item.requestSummary}</p></div>)}
          {(aiActions ?? []).length === 0 && <p className="muted">当前文档还没有 AI 操作记录。</p>}
        </div>
      </details>
    </aside>
  );
}

function actionLabel(value) {
  return {
    SECTION_GENERATE: "章节生成",
    SECTION_COWRITE_PREVIEW: "章节共写预览",
    SECTION_COWRITE_APPLY: "应用共写修改",
    SECTION_RESTORE: "恢复章节版本",
    DOCUMENT_ASSEMBLE: "组装整篇文档",
    DOCUMENT_EXPORT: "导出学术文档",
    SECTION_REVIEW: "AI 审查本章",
    DOCUMENT_REVIEW: "AI 审查整篇"
  }[value] ?? String(value || "AI 操作").replaceAll("_", " ");
}
