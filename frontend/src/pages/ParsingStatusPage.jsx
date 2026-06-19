import { useEffect, useMemo, useState } from "react";
import { api } from "../services/api";
import { StatusBadge } from "../components/StatusBadge";

const MATERIAL_CATEGORY_OPTIONS = [
  { value: "ASSIGNMENT_REQUIREMENT", label: "老师要求" },
  { value: "REFERENCE_MATERIAL", label: "参考文献" },
  { value: "USER_DRAFT", label: "用户草稿" },
  { value: "RESEARCH_RESULT", label: "研究结果" },
  { value: "CHART_OR_DATA", label: "图表/数据" },
  { value: "SUPPLEMENT_NOTE", label: "补充说明" },
  { value: "UNKNOWN", label: "未确定" }
];

export function ParsingStatusPage({ workspace, onParsed, onError }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState(false);
  const [activeMaterialId, setActiveMaterialId] = useState("");
  const [activeStep, setActiveStep] = useState("");
  const [runSummary, setRunSummary] = useState(null);
  const [supplementDrafts, setSupplementDrafts] = useState({});
  const [supplementPages, setSupplementPages] = useState({});
  const [supplementSavingId, setSupplementSavingId] = useState("");
  const [categorySavingId, setCategorySavingId] = useState("");

  async function loadMaterials(cancelled = false) {
    try {
      const data = await api.listMaterials(workspace.id);
      if (cancelled) return;
      setItems(data.items ?? []);
    } catch (error) {
      if (!cancelled) onError(error.message);
    } finally {
      if (!cancelled) setLoading(false);
    }
  }

  useEffect(() => {
    if (!workspace?.id) return;
    let cancelled = false;
    loadMaterials(cancelled);
    const timer = window.setInterval(() => loadMaterials(cancelled), 4000);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [workspace?.id]);

  const summary = useMemo(() => {
    const total = items.length;
    const parsedCount = items.filter((item) => normalizeStage(item.parseStage) === "AI_PARSED").length;
    const partialCount = items.filter((item) => normalizeStage(item.parseStage) === "AI_PARTIAL").length;
    const failedCount = items.filter((item) => normalizeStage(item.parseStage) === "AI_FAILED").length;
    const keyMaterials = items.filter((item) => item.isKeyMaterial);
    const keyTotal = keyMaterials.length;
    const keyParsed = keyMaterials.filter((item) => normalizeStage(item.parseStage) === "AI_PARSED").length;
    const gateMaterials = keyTotal > 0 ? keyMaterials : items;
    const keyReady = keyMaterials.filter(isMaterialReadyForGate).length;
    const readyCount = gateMaterials.filter(isMaterialReadyForGate).length;
    const qualityBlockingCount = gateMaterials.filter(isMaterialQualityBlocking).length;
    const progress = total === 0 ? 0 : Math.round((parsedCount / total) * 100);
    const isReadyToContinue = total > 0 && gateMaterials.length > 0 && readyCount === gateMaterials.length;

    return {
      total,
      parsedCount,
      partialCount,
      failedCount,
      keyTotal,
      keyParsed,
      keyReady,
      qualityBlockingCount,
      progress,
      isReadyToContinue
    };
  }, [items]);

  async function runParsingForItem(item) {
    setActiveMaterialId(item.id);
    setActiveStep("预处理");
    await api.preprocessMaterial(item.id);
    await loadMaterials(false);
    setActiveStep("AI 语义解析");
    await api.aiParseMaterial(item.id, true);
    await loadMaterials(false);
  }

  async function handleRunParsing() {
    try {
      setProcessing(true);
      const candidates = items.filter((item) => normalizeStage(item.parseStage) !== "AI_PARSED");
      let successCount = 0;
      let failedCount = 0;

      for (const item of candidates) {
        try {
          await runParsingForItem(item);
          successCount += 1;
        } catch (error) {
          failedCount += 1;
          onError(error.message);
        }
      }

      setRunSummary({
        processedCount: candidates.length,
        successCount,
        failedCount
      });
    } catch (error) {
      onError(error.message);
    } finally {
      setProcessing(false);
      setActiveMaterialId("");
      setActiveStep("");
    }
  }

  async function handleSupplementAndReparse(item) {
    const supplementText = (supplementDrafts[item.id] || "").trim();
    const pageRef = supplementPages[item.id];
    if (!supplementText) {
      onError("请先填写补充说明");
      return;
    }

    try {
      setSupplementSavingId(item.id);
      await api.supplementMaterial(item.id, supplementText, pageRef);
      await runParsingForItem(item);
      setRunSummary({
        processedCount: 1,
        successCount: 1,
        failedCount: 0
      });
      setSupplementDrafts((current) => ({ ...current, [item.id]: "" }));
      setSupplementPages((current) => ({ ...current, [item.id]: "" }));
    } catch (error) {
      onError(error.message);
    } finally {
      setSupplementSavingId("");
      setActiveMaterialId("");
      setActiveStep("");
      setProcessing(false);
    }
  }

  async function handleCategoryChange(item, materialCategory) {
    try {
      setCategorySavingId(item.id);
      await api.updateMaterialCategory(item.id, materialCategory);
      await loadMaterials(false);
      setRunSummary({
        processedCount: 1,
        successCount: 1,
        failedCount: 0
      });
    } catch (error) {
      onError(error.message);
    } finally {
      setCategorySavingId("");
    }
  }

  function handleUseSupplementPrompt(item, issue) {
    const prompt = issue.supplementPrompt || issue.suggestedAction || "";
    if (!prompt) return;
    setSupplementDrafts((current) => {
      const existing = (current[item.id] || "").trim();
      if (!existing) {
        return { ...current, [item.id]: prompt };
      }
      if (existing.includes(prompt)) {
        return current;
      }
      return { ...current, [item.id]: `${existing}\n${prompt}` };
    });
  }

  return (
    <section className="page-card">
      <h3 className="page-section-title">解析质量检查</h3>
      <p className="section-help">
        系统会先对上传输入做预处理，再执行 AI 语义解析。解析完成后会给出质量清单：可直接使用、建议确认、需要补充或解析失败。
      </p>

      <div className="parsing-summary-grid">
        <div className="summary-card">
          <strong>总材料数</strong>
          <span>{summary.total}</span>
        </div>
        <div className="summary-card">
          <strong>已完成 AI 解析</strong>
          <span>{summary.parsedCount}</span>
        </div>
        <div className="summary-card">
          <strong>关键材料可用度</strong>
          <span>
            {summary.keyReady}/{summary.keyTotal || 0}
          </span>
        </div>
        <div className="summary-card">
          <strong>需补充 / 失败</strong>
          <span>{summary.qualityBlockingCount}</span>
        </div>
      </div>

      <div className="progress-shell">
        <div className="progress-track">
          <div className="progress-fill" style={{ width: `${summary.progress}%` }} />
        </div>
        <p className="muted progress-copy">
          {processing
            ? `当前处理：${activeStep || "处理中"}`
            : summary.isReadyToContinue
              ? "关键材料质量已达到继续标准，可进入下一步。"
              : summary.qualityBlockingCount > 0
                ? `还有 ${summary.qualityBlockingCount} 项关键材料需要补充或重试。`
                : "仍有关键材料未完成 AI 解析。"}
        </p>
      </div>

      {runSummary && (
        <div className="mini-card parsing-feedback-card">
          <strong>最近一次解析结果</strong>
          <p className="muted">
            本轮共处理 {runSummary.processedCount} 项，成功 {runSummary.successCount} 项，失败 {runSummary.failedCount} 项。
          </p>
        </div>
      )}

      <div className="list-stack parsing-item-list">
        {loading ? (
          <div className="card-block">
            <p className="muted">正在读取材料状态...</p>
          </div>
        ) : items.length === 0 ? (
          <div className="card-block">
            <p className="muted">当前还没有可解析材料，请先回到上传页补充输入。</p>
          </div>
        ) : (
          items.map((item) => {
            const quality = item.parseQuality;
            const qualityIssues = Array.isArray(quality?.issues) ? quality.issues : [];
            const visibleIssues = qualityIssues.slice(0, 5);
            return (
              <div className={`mini-card parsing-item-card ${activeMaterialId === item.id ? "active" : ""}`} key={item.id}>
                <div className="parsing-item-head">
                  <div>
                    <strong>{item.filename}</strong>
                    <p className="muted">
                      类型：{item.fileType} ｜ 来源：{item.sourceType} ｜ 关键材料：{item.isKeyMaterial ? "是" : "否"}
                    </p>
                  </div>
                  <div className="parsing-item-badges">
                    <StatusBadge level={getParseBadgeLevel(item.parseStage)}>{renderParseLabel(item.parseStage)}</StatusBadge>
                    {quality && (
                      <StatusBadge level={getQualityBadgeLevel(quality.status)}>
                        {renderQualityLabel(quality.status)}
                      </StatusBadge>
                    )}
                  </div>
                </div>

                <div className="parsing-item-meta">
                  <span className="toolbar-pill">置信度：{formatConfidence(item.confidenceScore)}</span>
                  {quality?.score != null && (
                    <span className="toolbar-pill">质量分：{formatQualityScore(quality.score)}</span>
                  )}
                  {item.effectiveMaterialCategory && (
                    <span className="toolbar-pill">分类：{formatCategoryLabel(item.effectiveMaterialCategory)}</span>
                  )}
                  {item.categoryOverridden && <span className="toolbar-pill">已人工纠正</span>}
                  {activeMaterialId === item.id && processing && <span className="toolbar-pill">当前步骤：{activeStep}</span>}
                </div>

                {(item.aiMaterialCategory || item.summary || item.topicRelation) && (
                  <div className="parse-insight-box">
                    {item.summary && (
                      <p className="muted"><strong>解析摘要：</strong>{item.summary}</p>
                    )}
                    {item.topicRelation && (
                      <p className="muted"><strong>主题关系：</strong>{item.topicRelation}</p>
                    )}
                    {hasBibliographicMetadata(item.bibliographicMetadata) && (
                      <div className="bibliographic-box">
                        <strong>文献识别信息</strong>
                        <p className="muted">{formatBibliographicMetadata(item.bibliographicMetadata)}</p>
                      </div>
                    )}
                    <div className="field parse-category-field">
                      <label>材料角色纠正</label>
                      <select
                        value={item.effectiveMaterialCategory || item.aiMaterialCategory || "UNKNOWN"}
                        disabled={categorySavingId === item.id}
                        onChange={(event) => handleCategoryChange(item, event.target.value)}
                      >
                        {MATERIAL_CATEGORY_OPTIONS.map((option) => (
                          <option key={option.value} value={option.value}>
                            {option.label}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>
                )}

                {quality && (
                  <div className={`parse-quality-box parse-quality-box--${normalizeQualityStatus(quality.status).toLowerCase()}`}>
                    <div className="parse-quality-head">
                      <div>
                        <strong>解析质量清单</strong>
                        <p className="muted">{quality.nextAction || "请检查解析结果是否符合你的理解。"}</p>
                      </div>
                      <span className="parse-quality-count">{qualityIssues.length} 项问题</span>
                    </div>
                    {visibleIssues.length === 0 ? (
                      <p className="muted parse-quality-empty">当前没有发现会影响后续生成的解析问题。</p>
                    ) : (
                      <div className="parse-quality-issues">
                        {visibleIssues.map((issue) => (
                          <div className={`parse-quality-issue parse-quality-issue--${normalizeIssueLevel(issue.level).toLowerCase()}`} key={issue.code}>
                            <div className="parse-quality-issue-copy">
                              <div className="parse-quality-issue-title">
                                <strong>{issue.label}</strong>
                                <span>{formatIssueLevelLabel(issue.level)}</span>
                              </div>
                              <p>{issue.message}</p>
                              {issue.suggestedAction && <small>{issue.suggestedAction}</small>}
                            </div>
                            {issue.supplementPrompt && (
                              <button
                                className="ghost-btn parse-quality-fill-btn"
                                type="button"
                                onClick={() => handleUseSupplementPrompt(item, issue)}
                              >
                                填入补充说明
                              </button>
                            )}
                          </div>
                        ))}
                        {qualityIssues.length > visibleIssues.length && (
                          <p className="muted parse-quality-more">还有 {qualityIssues.length - visibleIssues.length} 项问题，可先处理上方关键项。</p>
                        )}
                      </div>
                    )}
                  </div>
                )}

                <div className="button-row" style={{ marginTop: 10 }}>
                  <button
                    className="ghost-btn"
                    disabled={processing}
                    onClick={async () => {
                      try {
                        setProcessing(true);
                        await runParsingForItem(item);
                      } catch (error) {
                        onError(error.message);
                      } finally {
                        setProcessing(false);
                        setActiveMaterialId("");
                        setActiveStep("");
                      }
                    }}
                  >
                    重新解析该项
                  </button>
                </div>

                {shouldShowSupplementBox(item, supplementDrafts[item.id]) && (
                  <div className="supplement-box">
                    <div className="field">
                      <label>补充说明</label>
                      <textarea
                        value={supplementDrafts[item.id] || ""}
                        onChange={(event) =>
                          setSupplementDrafts((current) => ({
                            ...current,
                            [item.id]: event.target.value
                          }))
                        }
                        placeholder="如果这份材料解析不完整，可以补充页码说明、图表含义、老师要求上下文等。"
                      />
                    </div>
                    <div className="field supplement-page-field">
                      <label>页码（可选）</label>
                      <input
                        value={supplementPages[item.id] || ""}
                        onChange={(event) =>
                          setSupplementPages((current) => ({
                            ...current,
                            [item.id]: event.target.value
                          }))
                        }
                        placeholder="例如 3"
                      />
                    </div>
                    <div className="button-row">
                      <button
                        className="secondary-btn"
                        disabled={processing || supplementSavingId === item.id}
                        onClick={() => handleSupplementAndReparse(item)}
                      >
                        {supplementSavingId === item.id ? "提交补充中..." : "补充说明并重新解析"}
                      </button>
                    </div>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>

      <div className="button-row" style={{ marginTop: 18 }}>
        <button className="secondary-btn" onClick={handleRunParsing} disabled={processing || loading || items.length === 0}>
          {processing ? "解析中..." : "执行解析"}
        </button>
        <button className="primary-btn" onClick={onParsed} disabled={!summary.isReadyToContinue}>
          继续材料检查
        </button>
      </div>
    </section>
  );
}

function shouldShowSupplementBox(item, draftText) {
  const status = normalizeQualityStatus(item.parseQuality?.status);
  const hasIssues = Array.isArray(item.parseQuality?.issues) && item.parseQuality.issues.length > 0;
  return normalizeStage(item.parseStage) !== "AI_PARSED"
    || hasIssues
    || Boolean(String(draftText || "").trim())
    || status === "NEEDS_SUPPLEMENT"
    || status === "FAILED";
}

function isMaterialReadyForGate(item) {
  const status = normalizeQualityStatus(item.parseQuality?.status);
  if (status) {
    return status === "READY" || status === "NEEDS_CONFIRMATION";
  }
  return normalizeStage(item.parseStage) === "AI_PARSED";
}

function isMaterialQualityBlocking(item) {
  const status = normalizeQualityStatus(item.parseQuality?.status);
  return status === "NEEDS_SUPPLEMENT" || status === "FAILED";
}

function normalizeQualityStatus(status) {
  return String(status || "").toUpperCase();
}

function renderQualityLabel(status) {
  switch (normalizeQualityStatus(status)) {
    case "READY":
      return "可用于生成";
    case "NEEDS_CONFIRMATION":
      return "建议确认";
    case "NEEDS_SUPPLEMENT":
      return "需要补充";
    case "FAILED":
      return "解析失败";
    default:
      return "待检查";
  }
}

function getQualityBadgeLevel(status) {
  switch (normalizeQualityStatus(status)) {
    case "READY":
      return "ready";
    case "NEEDS_CONFIRMATION":
      return "local_fix";
    case "NEEDS_SUPPLEMENT":
    case "FAILED":
      return "must_confirm";
    default:
      return "notice";
  }
}

function formatQualityScore(score) {
  if (score == null) return "-";
  const numeric = Number(score);
  if (Number.isNaN(numeric)) return "-";
  return `${(numeric * 100).toFixed(0)}%`;
}

function normalizeIssueLevel(level) {
  return String(level || "NOTICE").toUpperCase();
}

function formatIssueLevelLabel(level) {
  switch (normalizeIssueLevel(level)) {
    case "MUST_CONFIRM":
      return "需补充";
    case "LOCAL_FIX":
      return "建议确认";
    default:
      return "提示";
  }
}

function hasBibliographicMetadata(metadata) {
  if (!metadata) return false;
  return [
    ...(Array.isArray(metadata.authors) ? metadata.authors : []),
    metadata.year,
    metadata.title,
    metadata.sourceTitle,
    metadata.publisher,
    metadata.url,
    metadata.doi
  ].some((item) => String(item || "").trim());
}

function formatBibliographicMetadata(metadata) {
  const authors = Array.isArray(metadata.authors)
    ? metadata.authors.filter(Boolean).join("、")
    : "";
  return [
    authors && `作者：${authors}`,
    metadata.year && `年份：${metadata.year}`,
    metadata.title && `题名：${metadata.title}`,
    metadata.sourceTitle && `期刊/来源：${metadata.sourceTitle}`,
    metadata.publisher && `出版社：${metadata.publisher}`,
    metadata.doi && `DOI：${metadata.doi}`,
    metadata.url && `链接：${metadata.url}`
  ].filter(Boolean).join(" ｜ ");
}

function normalizeStage(parseStage) {
  return String(parseStage || "").toUpperCase();
}

function renderParseLabel(parseStage) {
  switch (normalizeStage(parseStage)) {
    case "AI_PARSED":
      return "已完成 AI 解析";
    case "AI_PARTIAL":
      return "部分解析";
    case "AI_FAILED":
      return "解析失败";
    default:
      return "待解析";
  }
}

function getParseBadgeLevel(parseStage) {
  switch (normalizeStage(parseStage)) {
    case "AI_PARSED":
      return "ready";
    case "AI_PARTIAL":
      return "local_fix";
    case "AI_FAILED":
      return "must_confirm";
    default:
      return "notice";
  }
}

function formatConfidence(confidence) {
  if (confidence == null) return "-";
  const numeric = Number(confidence);
  if (Number.isNaN(numeric)) return "-";
  return `${(numeric * 100).toFixed(0)}%`;
}

function formatCategoryLabel(category) {
  const match = MATERIAL_CATEGORY_OPTIONS.find((item) => item.value === String(category || "").toUpperCase());
  return match?.label || category || "未确定";
}
