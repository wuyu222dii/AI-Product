import { Bot, History, Save, Scissors, ShieldCheck, Sparkles, WandSparkles } from "lucide-react";
import { useEffect, useRef } from "react";

const CO_WRITE_ACTIONS = [
  { value: "improve_expression", label: "优化学术表达" },
  { value: "add_evidence", label: "补强证据" },
  { value: "add_original_evidence", label: "补原创实证" },
  { value: "adjust_structure", label: "调整论证结构" },
  { value: "reduce_repetition", label: "减少重复论证" },
  { value: "expand_argument", label: "展开论证" },
  { value: "shorten_text", label: "压缩正文" }
];

export function AcademicSectionEditor({
  section,
  draft,
  onDraftChange,
  readiness,
  busyAction,
  onSave,
  generateInstruction,
  onGenerateInstructionChange,
  onGenerate,
  coWriteAction,
  onCoWriteActionChange,
  coWriteInstruction,
  onCoWriteInstructionChange,
  controls,
  onControlsChange,
  onPreview,
  versions,
  onRestoreVersion,
  selectedRange,
  onSelectionChange,
  assistantOpen,
  onToggleAssistant,
  onOpenChecks,
  onReviewSection,
  onSplitLegacy
}) {
  const editorRef = useRef(null);

  useEffect(() => {
    if (!editorRef.current || !selectedRange) return;
    editorRef.current.focus();
    editorRef.current.setSelectionRange(selectedRange.start, selectedRange.end);
  }, [selectedRange?.start, selectedRange?.end, section?.id]);

  if (!section) {
    return <section className="academic-editor academic-editor--empty"><p>选择左侧章节开始写作。</p></section>;
  }

  const canGenerate = Boolean(readiness?.generationEligible);

  function captureSelection(event) {
    const { selectionStart, selectionEnd, value } = event.currentTarget;
    onSelectionChange?.(selectionStart === selectionEnd ? null : {
      start: selectionStart,
      end: selectionEnd,
      selectedText: value.slice(selectionStart, selectionEnd)
    });
  }
  return (
    <section className="academic-editor">
      <header className="academic-editor-head">
        <div>
          <span>{section.sectionType} · v{section.versionNo}</span>
          <input
            className="academic-section-title-input"
            value={draft.title}
            onChange={(event) => onDraftChange({ ...draft, title: event.target.value })}
            aria-label="章节标题"
          />
        </div>
        <div className="academic-editor-actions">
          {section.sectionType === "LEGACY_FULL_TEXT" && (
            <button className="ghost-btn icon-text-btn" type="button" onClick={onSplitLegacy}>
              <Scissors size={16} aria-hidden="true" /> 拆分章节
            </button>
          )}
          <button className="ghost-btn icon-text-btn" type="button" onClick={onOpenChecks}>
            <ShieldCheck size={16} aria-hidden="true" /> 本章检查
          </button>
          <button className="ghost-btn icon-text-btn" type="button" onClick={onReviewSection}>
            <Sparkles size={16} aria-hidden="true" /> AI 审查本章
          </button>
          <button className={`ghost-btn icon-text-btn ${assistantOpen ? "is-active" : ""}`} type="button" onClick={onToggleAssistant}>
            <Bot size={16} aria-hidden="true" /> AI 助手
          </button>
          <button className="primary-btn icon-text-btn" type="button" onClick={onSave} disabled={busyAction === "save"}>
            <Save size={16} aria-hidden="true" /> {busyAction === "save" ? "保存中" : "保存章节"}
          </button>
        </div>
      </header>

      <textarea
        ref={editorRef}
        className="academic-writing-area"
        value={draft.content}
        onChange={(event) => onDraftChange({ ...draft, content: event.target.value })}
        placeholder="在这里写当前章节。AI 只会在你主动生成或共写时调用，并保留章节版本和材料依据。"
        onSelect={captureSelection}
      />
      <div className="academic-writing-meta">
        <span>{draft.content.length} 字符</span>
        <span>目标 {section.targetLength ?? "未设置"}</span>
        <span>{section.status}</span>
        <span>{selectedRange ? `已选 ${selectedRange.end - selectedRange.start} 字符` : "未选择正文"}</span>
      </div>

      {assistantOpen && <div className="academic-ai-console academic-ai-console--focused">
        <div className="academic-ai-column">
          <div className="academic-tool-title">
            <Sparkles size={17} aria-hidden="true" />
            <div><strong>基于材料生成本章</strong><span>只生成当前章节，不改动其他文档或章节。</span></div>
          </div>
          <textarea
            value={generateInstruction}
            onChange={(event) => onGenerateInstructionChange(event.target.value)}
            placeholder="可选：说明本章重点、论证边界或必须保留的观点"
          />
          <button className="secondary-btn icon-text-btn" type="button" onClick={onGenerate} disabled={!canGenerate || busyAction === "generate"} title={canGenerate ? "基于当前文档材料生成章节" : "当前章节仍有阻断性材料缺口"}>
            <Sparkles size={16} aria-hidden="true" />
            {busyAction === "generate" ? "AI 正在生成" : "生成章节草稿"}
          </button>
          {!canGenerate && <p className="academic-block-note">材料不足时不会编造正文；请先处理右侧准备度中的阻断项。</p>}
        </div>

        <div className="academic-ai-column">
          <div className="academic-tool-title">
            <WandSparkles size={17} aria-hidden="true" />
            <div><strong>共写预览</strong><span>先查看候选正文和差异，再决定是否应用。</span></div>
          </div>
          <select value={coWriteAction} onChange={(event) => onCoWriteActionChange(event.target.value)}>
            {CO_WRITE_ACTIONS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
          </select>
          <textarea
            value={coWriteInstruction}
            onChange={(event) => onCoWriteInstructionChange(event.target.value)}
            placeholder="描述你希望如何修改当前章节"
          />
          <div className="academic-control-row">
            <select value={controls.rewriteDepth} onChange={(event) => onControlsChange({ ...controls, rewriteDepth: event.target.value })} aria-label="改写深度">
              <option value="light">轻度改写</option>
              <option value="balanced">平衡改写</option>
              <option value="deep">深度改写</option>
            </select>
            {[['keepCitations', '保留引用'], ['keepData', '保留数据'], ['noNewSources', '不新增来源'], ['keepStudentVoice', '保留作者声音']].map(([key, label]) => (
              <label key={key}>
                <input type="checkbox" checked={Boolean(controls[key])} onChange={(event) => onControlsChange({ ...controls, [key]: event.target.checked })} />
                {label}
              </label>
            ))}
          </div>
          <button className="secondary-btn icon-text-btn" type="button" onClick={onPreview} disabled={busyAction === "preview" || !draft.content.trim()}>
            <WandSparkles size={16} aria-hidden="true" />
            {busyAction === "preview" ? "正在生成预览" : "生成修改预览"}
          </button>
        </div>
      </div>}

      {versions.length > 1 && (
        <div className="academic-output-dock">
          <details>
            <summary><History size={15} aria-hidden="true" /> 章节历史版本</summary>
            <div className="academic-version-list">
              {versions.map((version) => (
                <button type="button" key={version.id} onClick={() => onRestoreVersion(version)} disabled={version.versionNo === section.versionNo}>
                  <strong>v{version.versionNo}</strong>
                  <span>{version.changeSummary || version.changeSource}</span>
                </button>
              ))}
            </div>
          </details>
        </div>
      )}
    </section>
  );
}
