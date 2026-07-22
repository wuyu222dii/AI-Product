import { useEffect, useMemo, useState } from "react";
import { api } from "../services/api";

export function KnowledgeBasePage({ workspace, draft, onContinue, onBackMaterials, onError }) {
  const [chunks, setChunks] = useState([]);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [buildSummary, setBuildSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [building, setBuilding] = useState(false);
  const [searching, setSearching] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  async function loadChunks(cancelled = false) {
    try {
      const data = await api.listKnowledgeChunks(workspace.id);
      if (!cancelled) {
        setChunks(data.items ?? []);
      }
    } catch (error) {
      if (!cancelled) onError(error.message);
    } finally {
      if (!cancelled) setLoading(false);
    }
  }

  useEffect(() => {
    if (!workspace?.id) return;
    let cancelled = false;
    loadChunks(cancelled);
    return () => {
      cancelled = true;
    };
  }, [workspace?.id]);

  const stats = useMemo(() => {
    const materialCount = new Set(chunks.map((item) => item.materialId)).size;
    return {
      chunkCount: chunks.length,
      materialCount,
      previewCount: Math.min(chunks.length, 6),
      mode: "关键词检索"
    };
  }, [chunks]);

  async function handleBuild() {
    try {
      setBuilding(true);
      const summary = await api.buildKnowledgeBase(workspace.id);
      setBuildSummary(summary);
      await loadChunks(false);
      setResults([]);
      setHasSearched(false);
    } catch (error) {
      onError(error.message);
    } finally {
      setBuilding(false);
    }
  }

  async function handleSearch(event) {
    event.preventDefault();
    if (!query.trim()) {
      onError("请输入要检索的问题或关键词");
      return;
    }
    try {
      setSearching(true);
      const data = await api.searchKnowledgeBase(workspace.id, query.trim(), 8);
      setResults(data.items ?? []);
      setHasSearched(true);
    } catch (error) {
      onError(error.message);
    } finally {
      setSearching(false);
    }
  }

  const visibleItems = hasSearched ? results : chunks.slice(0, 6);

  return (
    <section className="knowledge-page">
      <div className="page-card knowledge-hero-card">
        <div>
          <span className="eyebrow">研究资产</span>
          <h1 className="page-section-title">项目知识库</h1>
          <p className="section-help">
            知识库会把当前项目中已完成 AI 解析的材料整理成可检索证据片段，后续可用于正文共写、引用定位和审查补证。
          </p>
        </div>
        <div className="button-row">
          <button className="secondary-btn" onClick={onBackMaterials}>
            返回材料检查
          </button>
          <button className="primary-btn" onClick={onContinue}>
            进入学术文档与章节工作台
          </button>
        </div>
      </div>

      <div className="knowledge-grid">
        <div className="page-card knowledge-control-card">
          <div className="knowledge-stat-grid">
            <div className="summary-card">
              <strong>入库片段</strong>
              <span>{stats.chunkCount}</span>
            </div>
            <div className="summary-card">
              <strong>关联材料</strong>
              <span>{stats.materialCount}</span>
            </div>
            <div className="summary-card">
              <strong>预览片段</strong>
              <span>{stats.previewCount}</span>
            </div>
          </div>

          <div className="knowledge-build-box">
            <h4>构建方式</h4>
            <p className="muted">
              当前模式：{stats.mode}。系统只纳入已完成 AI 解析的材料，未解析材料不会进入知识库。
            </p>
            <button className="primary-btn" onClick={handleBuild} disabled={building}>
              {building ? "构建中..." : "构建 / 重建知识库"}
            </button>
          </div>

          {buildSummary && (
            <div className={`knowledge-summary ${buildSummary.chunkCount > 0 ? "ready" : "warning"}`}>
              <strong>{buildSummary.status}</strong>
              <p>{buildSummary.message}</p>
              <span>
                纳入材料 {buildSummary.materialCount} 份，生成片段 {buildSummary.chunkCount} 条。
              </span>
            </div>
          )}

          <form className="knowledge-search-form" onSubmit={handleSearch}>
            <div className="field">
              <label>问知识库</label>
              <textarea
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="例如：哪些材料能支撑“AI 提升资料整理效率但削弱独立思考”？"
              />
            </div>
            <button className="secondary-btn" disabled={searching || stats.chunkCount === 0}>
              {searching ? "检索中..." : "检索相关证据片段"}
            </button>
          </form>
        </div>

        <div className="page-card knowledge-result-card">
          <div className="knowledge-result-head">
            <div>
              <h3>{hasSearched ? "检索结果" : "已入库片段预览"}</h3>
              <p className="muted">
                {loading
                  ? "正在读取知识库..."
                  : hasSearched
                    ? `本次返回 ${results.length} 条相关片段`
                    : "先查看当前知识库里已有的材料片段"}
              </p>
            </div>
            <span className="hero-chip">v2.0 项目知识库</span>
          </div>

          <div className="knowledge-result-list">
            {loading ? (
              <div className="empty-state">
                <p>正在读取知识库片段...</p>
              </div>
            ) : stats.chunkCount === 0 ? (
              <div className="empty-state">
                <p>当前还没有知识库片段。请先完成材料 AI 解析，然后点击“构建 / 重建知识库”。</p>
              </div>
            ) : visibleItems.length === 0 ? (
              <div className="empty-state">
                <p>没有找到明显相关片段。可以换一个更具体的问题，或回到上传/解析页补充材料。</p>
              </div>
            ) : (
              visibleItems.map((item) => <KnowledgeChunkCard key={item.id} item={item} showScore={hasSearched} />)
            )}
          </div>
        </div>
      </div>
    </section>
  );
}

function KnowledgeChunkCard({ item, showScore }) {
  return (
    <article className="knowledge-chunk-card">
      <div className="knowledge-chunk-head">
        <div>
          <strong>{item.materialTitle || "未知材料"}</strong>
          <p className="muted">片段 #{item.chunkIndex} ｜ 当前使用关键词检索</p>
        </div>
        {showScore && <span className="score-pill">匹配度 {formatScore(item.score)}</span>}
      </div>
      <p className="knowledge-excerpt">{formatChunkPreview(item.sourceExcerpt || item.chunkText)}</p>
      {(item.keywords ?? []).length > 0 && (
        <div className="keyword-row">
          {item.keywords.slice(0, 8).map((keyword) => (
            <span key={keyword}>{keyword}</span>
          ))}
        </div>
      )}
    </article>
  );
}

function formatScore(score) {
  const numeric = Number(score);
  if (Number.isNaN(numeric)) return "-";
  return `${Math.round(numeric * 100)}%`;
}

function formatChunkPreview(value) {
  return String(value ?? "")
    .replaceAll("解析摘要：", "摘要：")
    .replaceAll("主题关系：", "关系：")
    .replaceAll("材料文件：", "来源：");
}
