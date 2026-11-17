import { test, expect, type Page } from "@playwright/test";
import { mkdirSync } from "node:fs";
import { resolve } from "node:path";

const id = "20000000-0000-0000-0000-000000000020";
const roundId = "30000000-0000-0000-0000-000000000020";
const now = "2026-11-17T00:00:00Z";
const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "Authorization,Content-Type", "Access-Control-Allow-Methods": "GET,PUT,OPTIONS" };

async function signIn(page: Page, role: "ADMIN" | "MENTOR") {
  await page.addInitScript(user => localStorage.setItem("rikkei-jobs-auth", JSON.stringify({ state: { token: "test-token", user }, version: 0 })),
    { id: `${role.toLowerCase()}-user`, fullName: role, email: `${role.toLowerCase()}@example.test`, role, companyId: null, mustChangePassword: false });
}

test("ADMIN xem đơn và số điện thoại nhưng không có thao tác chỉnh sửa", async ({ page }) => {
  await signIn(page, "ADMIN");
  await page.route("**/api/v1/**", route => {
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/auth/me")) return route.fulfill({ headers: cors, json: { id: "admin-user", role: "ADMIN" } });
    if (path.endsWith("/staff/applications")) return route.fulfill({ headers: cors, json: { items: [{ id, jobId: "job", jobTitle: "Java Developer", companyId: "company", companyName: "Công ty A", userId: "candidate", candidateName: "Ứng viên A", status: "INTERVIEW", createdAt: now, updatedAt: now }], total: 1, page: 1, size: 20 } });
    if (path.endsWith(`/applications/${id}`)) return route.fulfill({ headers: cors, json: { id, jobId: "job", jobTitle: "Java Developer", companyId: "company", companyName: "Công ty A", status: "INTERVIEW", createdAt: now, updatedAt: now, candidate: { userId: "candidate", fullName: "Ứng viên A", email: "a@example.test", phone: "0912345678" }, cvId: "cv", cvSnapshotUrl: "cv", coverLetter: null, match: null, rounds: [], roundRevisions: [], history: [], allowedTransitions: [] } });
    return route.fulfill({ headers: cors, json: {} });
  });
  await page.goto("/admin/applications");
  await page.getByRole("link", { name: /Ứng viên A/ }).click();
  await expect(page.getByText("0912345678")).toBeVisible();
  await expect(page.getByRole("button", { name: "Tải CV đã nộp" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Thêm vòng" })).toHaveCount(0);
  await expect(page.getByRole("heading", { name: "Chuyển bước" })).toHaveCount(0);
  mkdirSync(resolve(__dirname, "../../docs/api/screenshots"), { recursive: true });
  await page.screenshot({ path: resolve(__dirname, "../../docs/api/screenshots/ut02-admin-detail.png"), fullPage: true });
});

test("MENTOR chỉ chấm vòng được mời", async ({ page }) => {
  await signIn(page, "MENTOR");
  let result = "PENDING";
  await page.route("**/api/v1/**", route => {
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/auth/me")) return route.fulfill({ headers: cors, json: { id: "mentor-user", role: "MENTOR" } });
    if (path.endsWith("/mentor/rounds") && route.request().method() === "GET") return route.fulfill({ headers: cors, json: { items: [{ jobTitle: "Java Developer", companyName: "Công ty A", applicationStatus: "INTERVIEW", round: { id: roundId, applicationId: id, roundNo: 1, name: "Kỹ thuật", interviewerEmail: "mentor@example.test", scheduledAt: now, result, score: result === "PASS" ? 85 : null, note: null, createdAt: now } }], total: 1, page: 1, size: 20 } });
    if (path.endsWith(`/mentor/rounds/${roundId}`) && route.request().method() === "PUT") {
      result = route.request().postDataJSON().result;
      return route.fulfill({ headers: cors, json: { id: roundId, result, score: 85 } });
    }
    return route.fulfill({ headers: cors, json: {} });
  });
  await page.goto("/mentor/reviews");
  await expect(page.getByText("Java Developer")).toBeVisible();
  await expect(page.getByText("a@example.test")).toHaveCount(0);
  await page.getByLabel("Kết quả vòng 1").selectOption("PASS");
  await page.getByLabel("Điểm vòng 1").fill("85");
  await page.getByRole("button", { name: "Lưu kết quả" }).click();
  await expect(page.getByText("Điểm: 85/100")).toBeVisible();
  mkdirSync(resolve(__dirname, "../../docs/api/screenshots"), { recursive: true });
  await page.screenshot({ path: resolve(__dirname, "../../docs/api/screenshots/ut02-mentor-reviews.png"), fullPage: true });
});
