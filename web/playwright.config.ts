import { defineConfig, devices } from "@playwright/test";

// Keep browser fixtures independent of a developer's local mock server addresses.
// Explicit environment overrides still take precedence, as in the existing route tests.
process.env.NEXT_PUBLIC_JOB_API_PATH ??= "/candidate-career-fixture/jobs";
process.env.NEXT_PUBLIC_JOB_PAGE_PATH ??= "/candidate/jobs";
process.env.NEXT_PUBLIC_CV_API_PATH ??= "/profile/cvs";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  reporter: "list",
  use: { baseURL: "http://127.0.0.1:3101", trace: "retain-on-failure" },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"], channel: process.env.UT01_BROWSER_CHANNEL } }],
  webServer: {
    env: { PLAYWRIGHT_TEST: "1" },
    command: "npm run dev -- --hostname 127.0.0.1 --port 3101",
    url: "http://127.0.0.1:3101",
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
