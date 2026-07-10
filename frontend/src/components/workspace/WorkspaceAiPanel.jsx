import { useState } from "react";
import { CO_WRITE_ACTIONS } from "./constants.js";
import { WorkspacePanel } from "./WorkspacePanel.jsx";
import { describeSelection, hasValidRange } from "./workspaceUtils.js";

export function WorkspaceAiPanel({ coWriting, latestFeedback, selectedRange, onCoWrite }) {
  const [customInstruction, setCustomInstruction] = useState("");
  const [rewriteDepth, setRewriteDepth] = useState("balanced");
  const [showAdvancedActions, setShowAdvancedActions] = useState(false);
  const [guardrails, setGuardrails] = useState({
    keepCitations: true,
    keepData: true,
    noNewSources: true,
    keepStudentVoice: true
  });

  function currentControls() {
    return {
      rewriteDepth,
      ...guardrails
    };
  }

  function handleCustomSubmit() {
    const instruction = customInstruction.trim();
    if (!instruction || coWriting) return;
    onCoWrite("rewrite_selection", instruction, null, currentControls());
    setCustomInstruction("");
  }

  function handleCustomKeyDown(event) {
    if (event.key !== "Enter" || event.shiftKey || event.nativeEvent.isComposing) return;
    event.preventDefault();
    handleCustomSubmit();
  }

  const primaryActions = CO_WRITE_ACTIONS.filter((action) =>
    ["rewrite_selection", "add_evidence", "add_original_evidence", "improve_expression"].includes(action.key)
  );
  const advancedActions = CO_WRITE_ACTIONS.filter((action) =>
    !["rewrite_selection", "add_evidence", "add_original_evidence", "improve_expression"].includes(action.key)
  );

  return (
    <WorkspacePanel title="AI 共写" subtitle="先生成预览，确认后再应用为新版本" compact>
      <div className="panel-body">
        <div className={`ai-scope-card ${hasValidRange(selectedRange) ? "ai-scope-card--selected" : ""}`}>
          <span className="ai-feedback-label">作用范围</span>
          <p>{describeSelection(selectedRange)}</p>
        </div>

        <div className="rewrite-depth-control" role="group" aria-label="修改力度">
          <span className="ai-feedback-label">修改力度</span>
          <div className="segmented-control">
            <button
              type="button"
              className={rewriteDepth === "light" ? "active" : ""}
              onClick={() => setRewriteDepth("light")}
            >
              保守
            </button>
            <button
              type="button"
              className={rewriteDepth === "balanced" ? "active" : ""}
              onClick={() => setRewriteDepth("balanced")}
            >
              标准
            </button>
            <button
              type="button"
              className={rewriteDepth === "deep" ? "active" : ""}
              onClick={() => setRewriteDepth("deep")}
            >
              充分
            </button>
          </div>
        </div>

        <div className="guardrail-control">
          <span className="ai-feedback-label">保留约束</span>
          <div className="guardrail-grid">
            {[
              ["keepCitations", "保留引用"],
              ["keepData", "不改数据"],
              ["noNewSources", "不新增文献"],
              ["keepStudentVoice", "保留学生表达"]
            ].map(([key, label]) => (
              <label key={key} className="guardrail-chip">
                <input
                  type="checkbox"
                  checked={guardrails[key]}
                  onChange={(event) => {
                    setGuardrails((current) => ({ ...current, [key]: event.target.checked }));
                  }}
                />
                <span>{label}</span>
              </label>
            ))}
          </div>
        </div>

        <div className="ai-custom-block ai-custom-block--primary">
          <label className="field-label" htmlFor="ai-custom-instruction">
            你想怎么改？
          </label>
          <textarea
            id="ai-custom-instruction"
            value={customInstruction}
            onChange={(event) => setCustomInstruction(event.target.value)}
            onKeyDown={handleCustomKeyDown}
            placeholder="例如：让这一段更像本科生自然表达，同时保留引用和数据。"
            rows={4}
          />
          <button
            type="button"
            className="primary-btn ai-submit-btn"
            disabled={coWriting || !customInstruction.trim()}
            onClick={handleCustomSubmit}
          >
            {coWriting ? "生成预览中…" : "生成修改预览"}
          </button>
          <p className="muted ai-enter-hint">按 Enter 可直接生成预览；Shift + Enter 换行。</p>
        </div>

        <div className="quick-action-block">
          <div className="quick-action-head">
            <span className="ai-feedback-label">常用动作</span>
            <button
              type="button"
              className="ghost-btn ghost-btn--compact"
              onClick={() => setShowAdvancedActions((current) => !current)}
            >
              {showAdvancedActions ? "收起更多" : "更多动作"}
            </button>
          </div>
          <div className="action-grid action-grid--compact">
            {primaryActions.map((action) => (
            <button
              key={action.key}
              type="button"
              className="action-card"
              disabled={coWriting}
              onClick={() => onCoWrite(action.key, action.instruction, null, currentControls())}
            >
              <strong>{action.label}</strong>
              <span>{action.hint}</span>
            </button>
          ))}
          </div>
          {showAdvancedActions && (
            <div className="action-grid action-grid--advanced">
              {advancedActions.map((action) => (
                <button
                  key={action.key}
                  type="button"
                  className="action-card"
                  disabled={coWriting}
                  onClick={() => onCoWrite(action.key, action.instruction, null, currentControls())}
                >
                  <strong>{action.label}</strong>
                  <span>{action.hint}</span>
                </button>
              ))}
            </div>
          )}
        </div>

        <div className={`ai-feedback ${coWriting ? "ai-feedback--loading" : ""}`}>
          <span className="ai-feedback-label">最近结果</span>
          <p>{coWriting ? "AI 正在生成修改预览，请稍候…" : latestFeedback || "生成预览后，你可以先看差异，再决定是否应用为新版本。"}</p>
        </div>
      </div>
    </WorkspacePanel>
  );
}
