export const ACADEMIC_STAGE_OPTIONS = [
  { value: "UNDERGRADUATE", label: "本科阶段" },
  { value: "MASTER", label: "硕士阶段" },
  { value: "DOCTORAL", label: "博士阶段" },
  { value: "RESEARCHER", label: "科研人员" }
];

export const DISCIPLINE_OPTIONS = [
  { value: "STEM", label: "理工与计算" },
  { value: "MEDICINE_HEALTH", label: "医学与健康" },
  { value: "SOCIAL_SCIENCE", label: "社会科学" },
  { value: "HUMANITIES", label: "人文研究" },
  { value: "BUSINESS_LAW", label: "商科与法学" },
  { value: "INTERDISCIPLINARY", label: "交叉学科" }
];

export const PARADIGM_OPTIONS = [
  { value: "QUANTITATIVE", label: "定量研究" },
  { value: "QUALITATIVE", label: "定性研究" },
  { value: "MIXED_METHODS", label: "混合研究" },
  { value: "EXPERIMENTAL", label: "实验研究" },
  { value: "COMPUTATIONAL", label: "计算研究" },
  { value: "DESIGN_SCIENCE", label: "设计科学" },
  { value: "THEORETICAL", label: "理论研究" },
  { value: "SYSTEMATIC_REVIEW", label: "系统综述" },
  { value: "OTHER", label: "其他 / 待确认" }
];

export const DOCUMENT_TYPE_OPTIONS = [
  { value: "COURSE_PAPER", label: "课程论文" },
  { value: "RESEARCH_PROPOSAL", label: "研究计划 / 开题" },
  { value: "UNDERGRADUATE_THESIS", label: "本科毕业论文" },
  { value: "MASTER_THESIS", label: "硕士论文" },
  { value: "DOCTORAL_DISSERTATION", label: "博士论文" },
  { value: "JOURNAL_ARTICLE", label: "期刊论文稿" },
  { value: "CONFERENCE_PAPER", label: "会议论文稿" },
  { value: "LITERATURE_REVIEW", label: "文献综述" },
  { value: "RESEARCH_REPORT", label: "研究报告" }
];

export const AI_POLICY_OPTIONS = [
  { value: "GUIDANCE_ONLY", label: "仅指导与审查" },
  { value: "EVIDENCE_GROUNDED_DRAFTING", label: "基于证据生成与共写" },
  { value: "FULL_DRAFTING_ALLOWED", label: "允许完整生成，必须人工复核" }
];

export const DOCUMENT_TYPE_LABELS = Object.fromEntries(DOCUMENT_TYPE_OPTIONS.map((item) => [item.value, item.label]));
export const STAGE_LABELS = Object.fromEntries(ACADEMIC_STAGE_OPTIONS.map((item) => [item.value, item.label]));
export const PARADIGM_LABELS = Object.fromEntries(PARADIGM_OPTIONS.map((item) => [item.value, item.label]));

export function defaultDocumentForStage(stage) {
  return {
    UNDERGRADUATE: { documentType: "UNDERGRADUATE_THESIS", targetLength: 12000 },
    MASTER: { documentType: "MASTER_THESIS", targetLength: 40000 },
    DOCTORAL: { documentType: "DOCTORAL_DISSERTATION", targetLength: 80000 },
    RESEARCHER: { documentType: "RESEARCH_PROPOSAL", targetLength: 6000 }
  }[stage] ?? { documentType: "COURSE_PAPER", targetLength: 3000 };
}
