import { useEffect, useRef, useState } from "react";
import { WorkspacePanel } from "./WorkspacePanel.jsx";
import {
  buildCitationSuggestions,
  CITATION_STYLE_OPTIONS,
  citationTextForMaterial,
  collectReferenceMaterials,
  describeSelection,
  evidenceStrength,
  formatSourceLocation,
  hasValidRange,
  materialEvidenceBinding,
  materialTitle,
  materialsById,
  sourceTraceEntries,
  summarizeEvidenceCoverage,
  summarizeTrustChain
} from "./workspaceUtils.js";

export function WorkspaceEditorPanel({
  workspace,
  draft,
  draftText,
  saving,
  materials = [],
  citationStyle = "APA",
  selectedRange,
  onDraftTextChange,
  onSelectionChange,
  onCitationStyleChange,
  onInsertCitation,
  evidenceSummary,
  evidenceLoading = false,
  evidenceRebuildJob,
  onRebuildEvidenceBindings,
  onUpdateEvidenceBindingStatus,
  onPreviewMaterial,
  onLocateEvidence,
  writingRisks,
  writingRisksLoading = false,
  onLocateWritingRisk,
  onFixWritingRisk,
  knowledgeResults = [],
  knowledgeSearching = false,
  onKnowledgeSearch,
  onSaveDraftText
}) {
  const [activeAssistTab, setActiveAssistTab] = useState("trust");
  const textareaRef = useRef(null);
  const title = draft?.titleSuggestion || workspace?.title || "正文编辑";
  const traceEntries = sourceTraceEntries(draft?.sourceTraceMap);
  const trustSummary = summarizeTrustChain(draft);
  const materialLookup = materialsById(materials);
  const citationSuggestions = buildCitationSuggestions(draft, materials);
  const referenceMaterials = collectReferenceMaterials(draft, materials);
  const citationIndexByMaterialId = new Map(
    referenceMaterials.map((material, index) => [String(material.id), index + 1])
  );
  const selectedQuery = selectedRange?.selectedText || "";
  const evidenceParagraphs = evidenceSummary?.paragraphs ?? [];
  const hasEvidenceMap = evidenceParagraphs.length > 0;
  const evidenceJobLabel = evidenceRebuildJob ? formatEvidenceJob(evidenceRebuildJob) : null;
  const evidenceCoverage = evidenceSummary?.coverage ?? summarizeEvidenceCoverage(evidenceSummary);
  const citationConsistency = evidenceSummary?.citationConsistency;
  const writingRiskItems = writingRisks?.items ?? [];

  useEffect(() => {
    const textarea = textareaRef.current;
    if (!textarea || !hasValidRange(selectedRange)) return;
    textarea.focus();
    textarea.setSelectionRange(selectedRange.start, selectedRange.end);
  }, [selectedRange?.start, selectedRange?.end]);

  function handleSelect(event) {
    const { selectionStart, selectionEnd, value } = event.currentTarget;
    if (selectionStart === selectionEnd) {
      onSelectionChange(null);
      return;
    }
    onSelectionChange({
      start: selectionStart,
      end: selectionEnd,
      selectedText: value.slice(selectionStart, selectionEnd)
    });
  }

  return (
    <WorkspacePanel className="editor-card" compact>
      <div className="editor-toolbar">
        <span className="toolbar-pill toolbar-pill--primary">{title}</span>
        <span className="toolbar-pill">v{draft?.versionNo ?? "-"}</span>
        <span className="toolbar-pill">{draftText.length} 字</span>
        <span className={`toolbar-pill ${hasValidRange(selectedRange) ? "toolbar-pill--selected" : ""}`}>
          {hasValidRange(selectedRange) ? `选中 ${selectedRange.end - selectedRange.start} 字` : "全文模式"}
        </span>
      </div>
      <div className="editor-body">
        <div className="selection-status">
          <span>{describeSelection(selectedRange)}</span>
        </div>
        <textarea
          ref={textareaRef}
          value={draftText}
          onChange={(event) => {
            onDraftTextChange(event.target.value);
            handleSelect(event);
          }}
          onSelect={handleSelect}
          placeholder="在此编辑正文内容…"
          aria-label="正文编辑区"
        />
        <div className="editor-footer">
          <p className="editor-hint muted">修改后请保存，再进行 AI 共写或导出。</p>
          <button type="button" className="secondary-btn" onClick={onSaveDraftText} disabled={saving}>
            {saving ? "保存中…" : "保存修改"}
          </button>
        </div>

        <div className="source-trace-section">
          <div className="workspace-assist-tabs" role="tablist" aria-label="正文辅助工具">
            {[
              { key: "trust", label: "可信链", count: hasEvidenceMap ? evidenceParagraphs.length : traceEntries.length },
              { key: "originality", label: "原创补强", count: writingRiskItems.length },
              { key: "knowledge", label: "知识库", count: knowledgeResults.length },
              { key: "citation", label: "引用", count: citationSuggestions.length }
            ].map((tab) => (
              <button
                key={tab.key}
                type="button"
                role="tab"
                aria-selected={activeAssistTab === tab.key}
                className={`workspace-assist-tab ${activeAssistTab === tab.key ? "active" : ""}`}
                onClick={() => setActiveAssistTab(tab.key)}
              >
                {tab.label}
                <span>{tab.count}</span>
              </button>
            ))}
          </div>

          {activeAssistTab === "trust" && (
            <div className="assist-tab-panel">
              <div className="source-trace-header">
                <div className="source-trace-title">
                  <strong>材料可信链</strong>
                  <span className="muted">查看正文段落、证据片段、原始材料与正文引用之间的链路</span>
                  {evidenceJobLabel && <span className="muted">{evidenceJobLabel}</span>}
                </div>
                <div className="trust-summary">
                  <span>
                    {hasEvidenceMap
                      ? `${evidenceParagraphs.filter((item) => item.bindingStatus !== "MISSING").length}/${evidenceParagraphs.length} 段有来源`
                      : `${trustSummary.linkedParagraphs}/${trustSummary.totalParagraphs || 0} 段有来源`}
                  </span>
                  {hasEvidenceMap && <span>{evidenceCoverage.healthLabel}</span>}
                  {hasEvidenceMap && <span>确认率 {evidenceCoverage.confirmedRatio}%</span>}
                  {citationConsistency && <span>引用状态 {formatCitationStatus(citationConsistency.status)}</span>}
                  <span>{evidenceSummary?.usedMaterials?.length ?? trustSummary.uniqueMaterialCount} 份已使用材料</span>
                  <button type="button" className="ghost-btn" onClick={onRebuildEvidenceBindings} disabled={evidenceLoading}>
                    {evidenceLoading ? "重建中..." : "重建可信链"}
                  </button>
                </div>
              </div>
          {hasEvidenceMap ? (
            <div className="evidence-map-list">
              {evidenceParagraphs.map((paragraph) => (
                <EvidenceParagraphCard
                  key={paragraph.paragraphId}
                  paragraph={paragraph}
                  citationStyle={citationStyle}
                  referenceMaterials={referenceMaterials}
                  onInsertCitation={onInsertCitation}
                  onUpdateEvidenceBindingStatus={onUpdateEvidenceBindingStatus}
                  onPreviewMaterial={onPreviewMaterial}
                  onLocateEvidence={onLocateEvidence}
                />
              ))}
            </div>
          ) : traceEntries.length === 0 ? (
            <div className="mini-card">
              <p className="muted">当前版本还没有可展示的来源追溯信息。建议先补充材料或重新生成初稿。</p>
            </div>
          ) : (
            <div className="trust-chain-list">
              {traceEntries.map((entry) => (
                <div className="trust-chain-card" key={entry.paragraphId}>
                  <div className="trust-chain-card-head">
                    <strong>{entry.paragraphId}</strong>
                    <span className={`trust-state ${entry.materialIds.length > 0 ? "ready" : "warn"}`}>
                      {entry.confidenceLabel}
                    </span>
                  </div>
                  <p className="muted">
                    {entry.materialIds.length > 0
                      ? `主要依据：${entry.materialIds
                          .map((id) => materialLookup.get(id))
                          .filter(Boolean)
                          .map(materialTitle)
                          .join("、") || entry.materialIds.join("、")}`
                      : "这一段暂未绑定明确材料，建议补充来源或重新审查。"}
                  </p>
                </div>
              ))}
            </div>
          )}

          {hasEvidenceMap && (
            <div className="evidence-coverage-panel">
              <div>
                <strong>可信链覆盖率 {evidenceCoverage.coverageRatio}%</strong>
                <p className="muted">
                  {evidenceCoverage.confirmedParagraphs ?? evidenceCoverage.confirmed} 段已确认，
                  {evidenceCoverage.weakParagraphs ?? evidenceCoverage.weak} 段弱绑定，
                  {evidenceCoverage.missingParagraphs ?? evidenceCoverage.missing} 段缺少明确来源。
                </p>
                {(evidenceCoverage.recommendations ?? []).map((item) => (
                  <p className="muted" key={item}>{item}</p>
                ))}
              </div>
              <div className="evidence-material-summary">
                <span className="toolbar-pill">缺来源段落 {evidenceSummary?.missingParagraphIds?.length ?? 0}</span>
                <span className="toolbar-pill">未使用材料 {evidenceSummary?.unusedMaterials?.length ?? 0}</span>
              </div>
            </div>
          )}

          {citationConsistency && (
            <div className={`evidence-coverage-panel evidence-coverage-panel--${String(citationConsistency.status || "READY").toLowerCase()}`}>
              <div>
                <strong>引用一致性：{formatCitationStatus(citationConsistency.status)}</strong>
                <p className="muted">
                  正文检测到 {citationConsistency.detectedCitationCount} 个引用标记，
                  可信链使用 {citationConsistency.linkedMaterialCount} 份材料，
                  {citationConsistency.incompleteReferenceCount} 份文献信息待补全。
                </p>
                {(citationConsistency.issues ?? []).map((issue) => (
                  <p className="muted" key={issue}>{issue}</p>
                ))}
              </div>
            </div>
          )}
            </div>
          )}

          {activeAssistTab === "originality" && (
            <OriginalityRiskPanel
              writingRisks={writingRisks}
              loading={writingRisksLoading}
              onLocateWritingRisk={onLocateWritingRisk}
              onFixWritingRisk={onFixWritingRisk}
            />
          )}

          {activeAssistTab === "knowledge" && (
          <div className="knowledge-evidence-section assist-tab-panel">
            <div className="source-trace-header">
              <div>
                <strong>知识库证据检索</strong>
                <span className="muted">用当前选中文本或题目检索可支撑的材料片段</span>
              </div>
              <button
                type="button"
                className="secondary-btn"
                disabled={knowledgeSearching || !onKnowledgeSearch}
                onClick={() => onKnowledgeSearch(selectedQuery || title)}
              >
                {knowledgeSearching ? "检索中..." : selectedQuery ? "检索选中内容" : "检索标题主题"}
              </button>
            </div>
            {knowledgeResults.length === 0 ? (
              <div className="mini-card">
                <p className="muted">选中一个论点或句子后点击检索，可以把知识库片段转成引用或证据补充。</p>
              </div>
            ) : (
              <div className="knowledge-evidence-list">
                {knowledgeResults.map((item) => {
                  const material = materialLookup.get(String(item.materialId));
                  const fallbackMaterial = { id: item.materialId, filename: item.materialTitle };
                  const citationText = citationTextForMaterial(
                    material || fallbackMaterial,
                    citationStyle,
                    citationIndexByMaterialId.get(String(item.materialId))
                  );
                  return (
                    <article className="knowledge-evidence-inline-card" key={item.id}>
                      <div className="citation-evidence-head">
                        <strong>{item.materialTitle || "未知材料"}</strong>
                        <span className="score-pill">匹配度 {Math.round(Number(item.score || 0) * 100)}%</span>
                      </div>
                      <p className="muted">{item.sourceExcerpt || item.chunkText}</p>
                      <div className="review-actions">
                        <button type="button" className="citation-chip" onClick={() => onInsertCitation(citationText)}>
                          插入引用 {citationText}
                        </button>
                        <button
                          type="button"
                          className="ghost-btn"
                          onClick={() => onDraftTextChange(`${draftText}\n\n证据补充：${item.sourceExcerpt || item.chunkText}`)}
                        >
                          追加证据说明
                        </button>
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </div>
          )}

          {activeAssistTab === "citation" && (
          <div className="citation-suggestion-section assist-tab-panel">
            <div className="source-trace-header">
              <div>
                <strong>引用建议</strong>
                <span className="muted">根据段落来源线索生成可插入的正文引用</span>
              </div>
              <label className="citation-style-select">
                <span>引用格式</span>
                <select value={citationStyle} onChange={(event) => onCitationStyleChange(event.target.value)}>
                  {CITATION_STYLE_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            {citationSuggestions.length === 0 ? (
              <div className="mini-card">
                <p className="muted">当前没有可生成引用建议的来源线索。</p>
              </div>
            ) : (
              <div className="citation-suggestion-list">
                {citationSuggestions.map((suggestion) => (
                  <article className="citation-card" key={`citation-${suggestion.paragraphId}`}>
                    <div className="citation-card-head">
                      <strong>{suggestion.paragraphId}</strong>
                      <span className={`trust-state ${suggestion.materials.length > 0 ? "ready" : "warn"}`}>
                        {suggestion.materials.length > 0 ? `${suggestion.materials.length} 条建议` : "缺少材料详情"}
                      </span>
                    </div>
                    {suggestion.materials.length === 0 ? (
                      <p className="muted">该段有来源 ID，但当前材料列表中没有找到对应材料。</p>
                    ) : (
                      <div className="citation-chip-row">
                        {suggestion.materials.map((material) => (
                          <CitationEvidenceCard
                            key={`${suggestion.paragraphId}-${material.id}`}
                            material={material}
                            citationStyle={citationStyle}
                            referenceIndex={citationIndexByMaterialId.get(String(material.id))}
                            onInsertCitation={onInsertCitation}
                          />
                        ))}
                      </div>
                    )}
                  </article>
                ))}
              </div>
            )}
          </div>
          )}
        </div>
      </div>
    </WorkspacePanel>
  );
}

function OriginalityRiskPanel({ writingRisks, loading, onLocateWritingRisk, onFixWritingRisk }) {
  const items = writingRisks?.items ?? [];
  const score = writingRisks?.overallScore ?? 100;
  const status = writingRisks?.overallStatus || "READY";

  return (
    <div className="originality-risk-section assist-tab-panel">
      <div className="source-trace-header">
        <div className="source-trace-title">
          <strong>原创实证与 AI 写作味风险</strong>
          <span className="muted">识别空泛论证、模板化表达和缺少案例/数据/来源支撑的段落，引导补真实材料，不承诺规避检测。</span>
        </div>
        <span className={`status-badge ${status === "READY" ? "ready" : "local_fix"}`}>
          {loading ? "检查中" : originalityStatusLabel(status)}
        </span>
      </div>

      <div className="originality-summary-card">
        <div>
          <strong>风险质量分 {score}</strong>
          <p className="muted">
            {items.length === 0
              ? "当前未发现明显空泛无据段落，仍建议人工通读并核对数据、案例和引用真实。"
              : `发现 ${items.length} 个建议补强段落，优先处理“原创实证不足”和“空泛论证”。`}
          </p>
        </div>
        <div className="originality-metrics">
          <span>{items.filter((item) => item.level === "LOCAL_FIX").length} 项需局部补强</span>
          <span>{items.filter((item) => item.riskType === "original_evidence_missing").length} 项缺实证</span>
        </div>
      </div>

      {(writingRisks?.recommendations ?? []).length > 0 && (
        <div className="originality-recommendations">
          {writingRisks.recommendations.map((item) => (
            <p className="muted" key={item}>{item}</p>
          ))}
        </div>
      )}

      {items.length === 0 ? (
        <div className="mini-card">
          <p className="muted">这里会在发现高风险段落后展示补强建议。你也可以先选中正文，在右侧使用“补原创实证”。</p>
        </div>
      ) : (
        <div className="originality-risk-list">
          {items.map((item) => (
            <article className={`originality-risk-card originality-risk-card--${String(item.level || "NOTICE").toLowerCase()}`} key={`${item.paragraphId}-${item.riskType}`}>
              <div className="originality-risk-head">
                <div>
                  <strong>{item.paragraphId}｜{originalityRiskLabel(item.riskType)}</strong>
                  <span className="muted">{item.paragraphExcerpt}</span>
                </div>
                <span className={`trust-state ${item.level === "LOCAL_FIX" ? "warn" : "ready"}`}>
                  {item.level === "LOCAL_FIX" ? "建议补强" : "仅提示"}
                </span>
              </div>
              <div className="keyword-row">
                {(item.signals ?? []).map((signal) => (
                  <span key={signal}>{signal}</span>
                ))}
              </div>
              <p className="muted"><strong>建议：</strong>{item.suggestedAction}</p>
              <p className="muted"><strong>可补内容：</strong>{item.supplementPrompt}</p>
              <div className="review-actions">
                <button type="button" className="ghost-btn" onClick={() => onLocateWritingRisk?.(item)}>
                  定位段落
                </button>
                <button type="button" className="secondary-btn" onClick={() => onFixWritingRisk?.(item)}>
                  用已有材料补强
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}

function EvidenceParagraphCard({
  paragraph,
  citationStyle,
  referenceMaterials,
  onInsertCitation,
  onUpdateEvidenceBindingStatus,
  onPreviewMaterial,
  onLocateEvidence
}) {
  const statusLabel = {
    CONFIRMED: "已确认",
    USER_CONFIRMED: "用户确认",
    WEAK: "弱绑定",
    MISSING: "缺来源"
  }[paragraph.bindingStatus] || paragraph.bindingStatus;

  return (
    <article className={`evidence-map-card evidence-map-card--${String(paragraph.bindingStatus || "MISSING").toLowerCase()}`}>
      <div className="evidence-map-head">
        <div>
          <strong>{paragraph.paragraphId}</strong>
          <span className="muted">{paragraph.paragraphText || "暂无段落文本"}</span>
        </div>
        <span className={`trust-state ${paragraph.bindingStatus === "MISSING" ? "warn" : "ready"}`}>
          {statusLabel}
        </span>
      </div>

      {(paragraph.bindings ?? []).length === 0 ? (
        <p className="muted">这一段暂未找到明确材料来源，请补充文献/研究结果，或重新生成可信链。</p>
      ) : (
        <div className="evidence-binding-list">
          {paragraph.bindings.map((binding) => {
            const referenceIndex = referenceMaterials.findIndex((material) => String(material.id) === String(binding.materialId)) + 1;
            const strength = evidenceStrength(binding);
            const sourceLocation = formatSourceLocation(binding);
            const citationText = binding.citationText || citationTextForMaterial(
              {
                id: binding.materialId,
                filename: binding.materialTitle,
                bibliographicMetadata: binding.bibliographicMetadata
              },
              citationStyle,
              referenceIndex || undefined
            );
            return (
              <div className="evidence-binding-card" key={binding.id}>
                <div className="citation-evidence-head">
                  <strong>{binding.materialTitle || "暂未绑定材料"}</strong>
                  <span className={`trust-state ${strength.level === "strong" ? "ready" : "warn"}`}>
                    {strength.label}
                  </span>
                </div>
                <p className="muted evidence-strength-note">{strength.detail}</p>
                <div className={`source-location-note source-location-note--${sourceLocation.confidence}`}>
                  <strong>原始材料位置：{sourceLocation.label}</strong>
                  <p>{sourceLocation.detail}</p>
                </div>
                <p className="muted"><strong>段落主张：</strong>{binding.claimText || paragraph.paragraphText}</p>
                {binding.sourceExcerpt ? (
                  <p className="muted"><strong>证据片段：</strong>{binding.sourceExcerpt}</p>
                ) : (
                  <p className="muted"><strong>证据片段：</strong>暂无可确认片段，需要补充材料或重新解析。</p>
                )}
                {binding.bibliographicMetadata && Object.keys(binding.bibliographicMetadata).length > 0 && (
                  <p className="muted">
                    <strong>文献信息：</strong>
                    {[binding.bibliographicMetadata.authors?.join?.("、"), binding.bibliographicMetadata.year, binding.bibliographicMetadata.title]
                      .filter(Boolean)
                      .join(" ｜ ") || "已解析但信息不完整"}
                  </p>
                )}
                <EvidenceChainRail binding={binding} citationText={citationText} />
                <div className="review-actions">
                  <button type="button" className="ghost-btn" onClick={() => onLocateEvidence?.(binding)}>
                    定位正文
                  </button>
                  <button type="button" className="ghost-btn" onClick={() => onPreviewMaterial?.(binding)}>
                    打开原始材料
                  </button>
                  {citationText && (
                    <button type="button" className="citation-chip" onClick={() => onInsertCitation(citationText)}>
                      插入引用 {citationText}
                    </button>
                  )}
                  {binding.bindingStatus === "WEAK" && (
                    <button
                      type="button"
                      className="ghost-btn"
                      onClick={() => onUpdateEvidenceBindingStatus(binding, "USER_CONFIRMED")}
                    >
                      确认可信
                    </button>
                  )}
                  {binding.bindingStatus !== "MISSING" && (
                    <button
                      type="button"
                      className="ghost-btn"
                      onClick={() => onUpdateEvidenceBindingStatus(binding, "WEAK")}
                    >
                      标记需补充
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </article>
  );
}

function EvidenceChainRail({ binding, citationText }) {
  const metadata = binding.bibliographicMetadata || {};
  const hasBibliography = Object.keys(metadata).length > 0;
  const sourceLocation = formatSourceLocation(binding);
  const steps = [
    { label: "段落", value: binding.claimText || "当前段落主张", state: "ready" },
    { label: "证据", value: binding.sourceExcerpt || "缺少可确认片段", state: binding.sourceExcerpt ? "ready" : "warn" },
    { label: "材料", value: binding.materialTitle ? `${binding.materialTitle}｜${sourceLocation.label}` : "暂未绑定材料", state: binding.materialTitle ? "ready" : "warn" },
    {
      label: "文献",
      value: hasBibliography
        ? [metadata.authors?.join?.("、"), metadata.year, metadata.title].filter(Boolean).join(" ｜ ") || "文献信息待补全"
        : "文献信息待补全",
      state: hasBibliography ? "ready" : "warn"
    },
    { label: "引用", value: citationText || "待插入正文引用", state: citationText ? "ready" : "warn" }
  ];

  return (
    <div className="evidence-chain-rail" aria-label="段落证据链路">
      {steps.map((step, index) => (
        <div className={`evidence-chain-step evidence-chain-step--${step.state}`} key={`${step.label}-${index}`}>
          <span className="evidence-chain-dot">{index + 1}</span>
          <div>
            <strong>{step.label}</strong>
            <p>{step.value}</p>
          </div>
        </div>
      ))}
    </div>
  );
}

function originalityStatusLabel(status) {
  if (status === "READY") return "风险较低";
  if (status === "NEEDS_REVIEW") return "建议确认";
  if (status === "NEEDS_ORIGINAL_EVIDENCE") return "需要补实证";
  return status || "未检查";
}

function originalityRiskLabel(type) {
  const labels = {
    aigc_style_risk: "AI 写作味风险",
    generic_unsupported_claim: "空泛论证",
    original_evidence_missing: "原创实证不足"
  };
  return labels[type] || type || "写作风险";
}

function formatEvidenceJob(job) {
  if (!job) return null;
  if (job.status === "success") {
    return `重建任务已完成（${job.progress ?? 100}%）`;
  }
  if (job.status === "failed") {
    return `重建任务失败：${job.errorMessage || "请稍后重试"}`;
  }
  const progress = typeof job.progress === "number" ? `${job.progress}%` : "进行中";
  return `重建任务 ${progress}`;
}

function formatCitationStatus(status) {
  if (status === "READY") return "一致";
  if (status === "NEEDS_FIX") return "需修正";
  if (status === "NEEDS_REVIEW") return "建议确认";
  return status || "未检查";
}

function CitationEvidenceCard({ material, citationStyle, referenceIndex, onInsertCitation }) {
  const citationText = citationTextForMaterial(material, citationStyle, referenceIndex);
  const binding = materialEvidenceBinding(material);
  return (
    <div className="citation-evidence-card">
      <div className="citation-evidence-head">
        <strong>{materialTitle(material)}</strong>
        <button
          type="button"
          className="citation-chip"
          onClick={() => onInsertCitation(citationText)}
        >
          插入 {citationText}
        </button>
      </div>
      {binding.isEvidenceReady ? (
        <div className="citation-evidence-body">
          {binding.primaryClaim && <p className="muted"><strong>相关论点：</strong>{binding.primaryClaim}</p>}
          {binding.primaryEvidence && <p className="muted"><strong>证据摘录：</strong>{binding.primaryEvidence}</p>}
          {binding.primaryRequirement && <p className="muted"><strong>要求线索：</strong>{binding.primaryRequirement}</p>}
          <span className="toolbar-pill">
            {binding.claimCount} 条论点 / {binding.evidenceCount} 条证据
          </span>
        </div>
      ) : (
        <p className="muted citation-evidence-empty">这份材料暂未解析出明确论点或证据，建议补充说明后重新解析。</p>
      )}
    </div>
  );
}
