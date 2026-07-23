import { useMemo, useState } from "react";
import {
  ArrowLeft,
  ArrowRight,
  BookOpenText,
  Check,
  ClipboardCheck,
  Compass,
  Database,
  FileSearch,
  FolderKanban,
  Library,
  ShieldCheck,
  Upload
} from "lucide-react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  ACADEMIC_STAGE_OPTIONS,
  DISCIPLINE_OPTIONS,
  DOCUMENT_TYPE_OPTIONS,
  PARADIGM_OPTIONS,
  defaultDocumentForStage
} from "../components/academic/academicOptions.js";
import {
  GUIDE_MATERIAL_OPTIONS,
  GUIDE_PROGRESS_OPTIONS
} from "../components/guide/guideOptions.js";
import { api } from "../services/api.js";

const STEPS = ["认识工作流", "确定项目", "选择研究方式", "确认当前进度", "盘点已有内容", "预览项目路线"];

const ROUTE_PREVIEW = [
  { phase: "研究准备", title: "建立项目画像", icon: FolderKanban },
  { phase: "研究准备", title: "添加并确认材料", icon: Upload },
  { phase: "研究准备", title: "检查写作准备度", icon: ShieldCheck },
  { phase: "研究资产", title: "构建项目知识库", icon: Library, optional: true },
  { phase: "写作交付", title: "推进章节写作", icon: BookOpenText },
  { phase: "写作交付", title: "审查并完成交付", icon: ClipboardCheck }
];

export function OnboardingPage({ profile, onProfileChange, onError }) {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const tourOnly = searchParams.get("mode") === "tour";
  const [step, setStep] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState(() => initialForm());

  const selectedProgress = GUIDE_PROGRESS_OPTIONS.find((item) => item.value === form.currentProgress);
  const routePreview = useMemo(() => {
    const materialKnown = form.availableMaterials.length > 0 && !form.availableMaterials.includes("NONE");
    return ROUTE_PREVIEW.map((item, index) => ({
      ...item,
      state: index === 0 ? "ready" : index === 1 && materialKnown ? "ready" : index === 1 ? "next" : "later"
    }));
  }, [form.availableMaterials]);

  if (tourOnly) {
    return <SystemTour onClose={() => navigate("/app/projects")} />;
  }

  function updateField(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function updateStage(stage) {
    const defaults = defaultDocumentForStage(stage);
    setForm((current) => ({
      ...current,
      academicStage: stage,
      documentType: defaults.documentType,
      targetLength: defaults.targetLength
    }));
  }

  function toggleMaterial(value) {
    setForm((current) => {
      if (value === "NONE") {
        return { ...current, availableMaterials: current.availableMaterials.includes("NONE") ? [] : ["NONE"] };
      }
      const withoutNone = current.availableMaterials.filter((item) => item !== "NONE");
      const next = withoutNone.includes(value)
        ? withoutNone.filter((item) => item !== value)
        : [...withoutNone, value];
      return { ...current, availableMaterials: next };
    });
  }

  function canContinue() {
    if (step === 1) return Boolean(form.title.trim());
    return true;
  }

  async function skip() {
    try {
      setSubmitting(true);
      const nextProfile = await api.updateOnboarding({ status: "SKIPPED", onboardingVersion: "v1" });
      onProfileChange(nextProfile);
      navigate("/app/projects", { replace: true });
    } catch (error) {
      onError(error.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function complete() {
    if (!form.title.trim()) return;
    try {
      setSubmitting(true);
      const result = await api.completeOnboarding({
        onboardingVersion: "v1",
        workspace: buildWorkspaceRequest(form)
      });
      onProfileChange(result.user);
      navigate(`/app/projects/${result.workspace.id}`, { replace: true });
    } catch (error) {
      onError(error.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="onboarding-page">
      <header className="onboarding-head">
        <div>
          <span className="eyebrow">Research navigator</span>
          <h1>梳理你的研究项目</h1>
          <p>用几项基础信息生成第一条项目路线。所有建议都可以跳过或稍后调整。</p>
        </div>
        <button className="text-button" type="button" onClick={skip} disabled={submitting}>稍后再说</button>
      </header>

      <div className="onboarding-layout">
        <ol className="onboarding-stepper" aria-label="引导进度">
          {STEPS.map((label, index) => (
            <li key={label} className={index === step ? "is-current" : index < step ? "is-complete" : ""}>
              <span>{index < step ? <Check size={14} /> : index + 1}</span>
              <small>{label}</small>
            </li>
          ))}
        </ol>

        <div className="onboarding-stage">
          {step === 0 && <WorkflowIntroduction />}
          {step === 1 && (
            <div className="onboarding-form-section">
              <StepHeading number="02" title="先确定项目和交付文档" description="项目用于共享材料与知识库，文档是最终写作载体。" />
              <div className="onboarding-field-grid">
                <label className="field onboarding-title-field"><span>项目名称</span><input autoFocus value={form.title} onChange={(event) => updateField("title", event.target.value)} placeholder="例如：城市更新中的社区参与研究" /></label>
                <label className="field"><span>学术阶段</span><select value={form.academicStage} onChange={(event) => updateStage(event.target.value)}>{ACADEMIC_STAGE_OPTIONS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label>
                <label className="field"><span>首个学术文档</span><select value={form.documentType} onChange={(event) => updateField("documentType", event.target.value)}>{DOCUMENT_TYPE_OPTIONS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label>
              </div>
            </div>
          )}
          {step === 2 && (
            <div className="onboarding-form-section">
              <StepHeading number="03" title="选择研究方向和研究方式" description="还不确定也没关系，后续可以在项目画像中修改。" />
              <div className="onboarding-field-grid">
                <label className="field"><span>学科方向</span><select value={form.disciplineGroup} onChange={(event) => updateField("disciplineGroup", event.target.value)}>{DISCIPLINE_OPTIONS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label>
                <label className="field"><span>研究范式</span><select value={form.researchParadigm} onChange={(event) => updateField("researchParadigm", event.target.value)}>{PARADIGM_OPTIONS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label>
              </div>
              <p className="onboarding-context-note"><Database size={17} />研究范式只用于调整材料检查和章节建议，不替代导师、学校或期刊规范。</p>
            </div>
          )}
          {step === 3 && (
            <div className="onboarding-form-section">
              <StepHeading number="04" title="你现在进行到哪里了？" description="路线会从最接近你当前状态的位置开始。" />
              <div className="onboarding-choice-list">
                {GUIDE_PROGRESS_OPTIONS.map((item) => <ChoiceButton key={item.value} selected={form.currentProgress === item.value} label={item.label} description={item.description} onClick={() => updateField("currentProgress", item.value)} />)}
              </div>
            </div>
          )}
          {step === 4 && (
            <div className="onboarding-form-section">
              <StepHeading number="05" title="盘点已有内容和交付约束" description="这里只记录手头情况，真正参与写作的材料仍需上传并完成解析。" />
              <fieldset className="onboarding-materials"><legend>目前已有内容</legend>{GUIDE_MATERIAL_OPTIONS.map((item) => <label key={item.value}><input type="checkbox" checked={form.availableMaterials.includes(item.value)} onChange={() => toggleMaterial(item.value)} /><span>{item.label}</span></label>)}</fieldset>
              <div className="onboarding-field-grid onboarding-constraints">
                <label className="field"><span>截止日期（可选）</span><input type="date" value={form.targetDeadline} onChange={(event) => updateField("targetDeadline", event.target.value)} /></label>
                <label className="field"><span>目标篇幅</span><input type="number" min="100" max="300000" value={form.targetLength} onChange={(event) => updateField("targetLength", event.target.value)} /></label>
                <label className="field"><span>引用格式</span><select value={form.citationStyle} onChange={(event) => updateField("citationStyle", event.target.value)}><option value="APA">APA</option><option value="GB/T 7714">GB/T 7714</option><option value="MLA">MLA</option><option value="CHICAGO">Chicago</option></select></label>
              </div>
            </div>
          )}
          {step === 5 && (
            <div className="onboarding-form-section">
              <StepHeading number="06" title="你的第一条项目路线" description={`${selectedProgress?.label ?? "当前进度"}，系统会优先提示最需要处理的任务。`} />
              <div className="onboarding-summary-strip"><span><strong>{form.title}</strong>项目</span><span><strong>{form.availableMaterials.includes("NONE") || form.availableMaterials.length === 0 ? 0 : form.availableMaterials.length}</strong>类已有内容</span><span><strong>{form.targetDeadline || "未设置"}</strong>截止日期</span></div>
              <div className="onboarding-route-preview">{routePreview.map((item) => { const Icon = item.icon; const StateIcon = item.state === "ready" ? Check : ArrowRight; return <div key={item.title} className={`onboarding-route-row is-${item.state}`}><Icon size={18} /><span><small>{item.phase}{item.optional ? " · 推荐" : ""}</small><strong>{item.title}</strong></span><StateIcon size={15} /></div>; })}</div>
              <p className="onboarding-context-note"><ShieldCheck size={17} />路线不会锁定页面；材料不足或解析失败时，原有业务门槛仍会明确提示。</p>
            </div>
          )}

          <footer className="onboarding-actions">
            <button className="secondary-btn" type="button" disabled={step === 0 || submitting} onClick={() => setStep((value) => Math.max(0, value - 1))}><ArrowLeft size={16} />上一步</button>
            {step < STEPS.length - 1 ? (
              <button className="primary-btn" type="button" disabled={!canContinue()} onClick={() => setStep((value) => value + 1)}>继续<ArrowRight size={16} /></button>
            ) : (
              <button className="primary-btn" type="button" disabled={submitting} onClick={complete}>{submitting ? "正在创建项目..." : "创建项目并查看路线"}<ArrowRight size={16} /></button>
            )}
          </footer>
        </div>
      </div>
    </section>
  );
}

function WorkflowIntroduction() {
  return (
    <div className="onboarding-form-section">
      <StepHeading number="01" title="先认识平台的三段研究主线" description="你可以按建议推进，也可以直接进入任何需要的页面。" />
      <div className="onboarding-phases">
        <div><Compass size={21} /><span>01</span><strong>研究准备</strong><p>明确目标，汇集材料，确认解析与写作准备度。</p></div>
        <div><Database size={21} /><span>02</span><strong>研究资产</strong><p>把真实材料整理成可检索、可追溯的知识依据。</p></div>
        <div><BookOpenText size={21} /><span>03</span><strong>写作交付</strong><p>按章节写作，共写前预览，审查后完成交付。</p></div>
      </div>
      <div className="onboarding-principle"><FileSearch size={19} /><div><strong>AI 是研究共创助手，不是最终裁判</strong><p>系统提供结构、候选文本和检查建议；材料依据、修改与提交内容始终由你确认。</p></div></div>
    </div>
  );
}

function SystemTour({ onClose }) {
  return (
    <section className="onboarding-page system-tour">
      <header className="onboarding-head"><div><span className="eyebrow">Product guide</span><h1>使用指南</h1><p>平台围绕三段主线组织，项目路线只做推荐，不限制你的操作顺序。</p></div><button className="secondary-btn" type="button" onClick={onClose}>返回研究项目</button></header>
      <WorkflowIntroduction />
      <div className="system-tour-route"><h2>一个项目通常会经过这些任务</h2><div className="onboarding-route-preview">{ROUTE_PREVIEW.map((item) => { const Icon = item.icon; return <div key={item.title} className="onboarding-route-row"><Icon size={18} /><span><small>{item.phase}{item.optional ? " · 推荐" : ""}</small><strong>{item.title}</strong></span><ArrowRight size={15} /></div>; })}</div></div>
    </section>
  );
}

function StepHeading({ number, title, description }) {
  return <header className="onboarding-step-heading"><span>{number}</span><div><h2>{title}</h2><p>{description}</p></div></header>;
}

function ChoiceButton({ selected, label, description, onClick }) {
  return <button className={`onboarding-choice ${selected ? "is-selected" : ""}`} type="button" aria-pressed={selected} onClick={onClick}><span>{selected ? <Check size={15} /> : null}</span><strong>{label}</strong><small>{description}</small></button>;
}

function initialForm() {
  const defaults = defaultDocumentForStage("UNDERGRADUATE");
  return {
    title: "",
    academicStage: "UNDERGRADUATE",
    documentType: defaults.documentType,
    disciplineGroup: "INTERDISCIPLINARY",
    researchParadigm: "OTHER",
    currentProgress: "IDEA_ONLY",
    availableMaterials: [],
    targetDeadline: "",
    targetLength: defaults.targetLength,
    citationStyle: "APA"
  };
}

function buildWorkspaceRequest(form) {
  return {
    title: form.title.trim(),
    academicProfile: {
      academicStage: form.academicStage,
      disciplineGroup: form.disciplineGroup,
      researchParadigm: form.researchParadigm,
      primaryLanguage: "zh-CN",
      defaultCitationStyle: form.citationStyle,
      institution: null,
      aiUsagePolicy: "EVIDENCE_GROUNDED_DRAFTING",
      aiPolicy: { humanReviewRequired: true, disclosureRequired: true }
    },
    initialDocument: {
      title: form.title.trim(),
      documentType: form.documentType,
      targetInstitution: null,
      targetVenue: null,
      targetLength: Number(form.targetLength),
      lengthUnit: "WORDS",
      citationStyle: form.citationStyle,
      requirementProfile: {},
      primaryDocument: true
    },
    guideProfile: {
      currentProgress: form.currentProgress,
      availableMaterials: form.availableMaterials,
      targetDeadline: form.targetDeadline || null,
      preferredMode: "GUIDED"
    }
  };
}
