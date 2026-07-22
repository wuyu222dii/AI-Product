import { expect, test } from "@playwright/test";

const VIEWPORTS = [
  { name: "desktop", width: 1440, height: 900 },
  { name: "tablet", width: 1024, height: 768 },
  { name: "mobile", width: 390, height: 844 }
];

for (const viewport of VIEWPORTS) {
  test(`公开首页在 ${viewport.name} 视口无横向溢出`, async ({ page }) => {
    await page.setViewportSize({ width: viewport.width, height: viewport.height });
    await page.goto("/");
    await expect(page.getByRole("heading", { name: "AI 论文共写工作台", level: 1 })).toBeVisible();
    await expectNoPageOverflow(page);
    await page.screenshot({ path: `test-results/public-home-${viewport.name}.png`, fullPage: true, animations: "disabled" });
  });

  test(`研究项目页在 ${viewport.name} 视口可操作`, async ({ page }) => {
    await page.setViewportSize({ width: viewport.width, height: viewport.height });
    await installSession(page);
    await page.route("**/api/v1/**", async (route) => {
      const path = new URL(route.request().url()).pathname;
      expect(route.request().headers().authorization).toBe("Bearer e2e-access-token");
      if (path.endsWith("/me")) return apiReply(route, { id: USER_ID, email: "student@example.edu", displayName: "布局测试用户", avatarUrl: null });
      if (path.endsWith("/workspaces")) return apiReply(route, { items: [] });
      return apiReply(route, []);
    });
    await page.goto("/app/projects");
    await expect(page.getByRole("heading", { name: "研究项目" })).toBeVisible();
    if (viewport.width < 900) {
      await page.getByRole("button", { name: "打开导航" }).click();
      await expect(page.getByRole("navigation", { name: "主导航" })).toBeVisible();
      await page.getByRole("button", { name: "关闭导航" }).click();
    }
    await expectNoPageOverflow(page);
    await page.screenshot({ path: `test-results/app-projects-${viewport.name}.png`, fullPage: true, animations: "disabled" });
  });

  test(`项目概览在 ${viewport.name} 视口结构清晰`, async ({ page }) => {
    await page.setViewportSize({ width: viewport.width, height: viewport.height });
    await installSession(page);
    await page.route("**/api/v1/**", async (route) => {
      const path = new URL(route.request().url()).pathname;
      expect(route.request().headers().authorization).toBe("Bearer e2e-access-token");
      if (path.endsWith("/me")) return apiReply(route, { id: USER_ID, email: "student@example.edu", displayName: "布局测试用户", avatarUrl: null });
      if (path.endsWith("/workspaces/workspace-layout")) return apiReply(route, overviewWorkspaceFixture());
      if (path.endsWith("/workspaces/workspace-layout/materials")) return apiReply(route, { items: [overviewMaterialFixture()] });
      if (path.endsWith("/workspaces/workspace-layout/documents")) return apiReply(route, [overviewDocumentFixture()]);
      if (path.endsWith("/workspaces/workspace-layout/knowledge-base/chunks")) return apiReply(route, { items: [] });
      return apiReply(route, []);
    });
    await page.goto("/app/projects/workspace-layout");
    await expect(page.getByRole("heading", { name: "智能校园研究项目", level: 1 })).toBeVisible();
    await expect(page.getByRole("heading", { name: "项目路径" })).toBeVisible();
    await expect(page.getByText("研究准备", { exact: true })).toBeAttached();
    if (viewport.width < 900) {
      await page.getByRole("button", { name: "打开导航" }).click();
      await expect(page.getByRole("link", { name: "项目概览" })).toBeVisible();
      await page.getByRole("button", { name: "关闭导航" }).click();
    }
    await expectNoPageOverflow(page);
    await page.screenshot({ path: `test-results/app-overview-${viewport.name}.png`, fullPage: true, animations: "disabled" });
  });
}

for (const viewport of [VIEWPORTS[0], VIEWPORTS[2]]) {
  test(`文献补充入口在 ${viewport.name} 视口保持清晰层级`, async ({ page }) => {
    await page.setViewportSize({ width: viewport.width, height: viewport.height });
    await installSession(page);
    await page.route("**/api/v1/**", async (route) => {
      const request = route.request();
      const path = new URL(request.url()).pathname.replace(/^\/api\/v1/, "");
      expect(request.headers().authorization).toBe("Bearer e2e-access-token");
      if (path === "/me") return apiReply(route, { id: USER_ID, email: "student@example.edu", displayName: "布局测试用户", avatarUrl: null });
      if (path === "/workspaces/workspace-gate") return apiReply(route, gateWorkspaceFixture());
      if (path === "/workspaces/workspace-gate/documents") return apiReply(route, [gateDocumentFixture()]);
      if (path === "/workspaces/workspace-gate/requirement-snapshot") return apiReply(route, gateSnapshotFixture());
      if (path === "/workspaces/workspace-gate/literature-candidates") return apiReply(route, []);
      if (path === "/documents/document-gate/readiness-check") return apiReply(route, gateReadinessFixture());
      return apiReply(route, []);
    });

    await page.goto("/app/projects/workspace-gate/materials");
    await page.getByRole("button", { name: "检查当前文档准备度" }).click();
    await expect(page.getByRole("heading", { name: "去找可引用文献" })).toBeVisible();
    await expect(page.getByRole("button", { name: "检索文献线索" })).toBeVisible();
    await expect(page.getByText("选择检索目的", { exact: true })).toBeVisible();
    await expect(page.getByText("高级筛选", { exact: true })).toBeVisible();
    const advancedFiltersOpen = await page.locator("details.literature-advanced-filters").evaluate((element) => element.open);
    expect(advancedFiltersOpen).toBe(false);
    await expectNoPageOverflow(page);
    await page.screenshot({ path: `test-results/literature-rescue-${viewport.name}.png`, fullPage: true, animations: "disabled" });
  });
}

test("登录页通过基础无障碍检查", async ({ page }) => {
  await page.goto("/sign-in");
  await page.addScriptTag({ path: "node_modules/axe-core/axe.min.js" });
  const result = await page.evaluate(async () => window.axe.run(document, {
    runOnly: { type: "tag", values: ["wcag2a", "wcag2aa"] }
  }));
  const serious = result.violations.filter((item) => ["serious", "critical"].includes(item.impact));
  expect(serious, serious.map((item) => `${item.id}: ${item.help}`).join("\n")).toEqual([]);
});

async function expectNoPageOverflow(page) {
  const dimensions = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth
  }));
  expect(dimensions.scrollWidth).toBeLessThanOrEqual(dimensions.clientWidth + 1);
}

const USER_ID = "11111111-1111-1111-1111-111111111111";

async function installSession(page) {
  await page.addInitScript(({ key, session }) => localStorage.setItem(key, JSON.stringify(session)), {
    key: "sb-lrxkcxhftjnkpolezuvh-auth-token",
    session: {
      access_token: "e2e-access-token",
      refresh_token: "e2e-refresh-token",
      expires_in: 3600,
      expires_at: Math.floor(Date.now() / 1000) + 3600,
      token_type: "bearer",
      user: { id: USER_ID, aud: "authenticated", role: "authenticated", email: "student@example.edu", user_metadata: {} }
    }
  });
}

function apiReply(route, data) {
  return route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ success: true, data }) });
}

function overviewWorkspaceFixture() {
  return {
    id: "workspace-layout",
    title: "智能校园研究项目",
    status: "DRAFT",
    activeDocumentId: "document-layout",
    academicProfile: {
      academicStage: "MASTER",
      researchParadigm: "QUANTITATIVE",
      institution: "示例大学",
      defaultCitationStyle: "APA"
    },
    updatedAt: "2026-07-22T08:00:00Z"
  };
}

function overviewMaterialFixture() {
  return {
    id: "material-layout",
    filename: "研究数据.pdf",
    parseStage: "AI_PARSED",
    isKeyMaterial: true,
    parseQuality: { status: "READY" }
  };
}

function overviewDocumentFixture() {
  return {
    id: "document-layout",
    workspaceId: "workspace-layout",
    title: "智能校园能源管理研究",
    documentType: "MASTER_THESIS",
    status: "PLANNING",
    sectionCount: 6,
    updatedAt: "2026-07-22T08:00:00Z"
  };
}

function gateWorkspaceFixture() {
  return {
    id: "workspace-gate",
    title: "智能教室能源管理研究",
    status: "DRAFT",
    activeDocumentId: "document-gate",
    academicProfile: {
      academicStage: "MASTER",
      researchParadigm: "QUANTITATIVE",
      defaultCitationStyle: "APA"
    },
    updatedAt: "2026-07-22T08:00:00Z"
  };
}

function gateDocumentFixture() {
  return {
    id: "document-gate",
    workspaceId: "workspace-gate",
    title: "智能教室能源管理研究",
    documentType: "MASTER_THESIS",
    status: "PLANNING",
    targetLength: 30000,
    citationStyle: "APA",
    primaryDocument: true,
    sectionCount: 6,
    updatedAt: "2026-07-22T08:00:00Z"
  };
}

function gateSnapshotFixture() {
  return {
    id: "snapshot-gate",
    workspaceId: "workspace-gate",
    topic: "智能教室能源管理研究",
    wordCount: 30000,
    citationStyle: "APA"
  };
}

function gateReadinessFixture() {
  return {
    generationEligible: false,
    issues: [{
      code: "LITERATURE_MISSING",
      level: "BLOCKING",
      label: "缺少可引用参考资料",
      message: "当前材料缺少能够支撑理论基础与研究方法的真实文献。",
      suggestedAction: "建议补充 3-5 篇与研究主题直接相关的期刊论文。"
    }],
    nextAction: "先检索并上传真实文献原文。"
  };
}
