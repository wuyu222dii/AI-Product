export const CO_WRITE_ACTIONS = [
  { key: "rewrite_selection", label: "重写", hint: "保持原意，改善逻辑或表达", instruction: "请重写选中内容，保留核心观点和证据。" },
  { key: "add_evidence", label: "补证据", hint: "为论点补充来源支撑", instruction: "请为选中论点补充证据解释，不要编造不存在的引用。" },
  { key: "add_original_evidence", label: "补原创实证", hint: "补案例、数据或调研依据", instruction: "请基于已上传材料为选中段落补充原创案例、数据或实证支撑；材料不足时请列出需要我补充的内容，不要编造。" },
  { key: "adjust_structure", label: "调结构", hint: "优化段落层次与承接", instruction: "请优化选中内容的论证顺序、主题句和衔接。" },
  { key: "reduce_repetition", label: "压重复", hint: "压缩重复信息", instruction: "请压缩选中内容中的信息重复，保留必要的小结和回扣。" },
  { key: "improve_expression", label: "改表达", hint: "降低 AI 写作味", instruction: "请让选中内容更像学生自然写作，减少空泛连接词和套话。" },
  { key: "expand_argument", label: "扩写", hint: "补机制、条件或限制", instruction: "请扩展选中论证，补充机制分析、适用条件或必要限制。" },
  { key: "shorten_text", label: "缩写", hint: "保留要点并收束篇幅", instruction: "请缩短选中内容，保留核心观点、证据和逻辑关系。" }
];

export const REVIEW_TYPE_LABELS = {
  missing_evidence: "缺少证据",
  requirement_conflict: "要求冲突",
  repetition_issue: "重复问题",
  logic_gap: "逻辑断点",
  factual_risk: "事实风险",
  citation_missing: "正文缺少引用",
  citation_format_mismatch: "引用格式不一致",
  reference_orphan: "引用来源待确认",
  reference_not_cited: "参考文献未被引用",
  reference_metadata_incomplete: "文献信息不完整",
  aigc_style_risk: "AI 写作味风险",
  generic_unsupported_claim: "空泛论证",
  original_evidence_missing: "原创实证不足",
  unknown: "未知问题"
};

export const APPEAL_REASON_PRESETS = [
  "这条审查与老师要求不一致，请以我上传的要求为准复审。",
  "这里属于结构性重复，用于引言点题或结论回扣，请判断是否可以保留。",
  "该段已有来源支撑，只是表达比较隐含，请结合来源追溯重新判断。",
  "我接受风险，但希望 AI 降级为提示项而不是强制修改。"
];

export const SIDEBAR_TABS = [
  { key: "tasks", label: "推荐任务" },
  { key: "reviews", label: "审查结果" },
  { key: "activity", label: "活动记录" }
];

export const RECOMMENDED_TASKS = [
  {
    id: "sources",
    title: "补充文献来源",
    detail: "优先检查缺少来源支撑的段落，补 1–2 条核心引用。"
  },
  {
    id: "requirements",
    title: "确认老师格式要求",
    detail: "核对 Requirement Snapshot 中的引用格式与字数要求是否已确认。"
  }
];
