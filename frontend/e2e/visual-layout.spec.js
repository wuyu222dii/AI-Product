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
