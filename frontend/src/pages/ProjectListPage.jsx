import { useEffect, useState } from "react";
import { ArrowRight, BookOpenText, GraduationCap, Plus } from "lucide-react";
import { api } from "../services/api";
import { StatusBadge } from "../components/StatusBadge";
import {
  ACADEMIC_STAGE_OPTIONS,
  AI_POLICY_OPTIONS,
  DISCIPLINE_OPTIONS,
  DOCUMENT_TYPE_OPTIONS,
  DOCUMENT_TYPE_LABELS,
  PARADIGM_OPTIONS,
  PARADIGM_LABELS,
  STAGE_LABELS,
  defaultDocumentForStage
} from "../components/academic/academicOptions.js";

export function ProjectListPage({ onWorkspaceCreated, onWorkspaceSelected, onError }) {
  const [form, setForm] = useState(() => initialForm());
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showCreate, setShowCreate] = useState(false);

  async function load() {
    try {
      const data = await api.listWorkspaces();
      setItems(data.items ?? []);
    } catch (error) {
      onError(error.message);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function handleCreate(event) {
    event.preventDefault();
    if (!form.title.trim()) return;
    try {
      setLoading(true);
      const created = await api.createWorkspace({
        title: form.title.trim(),
        academicProfile: {
          academicStage: form.academicStage,
          disciplineGroup: form.disciplineGroup,
          researchParadigm: form.researchParadigm,
          primaryLanguage: form.primaryLanguage,
          defaultCitationStyle: form.citationStyle,
          institution: form.institution.trim() || null,
          aiUsagePolicy: form.aiUsagePolicy,
          aiPolicy: { humanReviewRequired: true, disclosureRequired: true }
        },
        initialDocument: {
          title: form.title.trim(),
          documentType: form.documentType,
          targetInstitution: form.institution.trim() || null,
          targetVenue: null,
          targetLength: Number(form.targetLength),
          lengthUnit: "WORDS",
          citationStyle: form.citationStyle,
          requirementProfile: {},
          primaryDocument: true
        }
      });
      setForm(initialForm());
      setShowCreate(false);
      await load();
      onWorkspaceCreated(created);
    } catch (error) {
      onError(error.message);
    } finally {
      setLoading(false);
    }
  }

  function updateField(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function updateStage(value) {
    const defaults = defaultDocumentForStage(value);
    setForm((current) => ({
      ...current,
      academicStage: value,
      documentType: defaults.documentType,
      targetLength: defaults.targetLength
    }));
  }

  return (
    <section className="project-hub">
      <div className="project-hub-head">
        <div>
          <span className="eyebrow">Evidence-driven research</span>
          <h3>研究项目</h3>
          <p>一个项目可以共享材料与知识库，并包含开题、学位论文、论文稿件等多个学术文档。</p>
        </div>
        <button className="primary-btn icon-text-btn" type="button" onClick={() => setShowCreate((value) => !value)}>
          <Plus size={17} aria-hidden="true" />
          {showCreate ? "收起创建" : "新建研究项目"}
        </button>
      </div>

      {showCreate && (
        <form className="project-create-panel" onSubmit={handleCreate}>
          <div className="project-create-intro">
            <GraduationCap size={24} aria-hidden="true" />
            <div>
              <strong>先建立学术项目画像</strong>
              <p>这些信息用于选择章节模板和材料门槛，不替代学校、导师或期刊的真实要求。</p>
            </div>
          </div>
          <div className="project-form-grid">
            <div className="field project-title-field">
              <label>研究项目名称</label>
              <input
                value={form.title}
                onChange={(event) => updateField("title", event.target.value)}
                placeholder="例如：智能教室能源管理研究"
                required
              />
            </div>
            <div className="field">
              <label>学术阶段</label>
              <select value={form.academicStage} onChange={(event) => updateStage(event.target.value)}>
                {ACADEMIC_STAGE_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
              </select>
            </div>
            <div className="field">
              <label>学科方向</label>
              <select value={form.disciplineGroup} onChange={(event) => updateField("disciplineGroup", event.target.value)}>
                {DISCIPLINE_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
              </select>
            </div>
            <div className="field">
              <label>研究范式</label>
              <select value={form.researchParadigm} onChange={(event) => updateField("researchParadigm", event.target.value)}>
                {PARADIGM_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
              </select>
            </div>
            <div className="field">
              <label>首个文档</label>
              <select value={form.documentType} onChange={(event) => updateField("documentType", event.target.value)}>
                {DOCUMENT_TYPE_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
              </select>
            </div>
            <div className="field">
              <label>目标篇幅（字 / 词）</label>
              <input type="number" min="100" max="300000" value={form.targetLength} onChange={(event) => updateField("targetLength", event.target.value)} />
            </div>
            <div className="field">
              <label>默认引用格式</label>
              <select value={form.citationStyle} onChange={(event) => updateField("citationStyle", event.target.value)}>
                <option value="APA">APA</option>
                <option value="GB/T 7714">GB/T 7714</option>
                <option value="MLA">MLA</option>
                <option value="CHICAGO">Chicago</option>
              </select>
            </div>
            <div className="field">
              <label>学校 / 机构（可选）</label>
              <input value={form.institution} onChange={(event) => updateField("institution", event.target.value)} placeholder="用于记录目标规范来源" />
            </div>
            <div className="field project-policy-field">
              <label>AI 使用策略</label>
              <select value={form.aiUsagePolicy} onChange={(event) => updateField("aiUsagePolicy", event.target.value)}>
                {AI_POLICY_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
              </select>
            </div>
          </div>
          <div className="project-create-footer">
            <p>AI 不能列为作者；生成与修改会记录依据和采纳状态，并由用户最终确认。</p>
            <button className="primary-btn icon-text-btn" disabled={loading}>
              <BookOpenText size={17} aria-hidden="true" />
              {loading ? "正在创建..." : "创建并进入材料准备"}
            </button>
          </div>
        </form>
      )}

      <div className="project-list-grid">
        {items.length === 0 ? (
          <div className="project-empty-state">
            <GraduationCap size={28} aria-hidden="true" />
            <strong>还没有研究项目</strong>
            <p>创建项目后，先上传真实研究材料，再进入多文档和章节级共写。</p>
          </div>
        ) : (
          items.map((item) => (
            <article className="project-list-item" key={item.id}>
              <div className="project-list-item-top">
                <div>
                  <span className="project-stage-label">{STAGE_LABELS[item.academicProfile?.academicStage] ?? "旧版项目"}</span>
                  <h4>{item.title}</h4>
                </div>
                <StatusBadge level={item.status === "READY" || item.status === "ready" ? "ready" : "notice"}>
                  {item.status}
                </StatusBadge>
              </div>
              <div className="project-list-meta">
                <span>{PARADIGM_LABELS[item.academicProfile?.researchParadigm] ?? "研究范式待确认"}</span>
                <span>{item.academicProfile?.defaultCitationStyle ?? "APA"}</span>
                <span>{item.activeDocumentId ? "已建立主文档" : DOCUMENT_TYPE_LABELS.COURSE_PAPER}</span>
              </div>
              <div className="project-list-item-footer">
                <span>更新于 {formatDate(item.updatedAt)}</span>
                <button className="ghost-btn icon-text-btn" type="button" onClick={() => onWorkspaceSelected(item)}>
                  进入项目 <ArrowRight size={16} aria-hidden="true" />
                </button>
              </div>
            </article>
          ))
        )}
      </div>
    </section>
  );
}

function initialForm() {
  const defaults = defaultDocumentForStage("UNDERGRADUATE");
  return {
    title: "",
    academicStage: "UNDERGRADUATE",
    disciplineGroup: "INTERDISCIPLINARY",
    researchParadigm: "OTHER",
    primaryLanguage: "zh-CN",
    citationStyle: "APA",
    institution: "",
    aiUsagePolicy: "EVIDENCE_GROUNDED_DRAFTING",
    documentType: defaults.documentType,
    targetLength: defaults.targetLength
  };
}

function formatDate(value) {
  if (!value) return "未知";
  return new Intl.DateTimeFormat("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" }).format(new Date(value));
}
