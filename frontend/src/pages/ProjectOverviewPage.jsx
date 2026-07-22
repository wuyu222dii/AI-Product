import { useEffect, useMemo, useState } from "react";
import {
  ArrowRight,
  BookOpenText,
  Check,
  FileSearch,
  FileText,
  Library,
  ShieldCheck,
  Upload
} from "lucide-react";
import { api } from "../services/api.js";
import {
  DOCUMENT_TYPE_LABELS,
  PARADIGM_LABELS,
  STAGE_LABELS
} from "../components/academic/academicOptions.js";

export function ProjectOverviewPage({ workspace, onNavigate, onError }) {
  const [materials, setMaterials] = useState([]);
  const [documents, setDocuments] = useState([]);
  const [knowledgeChunks, setKnowledgeChunks] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    setLoading(true);
    Promise.allSettled([
      api.listMaterials(workspace.id),
      api.listAcademicDocuments(workspace.id),
      api.listKnowledgeChunks(workspace.id)
    ])
      .then(([materialResult, documentResult, chunkResult]) => {
        if (!active) return;
        if (materialResult.status === "fulfilled") setMaterials(materialResult.value.items ?? []);
        if (documentResult.status === "fulfilled") setDocuments(documentResult.value ?? []);
        if (chunkResult.status === "fulfilled") setKnowledgeChunks(chunkResult.value.items ?? []);
        const failedResult = [materialResult, documentResult, chunkResult].find((result) => result.status === "rejected");
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

  const nextAction = useMemo(() => resolveNextAction(summary), [summary]);
  const NextActionIcon = nextAction.icon;
  const profile = workspace.academicProfile ?? {};
  const routes = [
    {
      icon: Upload,
      title: "研究输入",
      description: "上传论文、数据、研究记录、规范要求或链接。",
      target: "upload",
      meta: summary.materialCount > 0 ? `${summary.materialCount} 份材料` : "尚未添加",
      complete: summary.materialCount > 0
    },
    {
      icon: FileSearch,
      title: "解析质量",
      description: "确认 AI 是否准确理解每份材料，并补齐缺失信息。",
      target: "parsing",
      meta: summary.materialCount > 0 ? `${summary.parsedCount}/${summary.materialCount} 已解析` : "等待材料",
      complete: summary.materialCount > 0 && summary.parsedCount === summary.materialCount,
      attention: summary.blockingCount > 0
    },
    {
      icon: ShieldCheck,
      title: "材料检查",
      description: "判断当前材料能否支撑所选文档与研究范式。",
      target: "materials",
      meta: summary.blockingCount > 0 ? `${summary.blockingCount} 项关键问题` : "按需检查",
      attention: summary.blockingCount > 0
    },
    {
      icon: Library,
      title: "知识库",
      description: "把已解析材料整理为可检索的证据片段。",
      target: "knowledge",
      meta: summary.knowledgeCount > 0 ? `${summary.knowledgeCount} 个片段` : "尚未构建",
      complete: summary.knowledgeCount > 0
    },
    {
      icon: BookOpenText,
      title: "学术文档",
      description: "按章节写作、共写、审查并完成整篇交付。",
      target: "documents",
      meta: `${summary.documentCount} 个文档 · ${summary.sectionCount} 个章节`,
      complete: summary.sectionCount > 0
    }
  ];

  return (
    <section className="project-overview">
      <header className="project-overview-head">
        <div>
          <span className="eyebrow">Project overview</span>
          <h1>{workspace.title}</h1>
          <p>从这里掌握研究材料、知识资产和学术文档的整体状态，再进入具体任务。</p>
        </div>
        <div className="button-row">
          <button className="secondary-btn" type="button" onClick={() => onNavigate("upload")}>
            <Upload size={16} aria-hidden="true" />补充材料
          </button>
          <button className="primary-btn" type="button" onClick={() => onNavigate(nextAction.target)}>
            {nextAction.action}<ArrowRight size={16} aria-hidden="true" />
          </button>
        </div>
      </header>

      <section className="project-next-step" aria-labelledby="project-next-step-title">
        <div className="project-next-step-icon"><NextActionIcon size={22} aria-hidden="true" /></div>
        <div>
          <span>建议下一步</span>
          <h2 id="project-next-step-title">{nextAction.title}</h2>
          <p>{nextAction.description}</p>
        </div>
        <button className="text-button" type="button" onClick={() => onNavigate(nextAction.target)}>
          前往处理<ArrowRight size={15} aria-hidden="true" />
        </button>
      </section>

      <div className="project-overview-metrics" aria-label="项目状态摘要">
        <OverviewMetric label="研究材料" value={loading ? "-" : summary.materialCount} detail={`${summary.parsedCount} 份已完成解析`} />
        <OverviewMetric label="知识片段" value={loading ? "-" : summary.knowledgeCount} detail="来自当前项目真实材料" />
        <OverviewMetric label="学术文档" value={loading ? "-" : summary.documentCount} detail={`${summary.sectionCount} 个章节`} />
        <OverviewMetric label="关键问题" value={loading ? "-" : summary.blockingCount} detail={summary.blockingCount > 0 ? "建议先完成补充" : "暂未发现阻断项"} warning={summary.blockingCount > 0} />
      </div>

      <div className="project-overview-grid">
        <section className="project-overview-panel" aria-labelledby="project-route-title">
          <div className="project-overview-panel-head">
            <div>
              <span className="eyebrow">Research route</span>
              <h2 id="project-route-title">项目路径</h2>
            </div>
            <p>这些入口可以按需跳转，不要求机械地逐步完成。</p>
          </div>
          <div className="project-route-list">
            {routes.map((item, index) => {
              const Icon = item.icon;
              return (
                <button className={`project-route-item ${item.attention ? "is-attention" : ""}`} type="button" key={item.target} onClick={() => onNavigate(item.target)}>
                  <span className={`project-route-state ${item.complete ? "is-complete" : ""}`}>
                    {item.complete ? <Check size={15} aria-hidden="true" /> : index + 1}
                  </span>
                  <Icon size={19} aria-hidden="true" />
                  <span className="project-route-copy"><strong>{item.title}</strong><small>{item.description}</small></span>
                  <span className="project-route-meta">{item.meta}</span>
                  <ArrowRight size={16} aria-hidden="true" />
                </button>
              );
            })}
          </div>
        </section>

        <aside className="project-overview-panel project-profile-summary" aria-labelledby="project-profile-title">
          <div className="project-overview-panel-head">
            <div>
              <span className="eyebrow">Academic profile</span>
              <h2 id="project-profile-title">项目画像</h2>
            </div>
          </div>
          <dl>
            <div><dt>学术阶段</dt><dd>{STAGE_LABELS[profile.academicStage] ?? "待确认"}</dd></div>
            <div><dt>研究范式</dt><dd>{PARADIGM_LABELS[profile.researchParadigm] ?? "待确认"}</dd></div>
            <div><dt>机构</dt><dd>{profile.institution || "未设置"}</dd></div>
            <div><dt>默认引用</dt><dd>{profile.defaultCitationStyle || "APA"}</dd></div>
          </dl>
          <p className="project-profile-note">项目画像用于调整章节模板与材料要求，真实学校、导师或期刊规范始终优先。</p>
          <button className="secondary-btn" type="button" onClick={() => onNavigate("documents")}>在学术文档中管理画像</button>
        </aside>
      </div>

      <section className="project-overview-panel" aria-labelledby="recent-documents-title">
        <div className="project-overview-panel-head">
          <div>
            <span className="eyebrow">Documents</span>
            <h2 id="recent-documents-title">学术文档</h2>
          </div>
          <button className="text-button" type="button" onClick={() => onNavigate("documents")}>查看全部<ArrowRight size={15} aria-hidden="true" /></button>
        </div>
        {documents.length === 0 ? (
          <div className="project-overview-empty"><FileText size={22} aria-hidden="true" /><p>当前还没有学术文档，可进入工作台创建。</p></div>
        ) : (
          <div className="project-document-list">
            {documents.slice(0, 4).map((document) => (
              <button type="button" className="project-document-row" key={document.id} onClick={() => onNavigate(`documents/${document.id}`)}>
                <FileText size={18} aria-hidden="true" />
                <span><strong>{document.title}</strong><small>{DOCUMENT_TYPE_LABELS[document.documentType] ?? document.documentType}</small></span>
                <span>{document.sectionCount ?? 0} 个章节</span>
                <span>{formatDate(document.updatedAt)}</span>
                <ArrowRight size={16} aria-hidden="true" />
              </button>
            ))}
          </div>
        )}
      </section>
    </section>
  );
}

function OverviewMetric({ label, value, detail, warning = false }) {
  return (
    <div className={`project-overview-metric ${warning ? "is-warning" : ""}`}>
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{detail}</small>
    </div>
  );
}

function resolveNextAction(summary) {
  if (summary.materialCount === 0) {
    return { icon: Upload, target: "upload", action: "添加研究输入", title: "先添加真实研究材料", description: "上传已有论文、数据、研究记录或提交规范，建立后续写作依据。" };
  }
  if (summary.parsedCount < summary.materialCount || summary.blockingCount > 0) {
    return { icon: FileSearch, target: "parsing", action: "检查解析质量", title: "确认材料解析是否可靠", description: "修正未完成解析、低置信或信息缺失的材料，避免错误传入后续写作。" };
  }
  if (summary.knowledgeCount === 0) {
    return { icon: ShieldCheck, target: "materials", action: "检查材料准备度", title: "确认材料能否支撑当前文档", description: "先检查文档类型与研究范式需要哪些材料，再决定是否构建知识库或补充资料。" };
  }
  return { icon: BookOpenText, target: "documents", action: "继续章节写作", title: "进入学术文档继续推进", description: "当前材料已完成解析并形成知识片段，可以继续章节写作、共写或整篇检查。" };
}

function normalize(value) {
  return String(value ?? "").trim().toUpperCase();
}

function formatDate(value) {
  if (!value) return "更新时间未知";
  return new Intl.DateTimeFormat("zh-CN", { month: "2-digit", day: "2-digit" }).format(new Date(value));
}
