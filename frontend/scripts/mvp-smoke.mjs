import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "..");
const checks = [
  {
    file: "src/App.jsx",
    tokens: ["/projects", "/upload", "/parsing", "/gate", "/knowledge-base", "/documents", "/workspace", "/export"]
  },
  {
    file: "src/services/api.js",
    tokens: [
      "uploadMaterials",
      "aiParseMaterial",
      "checkMaterialSufficiency",
      "generateDraft",
      "previewCoWrite",
      "applyCoWritePreview",
      "previewMaterial",
      "getEvidenceBindings",
      "listAcademicDocuments",
      "reorderDocumentSections",
      "checkDocumentReadiness",
      "checkSectionReadiness",
      "previewSectionCoWrite",
      "getSectionEvidenceBindings",
      "getDocumentQualitySummary",
      "listDocumentReviewItems",
      "refreshSectionReviewItems",
      "assembleAcademicDocument",
      "listReviewItems",
      "createAppeal",
      "exportDraft",
      "getJob"
    ]
  },
  {
    file: "src/pages/AcademicDocumentsPage.jsx",
    tokens: ["Academic project workspace", "AcademicDocumentSwitcher", "章节写作", "整篇检查", "AcademicChecksDrawer", "AcademicDocumentQualityView", "项目画像"]
  },
  {
    file: "src/components/academic/AcademicSectionEditor.jsx",
    tokens: ["基于材料生成本章", "共写预览", "AI 审查本章", "保留作者声音"]
  },
  {
    file: "src/components/academic/AcademicCoWritePreviewDrawer.jsx",
    tokens: ["章节修改预览", "逐段接受", "局部接受", "应用整章为新版本"]
  },
  {
    file: "src/components/academic/AcademicDocumentQualityView.jsx",
    tokens: ["整篇视图只用于检查与交付", "刷新全部可信链", "AI 审查整篇", "导出文档"]
  },
  {
    file: "src/components/academic/AcademicSectionNavigator.jsx",
    tokens: ["DndContext", "MouseSensor", "TouchSensor", "KeyboardSensor", "拖动调整章节顺序"]
  },
  {
    file: "src/pages/ExportPage.jsx",
    tokens: ["导出定稿", "下载导出文件", "交付确认", "参考文献草案", "可信链覆盖率", "引用一致性"]
  },
  {
    file: "src/components/workspace/WorkspaceCoWritePreviewDrawer.jsx",
    tokens: ["AI 修改预览", "冲突提示", "逐段接受", "应用选中段落"]
  },
  {
    file: "src/components/workspace/WorkspaceEditorPanel.jsx",
    tokens: ["材料可信链", "可信链覆盖率", "引用一致性", "打开原始材料"]
  },
  {
    file: "src/pages/ParsingStatusPage.jsx",
    tokens: ["解析质量检查", "解析质量清单", "填入补充说明", "NEEDS_SUPPLEMENT"]
  },
  {
    file: "dist/index.html",
    tokens: ["type=\"module\"", "/assets/"]
  }
];

const failures = [];

for (const check of checks) {
  const path = resolve(root, check.file);
  if (!existsSync(path)) {
    failures.push(`${check.file} 不存在`);
    continue;
  }
  const content = readFileSync(path, "utf8");
  for (const token of check.tokens) {
    if (!content.includes(token)) {
      failures.push(`${check.file} 缺少关键标记：${token}`);
    }
  }
}

if (failures.length > 0) {
  console.error("MVP smoke check failed:");
  for (const failure of failures) {
    console.error(`- ${failure}`);
  }
  process.exit(1);
}

console.log("MVP smoke check passed: 主路由、核心 API、导出交付页和构建产物均可识别。");
