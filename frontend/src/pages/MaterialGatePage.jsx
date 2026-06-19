import { useEffect, useState } from "react";
import { api } from "../services/api";

const MISSING_ITEM_LABELS = {
  key_material: "核心材料尚未完成 AI 解析",
  assignment_requirement: "缺少老师要求或作业说明",
  reference_material: "缺少可引用参考资料",
  research_result: "缺少你的研究内容或写作基础"
};

function formatMissingItemLabel(item) {
  return item?.label || MISSING_ITEM_LABELS[item?.type] || "材料信息不完整";
}

export function MaterialGatePage({ workspace, onEligible, onBackUpload, onError }) {
  const [snapshot, setSnapshot] = useState(null);
  const [result, setResult] = useState(null);
  const [checking, setChecking] = useState(false);
  const [draftMode, setDraftMode] = useState("stable");

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

  return (
    <section className="page-card">
      <h3 className="page-section-title">材料充足性检查</h3>
      <p className="section-help">系统先判断当前材料是否足以支撑正文生成。材料不足时不会兜底写作。</p>

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

      <div className="button-row" style={{ marginTop: 16 }}>
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
        <div className="card-block" style={{ marginTop: 18 }}>
          <h4>当前无法生成</h4>
          <p className="section-help">
            系统不会在材料不足时兜底写作。请按下面提示补齐材料，完成 AI 解析后再重新检查。
          </p>
          <div className="list-stack">
            {(result.missingItems ?? []).map((item, index) => (
              <div className="mini-card" key={`${item.type}-${index}`}>
                <strong>{formatMissingItemLabel(item)}</strong>
                <p className="muted">{item.message}</p>
                {item.action && <p className="missing-action">下一步：{item.action}</p>}
              </div>
            ))}
          </div>

          {(result.recommendedSupplements ?? []).length > 0 && (
            <div className="recommended-supplements">
              <h4>建议补充</h4>
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
      )}
    </section>
  );
}
