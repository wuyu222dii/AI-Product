import { useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  ArrowRight,
  BookOpenText,
  Check,
  CircleDot,
  FileSearch,
  FileText,
  FolderKanban,
  Library,
  Settings2,
  ShieldCheck,
  Upload
} from "lucide-react";
import { api } from "../services/api.js";
import {
  DOCUMENT_TYPE_LABELS,
  PARADIGM_LABELS,
  STAGE_LABELS
} from "../components/academic/academicOptions.js";
import {
  GUIDE_MATERIAL_OPTIONS,
  GUIDE_MODE_OPTIONS,
  GUIDE_PROGRESS_OPTIONS,
  GUIDE_STATUS_LABELS
} from "../components/guide/guideOptions.js";

const TASK_ICONS = {
  project_setup: FolderKanban,
  materials: Upload,
  parsing: FileSearch,
  readiness: ShieldCheck,
  knowledge: Library,
  writing: BookOpenText,
  review_delivery: FileText
};

export function ProjectOverviewPage({ workspace, onNavigate, onError }) {
  const [materials, setMaterials] = useState([]);
  const [documents, setDocuments] = useState([]);
  const [knowledgeChunks, setKnowledgeChunks] = useState([]);
  const [guide, setGuide] = useState(null);
  const [guideDraft, setGuideDraft] = useState(null);
  const [showGuideSettings, setShowGuideSettings] = useState(false);
  const [savingGuide, setSavingGuide] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    setLoading(true);
    Promise.allSettled([
      api.listMaterials(workspace.id),
      api.listAcademicDocuments(workspace.id),
      api.listKnowledgeChunks(workspace.id),
      api.getProjectGuide(workspace.id)
    ])
      .then(([materialResult, documentResult, chunkResult, guideResult]) => {
        if (!active) return;
        if (materialResult.status === "fulfilled") setMaterials(materialResult.value.items ?? []);
        if (documentResult.status === "fulfilled") setDocuments(documentResult.value ?? []);
        if (chunkResult.status === "fulfilled") setKnowledgeChunks(chunkResult.value.items ?? []);
        if (guideResult.status === "fulfilled") {
          setGuide(guideResult.value);
          setGuideDraft(toGuideDraft(guideResult.value));
        }
        const failedResult = [materialResult, documentResult, chunkResult, guideResult]
          .find((result) => result.status === "rejected");
        if (failedResult) onError(`项目概览有部分状态未能读取：${failedResult.reason?.message || "请稍后重试"}`);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [workspace.id, onError]);

  const summary = useMemo(() => {
    const parsedCount = materials.filter((item) => normalize(item.parseStage) === "AI_PARSED").length;
    const keyMaterials = materials.filter((item) => item.isKeyMaterial);
    const gateMaterials = keyMaterials.length > 0 ? keyMaterials : materials;
    const blockingCount = gateMaterials.filter((item) => {
      return ["NEEDS_SUPPLEMENT", "FAILED"].includes(normalize(item.parseQuality?.status));
    }).length;
    const sectionCount = documents.reduce((total, item) => total + Number(item.sectionCount ?? 0), 0);
    return {
      materialCount: materials.length,
      parsedCount,
      blockingCount,
      documentCount: documents.length,
      sectionCount,
      knowledgeCount: knowledgeChunks.length
    };
  }, [documents, knowledgeChunks, materials]);

  const tasks = guide?.tasks?.length ? guide.tasks : fallbackTasks(workspace.id, summary);
  const currentTask = tasks.find((item) => item.id === guide?.currentTaskId)
    ?? tasks.find((item) => item.status === "NEEDS_ATTENTION")
    ?? tasks.find((item) => item.status === "CURRENT")
    ?? tasks[0];
  const NextActionIcon = TASK_ICONS[currentTask?.id] ?? BookOpenText;
  const profile = workspace.academicProfile ?? {};

  function updateGuideField(key, value) {
    setGuideDraft((current) => ({ ...current, [key]: value }));
  }

  function toggleGuideMaterial(value) {
    setGuideDraft((current) => {
      const available = current.availableMaterials ?? [];
      if (value === "NONE") {
        return { ...current, availableMaterials: available.includes("NONE") ? [] : ["NONE"] };
      }
      const withoutNone = available.filter((item) => item !== "NONE");
      return {
        ...current,
        availableMaterials: withoutNone.includes(value)
          ? withoutNone.filter((item) => item !== value)
          : [...withoutNone, value]
      };
    });
  }

  async function saveGuide() {
    try {
      setSavingGuide(true);
      const updated = await api.updateProjectGuide(workspace.id, {
        ...guideDraft,
        targetDeadline: guideDraft.targetDeadline || null
      });
      setGuide(updated);
      setGuideDraft(toGuideDraft(updated));
      setShowGuideSettings(false);
    } catch (error) {
      onError(error.message);
    } finally {
      setSavingGuide(false);
    }
  }

  return (
    <section className="project-overview">
      <header className="project-overview-head">
        <div>
          <span className="eyebrow">Project overview</span>
          <h1>{workspace.title}</h1>
          <p>查看当前研究进度、材料状态和下一项建议任务；所有路线建议都可以按你的习惯调整。</p>
        </div>
        <div className="button-row">
          <button className="secondary-btn" type="button" onClick={() => onNavigate("upload")}><Upload size={16} />补充材料</button>
          <button className="primary-btn" type="button" onClick={() => currentTask && onNavigate(currentTask.targetPath)}>
            {currentTask?.status === "NEEDS_ATTENTION" ? "处理当前问题" : "继续当前任务"}<ArrowRight size={16} />
          </button>
        </div>
      </header>

      <section className={`project-next-step ${currentTask?.status === "NEEDS_ATTENTION" ? "is-attention" : ""}`} aria-labelledby="project-next-step-title">
        <div className="project-next-step-icon"><NextActionIcon size={22} /></div>
        <div>
          <span>{currentTask?.status === "NEEDS_ATTENTION" ? "需要先处理" : "建议下一步"}</span>
          <h2 id="project-next-step-title">{currentTask?.title ?? "查看项目路线"}</h2>
          <p>{currentTask?.description ?? "系统正在计算当前项目状态。"}</p>
        </div>
        <button className="text-button" type="button" onClick={() => currentTask && onNavigate(currentTask.targetPath)}>前往处理<ArrowRight size={15} /></button>
      </section>

      <div className="project-overview-metrics" aria-label="项目状态摘要">
        <OverviewMetric label="路线进度" value={loading ? "-" : `${guide?.overallProgress ?? 0}%`} detail={guide?.preferredMode === "GUIDED" ? "当前使用引导模式" : "当前使用自由模式"} />
        <OverviewMetric label="研究材料" value={loading ? "-" : summary.materialCount} detail={`${summary.parsedCount} 份已完成解析`} />
        <OverviewMetric label="知识片段" value={loading ? "-" : summary.knowledgeCount} detail="来自当前项目真实材料" />
        <OverviewMetric label="关键问题" value={loading ? "-" : summary.blockingCount} detail={summary.blockingCount > 0 ? "建议先完成补充" : "暂未发现解析阻断项"} warning={summary.blockingCount > 0} />
      </div>

      <div className="project-overview-grid">
        <section className="project-overview-panel project-guide-panel" aria-labelledby="project-route-title">
          <div className="project-overview-panel-head">
            <div><span className="eyebrow">Personalized route</span><h2 id="project-route-title">个性化项目路线</h2></div>
            <button className="secondary-btn compact-btn" type="button" onClick={() => setShowGuideSettings((value) => !value)}><Settings2 size={15} />调整路线</button>
          </div>
          <div className="project-route-list">
            {tasks.map((task, index) => (
              <GuideTask key={task.id} task={task} index={index} current={task.id === currentTask?.id} onNavigate={onNavigate} />
            ))}
          </div>
          {showGuideSettings && guideDraft && (
            <div className="project-guide-settings">
              <div className="project-guide-settings-head"><div><strong>调整路线依据</strong><p>修改后会重新计算建议任务，不会改变已有材料和正文。</p></div><button className="text-button" type="button" onClick={() => setShowGuideSettings(false)}>取消</button></div>
              <div className="project-guide-settings-grid">
                <label className="field"><span>当前进度</span><select value={guideDraft.currentProgress} onChange={(event) => updateGuideField("currentProgress", event.target.value)}>{GUIDE_PROGRESS_OPTIONS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label>
                <label className="field"><span>截止日期</span><input type="date" value={guideDraft.targetDeadline} onChange={(event) => updateGuideField("targetDeadline", event.target.value)} /></label>
                <label className="field"><span>推进方式</span><select value={guideDraft.preferredMode} onChange={(event) => updateGuideField("preferredMode", event.target.value)}>{GUIDE_MODE_OPTIONS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label>
              </div>
              <fieldset className="project-guide-materials"><legend>手头已有内容</legend>{GUIDE_MATERIAL_OPTIONS.map((item) => <label key={item.value}><input type="checkbox" checked={guideDraft.availableMaterials.includes(item.value)} onChange={() => toggleGuideMaterial(item.value)} /><span>{item.label}</span></label>)}</fieldset>
              <div className="project-guide-settings-footer"><button className="primary-btn" type="button" disabled={savingGuide} onClick={saveGuide}>{savingGuide ? "正在保存..." : "保存并重新计算"}</button></div>
            </div>
          )}
        </section>

        <aside className="project-overview-panel project-profile-summary" aria-labelledby="project-profile-title">
          <div className="project-overview-panel-head"><div><span className="eyebrow">Academic profile</span><h2 id="project-profile-title">项目画像</h2></div></div>
          <dl>
            <div><dt>学术阶段</dt><dd>{STAGE_LABELS[profile.academicStage] ?? "待确认"}</dd></div>
            <div><dt>研究范式</dt><dd>{PARADIGM_LABELS[profile.researchParadigm] ?? "待确认"}</dd></div>
            <div><dt>机构</dt><dd>{profile.institution || "未设置"}</dd></div>
            <div><dt>默认引用</dt><dd>{profile.defaultCitationStyle || "APA"}</dd></div>
            <div><dt>目标日期</dt><dd>{guide?.targetDeadline || "未设置"}</dd></div>
          </dl>
          <p className="project-profile-note">项目画像用于调整章节模板与材料要求，真实学校、导师或期刊规范始终优先。</p>
          <button className="secondary-btn" type="button" onClick={() => onNavigate("documents")}>在学术文档中管理画像</button>
        </aside>
      </div>

      <section className="project-overview-panel" aria-labelledby="recent-documents-title">
        <div className="project-overview-panel-head"><div><span className="eyebrow">Documents</span><h2 id="recent-documents-title">学术文档</h2></div><button className="text-button" type="button" onClick={() => onNavigate("documents")}>查看全部<ArrowRight size={15} /></button></div>
        {documents.length === 0 ? (
          <div className="project-overview-empty"><FileText size={22} /><p>当前还没有学术文档，可进入工作台创建。</p></div>
        ) : (
          <div className="project-document-list">
            {documents.slice(0, 4).map((document) => (
              <button type="button" className="project-document-row" key={document.id} onClick={() => onNavigate(`documents/${document.id}`)}>
                <FileText size={18} /><span><strong>{document.title}</strong><small>{DOCUMENT_TYPE_LABELS[document.documentType] ?? document.documentType}</small></span><span>{document.sectionCount ?? 0} 个章节</span><span>{formatDate(document.updatedAt)}</span><ArrowRight size={16} />
              </button>
            ))}
          </div>
        )}
      </section>
    </section>
  );
}

function GuideTask({ task, index, current, onNavigate }) {
  const Icon = TASK_ICONS[task.id] ?? FileText;
  const complete = task.status === "COMPLETED";
  const attention = task.status === "NEEDS_ATTENTION";
  return (
    <article className={`project-route-item is-${String(task.status).toLowerCase()} ${current ? "is-current" : ""}`}>
      <span className={`project-route-state ${complete ? "is-complete" : attention ? "is-attention" : ""}`}>
        {complete ? <Check size={15} /> : attention ? <AlertTriangle size={14} /> : current ? <CircleDot size={14} /> : index + 1}
      </span>
      <Icon size={19} />
      <div className="project-route-copy">
        <span className="project-route-phase">{task.phase ?? fallbackPhase(task.id)}</span>
        <span className="project-route-title-line"><strong>{task.title}</strong><small className={`guide-status is-${String(task.status).toLowerCase()}`}>{GUIDE_STATUS_LABELS[task.status] ?? task.status}</small></span>
        <p>{task.description}</p>
        <details><summary>查看任务依据与预期产出</summary><dl><div><dt>为什么需要</dt><dd>{task.reason}</dd></div><div><dt>预期产出</dt><dd>{task.expectedOutcome}</dd></div></dl></details>
      </div>
      <span className="project-route-meta">{task.progressLabel}</span>
      <button className="project-route-action" type="button" onClick={() => onNavigate(task.targetPath)} aria-label={`前往${task.title}`}><ArrowRight size={16} /></button>
    </article>
  );
}

function OverviewMetric({ label, value, detail, warning = false }) {
  return <div className={`project-overview-metric ${warning ? "is-warning" : ""}`}><span>{label}</span><strong>{value}</strong><small>{detail}</small></div>;
}

function toGuideDraft(guide) {
  return {
    currentProgress: guide.currentProgress ?? "IDEA_ONLY",
    availableMaterials: guide.availableMaterials ?? [],
    targetDeadline: guide.targetDeadline ?? "",
    preferredMode: guide.preferredMode ?? "FLEXIBLE"
  };
}

function fallbackTasks(workspaceId, summary) {
  const root = `/app/projects/${workspaceId}`;
  return [
    { id: "project_setup", phase: "研究准备", title: "建立研究项目", description: "确认研究目标与文档类型。", reason: "项目画像决定后续建议。", expectedOutcome: "形成项目画像。", status: "COMPLETED", targetPath: root, progressLabel: "已建立" },
    { id: "materials", phase: "研究准备", title: "添加研究材料", description: "上传要求、文献、数据、笔记或草稿。", reason: "写作需要真实输入。", expectedOutcome: "材料进入解析。", status: summary.materialCount ? "COMPLETED" : "CURRENT", targetPath: `${root}/upload`, progressLabel: `${summary.materialCount} 份材料` },
    { id: "parsing", phase: "研究准备", title: "确认材料解析", description: "检查 AI 是否准确理解材料。", reason: "避免错误传入后续写作。", expectedOutcome: "获得可确认的解析结果。", status: summary.blockingCount ? "NEEDS_ATTENTION" : summary.parsedCount === summary.materialCount && summary.materialCount ? "COMPLETED" : "UPCOMING", targetPath: `${root}/parsing`, progressLabel: `${summary.parsedCount}/${summary.materialCount} 已解析` },
    { id: "readiness", phase: "研究准备", title: "检查写作准备度", description: "判断材料能否支撑当前文档。", reason: "提前发现材料缺口。", expectedOutcome: "得到待补材料清单。", status: "UPCOMING", targetPath: `${root}/materials`, progressLabel: "等待检查" },
    { id: "knowledge", phase: "研究资产", title: "构建项目知识库", description: "整理为可检索证据片段。", reason: "帮助快速定位依据。", expectedOutcome: "形成知识资产。", status: summary.knowledgeCount ? "COMPLETED" : "OPTIONAL", targetPath: `${root}/knowledge`, progressLabel: `${summary.knowledgeCount} 个片段` },
    { id: "writing", phase: "写作交付", title: "推进章节写作", description: "生成、编辑与保存章节版本。", reason: "章节是正文唯一可编辑来源。", expectedOutcome: "形成章节正文。", status: summary.sectionCount ? "IN_PROGRESS" : "UPCOMING", targetPath: `${root}/documents`, progressLabel: `${summary.sectionCount} 个章节` },
    { id: "review_delivery", phase: "写作交付", title: "审查并交付", description: "检查证据、引用和未解决问题。", reason: "交付前需要人工确认。", expectedOutcome: "得到可导出的完整文档。", status: "UPCOMING", targetPath: `${root}/documents`, progressLabel: "等待整篇检查" }
  ];
}

function fallbackPhase(taskId) {
  if (taskId === "knowledge") return "研究资产";
  if (["writing", "review_delivery"].includes(taskId)) return "写作交付";
  return "研究准备";
}

function normalize(value) {
  return String(value ?? "").trim().toUpperCase();
}

function formatDate(value) {
  if (!value) return "更新时间未知";
  return new Intl.DateTimeFormat("zh-CN", { month: "2-digit", day: "2-digit" }).format(new Date(value));
}
