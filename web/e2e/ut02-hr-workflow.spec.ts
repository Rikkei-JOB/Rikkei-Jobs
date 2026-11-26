import { test, expect, type Page } from "@playwright/test";
import { mkdirSync } from "node:fs";
import { resolve } from "node:path";

const id = "20000000-0000-0000-0000-000000000002";
const userId = "30000000-0000-0000-0000-000000000002";
const companyId = "40000000-0000-0000-0000-000000000002";
const jobId = "50000000-0000-0000-0000-000000000002";
const timestamp = "2026-11-17T00:00:00Z";
const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "Authorization,Content-Type", "Access-Control-Allow-Methods": "GET,POST,PUT,OPTIONS" };

async function signIn(page: Page, role: "HR" | "STAFF") {
  await page.addInitScript(user => localStorage.setItem("rikkei-jobs-auth", JSON.stringify({ state: { token: "test-token", user }, version: 0 })),
    { id: "hr-user", fullName: "HR A", email: "hr@example.test", role, companyId: role === "HR" ? companyId : null, mustChangePassword: false });
}

test("UT-02 HR chuyển bước và chỉ thêm vòng khi đã phỏng vấn", async ({ page }) => {
  await signIn(page, "HR");
  let status = "SUBMITTED";
  let round: Record<string, unknown> | null = null;
  const history = [{ id: "h1", fromStatus: null, toStatus: "SUBMITTED", actorId: userId, note: null, createdAt: timestamp }];
  await page.route("**/api/v1/**", async route => {
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/auth/me")) return route.fulfill({ headers: cors, json: { id: "hr-user", role: "HR", companyId } });
    if (path.endsWith(`/applications/${id}/transition`)) {
      const body = route.request().postDataJSON(); status = body.toStatus;
      history.push({ id: `h${history.length + 1}`, fromStatus: null, toStatus: status, actorId: "hr-user", note: body.note, createdAt: timestamp });
      return route.fulfill({ headers: cors, json: { id, status, updatedAt: timestamp } });
    }
    if (path.endsWith(`/applications/${id}/rounds`) && route.request().method() === "POST") {
      const body = route.request().postDataJSON(); round = { id: "round-1", applicationId: id, roundNo: 1, name: body.name,
        interviewerEmail: body.interviewerEmail, scheduledAt: body.scheduledAt, result: "PENDING", score: null, note: null, createdAt: timestamp };
      return route.fulfill({ status: 201, headers: cors, json: round });
    }
    if (path.endsWith("/rounds/round-1") && route.request().method() === "PUT") {
      round = { ...round, ...route.request().postDataJSON() }; return route.fulfill({ headers: cors, json: round });
    }
    if (path.endsWith(`/applications/${id}`)) return route.fulfill({ headers: cors, json: {
      id, jobId, jobTitle: "Java Developer", companyId, companyName: "Công ty A", status,
      createdAt: timestamp, updatedAt: timestamp, candidate: { userId, fullName: "Ứng viên A", email: "a@example.test" },
      cvId: "cv-1", cvSnapshotUrl: `/api/v1/recruitment/applications/${id}/cv`, coverLetter: "Xin chào",
      match: null, rounds: round ? [round] : [], roundRevisions: [], history,
      allowedTransitions: status === "SUBMITTED" ? ["SCREENING", "REJECTED"] : status === "SCREENING" ? ["INTERVIEW", "REJECTED"] : status === "INTERVIEW" ? ["REJECTED"] : [],
    } });
    return route.fulfill({ headers: cors, json: {} });
  });
  await page.goto(`/hr/recruitment/${id}`);
  await expect(page.getByText(/Tạm thời không lấy được điểm phù hợp/)).toBeVisible();
  await expect(page.getByRole("button", { name: "Thêm vòng" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Chuyển sang Phỏng vấn" })).toHaveCount(0);
  await page.getByRole("button", { name: "Chuyển sang Sàng lọc" }).click();
  await page.getByRole("button", { name: "Chuyển sang Phỏng vấn" }).click();
  await page.getByRole("tab", { name: /^Phỏng vấn/ }).click();
  await page.getByLabel("Tên vòng").fill("Kỹ thuật");
  await page.getByLabel("Email người phỏng vấn").fill("interviewer@example.test");
  await page.getByRole("button", { name: "Thêm vòng" }).click();
  await expect(page.getByRole("heading", { name: "Vòng 1: Kỹ thuật" })).toBeVisible();
  await page.getByLabel("Kết quả vòng 1").selectOption("PASS");
  await page.getByLabel("Điểm vòng 1").fill("85");
  await page.getByRole("button", { name: "Lưu kết quả" }).click();
  await expect(page.getByText("Điểm: 85/100")).toBeVisible();
  const shot = resolve(__dirname, "../../docs/api/screenshots/ut02-hr-detail.png");
  mkdirSync(resolve(__dirname, "../../docs/api/screenshots"), { recursive: true });
  await page.screenshot({ path: shot, fullPage: true });
});

test("UT-02 xung đột 409 hiển thị lỗi và nạp lại trạng thái", async ({ page }) => {
  await signIn(page, "HR");
  let status = "SUBMITTED";
  let reads = 0;
  await page.route("**/api/v1/**", route => {
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/auth/me")) return route.fulfill({ headers: cors, json: { id: "hr-user", role: "HR", companyId } });
    if (path.endsWith(`/applications/${id}/transition`)) {
      status = "SCREENING";
      return route.fulfill({ status: 409, headers: cors, json: { code: "INVALID_TRANSITION", message: "Đơn đã đổi trạng thái" } });
    }
    if (path.endsWith(`/applications/${id}`)) {
      reads++;
      return route.fulfill({ headers: cors, json: {
        id, jobId, jobTitle: "Java Developer", companyId, companyName: "Công ty A", status,
        createdAt: timestamp, updatedAt: timestamp, candidate: { userId, fullName: "Ứng viên A", email: "a@example.test" },
        cvId: "cv", cvSnapshotUrl: "cv", coverLetter: null, match: null, rounds: [], roundRevisions: [], history: [],
        allowedTransitions: status === "SUBMITTED" ? ["SCREENING"] : ["INTERVIEW"],
      } });
    }
    return route.fulfill({ headers: cors, json: {} });
  });
  await page.goto(`/hr/recruitment/${id}`);
  await page.getByRole("button", { name: "Chuyển sang Sàng lọc" }).click();
  await expect(page.getByText("Đơn đã đổi trạng thái")).toBeVisible();
  await expect(page.getByRole("button", { name: "Chuyển sang Phỏng vấn" })).toBeVisible();
  expect(reads).toBeGreaterThanOrEqual(2);
});
