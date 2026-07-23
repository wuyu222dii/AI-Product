import { expect, test } from "@playwright/test";

const USER_ID = "11111111-1111-1111-1111-111111111111";
const NOW = "2026-07-22T08:00:00Z";

test("新用户完成六步引导并创建个性化项目路线", async ({ page }) => {
  const state = { onboardingStatus: "NOT_STARTED", workspace: null, completedRequest: null };
  await installSession(page);
  await installOnboardingApi(page, state);

  await page.goto("/app/projects");
  await expect(page).toHaveURL(/\/app\/onboarding$/);
  await expect(page.getByRole("heading", { name: "先认识平台的三段研究主线" })).toBeVisible();

  await page.getByRole("button", { name: "继续" }).click();
  await page.getByLabel("项目名称").fill("社区韧性研究");
  await page.getByLabel("学术阶段").selectOption("MASTER");
  await page.getByRole("button", { name: "继续" }).click();

  await page.getByLabel("学科方向").selectOption("SOCIAL_SCIENCE");
  await page.getByLabel("研究范式").selectOption("QUALITATIVE");
  await page.getByRole("button", { name: "继续" }).click();

  await page.getByRole("button", { name: /正在搜集材料/ }).click();
  await page.getByRole("button", { name: "继续" }).click();

  await page.getByLabel("参考文献").check();
  await page.getByLabel("截止日期（可选）").fill("2026-12-20");
  await page.getByRole("button", { name: "继续" }).click();

  await expect(page.getByRole("heading", { name: "你的第一条项目路线" })).toBeVisible();
  await expect(page.getByText("构建项目知识库", { exact: true })).toBeVisible();
  await page.screenshot({ path: "test-results/onboarding-route-desktop.png", fullPage: true, animations: "disabled" });
  await page.getByRole("button", { name: "创建项目并查看路线" }).click();

  await expect(page).toHaveURL(/\/app\/projects\/workspace-onboarding$/);
  expect(state.completedRequest.workspace.guideProfile).toMatchObject({
    currentProgress: "MATERIALS_COLLECTING",
    availableMaterials: ["REFERENCES"],
    targetDeadline: "2026-12-20",
    preferredMode: "GUIDED"
  });
  await expect(page.getByRole("heading", { name: "个性化项目路线" })).toBeVisible();
  await expectNoOverflow(page);
  await page.screenshot({ path: "test-results/project-guide-desktop.png", fullPage: true, animations: "disabled" });
});

test("新用户可跳过引导并从顶栏重新查看使用指南", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  const state = { onboardingStatus: "NOT_STARTED", workspace: null, completedRequest: null };
  await installSession(page);
  await installOnboardingApi(page, state);

  await page.goto("/app/projects");
  await expect(page).toHaveURL(/\/app\/onboarding$/);
  await page.getByRole("button", { name: "稍后再说" }).click();
  await expect(page).toHaveURL(/\/app\/projects$/);
  await expect(page.getByRole("heading", { name: "研究项目" })).toBeVisible();
  expect(state.onboardingStatus).toBe("SKIPPED");

  await page.getByRole("button", { name: "使用指南" }).click();
  await expect(page).toHaveURL(/\/app\/onboarding\?mode=tour$/);
  await expect(page.getByRole("heading", { name: "使用指南", level: 1 })).toBeVisible();
  await expect(page.getByText("研究准备", { exact: true }).first()).toBeVisible();
  await expectNoOverflow(page);
  await page.screenshot({ path: "test-results/system-tour-mobile.png", fullPage: true, animations: "disabled" });
});

async function installOnboardingApi(page, state) {
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const method = request.method();
    const path = new URL(request.url()).pathname.replace(/^\/api\/v1/, "");
    const body = request.postData() ? JSON.parse(request.postData()) : null;

    expect(request.headers().authorization).toBe("Bearer e2e-access-token");

    if (path === "/me" && method === "GET") return reply(route, profile(state.onboardingStatus));
    if (path === "/me/onboarding" && method === "PATCH") {
      state.onboardingStatus = body.status;
      return reply(route, profile(body.status));
    }
    if (path === "/workspaces" && method === "GET") {
      return reply(route, { items: state.workspace ? [state.workspace] : [] });
    }
    if (path === "/onboarding/complete" && method === "POST") {
      state.completedRequest = body;
      state.onboardingStatus = "COMPLETED";
      state.workspace = workspaceFixture(body.workspace);
      return reply(route, {
        user: profile("COMPLETED"),
        workspace: state.workspace,
        guide: guideFixture()
      }, 201);
    }
    if (path === "/workspaces/workspace-onboarding" && method === "GET") {
      return reply(route, state.workspace);
    }
    if (path === "/workspaces/workspace-onboarding/guide" && method === "GET") {
      return reply(route, guideFixture());
    }
    if (path === "/workspaces/workspace-onboarding/materials") return reply(route, { items: [] });
    if (path === "/workspaces/workspace-onboarding/documents") return reply(route, [documentFixture()]);
    if (path === "/workspaces/workspace-onboarding/knowledge-base/chunks") return reply(route, { items: [] });
    return reply(route, []);
  });
}

function profile(status) {
  return {
    id: USER_ID,
    email: "student@example.edu",
    displayName: "新用户",
    avatarUrl: null,
    onboardingStatus: status,
    onboardingVersion: status === "NOT_STARTED" ? null : "v1",
    onboardingCompletedAt: status === "NOT_STARTED" ? null : NOW,
    createdAt: NOW,
    updatedAt: NOW
  };
}

function workspaceFixture(input) {
  return {
    id: "workspace-onboarding",
    title: input.title,
    status: "DRAFT",
    activeDocumentId: "document-onboarding",
    academicProfile: { ...input.academicProfile, workspaceId: "workspace-onboarding" },
    createdAt: NOW,
    updatedAt: NOW
  };
}

function documentFixture() {
  return {
    id: "document-onboarding",
    workspaceId: "workspace-onboarding",
    title: "社区韧性研究",
    documentType: "MASTER_THESIS",
    status: "PLANNING",
    sectionCount: 7,
    updatedAt: NOW
  };
}

function guideFixture() {
  return {
    workspaceId: "workspace-onboarding",
    guideVersion: "v1",
    currentProgress: "MATERIALS_COLLECTING",
    availableMaterials: ["REFERENCES"],
    targetDeadline: "2026-12-20",
    preferredMode: "GUIDED",
    overallProgress: 17,
    currentTaskId: "materials",
    updatedAt: NOW,
    tasks: [
      task("project_setup", "研究准备", "建立研究项目", "COMPLETED", "/app/projects/workspace-onboarding"),
      task("materials", "研究准备", "添加研究材料", "CURRENT", "/app/projects/workspace-onboarding/upload"),
      task("parsing", "研究准备", "确认材料解析", "UPCOMING", "/app/projects/workspace-onboarding/parsing"),
      task("readiness", "研究准备", "检查写作准备度", "UPCOMING", "/app/projects/workspace-onboarding/materials"),
      task("knowledge", "研究资产", "构建项目知识库", "OPTIONAL", "/app/projects/workspace-onboarding/knowledge"),
      task("writing", "写作交付", "推进章节写作", "UPCOMING", "/app/projects/workspace-onboarding/documents"),
      task("review_delivery", "写作交付", "审查并交付", "UPCOMING", "/app/projects/workspace-onboarding/documents")
    ]
  };
}

function task(id, phase, title, status, targetPath) {
  return {
    id,
    phase,
    title,
    status,
    targetPath,
    description: `${title}的任务说明。`,
    reason: "确保后续研究过程有明确依据。",
    expectedOutcome: "形成可继续使用的研究产出。",
    progressLabel: status === "COMPLETED" ? "已完成" : "等待处理",
    blocking: false
  };
}

function reply(route, data, status = 200) {
  return route.fulfill({ status, contentType: "application/json", body: JSON.stringify({ success: true, data }) });
}

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

async function expectNoOverflow(page) {
  const dimensions = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth
  }));
  expect(dimensions.scrollWidth).toBeLessThanOrEqual(dimensions.clientWidth + 1);
}
