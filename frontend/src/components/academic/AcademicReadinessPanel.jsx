import { Activity, AlertTriangle, Check, Database, FileCheck2, ShieldCheck } from "lucide-react";

const TABS = [
  { key: "readiness", label: "准备度", icon: FileCheck2 },
  { key: "materials", label: "材料", icon: Database },
  { key: "activity", label: "AI 记录", icon: Activity }
];

export function AcademicReadinessPanel({
  activeTab,
  onTabChange,
  readiness,
  materials,
  materialLinks,
  onToggleMaterial,
  aiActions,
  materialUpdating
}) {
  return (
    <aside className="academic-inspector">
      <div className="academic-inspector-tabs">
        {TABS.map(({ key, label, icon: Icon }) => (
          <button type="button" className={activeTab === key ? "is-active" : ""} onClick={() => onTabChange(key)} key={key}>
            <Icon size={15} aria-hidden="true" />
            {label}
          </button>
        ))}
      </div>

      {activeTab === "readiness" && <ReadinessView readiness={readiness} />}
      {activeTab === "materials" && (
        <MaterialsView
          materials={materials}
          materialLinks={materialLinks}
          onToggleMaterial={onToggleMaterial}
          updating={materialUpdating}
        />
      )}
      {activeTab === "activity" && <ActivityView items={aiActions} />}
    </aside>
  );
}

function ReadinessView({ readiness }) {
  if (!readiness) return <p className="academic-empty-copy">正在计算当前文档的材料与章节准备度。</p>;
  const isReady = readiness.generationEligible;
  return (
    <div className="academic-inspector-body">
      <div className={`academic-readiness-score ${isReady ? "is-ready" : "is-blocked"}`}>
        <div>
          {isReady ? <ShieldCheck size={22} aria-hidden="true" /> : <AlertTriangle size={22} aria-hidden="true" />}
          <span>{readiness.status === "READY" ? "可以按章节写作" : readiness.status === "NEEDS_CONFIRMATION" ? "建议先确认" : "需要补充材料"}</span>
        </div>
        <strong>{readiness.score}</strong>
      </div>
      <p className="academic-next-action">{readiness.nextAction}</p>
      <div className="academic-coverage-list">
        {Object.entries(readiness.artifactCoverage ?? {}).map(([key, value]) => (
          <span className={value ? "is-covered" : ""} key={key}>
            {value && <Check size={13} aria-hidden="true" />}
            {coverageLabel(key)}
          </span>
        ))}
      </div>
      <div className="academic-issue-list">
        {(readiness.issues ?? []).map((issue) => (
          <div className={`academic-issue academic-issue--${issue.level.toLowerCase()}`} key={issue.code}>
            <strong>{issue.label}</strong>
            <p>{issue.message}</p>
            <span>{issue.suggestedAction}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function MaterialsView({ materials, materialLinks, onToggleMaterial, updating }) {
  const linkMap = new Map(materialLinks.map((item) => [item.materialId, item]));
  const implicitAll = materialLinks.length === 0;
  return (
    <div className="academic-inspector-body">
      <p className="academic-next-action">
        {implicitAll ? "当前默认使用项目内全部材料。取消任一材料后，将切换为当前文档专属材料集。" : "仅勾选材料会进入该文档的知识检索、章节生成和共写上下文。"}
      </p>
      <div className="academic-material-list">
        {materials.map((material) => {
          const link = linkMap.get(material.id);
          const checked = implicitAll || Boolean(link?.included);
          return (
            <label key={material.id}>
              <input
                type="checkbox"
                checked={checked}
                disabled={materialUpdating}
                onChange={(event) => onToggleMaterial(material, event.target.checked)}
              />
              <span>
                <strong>{material.filename}</strong>
                <small>{material.materialRole ?? material.effectiveMaterialCategory ?? "用途待确认"} · {material.parseStage}</small>
              </span>
            </label>
          );
        })}
        {materials.length === 0 && <p className="academic-empty-copy">项目中还没有材料，请先去上传并完成 AI 解析。</p>}
      </div>
    </div>
  );
}

function ActivityView({ items }) {
  return (
    <div className="academic-inspector-body">
      <p className="academic-next-action">记录 AI 对哪个章节做了什么、使用了哪些材料，以及用户是否采纳，便于人工复核和使用披露。</p>
      <div className="academic-activity-list">
        {items.map((item) => (
          <div key={item.id}>
            <strong>{actionLabel(item.actionType)}</strong>
            <span>{formatDate(item.createdAt)} · {item.evidenceMaterialIds?.length ?? 0} 份材料</span>
            <p>{item.outputSummary || item.requestSummary || "已记录本次 AI 操作"}</p>
          </div>
        ))}
        {items.length === 0 && <p className="academic-empty-copy">当前文档还没有 AI 操作记录。</p>}
      </div>
    </div>
  );
}

function coverageLabel(key) {
  return {
    parsedKeyMaterial: "核心材料",
    literature: "可引用文献",
    submissionRequirement: "写作要求",
    researchArtifact: "研究成果",
    authorDraft: "作者草稿"
  }[key] ?? key;
}

function actionLabel(value) {
  return {
    SECTION_GENERATE: "章节生成",
    SECTION_COWRITE_PREVIEW: "章节共写预览"
  }[value] ?? value;
}

function formatDate(value) {
  if (!value) return "时间未知";
  return new Intl.DateTimeFormat("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" }).format(new Date(value));
}
