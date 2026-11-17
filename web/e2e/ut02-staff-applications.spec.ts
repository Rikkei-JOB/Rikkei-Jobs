import { test, expect } from "@playwright/test";
import { mkdirSync } from "node:fs";
import { resolve } from "node:path";

test("UT-02 STAFF lọc đơn và chỉ đọc chi tiết", async ({ page }) => {
  const id = "20000000-0000-0000-0000-000000000003";
  const companyId = "40000000-0000-0000-0000-000000000003";
  const jobId = "50000000-0000-0000-0000-000000000003";
  const time = "2026-11-17T00:00:00Z";
  await page.addInitScript(user => localStorage.setItem("rikkei-jobs-auth", JSON.stringify({ state: { token: "test-token", user }, version: 0 })),
    { id: "staff-user", fullName: "Staff", email: "staff@example.test", role: "STAFF", companyId: null, mustChangePassword: false });
  const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "Authorization,Content-Type", "Access-Control-Allow-Methods": "GET,POST,PUT,OPTIONS" };
  await page.route("**/api/v1/**", route => {
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    const url = new URL(route.request().url());
    if (url.pathname.endsWith("/auth/me")) return route.fulfill({ headers: cors, json: { id: "staff-user", role: "STAFF" } });
    if (url.pathname.endsWith("/staff/applications")) return route.fulfill({ headers: cors, json: {
      items: [{ id, jobId, jobTitle: "Java Developer", companyId, companyName: "Công ty B", userId: "candidate",
        candidateName: "Ứng viên B", status: "INTERVIEW", createdAt: time, updatedAt: time }], total: 1, page: 1, size: 20,
    } });
    if (url.pathname.endsWith(`/applications/${id}`)) return route.fulfill({ headers: cors, json: {
      id, jobId, jobTitle: "Java Developer", companyId, companyName: "Công ty B", status: "INTERVIEW", createdAt: time, updatedAt: time,
      candidate: { userId: "candidate", fullName: "Ứng viên B", email: "b@example.test" }, cvId: "cv", cvSnapshotUrl: "cv",
      coverLetter: null, match: null, rounds: [], roundRevisions: [], history: [], allowedTransitions: [],
    } });
    return route.fulfill({ headers: cors, json: {} });
  });
  await page.goto("/staff/applications");
  await expect(page.getByText("Ứng viên B")).toBeVisible();
  await page.getByLabel("Trạng thái").selectOption("INTERVIEW");
  await expect(page).toHaveURL(/status=INTERVIEW/);
  mkdirSync(resolve(__dirname, "../../docs/api/screenshots"), { recursive: true });
  await page.screenshot({ path: resolve(__dirname, "../../docs/api/screenshots/ut02-staff-list.png"), fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(391);
  await page.getByRole("link", { name: /Ứng viên B/ }).click();
  await expect(page.getByRole("button", { name: "Tải CV đã nộp" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Thêm vòng" })).toHaveCount(0);
  await expect(page.getByRole("heading", { name: "Chuyển bước" })).toHaveCount(0);
  const shot = resolve(__dirname, "../../docs/api/screenshots/ut02-staff-detail.png");
  await page.screenshot({ path: shot, fullPage: true });
});
