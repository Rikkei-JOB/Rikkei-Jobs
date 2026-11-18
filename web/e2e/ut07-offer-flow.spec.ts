import { test, expect, type Page } from "@playwright/test";

const applicationId = "20000000-0000-0000-0000-000000000007";
const offerId = "30000000-0000-0000-0000-000000000007";
const userId = "40000000-0000-0000-0000-000000000007";
const companyId = "50000000-0000-0000-0000-000000000007";
const jobId = "60000000-0000-0000-0000-000000000007";
const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "Authorization,Content-Type", "Access-Control-Allow-Methods": "GET,POST,OPTIONS" };

async function signIn(page: Page, role: "HR" | "CANDIDATE") {
  await page.addInitScript(user => localStorage.setItem("rikkei-jobs-auth", JSON.stringify({ state: { token: "test-token", user }, version: 0 })),
    { id: role === "HR" ? "hr-user" : userId, fullName: role === "HR" ? "HR A" : "Ứng viên A", email: "a@example.test", role,
      companyId: role === "HR" ? companyId : null, mustChangePassword: false });
}

function offer(status: string) {
  return { id: offerId, applicationId, position: "Java Developer", salary: 12000000,
    startDate: "2030-02-01", workLocation: "Hà Nội", note: null,
    responseDeadline: "2030-01-03T00:00:00Z", status, respondedAt: null, declineReason: null,
    createdAt: "2026-11-18T00:00:00Z" };
}

test("HR gửi offer lương gross theo tháng từ đơn có vòng cuối PASS", async ({ page }) => {
  await signIn(page, "HR");
  let current = "INTERVIEW";
  let sent: Record<string, unknown> | null = null;
  await page.route("**/api/v1/**", route => {
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/auth/me")) return route.fulfill({ headers: cors, json: { id: "hr-user", role: "HR", companyId } });
    if (path.endsWith(`/applications/${applicationId}/offer`) && route.request().method() === "POST") {
      sent = route.request().postDataJSON(); current = "OFFER";
      return route.fulfill({ status: 201, headers: cors, json: offer("SENT") });
    }
    if (path.endsWith(`/applications/${applicationId}`)) return route.fulfill({ headers: cors, json: {
      id: applicationId, jobId, jobTitle: "Java Developer", companyId, companyName: "Công ty A", status: current,
      createdAt: "2026-11-18T00:00:00Z", updatedAt: "2026-11-18T00:00:00Z",
      candidate: { userId, fullName: "Ứng viên A", email: "a@example.test" },
      cvId: "cv-1", cvSnapshotUrl: "cv", coverLetter: null, match: null,
      rounds: [{ id: "r1", applicationId, roundNo: 1, name: "Vòng cuối", interviewerEmail: "hr@example.test",
        scheduledAt: null, result: "PASS", score: 90, note: null, createdAt: "2026-11-18T00:00:00Z" }],
      roundRevisions: [], history: [], allowedTransitions: current === "INTERVIEW" ? ["REJECTED"] : [],
      offer: current === "OFFER" ? offer("SENT") : null,
    } });
    return route.fulfill({ headers: cors, json: {} });
  });
  await page.goto(`/hr/recruitment/${applicationId}`);
  await page.getByLabel("Vị trí offer").fill("Java Developer");
  await page.getByLabel("Lương gross/tháng (VND)").fill("12000000");
  await page.getByLabel("Ngày bắt đầu").fill("2030-02-01");
  await page.getByLabel("Địa điểm làm việc").fill("Hà Nội");
  await page.getByLabel("Hạn trả lời").fill("2030-01-03T07:00");
  await page.getByRole("button", { name: "Gửi offer" }).click();
  await expect(page.getByText(/Trạng thái: Đã gửi/)).toBeVisible();
  await page.screenshot({ path: "../docs/api/screenshots/ut07-hr-offer.png", fullPage: true });
  expect(sent).toMatchObject({ position: "Java Developer", salary: 12000000, startDate: "2030-02-01", workLocation: "Hà Nội" });
});

test("ứng viên nhận offer và thấy kinh nghiệm trên hồ sơ", async ({ page }) => {
  await signIn(page, "CANDIDATE");
  let current = "SENT";
  await page.route("**/api/v1/**", route => {
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/auth/me")) return route.fulfill({ headers: cors, json: { id: userId, role: "CANDIDATE", companyId: null } });
    if (path.endsWith(`/me/offers/${offerId}/accept`)) {
      current = "ACCEPTED"; return route.fulfill({ headers: cors, json: offer(current) });
    }
    if (path.endsWith(`/me/offers/${offerId}`)) return route.fulfill({ headers: cors, json: offer(current) });
    if (path.endsWith("/me/employment-records")) return route.fulfill({ headers: cors, json: current === "ACCEPTED" ? [{
      id: "record-1", applicationId, userId, companyId, jobId, companyName: "Công ty A",
      position: "Java Developer", startDate: "2030-02-01", confirmedAt: null,
    }] : [] });
    return route.fulfill({ headers: cors, json: [] });
  });
  await page.goto(`/candidate/offers/${offerId}`);
  await expect(page.getByText("12.000.000 VND/tháng (gross)")).toBeVisible();
  await page.screenshot({ path: "../docs/api/screenshots/ut07-candidate-offer.png", fullPage: true });
  await page.getByRole("button", { name: "Nhận offer" }).click();
  await expect(page.getByText("Đã nhận", { exact: true })).toBeVisible();
  await page.goto("/candidate/profile");
  await expect(page.getByText("Java Developer tại Công ty A")).toBeVisible();
  await page.screenshot({ path: "../docs/api/screenshots/ut07-profile-employment.png", fullPage: true });
});

test("từ chối cần lý do và offer quá hạn không thể nhận", async ({ page }) => {
  await signIn(page, "CANDIDATE");
  let status = "SENT";
  let pastDeadline = false;
  let declinedReason = "";
  await page.route("**/api/v1/**", route => {
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/auth/me")) return route.fulfill({ headers: cors, json: { id: userId, role: "CANDIDATE", companyId: null } });
    if (path.endsWith(`/me/offers/${offerId}/decline`)) {
      declinedReason = route.request().postDataJSON().reason;
      status = "DECLINED";
      return route.fulfill({ headers: cors, json: { ...offer(status), declineReason: declinedReason } });
    }
    if (path.endsWith(`/me/offers/${offerId}`)) return route.fulfill({ headers: cors, json: { ...offer(status), responseDeadline: pastDeadline ? "2020-01-01T00:00:00Z" : "2030-01-03T00:00:00Z" } });
    return route.fulfill({ headers: cors, json: [] });
  });
  await page.goto(`/candidate/offers/${offerId}`);
  await expect(page.getByRole("button", { name: "Từ chối offer" })).toBeDisabled();
  await page.getByLabel("Lý do từ chối").fill("Đã chọn công việc khác");
  await page.getByRole("button", { name: "Từ chối offer" }).click();
  await expect(page.getByText("Lý do từ chối: Đã chọn công việc khác")).toBeVisible();
  expect(declinedReason).toBe("Đã chọn công việc khác");
  status = "SENT";
  pastDeadline = true;
  await page.reload();
  await expect(page.getByText("Đã hết hạn trả lời")).toBeVisible();
  await expect(page.getByRole("button", { name: "Nhận offer" })).toHaveCount(0);
});

test("server báo 409 khi offer vừa hết hạn thì tải lại trạng thái", async ({ page }) => {
  await signIn(page, "CANDIDATE");
  let expired = false;
  await page.route("**/api/v1/**", route => {
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/auth/me")) return route.fulfill({ headers: cors, json: { id: userId, role: "CANDIDATE", companyId: null } });
    if (path.endsWith(`/me/offers/${offerId}/accept`)) {
      expired = true;
      return route.fulfill({ status: 409, headers: cors, json: { code: "OFFER_EXPIRED", message: "Offer đã hết hạn." } });
    }
    if (path.endsWith(`/me/offers/${offerId}`)) return route.fulfill({ headers: cors, json: {
      ...offer(expired ? "EXPIRED" : "SENT"), responseDeadline: expired ? "2020-01-01T00:00:00Z" : "2030-01-03T00:00:00Z",
    } });
    return route.fulfill({ headers: cors, json: [] });
  });
  await page.goto(`/candidate/offers/${offerId}`);
  await page.getByRole("button", { name: "Nhận offer" }).click();
  await expect(page.getByRole("alert").filter({ hasText: "Offer đã hết hạn." })).toBeVisible();
  await expect(page.getByText("Đã hết hạn", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Nhận offer" })).toHaveCount(0);
});
