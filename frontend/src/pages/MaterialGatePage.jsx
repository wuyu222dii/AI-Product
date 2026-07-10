import { useEffect, useMemo, useState } from "react";
import { api } from "../services/api";

const MISSING_ITEM_LABELS = {
  key_material: "核心材料尚未完成 AI 解析",
  assignment_requirement: "缺少老师要求或作业说明",
  reference_material: "缺少可引用参考资料",
  research_result: "缺少你的研究内容或写作基础"
};

const LITERATURE_SOURCE_LABELS = {
  "Google Scholar": "Google Scholar",
  CNKI: "知网",
  Crossref: "Crossref"
};

function formatMissingItemLabel(item) {
  return item?.label || MISSING_ITEM_LABELS[item?.type] || "材料信息不完整";
}

export function MaterialGatePage({ workspace, onEligible, onBackUpload, onError }) {
  const [snapshot, setSnapshot] = useState(null);
  const [result, setResult] = useState(null);
  const [checking, setChecking] = useState(false);
  const [draftMode, setDraftMode] = useState("stable");
  const [literatureQuery, setLiteratureQuery] = useState("");
  const [literatureResult, setLiteratureResult] = useState(null);
  const [literatureSearching, setLiteratureSearching] = useState(false);
  const [literatureError, setLiteratureError] = useState("");
  const [copiedKey, setCopiedKey] = useState("");

  useEffect(() => {
    if (!workspace?.id) return;
    let cancelled = false;

    async function bootstrap() {
      try {
        let snapshotData;
        try {
          snapshotData = await api.getRequirementSnapshot(workspace.id);
        } catch {
          snapshotData = await api.createRequirementSnapshot(workspace.id, {
            topic: workspace.title,
            wordCount: 3000,
            deadline: null,
            citationStyle: "APA",
            specialRequirements: { minReferences: 5 }
          });
        }
        if (!cancelled) setSnapshot(snapshotData);
      } catch (error) {
        if (!cancelled) onError(error.message);
      }
    }

    bootstrap();
    return () => {
      cancelled = true;
    };
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
      const checked = await api.checkMaterialSufficiency(workspace.id, snapshot.id);
      setResult(checked);
      if (checked.isGenerationEligible) {
        await api.generateDraft(workspace.id, snapshot.id, draftMode);
        const drafts = await api.listDrafts(workspace.id);
        const latest = drafts.items?.[0];
        if (latest) {
          const draft = await api.getDraft(latest.id);
          onEligible(draft);
        }
      }
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
        source: "crossref",
        limit: 10,
        missingItemType: "reference_material"
      });
      setLiteratureQuery(response.query || query);
      setLiteratureResult(response);
    } catch (error) {
      setLiteratureError(error.message);
    } finally {
      setLiteratureSearching(false);
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
      <h3 className="page-section-title">材料充足性检查</h3>
      <p className="section-help">系统先判断当前材料是否足以支撑正文生成。材料不足时不会兜底写作，但会帮你找到下一步可以补充的真实文献入口。</p>

      <div className="card-block">
        <h4>当前 Requirement Snapshot</h4>
        {snapshot ? (
          <p className="muted">
            题目：{snapshot.topic} ｜ 字数：{snapshot.wordCount} ｜ 引用格式：{snapshot.citationStyle}
          </p>
        ) : (
          <p className="muted">正在准备要求基准...</p>
        )}
      </div>

      <div className="button-row gate-action-row">
        <div className="field gate-mode-field">
          <label>初稿生成模式</label>
          <select value={draftMode} onChange={(event) => setDraftMode(event.target.value)}>
            <option value="stable">稳妥版</option>
            <option value="academic">学术版</option>
            <option value="quick">快速版</option>
          </select>
        </div>
        <button className="primary-btn" onClick={handleCheck} disabled={checking || !snapshot}>
          {checking ? "检查中..." : "执行材料检查并生成初稿"}
        </button>
        <button className="ghost-btn" onClick={onBackUpload}>
          返回继续补传
        </button>
      </div>

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
          onLiteratureSearch={handleLiteratureSearch}
          onCopyCitation={copyCitation}
          onBackUpload={onBackUpload}
        />
      )}
    </section>
  );
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
  onLiteratureSearch,
  onCopyCitation,
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
            <h4>去找可引用文献</h4>
            <p className="muted">
              站内先检索 Crossref 公开元数据；Google Scholar 和知网会作为外部入口打开，你可以用自己的学校账号下载原文。
            </p>
          </div>
          <span>{hasReferenceGap ? "推荐先处理" : "可选补强"}</span>
        </div>

        <form className="literature-search-form" onSubmit={onLiteratureSearch}>
          <div className="field">
            <label>检索关键词</label>
            <textarea
              value={literatureQuery}
              onChange={(event) => setLiteratureQuery(event.target.value)}
              placeholder="例如：智能教室 能源管理 机器学习 预测"
            />
          </div>
          <div className="button-row">
            <button className="primary-btn" disabled={literatureSearching}>
              {literatureSearching ? "检索中..." : "检索 Crossref 文献"}
            </button>
            <button className="ghost-btn" type="button" onClick={() => setLiteratureQuery(suggestedLiteratureQuery)}>
              填入推荐关键词
            </button>
            <button className="ghost-btn" type="button" onClick={onBackUpload}>
              我已下载，去上传
            </button>
          </div>
        </form>

        {literatureError && <p className="literature-error">{literatureError}</p>}

        <div className="external-search-strip">
          {externalLinks.map((link) => (
            <a key={link.provider} className="external-search-link" href={link.url} target="_blank" rel="noreferrer">
              打开 {LITERATURE_SOURCE_LABELS[link.provider] || link.provider}
            </a>
          ))}
        </div>

        <LiteratureSearchResults
          result={literatureResult}
          copiedKey={copiedKey}
          onCopyCitation={onCopyCitation}
          onBackUpload={onBackUpload}
        />
      </div>

      {(result.recommendedSupplements ?? []).length > 0 && (
        <div className="recommended-supplements">
          <h4>建议补充数量</h4>
          <div className="list-stack">
            {result.recommendedSupplements.map((item, index) => (
              <div className="mini-card" key={`recommend-${item.type}-${index}`}>
                <strong>{item.label || formatMissingItemLabel(item)}</strong>
                <p className="muted">
                  建议数量：{item.suggestedCount || "-"} ｜ {item.message}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function LiteratureSearchResults({ result, copiedKey, onCopyCitation, onBackUpload }) {
  if (!result) {
    return (
      <div className="literature-empty-state">
        <strong>先检索，再挑选</strong>
        <p className="muted">检索结果只作为候选文献，不会自动进入材料库。下载或复制链接后，请回到上传页补传并完成 AI 解析。</p>
      </div>
    );
  }

  const crossrefStatus = result.providerStatus?.crossref;
  const items = result.items ?? [];

  return (
    <div className="literature-results">
      <div className="literature-results-head">
        <div>
          <strong>Crossref 候选文献</strong>
          <p className="muted">检索词：{result.query}</p>
        </div>
        <span className={`status-badge ${crossrefStatus === "SUCCESS" ? "ready" : "local_fix"}`}>
          {formatProviderStatus(crossrefStatus)}
        </span>
      </div>

      {items.length === 0 ? (
        <div className="literature-empty-state">
          <strong>站内暂未返回可用候选</strong>
          <p className="muted">可以换一组关键词，或直接使用上方 Google Scholar / 知网入口继续查找。</p>
        </div>
      ) : (
        <div className="literature-result-list">
          {items.map((item, index) => {
            const key = `${item.doi || item.title || index}-${index}`;
            return (
              <article className="literature-card" key={key}>
                <div className="literature-card-main">
                  <span className="hero-chip">{item.provider}</span>
                  <h5>{item.title}</h5>
                  <p className="muted">{referenceMetaLine(item)}</p>
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
