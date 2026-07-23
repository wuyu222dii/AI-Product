export const GUIDE_PROGRESS_OPTIONS = [
  { value: "IDEA_ONLY", label: "只有初步想法", description: "还在明确研究方向和问题" },
  { value: "TOPIC_DEFINED", label: "题目或问题已定", description: "准备补齐要求和研究依据" },
  { value: "MATERIALS_COLLECTING", label: "正在搜集材料", description: "已经开始整理文献、数据或记录" },
  { value: "WRITING", label: "已经开始写作", description: "正在形成章节正文" },
  { value: "REVISING", label: "正在修改与交付", description: "重点处理证据、审查与格式" }
];

export const GUIDE_MATERIAL_OPTIONS = [
  { value: "REQUIREMENTS", label: "写作或提交要求" },
  { value: "REFERENCES", label: "参考文献" },
  { value: "DATA_RESULTS", label: "数据或研究结果" },
  { value: "NOTES_DRAFT", label: "笔记或已有草稿" },
  { value: "NONE", label: "暂无材料" }
];

export const GUIDE_MODE_OPTIONS = [
  { value: "GUIDED", label: "引导模式", description: "突出当前建议任务，适合第一次使用" },
  { value: "FLEXIBLE", label: "自由模式", description: "保留路线提示，但由你自行安排顺序" }
];

export const GUIDE_STATUS_LABELS = {
  COMPLETED: "已完成",
  CURRENT: "建议现在处理",
  IN_PROGRESS: "进行中",
  UPCOMING: "后续任务",
  OPTIONAL: "推荐步骤",
  NEEDS_ATTENTION: "需要处理"
};
