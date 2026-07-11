import { useEffect, useMemo, useState } from "react";
import { FileCheck2, PenLine, Plus, Save, Settings2, Upload } from "lucide-react";
import { api } from "../services/api.js";
import { AcademicDocumentSwitcher } from "../components/academic/AcademicDocumentSwitcher.jsx";
import { AcademicSectionNavigator } from "../components/academic/AcademicSectionNavigator.jsx";
import { AcademicSectionEditor } from "../components/academic/AcademicSectionEditor.jsx";
import { AcademicInspector } from "../components/academic/AcademicInspector.jsx";
import { AcademicChecksDrawer } from "../components/academic/AcademicChecksDrawer.jsx";
import { AcademicCoWritePreviewDrawer } from "../components/academic/AcademicCoWritePreviewDrawer.jsx";
import { AcademicDocumentQualityView } from "../components/academic/AcademicDocumentQualityView.jsx";
import { AcademicSplitModal } from "../components/academic/AcademicSplitModal.jsx";
import { WorkspaceReviewDrawer } from "../components/workspace/WorkspaceReviewDrawer.jsx";
import { WorkspaceAppealModal } from "../components/workspace/WorkspaceAppealModal.jsx";
import {
  ACADEMIC_STAGE_OPTIONS,
  AI_POLICY_OPTIONS,
  DISCIPLINE_OPTIONS,
  DOCUMENT_TYPE_OPTIONS,
  PARADIGM_OPTIONS,
  defaultDocumentForStage
} from "../components/academic/academicOptions.js";

export function AcademicDocumentsPage({
  workspace,
  activeDocument,
  onActiveDocumentChange,
  onGoUpload,
  onError
}) {
  const [profile, setProfile] = useState(workspace.academicProfile ?? null);
  const [documents, setDocuments] = useState([]);
  const [sections, setSections] = useState([]);
  const [selectedSectionId, setSelectedSectionId] = useState("");
  const [sectionDraft, setSectionDraft] = useState({ title: "", content: "" });
  const [readiness, setReadiness] = useState(null);
  const [sectionReadiness, setSectionReadiness] = useState(null);
  const [materials, setMaterials] = useState([]);
  const [materialLinks, setMaterialLinks] = useState([]);
  const [aiActions, setAiActions] = useState([]);
  const [versions, setVersions] = useState([]);
  const [workbenchMode, setWorkbenchMode] = useState("section");
  const [busyAction, setBusyAction] = useState("");
  const [materialUpdating, setMaterialUpdating] = useState(false);
  const [showCreateDocument, setShowCreateDocument] = useState(false);
  const [showCreateSection, setShowCreateSection] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [documentForm, setDocumentForm] = useState(() => initialDocumentForm(workspace.title, workspace.academicProfile?.academicStage));
  const [sectionForm, setSectionForm] = useState({ title: "", sectionType: "CUSTOM", targetLength: 1000 });
  const [profileForm, setProfileForm] = useState(workspace.academicProfile ?? null);
  const [generateInstruction, setGenerateInstruction] = useState("");
  const [coWriteAction, setCoWriteAction] = useState("improve_expression");
  const [coWriteInstruction, setCoWriteInstruction] = useState("");
  const [controls, setControls] = useState({
    rewriteDepth: "balanced",
    keepCitations: true,
    keepData: true,
    noNewSources: true,
    keepStudentVoice: true
  });
  const [preview, setPreview] = useState(null);
  const [assembled, setAssembled] = useState(null);
  const [downloadUrl, setDownloadUrl] = useState("");
  const [selectedRange, setSelectedRange] = useState(null);
  const [assistantOpen, setAssistantOpen] = useState(false);
  const [checksOpen, setChecksOpen] = useState(false);
  const [sectionEvidence, setSectionEvidence] = useState(null);
  const [sectionRisks, setSectionRisks] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [quality, setQuality] = useState(null);
  const [activeReview, setActiveReview] = useState(null);
  const [appealReview, setAppealReview] = useState(null);
  const [appealLoading, setAppealLoading] = useState(false);
  const [splitPreview, setSplitPreview] = useState(null);

  const selectedSection = useMemo(
    () => sections.find((item) => item.id === selectedSectionId) ?? sections[0] ?? null,
    [sections, selectedSectionId]
  );

  useEffect(() => {
    let cancelled = false;
    async function loadProject() {
      try {
        const [profileData, documentData, materialData] = await Promise.all([
          api.getAcademicProfile(workspace.id),
          api.listAcademicDocuments(workspace.id),
          api.listMaterials(workspace.id)
        ]);
        if (cancelled) return;
        const docs = documentData ?? [];
        const selected = docs.find((item) => item.id === activeDocument?.id)
          ?? docs.find((item) => item.id === workspace.activeDocumentId)
          ?? docs.find((item) => item.primaryDocument)
          ?? docs[0]
          ?? null;
        setProfile(profileData);
        setProfileForm(profileData);
        setDocuments(docs);
        setMaterials(materialData.items ?? []);
        if (selected) onActiveDocumentChange(selected);
      } catch (error) {
        if (!cancelled) onError(error.message);
      }
    }
    loadProject();
    return () => { cancelled = true; };
  }, [workspace.id]);

  useEffect(() => {
    if (!activeDocument?.id) return;
    let cancelled = false;
    async function loadDocumentContext() {
      try {
        const [sectionData, readinessData, linkData, actionData, reviewData, qualityData] = await Promise.all([
          api.listDocumentSections(activeDocument.id),
          api.checkDocumentReadiness(activeDocument.id),
          api.listDocumentMaterialLinks(activeDocument.id),
          api.listDocumentAiActions(activeDocument.id),
          api.listDocumentReviewItems(activeDocument.id),
          api.getDocumentQualitySummary(activeDocument.id)
        ]);
        if (cancelled) return;
        setSections(sectionData ?? []);
        setReadiness(readinessData);
        setMaterialLinks(linkData ?? []);
        setAiActions(actionData ?? []);
        setReviews(reviewData ?? []);
        setQuality(qualityData);
        const nextSection = (sectionData ?? []).find((item) => item.id === selectedSectionId) ?? sectionData?.[0] ?? null;
        setSelectedSectionId(nextSection?.id ?? "");
        setPreview(null);
        setAssembled(null);
        setDownloadUrl("");
        setSelectedRange(null);
      } catch (error) {
        if (!cancelled) onError(error.message);
      }
    }
    loadDocumentContext();
    return () => { cancelled = true; };
  }, [activeDocument?.id]);

  useEffect(() => {
    if (!selectedSection) {
      setSectionDraft({ title: "", content: "" });
      setVersions([]);
      setSectionReadiness(null);
      return;
    }
    setSectionDraft({ title: selectedSection.title, content: selectedSection.content ?? "" });
    setPreview(null);
    Promise.all([
      api.listDocumentSectionVersions(selectedSection.id),
      api.checkSectionReadiness(selectedSection.id),
      api.getSectionEvidenceBindings(selectedSection.id),
      api.getSectionWritingRisks(selectedSection.id),
      api.listDocumentReviewItems(activeDocument.id, { sectionId: selectedSection.id })
    ])
      .then(([versionData, readinessData, evidenceData, riskData, reviewData]) => {
        setVersions(versionData ?? []);
        setSectionReadiness(readinessData);
        setSectionEvidence(evidenceData);
        setSectionRisks(riskData);
        setReviews((current) => [
          ...current.filter((item) => item.sectionId !== selectedSection.id),
          ...(reviewData ?? [])
        ]);
      })
      .catch((error) => onError(error.message));
  }, [selectedSection?.id, selectedSection?.versionNo, activeDocument?.id]);

  async function refreshDocument(documentId = activeDocument?.id, preferredSectionId = selectedSectionId) {
    if (!documentId) return;
    const [documentData, sectionData, readinessData, linkData, actionData, reviewData, qualityData] = await Promise.all([
      api.getAcademicDocument(documentId),
      api.listDocumentSections(documentId),
      api.checkDocumentReadiness(documentId),
      api.listDocumentMaterialLinks(documentId),
      api.listDocumentAiActions(documentId),
      api.listDocumentReviewItems(documentId),
      api.getDocumentQualitySummary(documentId)
    ]);
    setDocuments((current) => current.map((item) => item.id === documentData.id ? documentData : item));
    onActiveDocumentChange(documentData);
    setSections(sectionData ?? []);
    setReadiness(readinessData);
    setMaterialLinks(linkData ?? []);
    setAiActions(actionData ?? []);
    setReviews(reviewData ?? []);
    setQuality(qualityData);
    const nextSection = (sectionData ?? []).find((item) => item.id === preferredSectionId) ?? sectionData?.[0] ?? null;
    setSelectedSectionId(nextSection?.id ?? "");
  }

  async function refreshSectionQuality(sectionId = selectedSection?.id) {
    if (!sectionId || !activeDocument?.id) return;
    const [evidenceData, riskData, reviewData, qualityData] = await Promise.all([
      api.getSectionEvidenceBindings(sectionId),
      api.getSectionWritingRisks(sectionId),
      api.listDocumentReviewItems(activeDocument.id, { sectionId }),
      api.getDocumentQualitySummary(activeDocument.id)
    ]);
    setSectionEvidence(evidenceData);
    setSectionRisks(riskData);
    setReviews((current) => [
      ...current.filter((item) => item.sectionId !== sectionId),
      ...(reviewData ?? [])
    ]);
    setQuality(qualityData);
  }

  async function waitForJob(jobId) {
    if (!jobId) return null;
    for (let attempt = 0; attempt < 30; attempt += 1) {
      const detail = await api.getJob(jobId);
      if (["success", "failed", "SUCCESS", "FAILED"].includes(detail.status)) {
        if (["failed", "FAILED"].includes(detail.status)) throw new Error(detail.errorMessage || "后台任务执行失败");
        return detail;
      }
      await new Promise((resolve) => window.setTimeout(resolve, 400));
    }
    throw new Error("后台任务仍未完成，请稍后重新查看。");
  }

  async function selectDocument(document) {
    try {
      setBusyAction("switch-document");
      const activated = await api.activateAcademicDocument(document.id);
      onActiveDocumentChange(activated);
    } catch (error) {
      onError(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function createDocument(event) {
    event.preventDefault();
    try {
      setBusyAction("create-document");
      const created = await api.createAcademicDocument(workspace.id, {
        ...documentForm,
        title: documentForm.title.trim(),
        targetLength: Number(documentForm.targetLength),
        targetInstitution: documentForm.targetInstitution.trim() || null,
        targetVenue: documentForm.targetVenue.trim() || null,
        requirementProfile: {},
        primaryDocument: false
      });
      const activated = await api.activateAcademicDocument(created.id);
      setDocuments((current) => [created, ...current]);
      onActiveDocumentChange(activated);
      setDocumentForm(initialDocumentForm(`${workspace.title} - 新文档`, profile?.academicStage));
      setShowCreateDocument(false);
    } catch (error) {
      onError(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function createSection(event) {
    event.preventDefault();
    if (!activeDocument?.id || !sectionForm.title.trim()) return;
    try {
      setBusyAction("create-section");
      const created = await api.createDocumentSection(activeDocument.id, {
        parentSectionId: null,
        sortOrder: sections.length + 1,
        sectionType: sectionForm.sectionType,
        title: sectionForm.title.trim(),
        content: "",
        targetLength: Number(sectionForm.targetLength)
      });
      setShowCreateSection(false);
      setSectionForm({ title: "", sectionType: "CUSTOM", targetLength: 1000 });
      await refreshDocument(activeDocument.id, created.id);
    } catch (error) {
      onError(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function reorderSections(sectionIds) {
    if (!activeDocument?.id || busyAction === "reorder-sections") return;
    const previousSections = sections;
    const sectionById = new Map(sections.map((section) => [section.id, section]));
    const optimisticSections = sectionIds.map((id, index) => ({
      ...sectionById.get(id),
      sortOrder: index + 1
    }));
    setSections(optimisticSections);
    try {
      setBusyAction("reorder-sections");
      const reordered = await api.reorderDocumentSections(activeDocument.id, sectionIds);
      setSections(reordered ?? optimisticSections);
    } catch (error) {
      setSections(previousSections);
      onError(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function saveSection() {
    if (!selectedSection) return;
    try {
      setBusyAction("save");
      await api.updateDocumentSection(selectedSection.id, {
        title: sectionDraft.title,
        content: sectionDraft.content,
        changeSummary: "手动编辑章节"
      });
      await refreshDocument(activeDocument.id, selectedSection.id);
      window.setTimeout(() => refreshSectionQuality(selectedSection.id).catch((error) => onError(error.message)), 700);
    } catch (error) {
      onError(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function generateSection() {
    if (!selectedSection) return;
    try {
      setBusyAction("generate");
      await api.generateDocumentSection(selectedSection.id, { instruction: generateInstruction, mode: "stable" });
      setGenerateInstruction("");
      await refreshDocument(activeDocument.id, selectedSection.id);
      window.setTimeout(() => refreshSectionQuality(selectedSection.id).catch((error) => onError(error.message)), 700);
    } catch (error) {
      onError(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function previewCoWrite() {
    if (!selectedSection) return;
    try {
      setBusyAction("preview");
      if (sectionDraft.content !== selectedSection.content || sectionDraft.title !== selectedSection.title) {
        await api.updateDocumentSection(selectedSection.id, {
          title: sectionDraft.title,
          content: sectionDraft.content,
          changeSummary: "共写预览前保存当前编辑"
        });
      }
      const data = await api.previewSectionCoWrite(selectedSection.id, {
        action: coWriteAction,
        instruction: coWriteInstruction,
        controls,
        targetRange: selectedRange
          ? { mode: "selection", start: selectedRange.start, end: selectedRange.end }
          : { mode: "full_draft" }
      });
      setPreview(data);
      await refreshDocument(activeDocument.id, selectedSection.id);
    } catch (error) {
      onError(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function applyPreview(mode = "ALL", selectedIds = []) {
    if (!preview) return;
    try {
      setBusyAction("apply-preview");
      await api.applySectionCoWritePreview(preview.id, { mode, selectedIds });
      setPreview(null);
      setCoWriteInstruction("");
      setSelectedRange(null);
      await refreshDocument(activeDocument.id, selectedSection.id);
      window.setTimeout(() => refreshSectionQuality(selectedSection.id).catch((error) => onError(error.message)), 700);
    } catch (error) {
      onError(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function discardPreview() {
    if (!preview) return;
    try {
      await api.discardSectionCoWritePreview(preview.id);
      setPreview(null);
    } catch (error) {
      onError(error.message);
    }
  }

  async function restoreVersion(version) {
    if (!window.confirm(`确认恢复到章节 v${version.versionNo}？当前内容会保留为历史版本。`)) return;
    try {
      setBusyAction("restore");
      await api.restoreDocumentSectionVersion(version.id);
      await refreshDocument(activeDocument.id, selectedSection.id);
    } catch (error) {
      onError(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function rebuildSectionEvidence(sectionId = selectedSection?.id) {
    if (!sectionId) return;
    try {
      setBusyAction("evidence");
      const job = await api.rebuildSectionEvidenceBindings(sectionId);
      await waitForJob(job.jobId);
      await refreshSectionQuality(sectionId);
    } catch (error) {
      onError(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function rebuildDocumentEvidence() {
    try {
      setBusyAction("document-evidence");
      const jobs = await Promise.all(sections.map((section) => api.rebuildSectionEvidenceBindings(section.id)));
      await Promise.all(jobs.map((job) => waitForJob(job.jobId)));
      await refreshDocument(activeDocument.id, selectedSectionId);
      if (selectedSectionId) await refreshSectionQuality(selectedSectionId);
    } catch (error) {
      onError(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function refreshSectionReviews(sectionId = selectedSection?.id) {
    if (!sectionId) return;
    try {
      setBusyAction("section-review");
      const job = await api.refreshSectionReviewItems(sectionId);
      await waitForJob(job.jobId);
      await refreshSectionQuality(sectionId);
      setChecksOpen(true);
    } catch (error) {
      onError(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function refreshDocumentReviews() {
    if (!activeDocument?.id) return;
    try {
      setBusyAction("document-review");
      const job = await api.refreshDocumentReviewItems(activeDocument.id);
      await waitForJob(job.jobId);
      await refreshDocument(activeDocument.id, selectedSectionId);
    } catch (error) {
      onError(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function updateEvidence(binding, status) {
    try {
      await api.updateEvidenceBindingStatus(binding.id, { status });
      await refreshSectionQuality(binding.sectionId ?? selectedSection?.id);
    } catch (error) {
      onError(error.message);
    }
  }

  async function previewMaterial(binding) {
    if (!binding?.materialId) return;
    try {
      const materialPreview = await api.previewMaterial(binding.materialId);
      const target = materialPreview.downloadUrl || materialPreview.externalLink;
      if (target) window.open(target, "_blank", "noopener,noreferrer");
    } catch (error) {
      onError(error.message);
    }
  }

  function insertCitation(citationText) {
    const citation = String(citationText || "").trim();
    if (!citation) return;
    const insertAt = selectedRange?.end ?? sectionDraft.content.length;
    setSectionDraft((current) => ({
      ...current,
      content: `${current.content.slice(0, insertAt)}${citation}${current.content.slice(insertAt)}`
    }));
    setSelectedRange(null);
    setChecksOpen(false);
  }

  function locateRange(range) {
    if (!range || typeof range.start !== "number" || typeof range.end !== "number") return;
    setWorkbenchMode("section");
    if (range.sectionId && range.sectionId !== selectedSectionId) setSelectedSectionId(range.sectionId);
    window.setTimeout(() => setSelectedRange({ start: range.start, end: range.end, selectedText: range.selectedText || "" }), 0);
    setChecksOpen(false);
  }

  function fixWritingRisk(risk) {
    locateRange(risk.targetRange);
    setCoWriteAction("add_original_evidence");
    setCoWriteInstruction(risk.coWriteInstruction || risk.suggestedAction || "只基于已上传材料补充原创实证，不得编造。");
    setAssistantOpen(true);
  }

  function fixReview(review) {
    locateRange(review.targetRange);
    setCoWriteAction(review.reviewType?.includes("citation") ? "add_evidence" : review.reviewType?.includes("original") ? "add_original_evidence" : "improve_expression");
    setCoWriteInstruction(review.suggestedFix || review.message || "请基于已有材料修正该审查问题，不要编造事实或来源。");
    setAssistantOpen(true);
    setActiveReview(null);
  }

  async function updateReviewStatus(review, status, resolutionNote) {
    try {
      const updated = await api.updateReviewStatus(review.id, { status, resolutionNote });
      setReviews((current) => current.map((item) => item.id === review.id ? updated : item));
      if (activeReview?.id === review.id) setActiveReview(updated);
      if (activeDocument?.id) setQuality(await api.getDocumentQualitySummary(activeDocument.id));
    } catch (error) {
      onError(error.message);
    }
  }

  async function recheckReview(review) {
    try {
      setBusyAction(`recheck-${review.id}`);
      const updated = await api.recheckReviewItem(review.id);
      setReviews((current) => current.map((item) => item.id === review.id ? updated : item));
      setActiveReview(updated);
      if (activeDocument?.id) setQuality(await api.getDocumentQualitySummary(activeDocument.id));
    } catch (error) {
      onError(error.message);
    } finally {
      setBusyAction("");
    }
  }

  function openReview(review) {
    if (review.sectionId && review.sectionId !== selectedSectionId) setSelectedSectionId(review.sectionId);
    setActiveReview(review);
  }

  async function submitAppeal(payload) {
    if (!appealReview) return;
    try {
      setAppealLoading(true);
      await api.createAppeal(appealReview.id, payload);
      setAppealReview(null);
      await refreshDocument(activeDocument.id, selectedSectionId);
    } catch (error) {
      onError(error.message);
    } finally {
      setAppealLoading(false);
    }
  }

  async function previewLegacySplit() {
    if (!selectedSection?.id) return;
    try {
      setBusyAction("split-preview");
      setSplitPreview(await api.previewSectionSplit(selectedSection.id));
    } catch (error) {
      onError(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function applyLegacySplit(body) {
    try {
      setBusyAction("split-apply");
      const splitSections = await api.applySectionSplit(selectedSection.id, body);
      setSplitPreview(null);
      await refreshDocument(activeDocument.id, splitSections[0]?.id);
    } catch (error) {
      onError(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function toggleMaterial(material, included) {
    if (!activeDocument?.id) return;
    try {
      setMaterialUpdating(true);
      if (materialLinks.length === 0) {
        await Promise.all(materials.map((item) => api.linkDocumentMaterial(activeDocument.id, {
          materialId: item.id,
          role: item.materialRole ?? "SUPPORTING",
          included: item.id === material.id ? included : true
        })));
      } else {
        await api.linkDocumentMaterial(activeDocument.id, {
          materialId: material.id,
          role: material.materialRole ?? "SUPPORTING",
          included
        });
      }
      await refreshDocument(activeDocument.id, selectedSectionId);
    } catch (error) {
      onError(error.message);
    } finally {
      setMaterialUpdating(false);
    }
  }

  async function assembleDocument() {
    if (!activeDocument?.id) return;
    try {
      setBusyAction("assemble");
      setAssembled(await api.assembleAcademicDocument(activeDocument.id));
    } catch (error) {
      onError(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function exportDocument() {
    if (!activeDocument?.id) return;
    try {
      setBusyAction("export");
      setDownloadUrl("");
      const job = await api.exportAcademicDocument(activeDocument.id, {
        format: "docx",
        includeComments: false,
        citationStyle: activeDocument.citationStyle ?? profile?.defaultCitationStyle ?? "APA"
      });
      const detail = await waitForJob(job.jobId);
      if (detail?.outputRef?.downloadUrl) setDownloadUrl(detail.outputRef.downloadUrl);
    } catch (error) {
      onError(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function saveProfile(event) {
    event.preventDefault();
    try {
      setBusyAction("profile");
      const updated = await api.updateAcademicProfile(workspace.id, {
        academicStage: profileForm.academicStage,
        disciplineGroup: profileForm.disciplineGroup,
        researchParadigm: profileForm.researchParadigm,
        primaryLanguage: profileForm.primaryLanguage ?? "zh-CN",
        defaultCitationStyle: profileForm.defaultCitationStyle ?? "APA",
        institution: profileForm.institution?.trim() || null,
        aiUsagePolicy: profileForm.aiUsagePolicy,
        aiPolicy: profileForm.aiPolicy ?? { humanReviewRequired: true, disclosureRequired: true }
      });
      setProfile(updated);
      setProfileForm(updated);
      setShowProfile(false);
      if (activeDocument?.id) await refreshDocument(activeDocument.id, selectedSectionId);
    } catch (error) {
      onError(error.message);
    } finally {
      setBusyAction("");
    }
  }

  return (
    <section className="academic-page">
      <header className="academic-page-head">
        <div>
          <span className="eyebrow">Academic project workspace</span>
          <h3>{workspace.title}</h3>
          <p>项目材料与知识库共享，正文、章节版本、材料范围和 AI 使用记录按文档隔离。</p>
        </div>
        <div className="button-row">
          <button className="ghost-btn icon-text-btn" type="button" onClick={() => setShowProfile((value) => !value)}>
            <Settings2 size={16} aria-hidden="true" /> 项目画像
          </button>
          <button className="ghost-btn icon-text-btn" type="button" onClick={onGoUpload}>
            <Upload size={16} aria-hidden="true" /> 补充材料
          </button>
        </div>
      </header>

      {showProfile && profileForm && (
        <form className="academic-inline-form academic-profile-form" onSubmit={saveProfile}>
          <ProfileFields value={profileForm} onChange={setProfileForm} />
          <button className="primary-btn icon-text-btn" disabled={busyAction === "profile"}><Save size={16} aria-hidden="true" /> 保存画像</button>
        </form>
      )}

      <AcademicDocumentSwitcher
        documents={documents}
        activeDocument={activeDocument}
        profile={profile}
        onSelect={selectDocument}
        onCreate={() => setShowCreateDocument((value) => !value)}
      />

      {showCreateDocument && (
        <form className="academic-inline-form" onSubmit={createDocument}>
          <div className="field"><label>文档标题</label><input value={documentForm.title} onChange={(event) => setDocumentForm({ ...documentForm, title: event.target.value })} required /></div>
          <div className="field"><label>文档类型</label><select value={documentForm.documentType} onChange={(event) => setDocumentForm({ ...documentForm, documentType: event.target.value })}>{DOCUMENT_TYPE_OPTIONS.map((item) => <option value={item.value} key={item.value}>{item.label}</option>)}</select></div>
          <div className="field"><label>目标篇幅</label><input type="number" min="100" max="300000" value={documentForm.targetLength} onChange={(event) => setDocumentForm({ ...documentForm, targetLength: event.target.value })} /></div>
          <div className="field"><label>引用格式</label><select value={documentForm.citationStyle} onChange={(event) => setDocumentForm({ ...documentForm, citationStyle: event.target.value })}><option value="APA">APA</option><option value="GB/T 7714">GB/T 7714</option><option value="MLA">MLA</option><option value="CHICAGO">Chicago</option></select></div>
          <div className="field"><label>目标学校 / 机构</label><input value={documentForm.targetInstitution} onChange={(event) => setDocumentForm({ ...documentForm, targetInstitution: event.target.value })} /></div>
          <div className="field"><label>目标期刊 / 场景</label><input value={documentForm.targetVenue} onChange={(event) => setDocumentForm({ ...documentForm, targetVenue: event.target.value })} /></div>
          <button className="primary-btn icon-text-btn" disabled={busyAction === "create-document"}><Plus size={16} aria-hidden="true" /> 创建文档与章节树</button>
        </form>
      )}

      {showCreateSection && (
        <form className="academic-inline-form academic-section-form" onSubmit={createSection}>
          <div className="field"><label>章节标题</label><input value={sectionForm.title} onChange={(event) => setSectionForm({ ...sectionForm, title: event.target.value })} placeholder="例如：研究限制" required /></div>
          <div className="field"><label>章节类型</label><input value={sectionForm.sectionType} onChange={(event) => setSectionForm({ ...sectionForm, sectionType: event.target.value.toUpperCase() })} /></div>
          <div className="field"><label>目标篇幅</label><input type="number" min="100" value={sectionForm.targetLength} onChange={(event) => setSectionForm({ ...sectionForm, targetLength: event.target.value })} /></div>
          <button className="primary-btn icon-text-btn"><Plus size={16} aria-hidden="true" /> 添加章节</button>
        </form>
      )}

      <div className="academic-workbench-mode" role="tablist" aria-label="学术文档工作模式">
        <button
          type="button"
          role="tab"
          aria-selected={workbenchMode === "section"}
          className={workbenchMode === "section" ? "is-active" : ""}
          onClick={() => setWorkbenchMode("section")}
        >
          <PenLine size={16} aria-hidden="true" /> 章节写作
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={workbenchMode === "whole"}
          className={workbenchMode === "whole" ? "is-active" : ""}
          onClick={() => setWorkbenchMode("whole")}
        >
          <FileCheck2 size={16} aria-hidden="true" /> 整篇检查
        </button>
      </div>

      {workbenchMode === "section" ? (
        <div className="academic-workbench academic-workbench--unified">
          <AcademicSectionNavigator
            sections={sections}
            selectedSectionId={selectedSection?.id}
            onSelect={(section) => setSelectedSectionId(section.id)}
            onCreate={() => setShowCreateSection((value) => !value)}
            onReorder={reorderSections}
            reordering={busyAction === "reorder-sections"}
          />
          <AcademicSectionEditor
            section={selectedSection}
            draft={sectionDraft}
            onDraftChange={setSectionDraft}
            readiness={sectionReadiness ?? readiness}
            busyAction={busyAction}
            onSave={saveSection}
            generateInstruction={generateInstruction}
            onGenerateInstructionChange={setGenerateInstruction}
            onGenerate={generateSection}
            coWriteAction={coWriteAction}
            onCoWriteActionChange={setCoWriteAction}
            coWriteInstruction={coWriteInstruction}
            onCoWriteInstructionChange={setCoWriteInstruction}
            controls={controls}
            onControlsChange={setControls}
            onPreview={previewCoWrite}
            versions={versions}
            onRestoreVersion={restoreVersion}
            selectedRange={selectedRange}
            onSelectionChange={setSelectedRange}
            assistantOpen={assistantOpen}
            onToggleAssistant={() => setAssistantOpen((value) => !value)}
            onOpenChecks={() => setChecksOpen(true)}
            onReviewSection={() => refreshSectionReviews()}
            onSplitLegacy={previewLegacySplit}
          />
          <AcademicInspector
            readiness={sectionReadiness ?? readiness}
            evidence={sectionEvidence}
            risks={sectionRisks}
            reviews={reviews.filter((item) => item.sectionId === selectedSection?.id)}
            materials={materials}
            materialLinks={materialLinks}
            onToggleMaterial={toggleMaterial}
            materialUpdating={materialUpdating}
            aiActions={aiActions}
            onOpenChecks={() => setChecksOpen(true)}
          />
        </div>
      ) : (
        <AcademicDocumentQualityView
          document={activeDocument}
          quality={quality}
          assembled={assembled}
          busyAction={busyAction}
          downloadUrl={downloadUrl}
          onAssemble={assembleDocument}
          onExport={exportDocument}
          onRefreshReview={refreshDocumentReviews}
          onRefreshEvidence={rebuildDocumentEvidence}
          onOpenSection={(sectionId) => {
            setSelectedSectionId(sectionId);
            setWorkbenchMode("section");
          }}
          onOpenReview={openReview}
        />
      )}

      <AcademicChecksDrawer
        open={checksOpen}
        onClose={() => setChecksOpen(false)}
        evidence={sectionEvidence}
        risks={sectionRisks}
        reviews={reviews.filter((item) => item.sectionId === selectedSection?.id)}
        loading={["evidence", "section-review"].includes(busyAction)}
        onRebuildEvidence={() => rebuildSectionEvidence()}
        onRefreshReviews={() => refreshSectionReviews()}
        onUpdateEvidence={updateEvidence}
        onPreviewMaterial={previewMaterial}
        onInsertCitation={insertCitation}
        onLocate={locateRange}
        onFixRisk={fixWritingRisk}
        onOpenReview={openReview}
      />
      <AcademicCoWritePreviewDrawer
        preview={preview}
        applying={busyAction === "apply-preview"}
        discarding={busyAction === "discard-preview"}
        onApply={applyPreview}
        onDiscard={discardPreview}
      />
      <WorkspaceReviewDrawer
        review={activeReview}
        onClose={() => setActiveReview(null)}
        onOpenAppeal={(review) => setAppealReview(review)}
        onFixReview={fixReview}
        onUpdateReviewStatus={updateReviewStatus}
        onRecheckReview={recheckReview}
        rechecking={busyAction === `recheck-${activeReview?.id}`}
      />
      <WorkspaceAppealModal
        review={appealReview}
        onClose={() => setAppealReview(null)}
        onSubmit={submitAppeal}
        loading={appealLoading}
      />
      <AcademicSplitModal
        preview={splitPreview}
        applying={busyAction === "split-apply"}
        onClose={() => setSplitPreview(null)}
        onApply={applyLegacySplit}
      />
    </section>
  );
}

function ProfileFields({ value, onChange }) {
  return (
    <>
      <div className="field"><label>学术阶段</label><select value={value.academicStage} onChange={(event) => onChange({ ...value, academicStage: event.target.value })}>{ACADEMIC_STAGE_OPTIONS.map((item) => <option value={item.value} key={item.value}>{item.label}</option>)}</select></div>
      <div className="field"><label>学科方向</label><select value={value.disciplineGroup} onChange={(event) => onChange({ ...value, disciplineGroup: event.target.value })}>{DISCIPLINE_OPTIONS.map((item) => <option value={item.value} key={item.value}>{item.label}</option>)}</select></div>
      <div className="field"><label>研究范式</label><select value={value.researchParadigm} onChange={(event) => onChange({ ...value, researchParadigm: event.target.value })}>{PARADIGM_OPTIONS.map((item) => <option value={item.value} key={item.value}>{item.label}</option>)}</select></div>
      <div className="field"><label>默认引用格式</label><select value={value.defaultCitationStyle} onChange={(event) => onChange({ ...value, defaultCitationStyle: event.target.value })}><option value="APA">APA</option><option value="GB/T 7714">GB/T 7714</option><option value="MLA">MLA</option><option value="CHICAGO">Chicago</option></select></div>
      <div className="field"><label>学校 / 机构</label><input value={value.institution ?? ""} onChange={(event) => onChange({ ...value, institution: event.target.value })} /></div>
      <div className="field"><label>AI 使用策略</label><select value={value.aiUsagePolicy} onChange={(event) => onChange({ ...value, aiUsagePolicy: event.target.value })}>{AI_POLICY_OPTIONS.map((item) => <option value={item.value} key={item.value}>{item.label}</option>)}</select></div>
    </>
  );
}

function initialDocumentForm(title, stage = "UNDERGRADUATE") {
  const defaults = defaultDocumentForStage(stage);
  return {
    title,
    documentType: defaults.documentType,
    targetLength: defaults.targetLength,
    lengthUnit: "WORDS",
    citationStyle: "APA",
    targetInstitution: "",
    targetVenue: ""
  };
}
