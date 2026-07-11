import { useEffect, useMemo, useState } from "react";
import { AlertTriangle, Check, ShieldCheck, X } from "lucide-react";

export function AcademicCoWritePreviewDrawer({
  preview,
  applying,
  discarding,
  onApply,
  onDiscard
}) {
  const [paragraphIds, setParagraphIds] = useState([]);
  const [diffIds, setDiffIds] = useState([]);

  useEffect(() => {
    if (!preview) {
      setParagraphIds([]);
      setDiffIds([]);
      return;
    }
    setParagraphIds((preview.paragraphDiffRows ?? []).filter((row) => row.changed).map((row) => row.id));
    setDiffIds((preview.diffRows ?? []).filter((row) => row.changed).map((row) => row.id));
  }, [preview?.id]);

  const changedParagraphs = useMemo(
    () => (preview?.paragraphDiffRows ?? []).filter((row) => row.changed),
    [preview]
  );
  const changedRows = useMemo(
    () => (preview?.diffRows ?? []).filter((row) => row.changed),
    [preview]
  );

  if (!preview) return null;
  const warnings = preview.diffSummary?.conflictWarnings ?? [];

  function toggle(id, setter) {
    setter((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id]);
  }

  return (
    <div className="overlay-shell" onClick={onDiscard}>
      <aside className="drawer-panel drawer-panel--wide academic-preview-drawer" onClick={(event) => event.stopPropagation()}>
        <header className="drawer-header">
          <div>
            <h3>章节修改预览</h3>
            <p className="muted">正文尚未改变。你可以整章应用，也可以只接受选中的段落或局部修改。</p>
          </div>
          <button className="ghost-btn" type="button" onClick={onDiscard} disabled={applying || discarding}><X size={17} /></button>
        </header>

        <div className="drawer-body academic-preview-body">
          <div className="academic-preview-summary">
            <span>动作：{actionLabel(preview.action)}</span>
            <span>范围：{preview.targetRange?.mode === "selection" ? `字符 ${preview.targetRange.start}-${preview.targetRange.end}` : "当前章节"}</span>
            <span>变化：{signed(preview.diffSummary?.characterDelta ?? 0)} 字符</span>
            <span>关联审查：{preview.relatedReviewItemIds?.length ?? 0} 项</span>
          </div>

          <section className="academic-guardrail-strip">
            <ShieldCheck size={17} />
            {Object.entries(preview.controls ?? {}).map(([key, value]) => (
              <span className={value === true ? "is-on" : ""} key={key}>{controlLabel(key, value)}</span>
            ))}
          </section>

          {warnings.length > 0 && (
            <section className="academic-conflict-list">
              {warnings.map((warning) => (
                <article key={warning.code}>
                  <AlertTriangle size={16} />
                  <div><strong>{warning.title}</strong><p>{warning.message}</p></div>
                </article>
              ))}
            </section>
          )}

          <div className="academic-preview-compare">
            <section><span>当前章节</span><pre>{preview.baseContent}</pre></section>
            <section><span>AI 候选</span><pre>{preview.candidateContent}</pre></section>
          </div>

          <details open className="academic-selective-apply">
            <summary>逐段接受 · 已选 {paragraphIds.length}/{changedParagraphs.length}</summary>
            {changedParagraphs.length === 0 ? <p className="muted">没有可单独接受的段落差异。</p> : changedParagraphs.map((row) => (
              <label key={row.id}>
                <input type="checkbox" checked={paragraphIds.includes(row.id)} onChange={() => toggle(row.id, setParagraphIds)} />
                <span><strong>{row.id} · {row.intentLabel}</strong><small>{row.candidateText || "删除该段落"}</small></span>
              </label>
            ))}
            <button className="secondary-btn" type="button" disabled={!paragraphIds.length || applying} onClick={() => onApply("PARAGRAPHS", paragraphIds)}>
              应用选中段落
            </button>
          </details>

          <details className="academic-selective-apply">
            <summary>局部接受 · 已选 {diffIds.length}/{changedRows.length}</summary>
            {changedRows.length === 0 ? <p className="muted">没有可单独接受的局部差异。</p> : changedRows.map((row) => (
              <label key={row.id}>
                <input type="checkbox" checked={diffIds.includes(row.id)} onChange={() => toggle(row.id, setDiffIds)} />
                <span><strong>{row.id}</strong><small>{row.candidateText || "删除该句"}</small></span>
              </label>
            ))}
            <button className="secondary-btn" type="button" disabled={!diffIds.length || applying} onClick={() => onApply("DIFF_ROWS", diffIds)}>
              应用选中修改
            </button>
          </details>
        </div>

        <footer className="drawer-footer">
          <button className="ghost-btn" type="button" onClick={onDiscard} disabled={applying || discarding}>{discarding ? "放弃中" : "放弃预览"}</button>
          <button className="primary-btn icon-text-btn" type="button" onClick={() => onApply("ALL", [])} disabled={applying || discarding}>
            <Check size={16} /> {applying ? "应用中" : "应用整章为新版本"}
          </button>
        </footer>
      </aside>
    </div>
  );
}

function signed(value) {
  const number = Number(value || 0);
  return number >= 0 ? `+${number}` : String(number);
}

function actionLabel(value) {
  return {
    improve_expression: "优化学术表达",
    add_evidence: "补强证据",
    add_original_evidence: "补原创实证",
    adjust_structure: "调整论证结构",
    reduce_repetition: "减少重复论证",
    expand_argument: "展开论证",
    shorten_text: "压缩正文"
  }[value] ?? value;
}

function controlLabel(key, value) {
  const labels = {
    keepCitations: "保留引用",
    keepData: "保留数据",
    noNewSources: "不新增来源",
    keepStudentVoice: "保留作者声音",
    rewriteDepth: `改写力度：${value}`
  };
  return labels[key] ?? key;
}
