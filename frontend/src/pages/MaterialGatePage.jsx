import { useEffect, useMemo, useState } from "react";
import { ExternalLink, Search, SlidersHorizontal, Sparkles, Upload } from "lucide-react";
import { api } from "../services/api";

const MISSING_ITEM_LABELS = {
  key_material: "核心材料尚未完成 AI 解析",
  assignment_requirement: "缺少明确的写作与提交要求",
  reference_material: "缺少可引用参考资料",
  research_result: "缺少当前文档所需的研究成果或分析依据"
};

const LITERATURE_SOURCE_LABELS = {
  "Google Scholar": "Google Scholar",
  CNKI: "知网",
  Crossref: "Crossref",
  OpenAlex: "OpenAlex",
  "Semantic Scholar": "Semantic Scholar"
};

const SEARCH_INTENT_OPTIONS = [
  { value: "theory", label: "理论基础", hint: "找综述、概念和相关研究" },
  { value: "method", label: "研究方法", hint: "找方法、模型和实验设计" },
  { value: "case", label: "案例材料", hint: "找场景、对象和应用案例" },
  { value: "data", label: "数据实证", hint: "找数据、调查和实证研究" }
];

const PROVIDER_OPTIONS = [
  { value: "crossref", label: "Crossref" },
  { value: "openalex", label: "OpenAlex" },
  { value: "semantic_scholar", label: "Semantic Scholar" }
];

const WORK_TYPE_OPTIONS = [
  { value: "journal_article", label: "期刊论文" },
  { value: "conference_paper", label: "会议论文" },
  { value: "book_chapter", label: "书籍章节" },
  { value: "dataset", label: "数据集" }
];

function formatMissingItemLabel(item) {
  return item?.label || MISSING_ITEM_LABELS[item?.type] || "材料信息不完整";
}

export function MaterialGatePage({ workspace, onReady, onEligible, onBackUpload, onError }) {
  const [snapshot, setSnapshot] = useState(null);
  const [academicDocument, setAcademicDocument] = useState(null);
  const [result, setResult] = useState(null);
  const [checking, setChecking] = useState(false);
  const [draftMode, setDraftMode] = useState("stable");
  const [literatureQuery, setLiteratureQuery] = useState("");
  const [literatureResult, setLiteratureResult] = useState(null);
  const [literatureSearching, setLiteratureSearching] = useState(false);
  const [literatureError, setLiteratureError] = useState("");
  const [copiedKey, setCopiedKey] = useState("");
  const [candidateSavingKey, setCandidateSavingKey] = useState("");
  const [literatureCandidates, setLiteratureCandidates] = useState([]);
  const [literatureFilters, setLiteratureFilters] = useState({
    searchIntent: "theory",
    providers: ["crossref", "openalex"],
    yearFrom: "",
    yearTo: "",
    workTypes: ["journal_article"],
    languageHint: ""
  });

  useEffect(() => {
    if (!workspace?.id) return;
    let cancelled = false;

    async function bootstrap() {
      try {
        const documents = await api.listAcademicDocuments(workspace.id);
        const selectedDocument = documents.find((item) => item.id === workspace.activeDocumentId)
          ?? documents.find((item) => item.primaryDocument)
          ?? documents[0]
          ?? null;
        let snapshotData = await api.getRequirementSnapshot(workspace.id, true);
        if (!snapshotData) {
          snapshotData = await api.createRequirementSnapshot(workspace.id, {
            documentId: selectedDocument?.id ?? null,
            sourceType: selectedDocument ? "DOCUMENT" : "PROJECT",
            topic: selectedDocument?.title ?? workspace.title,
            wordCount: selectedDocument?.targetLength ?? 3000,
            deadline: null,
            citationStyle: selectedDocument?.citationStyle ?? workspace.academicProfile?.defaultCitationStyle ?? "APA",
            specialRequirements: { minReferences: 5 }
          });
        }
        if (!cancelled) {
          setAcademicDocument(selectedDocument);
          setSnapshot(snapshotData);
        }
      } catch (error) {
        if (!cancelled) onError(error.message);
      }
    }

    bootstrap();
    return () => {
      cancelled = true;
    };
  }, [workspace?.id]);

  useEffect(() => {
    if (!workspace?.id) return;
    refreshLiteratureCandidates(true);
  }, [workspace?.id]);

  const suggestedLiteratureQuery = useMemo(() => {
    return [snapshot?.topic, workspace?.title, "相关研究 核心文献"]
      .filter(Boolean)
      .join(" ")
      .replace(/\s+/g, " ")
      .trim();
  }, [snapshot?.topic, workspace?.title]);

  useEffect(() => {
    if (!result || result.isGenerationEligible || literatureQuery.trim()) return;
    setLiteratureQuery(suggestedLiteratureQuery);
  }, [result, suggestedLiteratureQuery, literatureQuery]);

  async function handleCheck() {
    if (!workspace?.id || !snapshot?.id) return;
    try {
      setChecking(true);
      const checked = academicDocument?.id
        ? mapDocumentReadiness(await api.checkDocumentReadiness(academicDocument.id))
        : await api.checkMaterialSufficiency(workspace.id, snapshot.id);
      setResult(checked);
    } catch (error) {
      onError(error.message);
    } finally {
      setChecking(false);
    }
  }

  async function handleLegacyDraftGeneration() {
    if (!workspace?.id || !snapshot?.id) return;
    try {
      setChecking(true);
      const legacyCheck = await api.checkMaterialSufficiency(workspace.id, snapshot.id);
      if (!legacyCheck.isGenerationEligible) {
        setResult(legacyCheck);
        return;
      }
      await api.generateDraft(workspace.id, snapshot.id, draftMode);
      const drafts = await api.listDrafts(workspace.id);
      const latest = drafts.items?.[0];
      if (latest) onEligible(await api.getDraft(latest.id));
    } catch (error) {
      onError(error.message);
    } finally {
      setChecking(false);
    }
  }

  async function handleLiteratureSearch(event) {
    event?.preventDefault();
    if (!workspace?.id) return;
    const query = literatureQuery.trim() || suggestedLiteratureQuery;
    if (!query) {
      setLiteratureError("请先输入论文主题、关键词或研究方向。");
      return;
    }

    try {
      setLiteratureSearching(true);
      setLiteratureError("");
      const response = await api.searchLiterature(workspace.id, {
        query,
        source: literatureFilters.providers[0] || "crossref",
        limit: 10,
        missingItemType: "reference_material",
        providers: literatureFilters.providers,
        yearFrom: toOptionalNumber(literatureFilters.yearFrom),
        yearTo: toOptionalNumber(literatureFilters.yearTo),
        workTypes: literatureFilters.workTypes,
        languageHint: literatureFilters.languageHint || null,
        searchIntent: literatureFilters.searchIntent
      });
      setLiteratureQuery(response.query || query);
      setLiteratureResult(response);
    } catch (error) {
      setLiteratureError(error.message);
    } finally {
      setLiteratureSearching(false);
    }
  }

  async function refreshLiteratureCandidates(silent = false) {
    if (!workspace?.id) return;
    try {
      const response = await api.listLiteratureCandidates(workspace.id);
      setLiteratureCandidates(Array.isArray(response) ? response : []);
    } catch (error) {
      if (!silent) onError(error.message);
    }
  }

  async function handleSaveCandidate(item, index) {
    if (!workspace?.id) return;
    const key = literatureCandidateKey(item, index);
    try {
      setCandidateSavingKey(key);
      await api.saveLiteratureCandidate(workspace.id, item);
      await refreshLiteratureCandidates();
    } catch (error) {
      onError(error.message);
    } finally {
      setCandidateSavingKey("");
    }
  }

  async function copyCitation(item, index) {
    const text = item.citationPreview || [item.title, item.doi, item.url].filter(Boolean).join("\n");
    if (!text) return;
    try {
      await navigator.clipboard.writeText(text);
      setCopiedKey(`${item.doi || item.title || index}-${index}`);
    } catch {
      onError("复制失败，请手动复制文献信息。");
    }
  }

  return (
    <section className="page-card">
      <header className="workflow-page-header">
        <span className="eyebrow">研究准备 · 03</span>
        <h1 className="page-section-title">材料充足性检查</h1>
        <p className="section-help">系统按当前文档类型与研究范式判断材料是否足以支撑章节写作。材料不足时不会兜底写作，但会给出补充与真实文献检索入口。</p>
      </header>

      <div className="card-block">
        <h4>当前写作与提交要求基准</h4>
        {snapshot ? (
          <p className="muted">
            文档：{academicDocument?.title ?? snapshot.topic} ｜ 类型：{academicDocument?.documentType ?? "兼容项目"} ｜ 目标篇幅：{snapshot.wordCount} ｜ 引用格式：{snapshot.citationStyle}
          </p>
        ) : (
          <p className="muted">正在准备要求基准...</p>
        )}
      </div>

      <div className="button-row gate-action-row">
        <button className="primary-btn" onClick={handleCheck} disabled={checking || !snapshot}>
          {checking ? "检查中..." : "检查当前文档准备度"}
        </button>
        <button className="ghost-btn" onClick={onBackUpload}>
          返回继续补传
        </button>
      </div>

      {result?.isGenerationEligible && (
        <div className="gate-ready-panel">
          <div>
            <span className="status-badge ready">章节写作已就绪</span>
            <h4>材料可以支撑当前文档继续推进</h4>
            <p>{result.nextAction || "进入知识库确认材料片段后，可以按章节生成、共写和保存版本。"}</p>
          </div>
          <div className="gate-ready-actions">
            <button className="primary-btn" type="button" onClick={onReady}>进入项目知识库</button>
            <details className="legacy-action-disclosure">
              <summary>旧项目兼容操作</summary>
              <p>仅用于回归历史整篇草稿；新项目请进入学术文档按章节写作。</p>
              <div className="field gate-mode-field">
                <label>整篇初稿模式</label>
                <select value={draftMode} onChange={(event) => setDraftMode(event.target.value)}>
                  <option value="stable">稳妥版</option>
                  <option value="academic">学术版</option>
                  <option value="quick">快速版</option>
                </select>
              </div>
              <button className="ghost-btn" type="button" onClick={handleLegacyDraftGeneration} disabled={checking}>生成兼容整篇初稿</button>
            </details>
          </div>
        </div>
      )}

      {result && !result.isGenerationEligible && (
        <MaterialInsufficientPanel
          result={result}
          literatureQuery={literatureQuery}
          setLiteratureQuery={setLiteratureQuery}
          suggestedLiteratureQuery={suggestedLiteratureQuery}
          literatureResult={literatureResult}
          literatureSearching={literatureSearching}
          literatureError={literatureError}
          copiedKey={copiedKey}
          literatureFilters={literatureFilters}
          setLiteratureFilters={setLiteratureFilters}
          literatureCandidates={literatureCandidates}
          candidateSavingKey={candidateSavingKey}
          onLiteratureSearch={handleLiteratureSearch}
          onCopyCitation={copyCitation}
          onSaveCandidate={handleSaveCandidate}
          onBackUpload={onBackUpload}
        />
      )}
    </section>
  );
}

function mapDocumentReadiness(readiness) {
  const missingItems = (readiness.issues ?? [])
    .filter((item) => item.level === "BLOCKING")
    .map((item) => ({
      type: readinessIssueType(item.code),
      label: item.label,
      message: item.message,
      action: item.suggestedAction
    }));
  return {
    isGenerationEligible: readiness.generationEligible,
    missingItems,
    recommendedSupplements: missingItems.map((item) => ({
      type: item.type,
      label: item.label,
      suggestedCount: item.type === "reference_material" ? "3-5" : "1-2",
      message: item.action
    })),
    nextAction: readiness.nextAction,
    readiness
  };
}

function readinessIssueType(code) {
  if (code === "LITERATURE_MISSING") return "reference_material";
  if (code === "KEY_MATERIAL_NOT_PARSED") return "key_material";
  if (code === "REQUIREMENT_UNCONFIRMED") return "assignment_requirement";
  return "research_result";
}

function MaterialInsufficientPanel({
  result,
  literatureQuery,
  setLiteratureQuery,
  suggestedLiteratureQuery,
  literatureResult,
  literatureSearching,
  literatureError,
  copiedKey,
  literatureFilters,
  setLiteratureFilters,
  literatureCandidates,
  candidateSavingKey,
  onLiteratureSearch,
  onCopyCitation,
  onSaveCandidate,
  onBackUpload
}) {
  const hasReferenceGap = (result.missingItems ?? []).some((item) => item.type === "reference_material");
  const externalLinks = literatureResult?.externalSearchLinks?.length
    ? literatureResult.externalSearchLinks
    : buildClientExternalLinks(literatureQuery || suggestedLiteratureQuery);

  return (
    <div className="material-rescue-shell">
      <div className="material-rescue-head">
        <div>
          <span className="eyebrow">Material rescue</span>
          <h4>还差这些材料，先补文献或继续上传</h4>
          <p className="section-help">
            系统不会在材料不足时硬写正文。你可以先用公开学术源检索真实文献，自己下载后再回到这里上传解析。
          </p>
        </div>
        <span className="status-badge local_fix">{(result.missingItems ?? []).length} 项待补充</span>
      </div>

      <div className="material-gap-grid">
        {(result.missingItems ?? []).map((item, index) => (
          <div className="mini-card material-gap-card" key={`${item.type}-${index}`}>
            <strong>{formatMissingItemLabel(item)}</strong>
            <p className="muted">{item.message}</p>
            {item.action && <p className="missing-action">下一步：{item.action}</p>}
          </div>
        ))}
      </div>

      <div className={`literature-rescue-panel ${hasReferenceGap ? "is-highlighted" : ""}`}>
        <div className="literature-rescue-intro">
          <div>
            <div className="literature-rescue-title-row">
              <h4>去找可引用文献</h4>
              <span>{hasReferenceGap ? "推荐先处理" : "可选补强"}</span>
            </div>
            <p className="muted">
              站内检索 Crossref、OpenAlex 等公开元数据；需要全文时，再使用学校账号前往 Google Scholar 或知网下载。
            </p>
          </div>
        </div>

        <form className="literature-search-form" onSubmit={onLiteratureSearch}>
          <div className="literature-query-row">
            <div className="field">
              <label>检索关键词</label>
              <textarea
                value={literatureQuery}
                onChange={(event) => setLiteratureQuery(event.target.value)}
                placeholder="例如：智能教室 能源管理 机器学习 预测"
              />
            </div>
            <div className="literature-query-actions">
              <button className="primary-btn" disabled={literatureSearching}>
                <Search size={16} aria-hidden="true" />
                {literatureSearching ? "检索中..." : "检索文献线索"}
              </button>
              <button className="secondary-btn" type="button" onClick={() => setLiteratureQuery(suggestedLiteratureQuery)}>
                <Sparkles size={16} aria-hidden="true" />填入推荐词
              </button>
            </div>
          </div>
          <LiteratureSearchFilters
            filters={literatureFilters}
            setFilters={setLiteratureFilters}
          />
          <div className="literature-upload-handoff">
            <div>
              <strong>已经下载到原文？</strong>
              <span>上传并完成 AI 解析后，文献才会进入材料库与可信链。</span>
            </div>
            <button className="ghost-btn" type="button" onClick={onBackUpload}>
              <Upload size={16} aria-hidden="true" />去上传原文
            </button>
          </div>
        </form>

        {literatureError && <p className="literature-error">{literatureError}</p>}

        <div className="external-search-block">
          <div className="external-search-head">
            <strong>外部全文入口</strong>
            <span>站内结果不足时，带着当前关键词继续检索。</span>
          </div>
          <div className="external-search-strip">
            {externalLinks.map((link) => (
              <a key={link.provider} className="external-search-link" href={link.url} target="_blank" rel="noreferrer">
                {LITERATURE_SOURCE_LABELS[link.provider] || link.provider}<ExternalLink size={14} aria-hidden="true" />
              </a>
            ))}
          </div>
        </div>

        <LiteratureSearchResults
          result={literatureResult}
          copiedKey={copiedKey}
          candidates={literatureCandidates}
          candidateSavingKey={candidateSavingKey}
          onCopyCitation={onCopyCitation}
          onSaveCandidate={onSaveCandidate}
          onBackUpload={onBackUpload}
        />

        <LiteratureCandidateList candidates={literatureCandidates} onBackUpload={onBackUpload} />
      </div>

    </div>
  );
}

function LiteratureSearchFilters({ filters, setFilters }) {
  function toggleProvider(provider) {
    setFilters((current) => {
      const nextProviders = current.providers.includes(provider)
        ? current.providers.filter((item) => item !== provider)
        : [...current.providers, provider];
      return {
        ...current,
        providers: nextProviders.length > 0 ? nextProviders : ["crossref"]
      };
    });
  }

  function toggleWorkType(workType) {
    setFilters((current) => ({
      ...current,
      workTypes: current.workTypes.includes(workType)
        ? current.workTypes.filter((item) => item !== workType)
        : [...current.workTypes, workType]
    }));
  }

  const filterSummary = [
    `${filters.providers.length} 个站内来源`,
    filters.workTypes.length > 0 ? `${filters.workTypes.length} 类文献` : "全部文献类型",
    filters.yearFrom || filters.yearTo ? `${filters.yearFrom || "不限"}-${filters.yearTo || "至今"}` : "年份不限",
    filters.languageHint ? (filters.languageHint === "zh" ? "优先中文" : "优先英文") : "语言不限"
  ].join(" · ");

  return (
    <div className="literature-filter-panel">
      <div className="literature-filter-head">
        <div>
          <strong>选择检索目的</strong>
          <p className="muted">系统会根据目的调整检索词，不需要一次设置所有筛选项。</p>
        </div>
      </div>

      <div className="intent-chip-grid">
        {SEARCH_INTENT_OPTIONS.map((option) => (
          <button
            type="button"
            className={`intent-chip ${filters.searchIntent === option.value ? "selected" : ""}`}
            key={option.value}
            onClick={() => setFilters((current) => ({ ...current, searchIntent: option.value }))}
          >
            <strong>{option.label}</strong>
            <span>{option.hint}</span>
          </button>
        ))}
      </div>

      <details className="literature-advanced-filters">
        <summary>
          <span><SlidersHorizontal size={16} aria-hidden="true" />高级筛选</span>
          <small>{filterSummary}</small>
        </summary>
        <div className="literature-filter-grid">
          <div className="field">
            <label>站内来源</label>
            <div className="inline-checks">
              {PROVIDER_OPTIONS.map((option) => (
                <label className="inline-check" key={option.value}>
                  <input
                    type="checkbox"
                    checked={filters.providers.includes(option.value)}
                    onChange={() => toggleProvider(option.value)}
                  />
                  {option.label}
                </label>
              ))}
            </div>
          </div>
          <div className="field">
            <label>文献类型</label>
            <div className="inline-checks">
              {WORK_TYPE_OPTIONS.map((option) => (
                <label className="inline-check" key={option.value}>
                  <input
                    type="checkbox"
                    checked={filters.workTypes.includes(option.value)}
                    onChange={() => toggleWorkType(option.value)}
                  />
                  {option.label}
                </label>
              ))}
            </div>
          </div>
          <div className="field">
            <label>年份范围</label>
            <div className="year-range-row">
              <input
                value={filters.yearFrom}
                onChange={(event) => setFilters((current) => ({ ...current, yearFrom: event.target.value }))}
                placeholder="起始年"
                inputMode="numeric"
              />
              <span>至</span>
              <input
                value={filters.yearTo}
                onChange={(event) => setFilters((current) => ({ ...current, yearTo: event.target.value }))}
                placeholder="结束年"
                inputMode="numeric"
              />
            </div>
          </div>
          <div className="field">
            <label>语言倾向</label>
            <select
              value={filters.languageHint}
              onChange={(event) => setFilters((current) => ({ ...current, languageHint: event.target.value }))}
            >
              <option value="">不限</option>
              <option value="zh">优先中文</option>
              <option value="en">优先英文</option>
            </select>
          </div>
        </div>
      </details>
    </div>
  );
}

function LiteratureSearchResults({
  result,
  copiedKey,
  candidates,
  candidateSavingKey,
  onCopyCitation,
  onSaveCandidate,
  onBackUpload
}) {
  if (!result) {
    return (
      <div className="literature-empty-state">
        <strong>先检索，再挑选</strong>
        <p className="muted">检索结果只作为候选文献，不会自动进入材料库。下载或复制链接后，请回到上传页补传并完成 AI 解析。</p>
      </div>
    );
  }

  const items = result.items ?? [];
  const savedKeys = new Set((candidates ?? []).map((item) => item.duplicateGroupKey).filter(Boolean));

  return (
    <div className="literature-results">
      <div className="literature-results-head">
        <div>
          <strong>站内候选文献</strong>
          <p className="muted">检索词：{result.query}</p>
        </div>
        <div className="provider-status-row">
          {Object.entries(result.providerStatus ?? {}).map(([provider, status]) => (
            <span className={`status-badge ${status === "SUCCESS" ? "ready" : "local_fix"}`} key={provider}>
              {formatProviderName(provider)}：{formatProviderStatus(status)}
            </span>
          ))}
        </div>
      </div>

      {items.length === 0 ? (
        <div className="literature-empty-state">
          <strong>站内暂未返回可用候选</strong>
          <p className="muted">可以换一组关键词，或直接使用上方 Google Scholar / 知网入口继续查找。</p>
        </div>
      ) : (
        <div className="literature-result-list">
          {items.map((item, index) => {
            const key = literatureCandidateKey(item, index);
            const alreadySaved = Boolean(item.duplicateGroupKey && savedKeys.has(item.duplicateGroupKey));
            return (
              <article className="literature-card" key={key}>
                <div className="literature-card-main">
                  <div className="literature-card-meta-row">
                    <span className="hero-chip">{item.provider}</span>
                    <span className={`quality-chip ${qualityClass(item.qualityLabel)}`}>
                      {item.qualityLabel || "需人工确认"} · {item.qualityScore ?? "-"} 分
                    </span>
                  </div>
                  <h5>{item.title}</h5>
                  <p className="muted">{referenceMetaLine(item)}</p>
                  {item.recommendedUse && <p className="recommended-use">{item.recommendedUse}</p>}
                  {(item.matchedReasons ?? []).length > 0 && (
                    <div className="reason-chip-row">
                      {item.matchedReasons.map((reason) => (
                        <span key={reason}>{reason}</span>
                      ))}
                    </div>
                  )}
                  {(item.missingMetadata ?? []).length > 0 && (
                    <p className="metadata-warning">待人工确认：{item.missingMetadata.join("、")}</p>
                  )}
                  {item.abstractSnippet && <p className="literature-abstract">{item.abstractSnippet}</p>}
                  {item.citationPreview && <p className="literature-citation">{item.citationPreview}</p>}
                </div>
                <div className="literature-card-actions">
                  {item.url && (
                    <a className="ghost-btn" href={item.url} target="_blank" rel="noreferrer">
                      打开来源
                    </a>
                  )}
                  <button className="ghost-btn" type="button" onClick={() => onCopyCitation(item, index)}>
                    {copiedKey === key ? "已复制" : "复制 DOI / 引用"}
                  </button>
                  <button
                    className="ghost-btn"
                    type="button"
                    onClick={() => onSaveCandidate(item, index)}
                    disabled={alreadySaved || candidateSavingKey === key}
                  >
                    {alreadySaved ? "已加入清单" : candidateSavingKey === key ? "加入中..." : "加入待下载"}
                  </button>
                  <button className="secondary-btn" type="button" onClick={onBackUpload}>
                    我已下载，去上传
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}

function LiteratureCandidateList({ candidates, onBackUpload }) {
  if (!candidates?.length) {
    return null;
  }

  return (
    <div className="candidate-download-panel">
      <div className="candidate-download-head">
        <div>
          <strong>待下载清单</strong>
          <p className="muted">这些只是文献线索，还不会参与正文生成。下载原文后请回上传页关联候选并完成 AI 解析。</p>
        </div>
        <button className="ghost-btn" type="button" onClick={onBackUpload}>
          去上传原文
        </button>
      </div>
      <div className="candidate-download-list">
        {candidates.map((candidate) => (
          <article className="candidate-download-card" key={candidate.id}>
            <div>
              <div className="literature-card-meta-row">
                <span className="hero-chip">{candidate.provider}</span>
                <span className={`quality-chip ${qualityClass(candidate.qualityLabel)}`}>
                  {candidate.qualityLabel || "需人工确认"} · {candidate.qualityScore ?? "-"} 分
                </span>
                <span className={`status-pill ${candidate.status === "LINKED" ? "success" : "pending"}`}>
                  {candidate.status === "LINKED" ? "已关联上传材料" : "待下载原文"}
                </span>
              </div>
              <h5>{candidate.title}</h5>
              <p className="muted">{referenceMetaLine(candidate)}</p>
              {candidate.recommendedUse && <p className="recommended-use">{candidate.recommendedUse}</p>}
            </div>
            <div className="candidate-download-actions">
              {candidate.url && (
                <a className="ghost-btn" href={candidate.url} target="_blank" rel="noreferrer">
                  打开来源
                </a>
              )}
              <button className="secondary-btn" type="button" onClick={onBackUpload}>
                我已下载，去上传
              </button>
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}

function buildClientExternalLinks(query) {
  if (!query) return [];
  const encoded = encodeURIComponent(query);
  return [
    { provider: "Google Scholar", url: `https://scholar.google.com/scholar?q=${encoded}` },
    { provider: "CNKI", url: `https://oversea.cnki.net/kns/defaultresult/index?kw=${encoded}` },
    { provider: "Crossref", url: `https://search.crossref.org/?q=${encoded}` }
  ];
}

function referenceMetaLine(item) {
  const authors = Array.isArray(item.authors) && item.authors.length > 0 ? item.authors.join("、") : "作者待确认";
  return [
    authors,
    item.year || "年份待确认",
    item.sourceTitle || item.publisher || "来源待确认",
    item.doi && `DOI：${item.doi}`
  ]
    .filter(Boolean)
    .join(" ｜ ");
}

function formatProviderStatus(status) {
  switch (String(status || "").toUpperCase()) {
    case "SUCCESS":
      return "已返回结果";
    case "EMPTY":
      return "暂无结果";
    case "FAILED":
      return "站内检索失败";
    case "SKIPPED":
      return "未检索";
    default:
      return "待检索";
  }
}

function formatProviderName(provider) {
  switch (provider) {
    case "crossref":
      return "Crossref";
    case "openalex":
      return "OpenAlex";
    case "semantic_scholar":
      return "Semantic Scholar";
    default:
      return provider;
  }
}

function toOptionalNumber(value) {
  if (value == null || String(value).trim() === "") return null;
  const parsed = Number.parseInt(String(value).trim(), 10);
  return Number.isFinite(parsed) ? parsed : null;
}

function literatureCandidateKey(item, index) {
  return `${item.duplicateGroupKey || item.doi || item.title || index}-${index}`;
}

function qualityClass(label) {
  if (label === "推荐引用") return "quality-strong";
  if (label === "信息不完整") return "quality-weak";
  return "quality-check";
}
