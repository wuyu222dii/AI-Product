import { useEffect, useState } from "react";
import { api } from "../services/api.js";
import {
  actionForReview,
  applySelectedDiffRows,
  buildCoWriteTargetRange,
  buildReviewInstruction,
  clampRange,
  formatReviewType,
  normalizeCitationStyle
} from "../components/workspace/workspaceUtils.js";

const EVIDENCE_REBUILD_POLL_INTERVAL_MS = 1200;
const EVIDENCE_REBUILD_MAX_ATTEMPTS = 90;

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export function useWorkspace({ workspace, draft, onDraftChange, onError }) {
  const [draftText, setDraftText] = useState(draft?.draftText ?? "");
  const [reviews, setReviews] = useState([]);
  const [reviewsLoading, setReviewsLoading] = useState(false);
  const [coWriting, setCoWriting] = useState(false);
  const [savingDraft, setSavingDraft] = useState(false);
  const [latestFeedback, setLatestFeedback] = useState("");
  const [appealState, setAppealState] = useState({});
  const [activeReview, setActiveReview] = useState(null);
  const [appealReview, setAppealReview] = useState(null);
  const [appealLoading, setAppealLoading] = useState(false);
  const [versions, setVersions] = useState([]);
  const [materials, setMaterials] = useState([]);
  const [citationStyle, setCitationStyle] = useState("APA");
  const [selectedRange, setSelectedRange] = useState(null);
  const [knowledgeResults, setKnowledgeResults] = useState([]);
  const [knowledgeSearching, setKnowledgeSearching] = useState(false);
  const [restoringVersion, setRestoringVersion] = useState(false);
  const [evidenceSummary, setEvidenceSummary] = useState(null);
  const [evidenceLoading, setEvidenceLoading] = useState(false);
  const [evidenceRebuildJob, setEvidenceRebuildJob] = useState(null);
  const [coWritePreview, setCoWritePreview] = useState(null);
  const [applyingPreview, setApplyingPreview] = useState(false);
  const [discardingPreview, setDiscardingPreview] = useState(false);
  const [recheckingReviewId, setRecheckingReviewId] = useState(null);

  useEffect(() => {
    setDraftText(draft?.draftText ?? "");
    setSelectedRange(null);
    setCoWritePreview(null);
  }, [draft?.id]);

  async function loadVersions(workspaceId) {
    if (!workspaceId) return [];
    const data = await api.listDrafts(workspaceId);
    const items = data.items ?? [];
    setVersions(items);
    return items;
  }

  async function loadReviews(draftId, cancelled = false) {
    if (!draftId) return;
    try {
      setReviewsLoading(true);
      const data = await api.listReviewItems(draftId);
      if (!cancelled) {
        setReviews(data.items ?? []);
      }
    } catch (error) {
      if (!cancelled) onError(error.message);
    } finally {
      if (!cancelled) setReviewsLoading(false);
    }
  }

  async function loadEvidenceBindings(draftId, cancelled = false) {
    if (!draftId) return null;
    try {
      setEvidenceLoading(true);
      const data = await api.getEvidenceBindings(draftId);
      if (!cancelled) {
        setEvidenceSummary(data);
      }
      return data;
    } catch (error) {
      if (!cancelled) onError(error.message);
      return null;
    } finally {
      if (!cancelled) setEvidenceLoading(false);
    }
  }

  async function loadMaterials(workspaceId, cancelled = false) {
    if (!workspaceId) return;
    try {
      const data = await api.listMaterials(workspaceId);
      if (!cancelled) {
        setMaterials(data.items ?? []);
      }
    } catch (error) {
      if (!cancelled) onError(error.message);
    }
  }

  async function loadRequirementSnapshot(workspaceId, cancelled = false) {
    if (!workspaceId) return;
    try {
      const snapshot = await api.getRequirementSnapshot(workspaceId);
      if (!cancelled && snapshot?.citationStyle) {
        setCitationStyle(normalizeCitationStyle(snapshot.citationStyle));
      }
    } catch {
      // Citation style is optional for the editor; keep APA as the safe default.
    }
  }

  useEffect(() => {
    if (!draft?.id) return;
    let cancelled = false;
    async function bootstrap() {
      try {
        await loadVersions(workspace.id);
        await loadMaterials(workspace.id, cancelled);
        await loadRequirementSnapshot(workspace.id, cancelled);
        await loadReviews(draft.id, cancelled);
        await loadEvidenceBindings(draft.id, cancelled);
      } catch (error) {
        if (!cancelled) onError(error.message);
      }
    }
    bootstrap();
    return () => {
      cancelled = true;
    };
  }, [draft?.id, workspace?.id]);

  async function handleCoWrite(action, instruction, rangeOverride = null, controls = {}) {
    if (!workspace?.id || !draft?.id) return;
    try {
      setCoWriting(true);
      let effectiveRange = rangeOverride ?? selectedRange;
      if (!effectiveRange) {
        const reviewWithRange = reviews.find((review) => clampRange(review?.targetRange, draftText));
        if (reviewWithRange) {
          effectiveRange = clampRange(reviewWithRange.targetRange, draftText);
          setSelectedRange(effectiveRange);
          setLatestFeedback(`未手动选择正文，已优先定位到审查项「${formatReviewType(reviewWithRange.reviewType)}」`);
        }
      }
      if (!effectiveRange && draftText.length > 2500) {
        setLatestFeedback("当前正文较长。请先选中需要处理的段落，或打开某条审查项后再执行 AI 共写。");
        return;
      }
      const targetRange = buildCoWriteTargetRange(draftText, effectiveRange);
      const preview = await api.previewCoWrite(workspace.id, {
        draftVersionId: draft.id,
        action,
        targetRange,
        instruction,
        controls
      });
      setCoWritePreview(preview);
      const scopeLabel = targetRange.mode === "selection" ? `字符 ${targetRange.start}-${targetRange.end}` : "全文";
      const actionLabel = action.replace(/_/g, " ");
      setLatestFeedback(`AI 已生成${scopeLabel}的「${actionLabel}」修改预览，请确认差异后再应用为新版本。`);
    } catch (error) {
      onError(error.message);
    } finally {
      setCoWriting(false);
    }
  }

  async function handleFixReview(review) {
    const normalized = clampRange(review?.targetRange, draftText);
    if (normalized) {
      setSelectedRange(normalized);
    }
    setActiveReview(null);
    await handleCoWrite(actionForReview(review), buildReviewInstruction(review), normalized, {
      rewriteDepth: "balanced",
      keepCitations: true,
      keepData: true,
      noNewSources: true,
      keepStudentVoice: true
    });
  }

  async function handleReviewStatus(review, status, resolutionNote) {
    if (!review?.id || !draft?.id) return;
    try {
      const updated = await api.updateReviewStatus(review.id, { status, resolutionNote });
      setReviews((current) => current.map((item) => (item.id === review.id ? updated : item)));
      if (activeReview?.id === review.id) {
        setActiveReview(updated);
      }
      const statusLabel = status === "RESOLVED" ? "已解决" : status === "IGNORED" ? "已忽略" : "已重新打开";
      setLatestFeedback(`审查项「${formatReviewType(review.reviewType)}」${statusLabel}`);
    } catch (error) {
      onError(error.message);
    }
  }

  async function handleRecheckReview(review) {
    if (!review?.id || !draft?.id) return;
    try {
      setRecheckingReviewId(review.id);
      const updated = await api.recheckReviewItem(review.id);
      setReviews((current) => current.map((item) => (item.id === review.id ? updated : item)));
      if (activeReview?.id === review.id) {
        setActiveReview(updated);
      }
      setLatestFeedback(`已复查「${formatReviewType(review.reviewType)}」：${updated.recheckNote || "复查完成"}`);
    } catch (error) {
      onError(error.message);
    } finally {
      setRecheckingReviewId(null);
    }
  }

  async function handleSaveDraft() {
    if (!draft?.id) return;
    try {
      setSavingDraft(true);
      const updated = await api.updateDraft(draft.id, {
        titleSuggestion: draft.titleSuggestion,
        draftText
      });
      onDraftChange(updated);
      const evidence = await rebuildEvidenceBindingsWithJob(
        updated.id,
        `版本 v${updated.versionNo} 的正文修改已保存，可信链正在后台重建。`
      );
      setLatestFeedback(`版本 v${updated.versionNo} 已保存，可信链已重建：${evidence?.missingParagraphIds?.length ?? 0} 个段落仍缺少明确来源。`);
    } catch (error) {
      onError(error.message);
    } finally {
      setSavingDraft(false);
    }
  }

  async function handleRebuildEvidenceBindings() {
    if (!draft?.id) return;
    try {
      await rebuildEvidenceBindingsWithJob(draft.id, "可信链已进入后台重建，完成后会自动刷新证据地图。");
    } catch (error) {
      onError(error.message);
    }
  }

  async function rebuildEvidenceBindingsWithJob(draftId, startedMessage) {
    setEvidenceLoading(true);
    try {
      const job = await api.rebuildEvidenceBindings(draftId);
      if (job?.paragraphs) {
        setEvidenceSummary(job);
        return job;
      }

      setEvidenceRebuildJob({
        jobId: job.jobId,
        status: job.status || "running",
        progress: 5
      });
      setLatestFeedback(startedMessage);

      const finishedJob = await waitForEvidenceRebuildJob(job.jobId);
      const data = await api.getEvidenceBindings(draftId);
      setEvidenceSummary(data);
      setEvidenceRebuildJob(finishedJob);
      setLatestFeedback(`可信链重建完成：${data.missingParagraphIds?.length ?? 0} 个段落仍缺少明确来源。`);
      return data;
    } finally {
      setEvidenceLoading(false);
    }
  }

  async function waitForEvidenceRebuildJob(jobId) {
    if (!jobId) {
      throw new Error("可信链重建任务未正确创建，请稍后重试。");
    }

    for (let attempt = 0; attempt < EVIDENCE_REBUILD_MAX_ATTEMPTS; attempt += 1) {
      const job = await api.getJob(jobId);
      setEvidenceRebuildJob(job);
      if (job.status === "success") {
        return job;
      }
      if (job.status === "failed") {
        throw new Error(job.errorMessage || "可信链后台重建失败，请稍后重试。");
      }
      await sleep(EVIDENCE_REBUILD_POLL_INTERVAL_MS);
    }

    throw new Error("可信链后台重建仍未完成，请稍后刷新任务状态或重新尝试。");
  }

  async function handleUpdateEvidenceBindingStatus(binding, status) {
    if (!binding?.id) return;
    try {
      const updated = await api.updateEvidenceBindingStatus(binding.id, { status });
      setEvidenceSummary((current) => replaceEvidenceBinding(current, updated));
      const label = status === "USER_CONFIRMED" ? "已确认可信" : status === "WEAK" ? "已标记需补充" : "状态已更新";
      setLatestFeedback(`证据绑定「${binding.paragraphId}」${label}`);
    } catch (error) {
      onError(error.message);
    }
  }

  async function handleApplyCoWritePreview() {
    if (!coWritePreview?.id || !workspace?.id) return;
    try {
      setApplyingPreview(true);
      const nextDraft = await api.applyCoWritePreview(coWritePreview.id);
      setDraftText(nextDraft.draftText ?? "");
      setSelectedRange(null);
      setCoWritePreview(null);
      onDraftChange(nextDraft);
      await loadVersions(workspace.id);
      await loadReviews(nextDraft.id);
      await loadEvidenceBindings(nextDraft.id);
      setLatestFeedback(`预览已应用为新版本 v${nextDraft.versionNo}，建议复查相关审查项并查看可信链。`);
    } catch (error) {
      onError(error.message);
    } finally {
      setApplyingPreview(false);
    }
  }

  async function handleDiscardCoWritePreview() {
    if (!coWritePreview?.id) {
      setCoWritePreview(null);
      return;
    }
    try {
      setDiscardingPreview(true);
      await api.discardCoWritePreview(coWritePreview.id);
      setCoWritePreview(null);
      setLatestFeedback("已放弃本次 AI 修改预览，当前正文未发生变化。");
    } catch (error) {
      onError(error.message);
    } finally {
      setDiscardingPreview(false);
    }
  }

  function handleInsertCitation(citationText) {
    const citation = String(citationText || "").trim();
    if (!citation) return;
    const insertAt = selectedRange?.end ?? draftText.length;
    const nextText = `${draftText.slice(0, insertAt)}${citation}${draftText.slice(insertAt)}`;
    setDraftText(nextText);
    setSelectedRange(null);
    setLatestFeedback(`已插入引用 ${citation}，请确认位置后保存正文。`);
  }

  function handleLocateEvidence(binding) {
    const range = clampRange(binding?.targetRange, draftText);
    if (!range) {
      setLatestFeedback("这条证据暂时没有可定位的正文范围，请在段落卡片中人工核对。");
      return;
    }
    setSelectedRange(range);
    setLatestFeedback(`已定位可信链「${binding.paragraphId || "段落"}」对应的正文范围。`);
  }

  function handleApplySelectedCoWriteDiffRows(rows) {
    if (!coWritePreview) return;
    const selectedRows = rows.filter((row) => row.selected);
    if (selectedRows.length === 0) {
      setLatestFeedback("未选择任何差异，当前正文未发生变化。");
      return;
    }
    const nextText = applySelectedDiffRows(draftText, rows);
    setDraftText(nextText);
    setCoWritePreview(null);
    setLatestFeedback(`已将 ${selectedRows.length} 条选中差异应用到编辑区，请保存正文后重建可信链。`);
  }

  async function handleKnowledgeSearch(query) {
    if (!workspace?.id || !query?.trim()) return;
    try {
      setKnowledgeSearching(true);
      const data = await api.searchKnowledgeBase(workspace.id, query.trim(), 5);
      setKnowledgeResults(data.items ?? []);
      setLatestFeedback(`已从项目知识库找到 ${data.total ?? 0} 条相关证据片段`);
    } catch (error) {
      onError(error.message);
    } finally {
      setKnowledgeSearching(false);
    }
  }

  function handleAcceptCurrentVersion() {
    if (!draft?.versionNo) return;
    setLatestFeedback(`已保留当前版本 v${draft.versionNo}，可继续共写、审查或导出。`);
  }

  async function handleRestoreVersion(version) {
    if (!version?.id || !workspace?.id) return;
    try {
      setRestoringVersion(true);
      const restored = await api.restoreDraft(version.id);
      setDraftText(restored.draftText ?? "");
      setSelectedRange(null);
      onDraftChange(restored);
      await loadVersions(workspace.id);
      await loadReviews(restored.id);
      await loadEvidenceBindings(restored.id);
      setLatestFeedback(`已撤回并恢复到版本 v${restored.versionNo}`);
    } catch (error) {
      onError(error.message);
    } finally {
      setRestoringVersion(false);
    }
  }

  async function handleSubmitAppeal(payload) {
    if (!appealReview) return;
    try {
      setAppealLoading(true);
      const appeal = await api.createAppeal(appealReview.id, payload);
      if (appeal?.id) {
        const latestAppeal = await api.getAppeal(appeal.id);
        setAppealState((prev) => ({
          ...prev,
          [appealReview.id]: latestAppeal.reviewOutcome || "已提交复审"
        }));
        await loadReviews(draft.id);
        setLatestFeedback(`审查项「${appealReview.reviewType}」的复审请求已提交`);
      }
      setAppealReview(null);
    } catch (error) {
      onError(error.message);
    } finally {
      setAppealLoading(false);
    }
  }

  function openAppeal(review) {
    setActiveReview(null);
    setAppealReview(review);
  }

  function handleSelectionChange(range) {
    setSelectedRange(clampRange(range, draftText));
  }

  function openReview(review) {
    setActiveReview(review);
    const normalized = clampRange(review?.targetRange, draftText);
    if (normalized) {
      setSelectedRange(normalized);
      setLatestFeedback(`已定位审查项「${formatReviewType(review.reviewType)}」对应的正文范围`);
    }
  }

  return {
    draftText,
    setDraftText,
    reviews,
    reviewsLoading,
    coWriting,
    savingDraft,
    latestFeedback,
    appealState,
    activeReview,
    setActiveReview,
    appealReview,
    setAppealReview,
    appealLoading,
    versions,
    materials,
    citationStyle,
    setCitationStyle,
    selectedRange,
    knowledgeResults,
    knowledgeSearching,
    restoringVersion,
    evidenceSummary,
    evidenceLoading,
    evidenceRebuildJob,
    coWritePreview,
    applyingPreview,
    discardingPreview,
    recheckingReviewId,
    handleCoWrite,
    handleFixReview,
    handleReviewStatus,
    handleRecheckReview,
    handleSaveDraft,
    handleRebuildEvidenceBindings,
    handleUpdateEvidenceBindingStatus,
    handleLocateEvidence,
    handleInsertCitation,
    handleKnowledgeSearch,
    handleAcceptCurrentVersion,
    handleRestoreVersion,
    handleApplyCoWritePreview,
    handleApplySelectedCoWriteDiffRows,
    handleDiscardCoWritePreview,
    handleSubmitAppeal,
    handleSelectionChange,
    openReview,
    openAppeal
  };
}

function replaceEvidenceBinding(summary, updated) {
  if (!summary?.paragraphs || !updated?.id) return summary;
  return {
    ...summary,
    paragraphs: summary.paragraphs.map((paragraph) => ({
      ...paragraph,
      bindings: (paragraph.bindings ?? []).map((binding) => (binding.id === updated.id ? updated : binding)),
      bindingStatus: paragraph.bindings?.some((binding) => binding.id === updated.id)
        ? recomputeParagraphStatus((paragraph.bindings ?? []).map((binding) => (binding.id === updated.id ? updated : binding)))
        : paragraph.bindingStatus
    }))
  };
}

function recomputeParagraphStatus(bindings = []) {
  if (bindings.length === 0) return "MISSING";
  if (bindings.some((item) => item.bindingStatus === "MISSING")) return "MISSING";
  if (bindings.some((item) => item.bindingStatus === "WEAK")) return "WEAK";
  if (bindings.some((item) => item.bindingStatus === "USER_CONFIRMED")) return "USER_CONFIRMED";
  return "CONFIRMED";
}
