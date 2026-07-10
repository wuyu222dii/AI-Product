import { REVIEW_TYPE_LABELS } from "./constants.js";

export function normalizeImpactLevel(level) {
  return String(level ?? "").toLowerCase();
}

export function formatReviewType(type) {
  return REVIEW_TYPE_LABELS[type] || type || "未知问题";
}

export function formatImpactLabel(level) {
  const normalized = normalizeImpactLevel(level);
  if (normalized === "must_confirm") return "必须确认";
  if (normalized === "local_fix") return "局部修正";
  if (normalized === "notice") return "仅提示";
  return level || "未知";
}

export function formatReviewStatusLabel(status) {
  const normalized = String(status || "OPEN").toUpperCase();
  if (normalized === "RESOLVED") return "已解决";
  if (normalized === "IGNORED") return "已忽略";
  return "待处理";
}

export function countMustConfirm(reviews) {
  return reviews.filter((item) => normalizeImpactLevel(item.reviewImpactLevel) === "must_confirm" && item.reviewStatus !== "RESOLVED" && item.reviewStatus !== "IGNORED").length;
}

export function hasValidRange(range) {
  return Number.isFinite(Number(range?.start)) && Number.isFinite(Number(range?.end)) && Number(range.end) > Number(range.start);
}

export function clampRange(range, text = "") {
  if (!hasValidRange(range)) return null;
  const length = text.length;
  const start = Math.max(0, Math.min(Number(range.start), length));
  const end = Math.max(start, Math.min(Number(range.end), length));
  if (end <= start) return null;
  return {
    start,
    end,
    selectedText: text.slice(start, end)
  };
}

export function describeSelection(selection) {
  if (!hasValidRange(selection)) return "未选择局部内容，将作用于全文。";
  const selectedText = String(selection.selectedText || "");
  const preview = selectedText.length > 56 ? `${selectedText.slice(0, 56)}...` : selectedText;
  return `已选择字符 ${selection.start}-${selection.end}：${preview || "局部内容"}`;
}

export function buildCoWriteTargetRange(draftText, selection) {
  const normalized = clampRange(selection, draftText);
  if (!normalized) {
    return {
      mode: "full_draft",
      start: 0,
      end: draftText.length,
      selectedText: draftText.slice(0, 1200)
    };
  }
  return {
    mode: "selection",
    start: normalized.start,
    end: normalized.end,
    selectedText: normalized.selectedText
  };
}

export function sourceTraceEntries(sourceTraceMap = {}) {
  return Object.entries(sourceTraceMap ?? {}).map(([paragraphId, materialIds]) => {
    const ids = Array.isArray(materialIds) ? materialIds : [materialIds].filter(Boolean);
    return {
      paragraphId,
      materialIds: ids.map((item) => String(item)),
      confidenceLabel: ids.length > 0 ? "已有来源线索" : "待补来源"
    };
  });
}

export function materialTitle(material) {
  const title = material?.bibliographicMetadata?.title;
  if (title && String(title).trim()) return String(title).trim();
  const filename = String(material?.filename || "未命名材料");
  return filename.replace(/\.[^.]+$/, "").trim() || filename;
}

export function materialYear(material) {
  const year = material?.bibliographicMetadata?.year;
  if (year && String(year).trim()) return String(year).trim();
  const fromName = String(material?.filename || "").match(/\b(19|20)\d{2}\b/);
  if (fromName) return fromName[0];
  return "n.d.";
}

export const CITATION_STYLE_OPTIONS = [
  { value: "APA", label: "APA 作者-年份" },
  { value: "GBT_7714", label: "GB/T 7714 编号制" }
];

export function normalizeCitationStyle(style) {
  const normalized = String(style || "").toLowerCase();
  if (normalized.includes("gb") || normalized.includes("7714")) return "GBT_7714";
  return "APA";
}

export function citationTextForMaterial(material, citationStyle = "APA", referenceIndex) {
  if (normalizeCitationStyle(citationStyle) === "GBT_7714") {
    return `[${referenceIndex || "?"}]`;
  }
  return `（${citationAuthorLabel(material)}，${materialYear(material)}）`;
}

export function referenceTextForMaterial(material, citationStyle = "APA", referenceIndex) {
  if (normalizeCitationStyle(citationStyle) === "GBT_7714") {
    return formatGbtReference(material, referenceIndex);
  }
  return formatApaReference(material);
}

function formatApaReference(material) {
  const authors = referenceAuthors(material);
  const title = materialTitle(material);
  const year = materialYear(material);
  const source = firstNonEmpty(
    material?.bibliographicMetadata?.sourceTitle,
    material?.bibliographicMetadata?.publisher,
    formatMaterialCategory(material?.effectiveMaterialCategory || material?.aiMaterialCategory || material?.fileType)
  );
  const locator = firstNonEmpty(material?.bibliographicMetadata?.doi, material?.bibliographicMetadata?.url);
  if (!authors) {
    return joinReferenceParts([`${title}. (${year}).`, source, locator]);
  }
  return joinReferenceParts([`${authors}. (${year}).`, title, source, locator]);
}

function formatGbtReference(material, referenceIndex) {
  const authors = referenceAuthors(material);
  const title = materialTitle(material);
  const type = gbtPublicationType(material);
  const source = firstNonEmpty(
    material?.bibliographicMetadata?.sourceTitle,
    material?.bibliographicMetadata?.publisher,
    formatMaterialCategory(material?.effectiveMaterialCategory || material?.aiMaterialCategory || material?.fileType)
  );
  const year = materialYear(material);
  const locator = firstNonEmpty(material?.bibliographicMetadata?.doi, material?.bibliographicMetadata?.url);
  const leading = authors ? `${authors}. ${title}${type}` : `${title}${type}`;
  return joinReferenceParts([`[${referenceIndex || "?"}] ${leading}`, source && year ? `${source}, ${year}` : source || year, locator]);
}

function materialAuthors(material) {
  const authors = material?.bibliographicMetadata?.authors;
  return Array.isArray(authors) ? authors.map((item) => String(item).trim()).filter(Boolean) : [];
}

function citationAuthorLabel(material) {
  const authors = materialAuthors(material);
  if (authors.length === 0) return materialTitle(material);
  if (authors.length === 1) return authors[0];
  return `${authors[0]}等`;
}

function referenceAuthors(material) {
  return materialAuthors(material).join(", ");
}

function firstNonEmpty(...values) {
  return values.map((value) => String(value || "").trim()).find(Boolean) || "";
}

function joinReferenceParts(parts) {
  return parts
    .map((part) => String(part || "").trim())
    .filter(Boolean)
    .map((part) => (part.endsWith(".") ? part : `${part}.`))
    .join(" ");
}

function gbtPublicationType(material) {
  const publicationType = String(material?.bibliographicMetadata?.publicationType || "").toUpperCase();
  if (publicationType.includes("JOURNAL")) return "[J]";
  if (publicationType.includes("BOOK")) return "[M]";
  if (publicationType.includes("THESIS")) return "[D]";
  if (publicationType.includes("CONFERENCE")) return "[C]";
  if (publicationType.includes("REPORT")) return "[R]";
  if (material?.bibliographicMetadata?.url) return "[EB/OL]";
  return "[Z]";
}

export function formatMaterialCategory(category) {
  const normalized = String(category || "").toLowerCase();
  const labels = {
    assignment_requirement: "作业要求",
    reference_material: "参考资料",
    user_draft: "用户草稿",
    research_result: "研究成果",
    chart_or_data: "图表或数据",
    supplement_note: "补充说明",
    unknown: "上传材料"
  };
  return labels[normalized] || category || "上传材料";
}

export function materialsById(materials = []) {
  return new Map(materials.map((material) => [String(material.id), material]));
}

export function buildCitationSuggestions(draft, materials = []) {
  const lookup = materialsById(materials);
  return sourceTraceEntries(draft?.sourceTraceMap).map((entry) => {
    const linkedMaterials = entry.materialIds
      .map((id) => lookup.get(String(id)))
      .filter(Boolean);
    return {
      ...entry,
      materials: linkedMaterials,
      missingMaterialIds: entry.materialIds.filter((id) => !lookup.has(String(id)))
    };
  });
}

export function materialEvidenceBinding(material) {
  const claims = Array.isArray(material?.detectedClaims) ? material.detectedClaims.filter(Boolean) : [];
  const evidence = Array.isArray(material?.detectedEvidence) ? material.detectedEvidence.filter(Boolean) : [];
  const requirements = Array.isArray(material?.detectedRequirements) ? material.detectedRequirements.filter(Boolean) : [];
  return {
    primaryClaim: claims[0] || "",
    primaryEvidence: evidence[0] || "",
    primaryRequirement: requirements[0] || "",
    claimCount: claims.length,
    evidenceCount: evidence.length,
    requirementCount: requirements.length,
    isEvidenceReady: evidence.length > 0 || claims.length > 0
  };
}

export function collectReferenceMaterials(draft, materials = []) {
  const lookup = materialsById(materials);
  const ids = sourceTraceEntries(draft?.sourceTraceMap).flatMap((entry) => entry.materialIds);
  return Array.from(new Set(ids))
    .map((id) => lookup.get(String(id)))
    .filter(Boolean);
}

export function summarizeTrustChain(draft) {
  const entries = sourceTraceEntries(draft?.sourceTraceMap);
  const linkedParagraphs = entries.filter((entry) => entry.materialIds.length > 0).length;
  const uniqueMaterials = new Set(entries.flatMap((entry) => entry.materialIds));
  return {
    linkedParagraphs,
    uniqueMaterialCount: uniqueMaterials.size,
    totalParagraphs: entries.length
  };
}

export function summarizeEvidenceCoverage(evidenceSummary) {
  const paragraphs = evidenceSummary?.paragraphs ?? [];
  const total = paragraphs.length;
  const confirmed = paragraphs.filter((item) => item.bindingStatus === "CONFIRMED" || item.bindingStatus === "USER_CONFIRMED").length;
  const weak = paragraphs.filter((item) => item.bindingStatus === "WEAK").length;
  const missing = paragraphs.filter((item) => item.bindingStatus === "MISSING").length;
  const coverageRatio = total === 0 ? 0 : Math.round(((confirmed + weak) / total) * 100);
  const confirmedRatio = total === 0 ? 0 : Math.round((confirmed / total) * 100);
  const healthLabel = confirmedRatio >= 80 && missing === 0
    ? "可信链健康"
    : coverageRatio >= 70
      ? "仍需补强"
      : "证据不足";
  return {
    total,
    confirmed,
    weak,
    missing,
    coverageRatio,
    confirmedRatio,
    healthLabel
  };
}

export function buildReviewInstruction(review) {
  if (!review) return "请根据审查意见对选中内容进行局部修改。";
  const type = review.reviewType;
  const fix = review.suggestedFix ? `建议修正：${review.suggestedFix}` : "";
  const message = review.message ? `问题说明：${review.message}` : "";
  const action = {
    missing_evidence: "请补充或强化证据解释，不要编造不存在的引用。",
    requirement_conflict: "请按老师要求和作业说明修正这段内容。",
    repetition_issue: "请压缩信息重复，但保留必要的结构性回扣。",
    logic_gap: "请补足论证跳步，让观点、原因和结论衔接更自然。",
    factual_risk: "请降低事实性断言风险，并提示需要核实的地方。",
    citation_missing: "请在已有材料支撑的位置补充合适引用，不要编造不存在的来源。",
    citation_format_mismatch: "请按当前要求统一引用格式，不改变事实和来源含义。",
    reference_orphan: "请标出需要确认来源的引用，并避免保留无法追溯的引用。",
    reference_not_cited: "请把参考文献与正文论点建立对应关系，或提示删除未使用条目。",
    reference_metadata_incomplete: "请提示用户先补全文献信息，不要自行编造作者、年份或题名。",
    aigc_style_risk: "请减少模板化套话，补入更具体的研究对象、情境或材料依据。",
    generic_unsupported_claim: "请把空泛判断改成具体场景、数据或案例支撑的论证。",
    original_evidence_missing: "请只基于已上传材料补充原创实证；材料不足时列出需要用户补充的案例、数据或调研信息。"
  }[type] || "请根据审查意见进行局部修正。";
  return [message, fix, action].filter(Boolean).join("\n");
}

export function actionForReview(review) {
  const type = review?.reviewType;
  if (type === "missing_evidence") return "add_evidence";
  if (type === "aigc_style_risk" || type === "generic_unsupported_claim" || type === "original_evidence_missing") return "add_original_evidence";
  if (type === "citation_missing" || type === "reference_orphan" || type === "reference_not_cited") return "add_evidence";
  if (type === "citation_format_mismatch" || type === "reference_metadata_incomplete") return "adjust_structure";
  if (type === "repetition_issue") return "reduce_repetition";
  if (type === "logic_gap" || type === "requirement_conflict") return "adjust_structure";
  return "rewrite_selection";
}

export function summarizeReviewBasis(review) {
  const messages = {
    missing_evidence: "系统会优先检查论点是否能在来源追溯或正文附近找到支撑。",
    requirement_conflict: "系统会把老师要求、字数、格式和特殊说明作为主要判断依据。",
    repetition_issue: "系统会区分结构性回扣和信息重复，必要回扣可以申诉保留。",
    logic_gap: "系统会查看段落之间是否存在缺少解释、跳步或结论过快。",
    factual_risk: "系统会提示可能需要核实的事实、数据、概念或引用。",
    citation_missing: "系统会检查来源追溯中已有材料但正文缺少可识别引用的位置。",
    citation_format_mismatch: "系统会对照老师要求或导出设置，检查 APA 与 GB/T 7714 是否混用。",
    reference_orphan: "系统会检查正文引用是否能对应到已上传并解析的材料，避免疑似编造来源。",
    reference_not_cited: "系统会检查参考文献区是否存在正文未实际引用的条目。",
    reference_metadata_incomplete: "系统会检查已引用材料是否缺少作者、年份、题名等关键文献信息。",
    aigc_style_risk: "系统会检查模板化表达、泛化总结和缺少具体对象的段落。",
    generic_unsupported_claim: "系统会检查抽象判断是否缺少具体案例、数据或真实材料依据。",
    original_evidence_missing: "系统会检查较长论证段是否缺少引用、数据、案例或原创实证线索。"
  };
  return messages[review?.reviewType] || "系统会结合正文、要求快照和来源追溯给出该项判断。";
}

export function suggestReviewAction(review) {
  const level = normalizeImpactLevel(review?.reviewImpactLevel);
  if (level === "must_confirm") {
    return "建议先确认该项是否确实影响选题、要求或关键结论，再决定修改或申诉。";
  }
  if (level === "local_fix") {
    return "建议优先做局部修改，修改后重新查看审查结果。";
  }
  if (review?.reviewType === "reference_metadata_incomplete") {
    return "建议到导出页编辑文献信息，补全后重新审查或导出。";
  }
  return "这类提示通常不会阻断定稿，可以作为润色参考。";
}

export function reviewEvidenceChecklist(review) {
  const type = review?.reviewType;
  const common = [
    review?.targetRange?.selectedText ? "已定位到正文影响范围，可以直接查看原句。" : "暂未定位具体正文范围，建议先人工确认问题位置。",
    review?.suggestedFix ? "已有修改建议，可先生成预览再决定是否应用。" : "暂无具体修改建议，建议先补充材料或人工判断。"
  ];
  const typeSpecific = {
    missing_evidence: ["优先检查该段是否缺少材料支撑。", "如果材料已上传但未绑定，可以先重建可信链。"],
    citation_missing: ["优先检查该段是否已有来源但缺少正文引用。", "可从材料可信链中插入引用。"],
    factual_risk: ["优先核对数据、年份、专有名词和事实性断言。", "如果不能确认，建议降低断言强度。"],
    requirement_conflict: ["优先对照老师要求快照。", "如果老师要求本身模糊，建议补录要求后再复查。"],
    repetition_issue: ["区分必要的结构性回扣和真正的信息重复。", "如属于章节小结或结论回顾，可申诉保留。"],
    citation_format_mismatch: ["优先确认当前导出引用格式。", "建议统一 APA 或 GB/T 7714，不要混用。"],
    reference_metadata_incomplete: ["优先补全作者、年份、题名、期刊/出版社和链接。", "不要让 AI 自行编造缺失文献信息。"],
    aigc_style_risk: ["优先减少空泛套话和万能总结。", "如果缺少具体材料，建议先补充真实案例或数据。"],
    generic_unsupported_claim: ["优先把抽象判断改成具体对象、材料证据和分析解释。", "不要为了自然化而编造不存在的事实。"],
    original_evidence_missing: ["优先补充问卷、访谈、实验、课程案例或真实文献依据。", "已有材料不足时，应先上传或补充说明，再生成修改预览。"]
  }[type] || ["结合正文、老师要求和材料可信链人工确认。"];
  return [...typeSpecific, ...common];
}

export function reviewRecheckActionLabel(review) {
  const note = String(review?.recheckNote || "");
  if (note.includes("RESOLVED")) return "复查显示已解决，可保持关闭状态。";
  if (note.includes("NEEDS_MORE_EVIDENCE")) return "复查仍需要更多证据，建议补充材料或插入引用。";
  if (note.includes("DOWNGRADED")) return "复查已降级，建议确认是否仍需局部处理。";
  if (note.includes("STILL_OPEN")) return "复查后仍待处理，建议生成修改预览或发起申诉。";
  return "修改后可手动复查此项，确认是否真正解决。";
}

export function summarizeVersionDiff(current, previous) {
  if (!current) return "暂无版本信息。";
  if (!previous) {
    return `当前为首个版本 v${current.versionNo}，主要用于形成初始正文框架与首版草稿。`;
  }

  const currentLength = current.draftText?.length ?? 0;
  const previousLength = previous.draftText?.length ?? 0;
  const delta = currentLength - previousLength;

  if (delta > 0) {
    return `与 v${previous.versionNo} 相比，v${current.versionNo} 增加了约 ${delta} 个字符，通常意味着补充了论证、来源或表达内容。`;
  }
  if (delta < 0) {
    return `与 v${previous.versionNo} 相比，v${current.versionNo} 减少了约 ${Math.abs(delta)} 个字符，通常意味着压缩了重复内容或收束了表达。`;
  }
  return `v${current.versionNo} 与 v${previous.versionNo} 长度接近，更可能是局部结构或表达调整。`;
}

export function buildTextDiffSummary(currentText = "", previousText = "") {
  if (!currentText && !previousText) {
    return { added: [], removed: [], unchangedRatio: 1 };
  }
  const currentUnits = splitTextUnits(currentText);
  const previousUnits = splitTextUnits(previousText);
  const previousSet = new Set(previousUnits);
  const currentSet = new Set(currentUnits);
  const added = currentUnits.filter((unit) => !previousSet.has(unit)).slice(0, 4);
  const removed = previousUnits.filter((unit) => !currentSet.has(unit)).slice(0, 4);
  const unchanged = currentUnits.filter((unit) => previousSet.has(unit)).length;
  return {
    added,
    removed,
    unchangedRatio: currentUnits.length === 0 ? 0 : Math.round((unchanged / currentUnits.length) * 100) / 100
  };
}

export function buildSentenceDiffRows(candidateText = "", currentText = "") {
  const candidateUnits = splitTextUnits(candidateText);
  const currentUnits = splitTextUnits(currentText);
  const currentSet = new Set(currentUnits);
  const candidateSet = new Set(candidateUnits);
  const added = candidateUnits
    .filter((unit) => !currentSet.has(unit))
    .slice(0, 6)
    .map((text) => ({ type: "added", label: "新增", text }));
  const removed = currentUnits
    .filter((unit) => !candidateSet.has(unit))
    .slice(0, 6)
    .map((text) => ({ type: "removed", label: "删除", text }));
  return [...added, ...removed].slice(0, 8);
}

export function buildDetailedDiffRows(candidateText = "", currentText = "") {
  const candidateUnits = splitTextUnits(candidateText);
  const currentUnits = splitTextUnits(currentText);
  const currentSet = new Set(currentUnits);
  const candidateSet = new Set(candidateUnits);
  const rows = [];
  candidateUnits.forEach((unit, index) => {
    if (!currentSet.has(unit)) {
      rows.push({
        id: `added-${index}`,
        type: "added",
        label: "新增",
        text: unit,
        selected: true
      });
    }
  });
  currentUnits.forEach((unit, index) => {
    if (!candidateSet.has(unit)) {
      rows.push({
        id: `removed-${index}`,
        type: "removed",
        label: "删除",
        text: unit,
        selected: false
      });
    }
  });
  return rows.slice(0, 12);
}

export function buildParagraphDiffRows(preview) {
  const rows = preview?.diffSummary?.paragraphDiffs;
  if (Array.isArray(rows) && rows.length > 0) {
    return rows.map((row, index) => ({
      id: row.paragraphId || `paragraph-${index}`,
      paragraphId: row.paragraphId || `p${index + 1}`,
      type: row.changeType || "modified",
      label: paragraphChangeLabel(row.changeType),
      intentLabel: row.intentLabel || "表达优化",
      originalText: row.originalText || "",
      candidateText: row.candidateText || "",
      selected: row.selectedByDefault !== false
    }));
  }
  return [];
}

export function applySelectedParagraphRows(currentText = "", rows = []) {
  const selected = rows.filter((row) => row.selected && row.candidateText);
  if (selected.length === 0) return currentText;
  const paragraphs = splitParagraphsPreserve(currentText);
  selected.forEach((row) => {
    const index = Number(String(row.paragraphId || "").replace(/\D/g, "")) - 1;
    if (Number.isFinite(index) && index >= 0 && index < paragraphs.length) {
      paragraphs[index] = row.candidateText;
    } else {
      paragraphs.push(row.candidateText);
    }
  });
  return paragraphs.map((item) => item.trim()).filter(Boolean).join("\n\n");
}

function paragraphChangeLabel(type) {
  if (type === "added") return "新增段落";
  if (type === "removed") return "删除段落";
  return "修改段落";
}

export function buildHighlightedDiffBlocks(text = "", compareText = "", mode = "candidate") {
  const units = splitTextUnits(text);
  const compareSet = new Set(splitTextUnits(compareText));
  return units.slice(0, 32).map((unit, index) => {
    const changed = !compareSet.has(unit);
    return {
      id: `${mode}-${index}`,
      text: unit,
      type: changed ? (mode === "candidate" ? "added" : "removed") : "unchanged"
    };
  });
}

export function explainCoWriteChange(row, preview) {
  const action = String(preview?.action || "");
  const instruction = String(preview?.instruction || "");
  if (row?.type === "removed") {
    if (action.includes("reduce") || instruction.includes("重复") || instruction.includes("压缩")) {
      return "删除理由：压缩重复表达，减少同一信息反复出现。";
    }
    return "删除理由：AI 判断该句可能与本次目标不强相关，应用前建议人工确认。";
  }
  if (action.includes("original") || instruction.includes("原创") || instruction.includes("实证")) {
    return "新增理由：尝试用已有材料补充具体案例、数据或调研依据，降低空泛论证。";
  }
  if (action.includes("evidence") || instruction.includes("证据") || instruction.includes("引用")) {
    return "新增理由：补强论点支撑或让证据与正文关系更清楚。";
  }
  if (action.includes("structure") || instruction.includes("结构") || instruction.includes("逻辑")) {
    return "新增理由：补足段落衔接，让观点、原因和结论更连贯。";
  }
  if (instruction.includes("降") || instruction.includes("AI") || instruction.includes("自然")) {
    return "修改理由：降低模板化表达，让语气更接近学生自己的写作。";
  }
  return "修改理由：根据本次指令优化表达或补充必要解释，应用前仍建议预览确认。";
}

export function findRelatedReviewsForPreview(preview, reviews = [], currentDraftText = "") {
  if (!preview) return [];
  const candidate = String(preview.candidateDraftText || "");
  const instruction = String(preview.instruction || "");
  const diffRows = buildDetailedDiffRows(candidate, currentDraftText);
  const addedText = diffRows.filter((row) => row.type === "added").map((row) => row.text).join(" ");
  const targetRange = preview.targetRange || {};
  return reviews
    .filter((review) => String(review.reviewStatus || "OPEN").toUpperCase() === "OPEN")
    .map((review) => {
      const rangeScore = rangesOverlap(targetRange, review.targetRange) ? 2 : 0;
      const typeScore = coWriteReviewTypeScore(preview.action, review.reviewType);
      const textScore = sharedTokenScore([instruction, addedText].join(" "), [review.message, review.suggestedFix, review.targetRange?.selectedText].join(" "));
      const score = rangeScore + typeScore + Math.min(textScore, 2);
      return {
        ...review,
        relationScore: score,
        relationReason: explainReviewRelation({ rangeScore, typeScore, textScore }, review, preview)
      };
    })
    .filter((review) => review.relationScore > 0)
    .sort((left, right) => right.relationScore - left.relationScore)
    .slice(0, 5);
}

export function formatSourceLocation(binding) {
  const location = binding?.sourceLocation || {};
  if (location.type === "missing") {
    return {
      label: location.label || "暂无可定位材料",
      detail: "需要先补充材料、重新解析或重建可信链。",
      confidence: "none"
    };
  }
  if (location.type === "page_hint") {
    return {
      label: location.label || `第 ${location.page} 页附近`,
      detail: "页码来自用户补充、材料文本或 AI 解析片段，可优先回原文件核对。",
      confidence: location.confidence || "explicit"
    };
  }
  if (location.type === "knowledge_chunk") {
    return {
      label: location.label || "知识库片段",
      detail: "该位置来自知识库切片匹配，建议结合证据摘录核对原材料上下文。",
      confidence: location.confidence || "inferred"
    };
  }
  if (location.type === "draft_range") {
    return {
      label: location.label || "正文对应材料摘录",
      detail: "当前能定位到正文段落与材料摘录的对应关系，尚未精确到原文件页码。",
      confidence: location.confidence || "inferred"
    };
  }
  return {
    label: location.label || "材料摘录位置",
    detail: "当前只能定位到材料摘录级别，后续可继续补页码或原文定位。",
    confidence: location.confidence || "inferred"
  };
}

export function applySelectedDiffRows(currentText = "", rows = []) {
  const additions = rows.filter((row) => row.type === "added" && row.selected).map((row) => row.text);
  const removals = rows.filter((row) => row.type === "removed" && row.selected).map((row) => row.text);
  let nextText = String(currentText || "");
  removals.forEach((text) => {
    nextText = nextText.replace(text, "");
  });
  if (additions.length > 0) {
    const suffix = nextText.endsWith("\n") ? "" : "\n\n";
    nextText = `${nextText}${suffix}${additions.join("\n")}`;
  }
  return nextText.replace(/\n{3,}/g, "\n\n").trim();
}

function rangesOverlap(left, right) {
  if (!hasValidRange(left) || !hasValidRange(right)) return false;
  return Number(left.start) < Number(right.end) && Number(right.start) < Number(left.end);
}

function coWriteReviewTypeScore(action, reviewType) {
  const normalizedAction = String(action || "");
  const normalizedType = String(reviewType || "");
  if (normalizedAction.includes("original") && ["aigc_style_risk", "generic_unsupported_claim", "original_evidence_missing"].includes(normalizedType)) return 2;
  if (normalizedAction.includes("evidence") && ["missing_evidence", "citation_missing", "reference_not_cited"].includes(normalizedType)) return 2;
  if (normalizedAction.includes("structure") && ["logic_gap", "requirement_conflict"].includes(normalizedType)) return 2;
  if (normalizedAction.includes("reduce") && normalizedType === "repetition_issue") return 2;
  if (normalizedAction.includes("rewrite") && ["logic_gap", "factual_risk", "repetition_issue"].includes(normalizedType)) return 1;
  return 0;
}

function sharedTokenScore(left, right) {
  const leftTokens = lexicalPreviewTokens(left);
  const rightTokens = lexicalPreviewTokens(right);
  if (leftTokens.size === 0 || rightTokens.size === 0) return 0;
  let score = 0;
  leftTokens.forEach((token) => {
    if (rightTokens.has(token)) score += 1;
  });
  return score;
}

function lexicalPreviewTokens(text) {
  const compact = String(text || "").replace(/\s+/g, "");
  const tokens = new Set();
  String(text || "")
    .split(/[\s,，。；;：:、（）()[\]{}"'“”‘’!?！？]+/)
    .map((token) => token.trim())
    .filter((token) => token.length >= 2)
    .forEach((token) => tokens.add(token));
  for (let index = 0; index < compact.length - 1 && tokens.size < 80; index += 1) {
    tokens.add(compact.slice(index, index + 2));
  }
  return tokens;
}

function explainReviewRelation(scores, review, preview) {
  if (scores.rangeScore > 0) {
    return "本次共写范围与该审查项定位范围重叠，应用后建议优先复查。";
  }
  if (scores.typeScore > 0) {
    return `本次动作「${String(preview?.action || "共写").replace(/_/g, " ")}」与该问题类型匹配。`;
  }
  return "本次修改内容与审查项描述存在关键词重合，建议应用后人工确认。";
}

export function buildCoWriteGuardrailChecks(preview, currentDraftText = "") {
  const guardrails = preview?.diffSummary?.guardrails || preview?.controls || {};
  const candidate = String(preview?.candidateDraftText || "");
  const current = String(currentDraftText || "");
  const citationPattern = /（[^）]{1,40}，\s*((?:19|20)\d{2}|n\.d\.)）|\[\d+\]/g;
  const currentCitations = current.match(citationPattern) ?? [];
  const candidateCitations = candidate.match(citationPattern) ?? [];
  const currentNumbers = current.match(/\d+(?:\.\d+)?%?/g) ?? [];
  const candidateNumbers = candidate.match(/\d+(?:\.\d+)?%?/g) ?? [];
  const lostCitations = currentCitations.filter((item) => !candidateCitations.includes(item));
  const changedNumbers = currentNumbers.filter((item) => !candidateNumbers.includes(item));
  return [
    {
      key: "keepCitations",
      label: "保留引用",
      enabled: Boolean(guardrails.keepCitations),
      passed: !guardrails.keepCitations || lostCitations.length === 0,
      detail: lostCitations.length === 0 ? "未发现明显引用丢失。" : `可能丢失引用：${lostCitations.slice(0, 3).join("、")}`
    },
    {
      key: "keepData",
      label: "不改数据",
      enabled: Boolean(guardrails.keepData),
      passed: !guardrails.keepData || changedNumbers.length === 0,
      detail: changedNumbers.length === 0 ? "未发现明显数字丢失。" : `需核对数字：${changedNumbers.slice(0, 5).join("、")}`
    },
    {
      key: "noNewSources",
      label: "不新增文献",
      enabled: Boolean(guardrails.noNewSources),
      passed: !guardrails.noNewSources || candidateCitations.length <= currentCitations.length + 1,
      detail: candidateCitations.length <= currentCitations.length + 1 ? "未发现明显新增大量引用。" : "候选文本引用数量明显增加，建议人工核对来源。"
    },
    {
      key: "keepStudentVoice",
      label: "保留学生表达",
      enabled: Boolean(guardrails.keepStudentVoice),
      passed: true,
      detail: "表达风格需要人工最终确认，建议重点看新增句和改写句。"
    }
  ];
}

export function evidenceStrength(binding) {
  const score = Number(binding?.confidenceScore || 0);
  if (binding?.bindingStatus === "USER_CONFIRMED") {
    return {
      level: "strong",
      label: "用户确认",
      detail: "这条证据已由用户人工确认，可信度最高。"
    };
  }
  if (binding?.bindingStatus === "CONFIRMED" || score >= 0.75) {
    return {
      level: "strong",
      label: "强证据",
      detail: "该段落同时具备来源追溯或知识库片段支撑，适合作为正文依据。"
    };
  }
  if (binding?.bindingStatus === "WEAK" || score >= 0.45) {
    return {
      level: "medium",
      label: "弱绑定",
      detail: "系统找到来源线索，但证据片段或解析信息仍不够充分，建议人工确认。"
    };
  }
  return {
    level: "weak",
    label: "缺来源",
    detail: "当前段落缺少可确认材料来源，建议补充材料、重新解析或调整表述。"
  };
}

function splitTextUnits(text) {
  return String(text)
    .split(/(?<=[。！？!?；;])|\n+/)
    .map((unit) => unit.trim())
    .filter((unit) => unit.length > 8);
}

function splitParagraphsPreserve(text) {
  return String(text || "")
    .split(/\n\s*\n/)
    .map((item) => item.trim())
    .filter(Boolean);
}

export function inferVersionTags(current, previous) {
  if (!current) return [];
  if (!previous) return ["首版草稿", "初始框架"];

  const tags = [];
  const currentLength = current.draftText?.length ?? 0;
  const previousLength = previous.draftText?.length ?? 0;
  const delta = currentLength - previousLength;

  if (delta > 120) tags.push("扩展论证");
  if (delta < -120) tags.push("压缩重复");
  if (Math.abs(delta) <= 120) tags.push("局部润色");
  if (current.titleSuggestion && current.titleSuggestion !== previous.titleSuggestion) tags.push("标题调整");
  if (JSON.stringify(current.sourceTraceMap ?? {}) !== JSON.stringify(previous.sourceTraceMap ?? {})) {
    tags.push("来源更新");
  }

  return tags.length > 0 ? tags : ["版本更新"];
}
