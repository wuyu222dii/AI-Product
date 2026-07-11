import { useEffect, useMemo, useState } from "react";
import { api } from "../services/api";
import {
  CITATION_STYLE_OPTIONS,
  collectReferenceMaterials,
  normalizeCitationStyle,
  referenceTextForMaterial
} from "../components/workspace/workspaceUtils.js";

export function ExportPage({ workspace, draft, onBack, onError }) {
  const [format, setFormat] = useState("docx");
  const [includeComments, setIncludeComments] = useState(false);
  const [status, setStatus] = useState("");
  const [downloadUrl, setDownloadUrl] = useState("");
  const [downloadMeta, setDownloadMeta] = useState(null);
  const [materials, setMaterials] = useState([]);
  const [citationStyle, setCitationStyle] = useState("APA");
  const [editingMaterialId, setEditingMaterialId] = useState("");
  const [metadataDraft, setMetadataDraft] = useState(null);
  const [metadataSaving, setMetadataSaving] = useState(false);
  const [evidenceSummary, setEvidenceSummary] = useState(null);
  const [writingRisks, setWritingRisks] = useState(null);

  useEffect(() => {
    if (!workspace?.id) return;
    let cancelled = false;
    async function loadPageData() {
      try {
        const data = await api.listMaterials(workspace.id);
        if (!cancelled) setMaterials(data.items ?? []);
      } catch (error) {
        if (!cancelled) onError(error.message);
      }
      try {
        const snapshot = await api.getRequirementSnapshot(workspace.id, true);
        if (!cancelled && snapshot?.citationStyle) {
          setCitationStyle(normalizeCitationStyle(snapshot.citationStyle));
        }
      } catch {
        // Requirement snapshot is optional for export preview; default to APA.
      }
      try {
        if (draft?.id) {
          const evidence = await api.getEvidenceBindings(draft.id);
          if (!cancelled) setEvidenceSummary(evidence);
        }
      } catch {
        // 导出页不因可信链加载失败阻断用户，保留基础交付确认。
      }
      try {
        if (draft?.id) {
          const risks = await api.getWritingRisks(draft.id);
          if (!cancelled) setWritingRisks(risks);
        }
      } catch {
        // 风险汇总属于导出前提醒，失败时仍保留本地兜底提示。
      }
    }
    loadPageData();
    return () => {
      cancelled = true;
    };
  }, [workspace?.id, draft?.id, onError]);

  const referenceMaterials = useMemo(() => collectReferenceMaterials(draft, materials), [draft, materials]);
  const exportReadiness = useMemo(
    () => buildExportReadiness(referenceMaterials, evidenceSummary, writingRisks, draft?.draftText),
    [referenceMaterials, evidenceSummary, writingRisks, draft?.draftText]
  );

  function startEditMetadata(material) {
    const metadata = material.bibliographicMetadata || {};
    setEditingMaterialId(material.id);
    setMetadataDraft({
      authors: Array.isArray(metadata.authors) ? metadata.authors.join("、") : "",
      year: metadata.year || "",
      title: metadata.title || "",
      sourceTitle: metadata.sourceTitle || "",
      publisher: metadata.publisher || "",
      doi: metadata.doi || "",
      url: metadata.url || "",
      publicationType: metadata.publicationType || "UNKNOWN"
    });
  }

  async function saveMetadata(material) {
    if (!metadataDraft) return;
    try {
      setMetadataSaving(true);
      const updated = await api.updateBibliographicMetadata(material.id, {
        ...metadataDraft,
        authors: metadataDraft.authors
          .split(/[、,，;]/)
          .map((item) => item.trim())
          .filter(Boolean)
      });
      setMaterials((current) => current.map((item) => (item.id === updated.id ? updated : item)));
      setEditingMaterialId("");
      setMetadataDraft(null);
    } catch (error) {
      onError(error.message);
    } finally {
      setMetadataSaving(false);
    }
  }

  async function handleExport() {
    if (!draft?.id) {
      onError("当前没有可导出的草稿");
      return;
    }
    try {
      setStatus("导出中...");
      setDownloadUrl("");
      setDownloadMeta(null);
      const job = await api.exportDraft(draft.id, { format, includeComments, citationStyle });
      if (job?.jobId) {
        const detail = await api.getJob(job.jobId);
        setStatus(`导出任务状态：${detail.status}`);
        if (detail.outputRef?.downloadUrl) {
          setDownloadUrl(detail.outputRef.downloadUrl);
          setDownloadMeta(detail.outputRef);
        }
      } else {
        setStatus("导出任务已创建");
      }
    } catch (error) {
      onError(error.message);
      setStatus("");
    }
  }

  return (
    <section className="page-card">
      <h3 className="page-section-title">导出定稿</h3>
      <p className="section-help">支持 docx / pdf 导出。系统会根据正文来源追溯生成参考文献草案，并追加到导出文件末尾。</p>
      <div className="grid-2">
        <div className="card-block">
          <div className="field">
            <label>导出格式</label>
            <select value={format} onChange={(event) => setFormat(event.target.value)}>
              <option value="docx">docx</option>
              <option value="pdf">pdf</option>
            </select>
          </div>
          <div className="field">
            <label>参考文献格式</label>
            <select value={citationStyle} onChange={(event) => setCitationStyle(event.target.value)}>
              {CITATION_STYLE_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>
              <input
                type="checkbox"
                checked={includeComments}
                onChange={(event) => setIncludeComments(event.target.checked)}
                style={{ width: "auto", marginRight: 8 }}
              />
              包含批注
            </label>
          </div>
          <div className="button-row">
            <button className="primary-btn" onClick={handleExport}>
              发起导出
            </button>
            <button className="ghost-btn" onClick={onBack}>
              返回工作台
            </button>
          </div>
        </div>
        <div className="card-block">
          <h4>导出状态</h4>
          <p className="muted">{status || "尚未发起导出"}</p>
          {downloadUrl && (
            <>
              <div className="export-success-card">
                <div>
                  <span className="status-badge ready">导出成功</span>
                  <strong>定稿文件已生成</strong>
                  <p className="muted">
                    文件名：{downloadMeta?.fileName ?? "已生成"} ｜ 格式：{downloadMeta?.format ?? format} ｜ 引用格式：{citationStyle}
                  </p>
                </div>
                <div className="export-success-meta">
                  <span>{includeComments ? "包含批注" : "不含批注"}</span>
                  <span>{referenceMaterials.length} 条参考文献</span>
                </div>
              </div>
              <div className="button-row" style={{ marginTop: 12 }}>
                <a className="secondary-btn" href={downloadUrl} target="_blank" rel="noreferrer">
                  下载导出文件
                </a>
                <button type="button" className="ghost-btn" onClick={onBack}>
                  回到工作台继续修改
                </button>
              </div>
            </>
          )}
        </div>
      </div>
      <div className="card-block export-readiness-block">
        <div className="reference-item-head">
          <div>
            <h4>交付确认</h4>
            <p className="section-help">导出前建议快速确认正文、引用和文献信息。这里不阻断导出，只帮你降低交付风险。</p>
          </div>
          <span className={`status-badge ${exportReadiness.level}`}>
            {exportReadiness.label}
          </span>
        </div>
        <div className="export-check-grid">
          {exportReadiness.items.map((item) => (
            <div className={`export-check-card export-check-card--${item.level}`} key={item.key}>
              <strong>{item.title}</strong>
              <p>{item.detail}</p>
            </div>
          ))}
        </div>
      </div>
      <div className="card-block reference-preview-block">
        <h4>参考文献草案</h4>
        <p className="section-help">
          以下内容根据材料可信链自动整理。导出前建议补全作者、年份、标题和来源链接。
        </p>
        {referenceMaterials.length === 0 ? (
          <div className="empty-state">
            <p className="muted">当前草稿还没有可整理为参考文献的来源材料。</p>
          </div>
        ) : (
          <div className="reference-list">
            {referenceMaterials.map((material) => (
              <ReferencePreviewItem
                key={material.id}
                material={material}
                citationStyle={citationStyle}
                referenceIndex={referenceMaterials.findIndex((item) => item.id === material.id) + 1}
                editing={editingMaterialId === material.id}
                metadataDraft={metadataDraft}
                metadataSaving={metadataSaving}
                onEdit={() => startEditMetadata(material)}
                onCancel={() => {
                  setEditingMaterialId("");
                  setMetadataDraft(null);
                }}
                onDraftChange={setMetadataDraft}
                onSave={() => saveMetadata(material)}
              />
            ))}
          </div>
        )}
      </div>
    </section>
  );
}

function buildExportReadiness(referenceMaterials, evidenceSummary, writingRisks, draftText = "") {
  const totalReferences = referenceMaterials.length;
  const incompleteReferences = referenceMaterials.filter((material) => {
    const metadata = material.bibliographicMetadata || {};
    return !metadata.title || !metadata.year || !Array.isArray(metadata.authors) || metadata.authors.length === 0;
  }).length;
  const coverage = evidenceSummary?.coverage;
  const citationConsistency = evidenceSummary?.citationConsistency;
  const writingRiskItems = buildWritingRiskItems(writingRisks, draftText);
  const items = [
    {
      key: "references",
      title: "参考文献",
      level: totalReferences > 0 ? "ready" : "warn",
      detail: totalReferences > 0
        ? `当前可整理 ${totalReferences} 条参考文献。`
        : "当前草稿还没有可整理的来源材料，建议回工作台补充可信链。"
    },
    {
      key: "metadata",
      title: "文献信息",
      level: incompleteReferences === 0 ? "ready" : "warn",
      detail: incompleteReferences === 0
        ? "已引用材料的作者、年份、题名信息较完整。"
        : `${incompleteReferences} 条参考文献信息不完整，建议导出前补齐。`
    },
    {
      key: "evidence_coverage",
      title: "可信链覆盖率",
      level: !coverage || coverage.coverageRatio >= 70 ? "ready" : "warn",
      detail: coverage
        ? `当前可信链覆盖率 ${coverage.coverageRatio}%，确认率 ${coverage.confirmedRatio}%，缺来源段落 ${coverage.missingParagraphs} 个。`
        : "暂未读取到可信链结果，建议回工作台重建可信链后再导出。"
    },
    {
      key: "citation_consistency",
      title: "引用一致性",
      level: !citationConsistency || citationConsistency.status === "READY" ? "ready" : "warn",
      detail: citationConsistency
        ? citationConsistency.issues?.[0] || "正文引用、材料来源和文献信息未发现明显冲突。"
        : "暂未读取到引用一致性检查结果。"
    },
    ...writingRiskItems,
    {
      key: "format",
      title: "导出格式",
      level: "ready",
      detail: "支持 DOCX / PDF 导出，可按需要选择是否包含批注。"
    }
  ];
  const hasWarning = items.some((item) => item.level === "warn");
  return {
    level: hasWarning ? "local_fix" : "ready",
    label: hasWarning ? "建议确认" : "可以交付",
    items
  };
}

function buildWritingRiskItems(writingRisks, text = "") {
  if (writingRisks?.items) {
    const riskCount = writingRisks.items.length;
    const localFixCount = writingRisks.items.filter((item) => item.level === "LOCAL_FIX").length;
    const topRisk = writingRisks.items[0];
    return [
      {
        key: "original_evidence_risk",
        title: "原创实证与 AI 写作味",
        level: writingRisks.overallStatus === "READY" ? "ready" : "warn",
        detail: writingRisks.overallStatus === "READY"
          ? `风险质量分 ${writingRisks.overallScore}，暂未发现明显空泛无据段落。`
          : `风险质量分 ${writingRisks.overallScore}，${localFixCount || riskCount} 个段落建议补真实案例、数据或来源支撑。`
      },
      {
        key: "original_evidence_top_action",
        title: "重点补强动作",
        level: riskCount === 0 ? "ready" : "warn",
        detail: topRisk
          ? `${topRisk.paragraphId}：${topRisk.suggestedAction}`
          : "当前没有段落级原创补强建议，导出前仍建议人工通读。"
      }
    ];
  }

  const source = String(text || "");
  const vaguePhrases = ["具有重要意义", "显著提升", "有效促进", "综上所述", "不可忽视", "进一步研究"];
  const vagueCount = vaguePhrases.reduce((count, phrase) => count + (source.includes(phrase) ? 1 : 0), 0);
  const paragraphCount = source.split(/\n\s*\n/).filter((item) => item.trim().length > 0).length;
  const longParagraphCount = source.split(/\n\s*\n/).filter((item) => item.trim().length > 600).length;
  return [
    {
      key: "aigc_style",
      title: "原创实证与 AI 写作味",
      level: vagueCount >= 3 ? "warn" : "ready",
      detail: vagueCount >= 3
        ? `检测到 ${vagueCount} 类偏模板化表达，建议回工作台做自然化改写。`
        : "暂未发现明显模板化高频表达，仍建议人工通读。"
    },
    {
      key: "academic_expression",
      title: "学术表达结构",
      level: paragraphCount > 0 && longParagraphCount <= Math.max(1, Math.floor(paragraphCount / 3)) ? "ready" : "warn",
      detail: longParagraphCount > 0
        ? `有 ${longParagraphCount} 个段落较长，建议拆分论点、证据和结论。`
        : "段落长度整体较均衡。"
    }
  ];
}

function ReferencePreviewItem({
  material,
  citationStyle,
  referenceIndex,
  editing,
  metadataDraft,
  metadataSaving,
  onEdit,
  onCancel,
  onDraftChange,
  onSave
}) {
  const metadata = material.bibliographicMetadata || {};
  const authors = Array.isArray(metadata.authors) ? metadata.authors.filter(Boolean).join("、") : "";
  if (editing && metadataDraft) {
    return (
      <article className="reference-item reference-item--editing">
        <div className="reference-edit-head">
          <strong>编辑文献信息</strong>
          <div className="button-row">
            <button type="button" className="ghost-btn" onClick={onCancel} disabled={metadataSaving}>
              取消
            </button>
            <button type="button" className="secondary-btn" onClick={onSave} disabled={metadataSaving}>
              {metadataSaving ? "保存中..." : "保存"}
            </button>
          </div>
        </div>
        <div className="reference-edit-grid">
          <MetadataField label="作者（多个用顿号/逗号分隔）" field="authors" draft={metadataDraft} onDraftChange={onDraftChange} />
          <MetadataField label="年份" field="year" draft={metadataDraft} onDraftChange={onDraftChange} />
          <MetadataField label="题名" field="title" draft={metadataDraft} onDraftChange={onDraftChange} />
          <MetadataField label="期刊/会议/网站/书名" field="sourceTitle" draft={metadataDraft} onDraftChange={onDraftChange} />
          <MetadataField label="出版社" field="publisher" draft={metadataDraft} onDraftChange={onDraftChange} />
          <MetadataField label="DOI" field="doi" draft={metadataDraft} onDraftChange={onDraftChange} />
          <MetadataField label="链接" field="url" draft={metadataDraft} onDraftChange={onDraftChange} />
          <label className="field">
            <span>文献类型</span>
            <select
              value={metadataDraft.publicationType}
              onChange={(event) => onDraftChange((current) => ({ ...current, publicationType: event.target.value }))}
            >
              <option value="JOURNAL_ARTICLE">期刊论文</option>
              <option value="BOOK">图书</option>
              <option value="REPORT">报告</option>
              <option value="WEBPAGE">网页</option>
              <option value="THESIS">学位论文</option>
              <option value="CONFERENCE">会议论文</option>
              <option value="UNKNOWN">未确定</option>
            </select>
          </label>
        </div>
      </article>
    );
  }

  return (
    <article className="reference-item">
      <div className="reference-item-head">
        <strong>{referenceTextForMaterial(material, citationStyle, referenceIndex)}</strong>
        <button type="button" className="ghost-btn" onClick={onEdit}>
          编辑文献信息
        </button>
      </div>
      <p className="muted">
        材料文件：{material.filename} ｜ 类型：{material.effectiveMaterialCategory || material.fileType}
      </p>
      {(authors || metadata.year || metadata.title || metadata.sourceTitle || metadata.publisher || metadata.url) && (
        <p className="muted">
          识别信息：
          {authors && ` 作者：${authors}`}
          {metadata.year && ` ｜ 年份：${metadata.year}`}
          {metadata.title && ` ｜ 题名：${metadata.title}`}
          {(metadata.sourceTitle || metadata.publisher) && ` ｜ 来源：${metadata.sourceTitle || metadata.publisher}`}
          {metadata.url && ` ｜ 链接：${metadata.url}`}
        </p>
      )}
    </article>
  );
}

function MetadataField({ label, field, draft, onDraftChange }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input
        value={draft[field] || ""}
        onChange={(event) => onDraftChange((current) => ({ ...current, [field]: event.target.value }))}
      />
    </label>
  );
}
