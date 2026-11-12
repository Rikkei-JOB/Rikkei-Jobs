import { test, expect } from "@playwright/test";
import { signIn, apiFixtures, application, applicationId } from "./ut01-fixtures";

test.beforeEach(async ({ page }) => { await signIn(page); await apiFixtures(page); });

test("keeps status filter on refresh and displays application cards", async ({ page }) => {
  await page.goto("/candidate/applications");
  await expect(page.getByText("Java Developer")).toBeVisible();
  await page.getByLabel("Lọc theo trạng thái").selectOption("REJECTED");
  await expect(page).toHaveURL(/status=REJECTED/);
  await page.reload();
  await expect(page.getByLabel("Lọc theo trạng thái")).toHaveValue("REJECTED");
});

test("withdrawal requires a reason and refreshes status and history", async ({ page }) => {
  let withdrawn = false;
  await page.route(new RegExp(`/api/v1/recruitment/me/applications/${applicationId}(?:/withdraw)?$`), async route => {
    const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "Authorization,Content-Type", "Access-Control-Allow-Methods": "GET,POST,OPTIONS" };
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    if (route.request().method() === "POST") {
      expect(route.request().postDataJSON()).toEqual({ reason: "Tôi đổi định hướng." }); withdrawn = true;
    }
    await route.fulfill({ headers: cors, json: withdrawn ? { ...application, status: "WITHDRAWN", canWithdraw: false,
      history: [...application.history, { id: "history-2", fromStatus: "SUBMITTED", toStatus: "WITHDRAWN", actorId: "user", note: "Tôi đổi định hướng.", createdAt: "2026-11-12T02:00:00Z" }] } : application });
  });
  await page.goto(`/candidate/applications/${applicationId}`);
  await page.getByRole("button", { name: "Rút đơn", exact: true }).click();
  await page.getByLabel("Lý do rút đơn").fill("Tôi đổi định hướng.");
  await page.getByRole("button", { name: "Xác nhận rút đơn" }).click();
  await expect(page.getByText("Tôi đổi định hướng.")).toBeVisible();
  await expect(page.getByRole("link", { name: "Ứng tuyển lại" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Rút đơn", exact: true })).toHaveCount(0);
});

test("offer hides withdrawal and rejected offers reapplication", async ({ page }) => {
  await page.route(`**/api/v1/recruitment/me/applications/${applicationId}`, r => r.fulfill({ headers: { "Access-Control-Allow-Origin": "*" }, json: { ...application, status: "OFFER", canWithdraw: false } }));
  await page.goto(`/candidate/applications/${applicationId}`);
  await expect(page.getByRole("heading", { name: "Java Developer" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Rút đơn", exact: true })).toHaveCount(0);
  await page.route(`**/api/v1/recruitment/me/applications/${applicationId}`, r => r.fulfill({ headers: { "Access-Control-Allow-Origin": "*" }, json: { ...application, status: "REJECTED", canWithdraw: false } }));
  await page.reload();
  const jobsRoot = (process.env.NEXT_PUBLIC_JOB_PAGE_PATH ?? "/jobs").replace(/\/$/, "");
  await expect(page.getByRole("link", { name: "Ứng tuyển lại" })).toHaveAttribute("href", `${jobsRoot}/${application.jobId}`);
});

test("discovery links honor the configured jobs root", async ({ page }) => {
  const jobsRoot = (process.env.NEXT_PUBLIC_JOB_PAGE_PATH ?? "/jobs").replace(/\/$/, "");
  await page.route("**/api/v1/recruitment/me/applications*", r => r.fulfill({ headers: { "Access-Control-Allow-Origin": "*" }, json: { items: [], total: 0, page: 1, size: 20 } }));
  await page.goto("/candidate/applications");
  await expect(page.getByRole("link", { name: "Khám phá việc làm" })).toHaveAttribute("href", jobsRoot);
  await page.goto("/candidate/applications/new");
  await expect(page.getByRole("link", { name: "trang Việc làm" })).toHaveAttribute("href", jobsRoot);
});
