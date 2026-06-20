import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "..");
const checks = [
  {
    file: "src/App.jsx",
    tokens: ["/projects", "/upload", "/parsing", "/gate", "/knowledge-base", "/workspace", "/export"]
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
      "listReviewItems",
      "createAppeal",
      "exportDraft",
      "getJob"
    ]
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
