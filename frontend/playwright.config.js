import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  reporter: [["line"]],
  timeout: 45_000,
  expect: {
    timeout: 8_000
  },
  use: {
    ...devices["Desktop Chrome"],
    baseURL: "http://127.0.0.1:4173",
    channel: "chrome",
    headless: true,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "off"
  },
  webServer: {
    command: "npm run dev -- --host 127.0.0.1 --port 4173",
    url: "http://127.0.0.1:4173",
    reuseExistingServer: true,
    timeout: 120_000
  }
});
