import { expect, test } from "@playwright/test";

const NOW = "2026-07-11T05:00:00Z";

test("创建学术画像、上传材料并完成章节级生成与共写", async ({ page }) => {
  const state = createState();
  await installAuthenticatedSession(page);
  await installApiMock(page, state);

  await page.goto("/app/projects");
  await page.getByRole("button", { name: "新建研究项目" }).click();
  await fieldControl(page, "研究项目名称", "input").fill("硕士研究项目 E2E");
  await fieldControl(page, "学术阶段", "select").selectOption("MASTER");
  await fieldControl(page, "学科方向", "select").selectOption("STEM");
  await fieldControl(page, "研究范式", "select").selectOption("QUANTITATIVE");
  await fieldControl(page, "学校 / 机构（可选）", "input").fill("示例大学");
  await page.getByRole("button", { name: "创建并进入材料准备" }).click();

  await expect(page).toHaveURL(/\/app\/projects\/workspace-e2e\/upload$/);
  await page.locator(".upload-source-card").filter({ hasText: "粘贴文字" }).locator("textarea").fill("研究样本为 120 名学生，包含问卷数据、访谈摘要和能源消耗记录。");
  await page.getByRole("button", { name: "提交并进入解析" }).click();
  await expect(page).toHaveURL(/\/parsing$/);

  await page.getByRole("link", { name: "学术文档" }).click();
  await expect(page).toHaveURL(/\/documents(?:\/[^/]+)?$/);
  await expect(page.getByRole("heading", { name: "学术文档工作台" })).toBeVisible();
  await expect(page.getByText(/硕士研究项目 E2E · 项目材料与知识库共享/)).toBeVisible();
  await expect(page.getByRole("tab", { name: /硕士研究项目 E2E/ })).toBeVisible();

  await page.getByRole("button", { name: "新建文档" }).click();
  await fieldControl(page, "文档标题", "input").fill("研究计划补充稿");
  await fieldControl(page, "文档类型", "select").selectOption("RESEARCH_PROPOSAL");
  await page.getByRole("button", { name: "创建文档与章节树" }).click();
  await expect(page.getByRole("tab", { name: /研究计划补充稿/ })).toHaveAttribute("aria-selected", "true");

  await page.getByRole("button", { name: "AI 助手" }).click();
  await page.getByRole("button", { name: "生成章节草稿" }).click();
  const editor = page.locator(".academic-writing-area");
  await expect(editor).toHaveValue(/基于已上传的真实材料/);

  await page.getByPlaceholder("描述你希望如何修改当前章节").fill("强化研究问题与样本数据之间的对应关系");
  await page.getByRole("button", { name: "生成修改预览" }).click();
  await expect(page.getByRole("heading", { name: "章节修改预览" })).toBeVisible();
  await expect(page.getByText("AI 候选", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "应用整章为新版本" }).click();
  await expect(editor).toHaveValue(/明确对应 120 名学生样本/);
  await page.getByRole("button", { name: "AI 助手" }).click();
  await page.evaluate(() => window.scrollTo(0, 0));
  await page.screenshot({ path: "public/assets/workspace-product.png", animations: "disabled" });

  await page.getByRole("tab", { name: "整篇检查" }).click();
  await page.getByRole("button", { name: "组装预览" }).click();
  await expect(page.getByText(/组装预览 ·/)).toBeVisible();
  await page.getByRole("button", { name: "导出文档" }).click();
  await expect(page.getByRole("button", { name: "下载导出文件" })).toBeVisible();

  await page.getByRole("tab", { name: "章节写作" }).click();
  await page.locator("summary").filter({ hasText: "AI 使用记录" }).click();
  await expect(page.getByText("章节生成", { exact: true })).toBeVisible();
  await expect(page.getByText("章节共写预览", { exact: true })).toBeVisible();

  await page.getByRole("tab", { name: /硕士研究项目 E2E/ }).click();
  await expect(page.getByRole("tab", { name: /硕士研究项目 E2E/ })).toHaveAttribute("aria-selected", "true");
  const introductionHandle = page.getByRole("button", { name: "拖动章节 绪论" });
  const methodologyHandle = page.getByRole("button", { name: "拖动章节 研究方法" });
  await introductionHandle.scrollIntoViewIfNeeded();
  await expect(introductionHandle).toBeVisible();
  await expect(methodologyHandle).toBeVisible();
  const sourceBox = await introductionHandle.boundingBox();
  const targetBox = await methodologyHandle.boundingBox();
  const reorderResponse = page.waitForResponse((response) =>
    response.request().method() === "PATCH"
      && response.url().includes(`/documents/document-primary/sections/order`)
  );
  await page.mouse.move(sourceBox.x + sourceBox.width / 2, sourceBox.y + sourceBox.height / 2);
  await page.mouse.down();
  await page.waitForTimeout(100);
  await page.mouse.move(targetBox.x + targetBox.width / 2, targetBox.y + targetBox.height * 0.8, { steps: 18 });
  await page.waitForTimeout(100);
  await page.mouse.up();
  await reorderResponse;
  await expect.poll(async () => page.locator(".academic-section-item strong").allTextContents())
    .toEqual(["文献综述", "研究方法", "绪论"]);
});

test("兼容工作台可复查审查项并完成导出", async ({ page }) => {
  const state = createState();
  state.workspace = createWorkspaceFixture();
  state.profile = state.workspace.academicProfile;
  state.workspaces = [state.workspace];
  state.materials = [createMaterialFixture()];
  state.reviews = [createReviewFixture()];
  state.draft = createDraftFixture();

  await installAuthenticatedSession(page);
  await installApiMock(page, state);

  await page.goto("/app/projects/workspace-legacy/legacy-workspace/draft-legacy");
  await expect(page.getByText("该段论证缺少样本数据支撑。", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "审查列表" }).click();
  await expect(page.getByRole("heading", { name: "审查详情" })).toBeVisible();
  await page.getByRole("button", { name: "复查此项" }).click();
  await expect(page.getByText("结论：复查通过，当前段落已有真实数据与来源支撑。", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "关闭" }).click();

  await page.getByRole("button", { name: "导出定稿" }).click();
  await expect(page).toHaveURL(/\/legacy-export\/draft-legacy$/);
  await page.getByRole("button", { name: "发起导出" }).click();
  await expect(page.getByText("导出成功")).toBeVisible();
  await expect(page.getByRole("button", { name: "下载导出文件" })).toBeVisible();
});

test("匿名用户被送回登录页并可请求六位邮箱验证码", async ({ page }) => {
  await page.route("**/auth/v1/otp", (route) => route.fulfill({ status: 200, contentType: "application/json", body: "{}" }));
  await page.goto("/app/projects");
  await expect(page).toHaveURL(/\/sign-in\?returnTo=/);
  await page.getByLabel("邮箱地址").fill("student@example.edu");
  await page.getByRole("button", { name: "发送 6 位验证码" }).click();
  await expect(page.getByLabel("6 位验证码")).toBeVisible();
});

function createState() {
  return {
    workspace: null,
    workspaces: [],
    profile: null,
    documents: [],
    sections: new Map(),
    versions: new Map(),
    materials: [],
    materialLinks: new Map(),
    aiActions: new Map(),
    previews: new Map(),
    reviews: [],
    draft: null,
    nextDocument: 1
  };
}

async function installApiMock(page, state) {
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const method = request.method();
    const path = new URL(request.url()).pathname.replace(/^\/api\/v1/, "");
    const body = readJsonBody(request);

    expect(request.headers().authorization).toBe("Bearer e2e-access-token");

    if (path === "/me" && method === "GET") {
      return reply(route, {
        id: "11111111-1111-1111-1111-111111111111",
        email: "student@example.edu",
        displayName: "E2E 研究者",
        avatarUrl: null,
        createdAt: NOW,
        updatedAt: NOW
      });
    }

    if (path === "/workspaces" && method === "GET") {
      return reply(route, { items: state.workspaces });
    }
    if (path === "/workspaces" && method === "POST") {
      state.profile = {
        workspaceId: "workspace-e2e",
        ...body.academicProfile,
        createdAt: NOW,
        updatedAt: NOW
      };
      const document = createDocumentFixture("document-primary", body.initialDocument, true);
      state.documents = [document];
      const initialSections = [
        createSectionFixture("section-primary", document.id, "绪论", 1, "INTRODUCTION"),
        createSectionFixture("section-literature", document.id, "文献综述", 2, "LITERATURE_REVIEW"),
        createSectionFixture("section-methodology", document.id, "研究方法", 3, "METHODOLOGY")
      ];
      document.sectionCount = initialSections.length;
      state.sections.set(document.id, initialSections);
      initialSections.forEach((section) => {
        state.versions.set(section.id, [createVersionFixture(`version-${section.id}-1`, section.id, 1, "")]);
      });
      state.aiActions.set(document.id, []);
      state.workspace = {
        id: "workspace-e2e",
        title: body.title,
        status: "DRAFT",
        currentDraftVersionId: null,
        activeDocumentId: document.id,
        academicProfile: state.profile,
        createdAt: NOW,
        updatedAt: NOW
      };
      state.workspaces = [state.workspace];
      return reply(route, state.workspace);
    }
    const workspaceMatch = path.match(/^\/workspaces\/([^/]+)$/);
    if (workspaceMatch && method === "GET") return reply(route, state.workspace);

    const workspaceProfileMatch = path.match(/^\/workspaces\/([^/]+)\/academic-profile$/);
    if (workspaceProfileMatch && method === "GET") return reply(route, state.profile);
    if (workspaceProfileMatch && method === "PATCH") {
      state.profile = { ...state.profile, ...body, updatedAt: NOW };
      state.workspace = { ...state.workspace, academicProfile: state.profile };
      return reply(route, state.profile);
    }

    const workspaceMaterialsMatch = path.match(/^\/workspaces\/([^/]+)\/materials$/);
    if (workspaceMaterialsMatch && method === "GET") return reply(route, { items: state.materials });
    if (workspaceMaterialsMatch && method === "POST") {
      const material = createMaterialFixture(`material-${state.materials.length + 1}`);
      state.materials.push(material);
      return reply(route, { items: [material] });
    }

    const workspaceDocumentsMatch = path.match(/^\/workspaces\/([^/]+)\/documents$/);
    if (workspaceDocumentsMatch && method === "GET") return reply(route, state.documents);
    if (workspaceDocumentsMatch && method === "POST") {
      const id = `document-${state.nextDocument++}`;
      const document = createDocumentFixture(id, body, false);
      const section = createSectionFixture(`section-${id}`, id, "研究背景");
      state.documents = [document, ...state.documents];
      state.sections.set(id, [section]);
      state.versions.set(section.id, [createVersionFixture(`version-${section.id}-1`, section.id, 1, "")]);
      state.aiActions.set(id, []);
      return reply(route, document);
    }

    const activateMatch = path.match(/^\/documents\/([^/]+)\/activate$/);
    if (activateMatch && method === "POST") {
      const document = findDocument(state, activateMatch[1]);
      state.workspace.activeDocumentId = document.id;
      return reply(route, document);
    }
    const documentMatch = path.match(/^\/documents\/([^/]+)$/);
    if (documentMatch && method === "GET") return reply(route, findDocument(state, documentMatch[1]));
    if (documentMatch && method === "PATCH") {
      const document = findDocument(state, documentMatch[1]);
      Object.assign(document, body, { updatedAt: NOW });
      return reply(route, document);
    }

    const documentSectionsMatch = path.match(/^\/documents\/([^/]+)\/sections$/);
    if (documentSectionsMatch && method === "GET") return reply(route, state.sections.get(documentSectionsMatch[1]) ?? []);
    if (documentSectionsMatch && method === "POST") {
      const section = createSectionFixture(`section-custom-${Date.now()}`, documentSectionsMatch[1], body.title);
      section.sectionType = body.sectionType;
      section.targetLength = body.targetLength;
      const sections = state.sections.get(documentSectionsMatch[1]) ?? [];
      sections.push(section);
      state.sections.set(documentSectionsMatch[1], sections);
      state.versions.set(section.id, [createVersionFixture(`version-${section.id}-1`, section.id, 1, "")]);
      updateSectionCount(state, documentSectionsMatch[1]);
      return reply(route, section);
    }
    const reorderSectionsMatch = path.match(/^\/documents\/([^/]+)\/sections\/order$/);
    if (reorderSectionsMatch && method === "PATCH") {
      const currentSections = state.sections.get(reorderSectionsMatch[1]) ?? [];
      const sectionById = new Map(currentSections.map((section) => [section.id, section]));
      const reordered = body.sectionIds.map((id, index) => ({
        ...sectionById.get(id),
        sortOrder: index + 1,
        updatedAt: NOW
      }));
      state.sections.set(reorderSectionsMatch[1], reordered);
      return reply(route, reordered);
    }

    const documentReadinessMatch = path.match(/^\/documents\/([^/]+)\/readiness-check$/);
    if (documentReadinessMatch && method === "POST") return reply(route, readinessFixture());
    const documentLinksMatch = path.match(/^\/documents\/([^/]+)\/materials$/);
    if (documentLinksMatch && method === "GET") return reply(route, state.materialLinks.get(documentLinksMatch[1]) ?? []);
    if (documentLinksMatch && method === "POST") {
      const links = state.materialLinks.get(documentLinksMatch[1]) ?? [];
      const next = {
        id: `link-${body.materialId}`,
        documentId: documentLinksMatch[1],
        materialId: body.materialId,
        filename: state.materials.find((item) => item.id === body.materialId)?.filename ?? "材料",
        role: body.role,
        included: body.included,
        createdAt: NOW,
        updatedAt: NOW
      };
      state.materialLinks.set(documentLinksMatch[1], [...links.filter((item) => item.materialId !== body.materialId), next]);
      return reply(route, next);
    }
    const documentActionsMatch = path.match(/^\/documents\/([^/]+)\/ai-actions$/);
    if (documentActionsMatch && method === "GET") return reply(route, state.aiActions.get(documentActionsMatch[1]) ?? []);
    const documentReviewsMatch = path.match(/^\/documents\/([^/]+)\/review-items$/);
    if (documentReviewsMatch && method === "GET") {
      const sectionId = new URL(request.url()).searchParams.get("sectionId");
      return reply(route, sectionId ? state.reviews.filter((item) => item.sectionId === sectionId) : state.reviews);
    }
    const documentQualityMatch = path.match(/^\/documents\/([^/]+)\/quality-summary$/);
    if (documentQualityMatch && method === "GET") return reply(route, documentQualityFixture(state, documentQualityMatch[1]));
    const documentEvidenceMatch = path.match(/^\/documents\/([^/]+)\/evidence-summary$/);
    if (documentEvidenceMatch && method === "GET") return reply(route, documentEvidenceFixture(documentEvidenceMatch[1]));
    const assembleMatch = path.match(/^\/documents\/([^/]+)\/assemble$/);
    if (assembleMatch && method === "POST") {
      const content = (state.sections.get(assembleMatch[1]) ?? [])
        .map((section) => `# ${section.title}\n\n${section.content}`)
        .join("\n\n");
      return reply(route, {
        documentId: assembleMatch[1],
        title: findDocument(state, assembleMatch[1]).title,
        content,
        characterCount: content.length,
        sectionIds: (state.sections.get(assembleMatch[1]) ?? []).map((item) => item.id),
        sourceTraceMap: {}
      });
    }
    const academicExportMatch = path.match(/^\/documents\/([^/]+)\/export$/);
    if (academicExportMatch && method === "POST") return reply(route, { jobId: "job-academic-export", status: "PENDING" });

    const sectionMatch = path.match(/^\/sections\/([^/]+)$/);
    if (sectionMatch && method === "PATCH") {
      const section = findSection(state, sectionMatch[1]);
      Object.assign(section, body, {
        status: body.content?.trim() ? "DRAFTING" : section.status,
        versionNo: section.versionNo + 1,
        updatedAt: NOW
      });
      addVersion(state, section, body.changeSummary ?? "手动编辑章节");
      return reply(route, section);
    }
    const sectionVersionsMatch = path.match(/^\/sections\/([^/]+)\/versions$/);
    if (sectionVersionsMatch && method === "GET") return reply(route, state.versions.get(sectionVersionsMatch[1]) ?? []);
    const sectionReadinessMatch = path.match(/^\/sections\/([^/]+)\/readiness-check$/);
    if (sectionReadinessMatch && method === "POST") return reply(route, readinessFixture());
    const sectionEvidenceMatch = path.match(/^\/sections\/([^/]+)\/evidence-bindings$/);
    if (sectionEvidenceMatch && method === "GET") return reply(route, sectionEvidenceFixture(sectionEvidenceMatch[1]));
    const sectionRisksMatch = path.match(/^\/sections\/([^/]+)\/writing-risks$/);
    if (sectionRisksMatch && method === "GET") return reply(route, sectionRiskFixture(sectionRisksMatch[1]));
    const sectionGenerateMatch = path.match(/^\/sections\/([^/]+)\/generate$/);
    if (sectionGenerateMatch && method === "POST") {
      const section = findSection(state, sectionGenerateMatch[1]);
      section.content = "本章基于已上传的真实材料展开，研究样本包含 120 名学生。";
      section.status = "DRAFTING";
      section.versionNo += 1;
      section.updatedAt = NOW;
      addVersion(state, section, "AI 生成章节");
      addAiAction(state, section.documentId, "SECTION_GENERATE", "基于 1 份材料生成章节草稿");
      return reply(route, section);
    }
    const sectionPreviewMatch = path.match(/^\/sections\/([^/]+)\/co-write\/preview$/);
    if (sectionPreviewMatch && method === "POST") {
      const section = findSection(state, sectionPreviewMatch[1]);
      const preview = {
        id: `preview-${section.id}`,
        sectionId: section.id,
        baseVersionNo: section.versionNo,
        action: body.action,
        instruction: body.instruction,
        baseContent: section.content,
        candidateContent: `${section.content} 研究问题与能源记录明确对应 120 名学生样本。`,
        candidateSourceTraceMap: { materials: state.materials.map((item) => item.id) },
        targetRange: body.targetRange,
        controls: body.controls,
        diffSummary: { characterDelta: 27, summary: "补充样本对应关系", conflictWarnings: [] },
        paragraphDiffRows: [{ id: "p1", changed: true, intentLabel: "补强证据", originalText: section.content, candidateText: `${section.content} 研究问题与能源记录明确对应 120 名学生样本。` }],
        diffRows: [{ id: "d1", changed: true, originalText: section.content, candidateText: `${section.content} 研究问题与能源记录明确对应 120 名学生样本。` }],
        relatedReviewItemIds: [],
        status: "PREVIEW",
        createdAt: NOW,
        updatedAt: NOW
      };
      state.previews.set(preview.id, preview);
      addAiAction(state, section.documentId, "SECTION_COWRITE_PREVIEW", "生成章节共写预览，等待用户确认");
      return reply(route, preview);
    }
    const applyPreviewMatch = path.match(/^\/section-co-write-previews\/([^/]+)\/apply$/);
    if (applyPreviewMatch && method === "POST") {
      const preview = state.previews.get(applyPreviewMatch[1]);
      const section = findSection(state, preview.sectionId);
      section.content = preview.candidateContent;
      section.versionNo += 1;
      section.updatedAt = NOW;
      addVersion(state, section, "应用 AI 共写预览");
      preview.status = "APPLIED";
      return reply(route, section);
    }
    const discardPreviewMatch = path.match(/^\/section-co-write-previews\/([^/]+)\/discard$/);
    if (discardPreviewMatch && method === "POST") {
      const preview = state.previews.get(discardPreviewMatch[1]);
      if (preview) preview.status = "DISCARDED";
      return reply(route, preview ?? {});
    }

    const workspaceDraftsMatch = path.match(/^\/workspaces\/([^/]+)\/drafts$/);
    if (workspaceDraftsMatch && method === "GET") return reply(route, { items: state.draft ? [state.draft] : [] });
    const requirementMatch = path.match(/^\/workspaces\/([^/]+)\/requirement-snapshot$/);
    if (requirementMatch && method === "GET") return reply(route, { citationStyle: "APA" });
    const reviewItemsMatch = path.match(/^\/drafts\/([^/]+)\/review-items$/);
    if (reviewItemsMatch && method === "GET") return reply(route, { items: state.reviews });
    const recheckMatch = path.match(/^\/review-items\/([^/]+)\/recheck$/);
    if (recheckMatch && method === "POST") {
      const review = state.reviews.find((item) => item.id === recheckMatch[1]);
      Object.assign(review, {
        reviewStatus: "RESOLVED",
        lastRecheckedAt: NOW,
        recheckNote: "复查通过，当前段落已有真实数据与来源支撑。",
        recheckHistory: [{ id: "recheck-1", outcome: "RESOLVED", previousStatus: "OPEN", nextStatus: "RESOLVED", note: "证据已补充", createdAt: NOW }]
      });
      return reply(route, review);
    }
    const evidenceMatch = path.match(/^\/drafts\/([^/]+)\/evidence-bindings$/);
    if (evidenceMatch && method === "GET") {
      return reply(route, {
        paragraphs: [],
        missingParagraphIds: [],
        usedMaterials: state.materials,
        unusedMaterials: [],
        coverage: { coverageRate: 1, confirmedRate: 1 },
        citationConsistency: { status: "CONSISTENT", issues: [] }
      });
    }
    const writingRisksMatch = path.match(/^\/drafts\/([^/]+)\/writing-risks$/);
    if (writingRisksMatch && method === "GET") return reply(route, { overallStatus: "READY", overallScore: 92, items: [], recommendations: [] });
    const legacyExportMatch = path.match(/^\/drafts\/([^/]+)\/export$/);
    if (legacyExportMatch && method === "POST") return reply(route, { jobId: "job-legacy-export", status: "PENDING" });
    const draftMatch = path.match(/^\/drafts\/([^/]+)$/);
    if (draftMatch && method === "GET") return reply(route, state.draft);

    const jobMatch = path.match(/^\/jobs\/([^/]+)$/);
    if (jobMatch && method === "GET") {
      return reply(route, {
        jobId: jobMatch[1],
        status: "SUCCESS",
        outputRef: {
          downloadUrl: "/downloads/e2e-paper.docx",
          fileName: "e2e-paper.docx",
          format: "docx"
        }
      });
    }

    return reply(route, {});
  });
}

async function installAuthenticatedSession(page) {
  await page.addInitScript(({ key, session }) => {
    window.localStorage.setItem(key, JSON.stringify(session));
  }, {
    key: "sb-lrxkcxhftjnkpolezuvh-auth-token",
    session: {
      access_token: "e2e-access-token",
      refresh_token: "e2e-refresh-token",
      expires_in: 3600,
      expires_at: Math.floor(Date.now() / 1000) + 3600,
      token_type: "bearer",
      user: {
        id: "11111111-1111-1111-1111-111111111111",
        aud: "authenticated",
        role: "authenticated",
        email: "student@example.edu",
        user_metadata: { full_name: "E2E 研究者" }
      }
    }
  });
}

function readJsonBody(request) {
  try {
    return request.postDataJSON() ?? {};
  } catch {
    return {};
  }
}

function reply(route, data) {
  return route.fulfill({
    status: 200,
    contentType: "application/json",
    body: JSON.stringify({ success: true, data })
  });
}

function createWorkspaceFixture() {
  const profile = {
    workspaceId: "workspace-legacy",
    academicStage: "MASTER",
    disciplineGroup: "STEM",
    researchParadigm: "QUANTITATIVE",
    primaryLanguage: "zh-CN",
    defaultCitationStyle: "APA",
    institution: "示例大学",
    aiUsagePolicy: "EVIDENCE_GROUNDED_DRAFTING",
    aiPolicy: { humanReviewRequired: true, disclosureRequired: true },
    createdAt: NOW,
    updatedAt: NOW
  };
  return {
    id: "workspace-legacy",
    title: "证据驱动研究项目",
    status: "READY",
    currentDraftVersionId: "draft-legacy",
    activeDocumentId: null,
    academicProfile: profile,
    createdAt: NOW,
    updatedAt: NOW
  };
}

function createDocumentFixture(id, input, primaryDocument) {
  return {
    id,
    workspaceId: "workspace-e2e",
    documentType: input.documentType,
    title: input.title,
    status: "PLANNING",
    targetInstitution: input.targetInstitution ?? null,
    targetVenue: input.targetVenue ?? null,
    targetLength: Number(input.targetLength),
    lengthUnit: input.lengthUnit ?? "WORDS",
    citationStyle: input.citationStyle ?? "APA",
    requirementProfile: input.requirementProfile ?? {},
    primaryDocument,
    sectionCount: 1,
    createdAt: NOW,
    updatedAt: NOW
  };
}

function createSectionFixture(id, documentId, title, sortOrder = 1, sectionType = "INTRODUCTION") {
  return {
    id,
    documentId,
    parentSectionId: null,
    sortOrder,
    sectionType,
    title,
    content: "",
    targetLength: 1200,
    status: "EMPTY",
    sourceTraceMap: {},
    versionNo: 1,
    createdAt: NOW,
    updatedAt: NOW
  };
}

function createVersionFixture(id, sectionId, versionNo, content) {
  return {
    id,
    sectionId,
    versionNo,
    title: "章节",
    content,
    sourceTraceMap: {},
    changeSource: "USER",
    changeSummary: "创建章节",
    createdAt: NOW
  };
}

function createMaterialFixture(id = "material-e2e") {
  return {
    id,
    workspaceId: "workspace-e2e",
    filename: "真实研究数据.md",
    sourceType: "pasted_text",
    isKeyMaterial: true,
    parseStage: "AI_PARSED",
    effectiveMaterialCategory: "RESEARCH_RESULT",
    materialRole: "RESEARCH_EVIDENCE",
    researchArtifactType: "DATASET",
    bibliographicMetadata: {
      authors: ["示例作者"],
      year: "2026",
      title: "真实研究数据",
      sourceTitle: "项目材料",
      url: "https://example.test/material"
    },
    createdAt: NOW,
    updatedAt: NOW
  };
}

function createDraftFixture() {
  return {
    id: "draft-legacy",
    workspaceId: "workspace-legacy",
    versionNo: 2,
    titleSuggestion: "证据驱动研究论文",
    draftText: "本研究基于 120 名学生样本分析智能教室能源使用情况。",
    sourceTraceMap: {},
    generationStatus: "SUCCESS",
    createdAt: NOW,
    updatedAt: NOW
  };
}

function createReviewFixture() {
  return {
    id: "review-1",
    draftVersionId: "draft-legacy",
    reviewType: "generic_unsupported_claim",
    reviewImpactLevel: "must_confirm",
    message: "该段论证缺少样本数据支撑。",
    targetRange: { start: 0, end: 18, selectedText: "本研究基于 120 名学生样本" },
    suggestedFix: "补充样本来源和数据采集方法。",
    canBypass: true,
    reviewStatus: "OPEN",
    recheckHistory: [],
    createdAt: NOW,
    updatedAt: NOW
  };
}

function readinessFixture() {
  return {
    status: "READY",
    score: 92,
    generationEligible: true,
    nextAction: "当前文档材料可支持章节写作。",
    artifactCoverage: {
      parsedKeyMaterial: true,
      literature: true,
      submissionRequirement: true,
      researchArtifact: true,
      authorDraft: true
    },
    issues: []
  };
}

function sectionEvidenceFixture(sectionId) {
  return {
    scopeType: "SECTION",
    documentId: "document-primary",
    sectionId,
    sectionVersionNo: 1,
    analysisState: "CURRENT",
    paragraphs: [],
    missingParagraphIds: [],
    usedMaterials: [],
    unusedMaterials: [],
    coverage: { totalParagraphs: 1, supportedParagraphs: 1, coverageRatio: 100 },
    citationConsistency: { status: "CONSISTENT", issues: [] }
  };
}

function documentEvidenceFixture(documentId) {
  return {
    documentId,
    sections: [],
    coverage: { totalParagraphs: 3, supportedParagraphs: 3, coverageRatio: 100 },
    citationConsistency: { status: "CONSISTENT", issues: [] },
    analysisState: "CURRENT"
  };
}

function sectionRiskFixture(sectionId) {
  return {
    scopeType: "SECTION",
    sectionId,
    analysisState: "CURRENT",
    overallStatus: "READY",
    overallScore: 94,
    items: [],
    recommendations: []
  };
}

function documentQualityFixture(state, documentId) {
  const sections = state.sections.get(documentId) ?? [];
  return {
    documentId,
    status: "READY",
    score: 93,
    evidence: documentEvidenceFixture(documentId),
    writingRisks: { overallStatus: "READY", overallScore: 94, items: [], recommendations: [] },
    reviewItems: state.reviews.filter((item) => item.documentId === documentId),
    sections: sections.map((section) => ({
      sectionId: section.id,
      title: section.title,
      versionNo: section.versionNo,
      evidenceCoverage: 100,
      writingRiskScore: 94,
      openReviewCount: 0,
      analysisState: "CURRENT"
    })),
    recommendations: ["当前章节材料与来源链已同步，导出前请进行人工通读。"]
  };
}

function findDocument(state, documentId) {
  const document = state.documents.find((item) => item.id === documentId);
  if (!document) throw new Error(`Document not found: ${documentId}`);
  return document;
}

function findSection(state, sectionId) {
  for (const sections of state.sections.values()) {
    const section = sections.find((item) => item.id === sectionId);
    if (section) return section;
  }
  throw new Error(`Section not found: ${sectionId}`);
}

function updateSectionCount(state, documentId) {
  const document = findDocument(state, documentId);
  document.sectionCount = (state.sections.get(documentId) ?? []).length;
}

function addVersion(state, section, changeSummary) {
  const versions = state.versions.get(section.id) ?? [];
  state.versions.set(section.id, [
    {
      ...createVersionFixture(`version-${section.id}-${section.versionNo}`, section.id, section.versionNo, section.content),
      title: section.title,
      changeSource: "AI",
      changeSummary
    },
    ...versions
  ]);
}

function addAiAction(state, documentId, actionType, outputSummary) {
  const items = state.aiActions.get(documentId) ?? [];
  state.aiActions.set(documentId, [
    {
      id: `action-${documentId}-${items.length + 1}`,
      documentId,
      actionType,
      evidenceMaterialIds: state.materials.map((item) => item.id),
      requestSummary: "E2E 模拟请求",
      outputSummary,
      accepted: false,
      disclosureRequired: true,
      createdAt: NOW
    },
    ...items
  ]);
}

function fieldControl(page, label, control) {
  return page.locator(".field").filter({ has: page.getByText(label, { exact: true }) }).locator(control).first();
}
