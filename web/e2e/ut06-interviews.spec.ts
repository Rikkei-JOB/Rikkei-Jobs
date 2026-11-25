import { test, expect, type Page } from "@playwright/test";
test.use({ timezoneId: "America/New_York" });
const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "Authorization,Content-Type", "Access-Control-Allow-Methods": "GET,POST,OPTIONS" };
async function auth(page: Page, role: "HR" | "CANDIDATE") {
  await page.addInitScript(role => localStorage.setItem("rikkei-jobs-auth", JSON.stringify({ state: { token: "test-token", user: { id: "candidate", fullName: "Test", email: "a@test.local", role, companyId: role === "HR" ? "company" : null, mustChangePassword: false } }, version: 0 })), role);
  await page.route("**/api/v1/auth/me", route => route.fulfill({ headers: cors, json: { id: "candidate", fullName: "Test", email: "a@test.local", role, companyId: role === "HR" ? "company" : null, mustChangePassword: false } }));
}
const slot = { id: "slot", roundId: "round", proposalId: "proposal", startAt: "2030-01-01T02:00:00Z", endAt: "2030-01-01T02:30:00Z", status: "OPEN" };
const schedule = { id: "schedule", roundId: "round", slotId: "slot", startAt: slot.startAt, endAt: slot.endAt, meetUrl: "https://meet.test/room", status: "SCHEDULED" };
test("Ứng viên chọn giờ và vào phòng, gửi một yêu cầu", async ({ page }) => {
  await auth(page, "CANDIDATE"); let picked = false, posts = 0;
  await page.route("**/api/v1/**", async route => {
    const path = new URL(route.request().url()).pathname;
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    if (path.endsWith("/pick")) { posts++; picked = true; return route.fulfill({ headers: cors, json: schedule }); }
    if (path.endsWith("/me/interviews")) return route.fulfill({ headers: cors, json: { items: [{ roundId: "round", roundName: "Kỹ thuật", jobTitle: "Java", companyName: "Rikkei", interviewerEmail: "hr@test.local", slots: picked ? [] : [slot], schedule: picked ? schedule : null }], total: 1, page: 1, size: 100 } });
    return path.endsWith("/auth/me") ? route.fallback() : route.fulfill({ headers: cors, json: {} });
  });
  await page.goto("/candidate/interviews");
  await page.getByRole("radio").check(); await page.getByRole("button", { name: "Xác nhận khung giờ" }).click();
  await expect(page.getByRole("link", { name: "Vào phòng họp" })).toHaveAttribute("href", "https://meet.test/room");
  await expect(page.getByText(/09:00/)).toBeVisible(); expect(posts).toBe(1);
  await page.screenshot({ path: "../docs/api/screenshots/ut06-candidate.png", fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.getByRole("link", { name: "Vào phòng họp" })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await page.screenshot({ path: "../docs/api/screenshots/ut06-candidate-mobile.png", fullPage: true });
});
test("Khung bị huỷ trả 409 thì tải lại lời mời và báo lỗi", async ({ page }) => {
  await auth(page, "CANDIDATE"); let conflict = false;
  await page.route("**/api/v1/**", route => {
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/pick")) { conflict = true; return route.fulfill({ status: 409, headers: cors, json: { code: "CONFLICT", message: "Khung giờ không còn khả dụng." } }); }
    if (path.endsWith("/me/interviews")) return route.fulfill({ headers: cors, json: { items: [{ roundId: "round", roundName: "Kỹ thuật", jobTitle: "Java", companyName: "Rikkei", interviewerEmail: "hr@test.local", slots: conflict ? [] : [slot], schedule: null }], total: 1, page: 1, size: 100 } });
    return path.endsWith("/auth/me") ? route.fallback() : route.fulfill({ headers: cors, json: {} });
  });
  await page.goto("/candidate/interviews"); await page.getByRole("radio").check(); await page.getByRole("button", { name: "Xác nhận khung giờ" }).click();
  await expect(page.getByRole("alert").filter({ hasText: "khả dụng" })).toBeVisible(); await expect(page.getByRole("radio")).toHaveCount(0);
});
test("HR đề xuất 1–5 khung, validation và gửi giờ +07", async ({ page }) => {
  await auth(page, "HR"); let sent: { startAt: string; endAt: string }[] = [];
  await page.route("**/api/v1/**", route => {
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/rounds/round/slots")) { sent = route.request().postDataJSON(); return route.fulfill({ status: 201, headers: cors, json: { proposalId: "proposal", roundId: "round", slots: [slot] } }); }
    if (path.endsWith("/applications/app")) return route.fulfill({ headers: cors, json: { id: "app", jobId: "job", companyId: "company", companyName: "Rikkei", jobTitle: "Java", status: "INTERVIEW", candidate: { userId: "candidate", fullName: "Ứng viên", email: "a@test.local" }, cvId: "cv", cvSnapshotUrl: "cv", createdAt: "2026-11-19T00:00:00Z", updatedAt: "2026-11-19T00:00:00Z", rounds: [{ id: "round", roundNo: 1, name: "Kỹ thuật", interviewerEmail: "hr@test.local", result: "PENDING", slots: [], schedule: null }], roundRevisions: [], history: [], allowedTransitions: [], offer: null } });
    return path.endsWith("/auth/me") ? route.fallback() : route.fulfill({ headers: cors, json: {} });
  });
  await page.goto("/hr/recruitment/app?tab=interviews");
  await page.getByLabel("Bắt đầu khung 1").fill("2030-01-01T09:00"); await page.getByLabel("Kết thúc khung 1").fill("2030-01-01T09:15");
  await page.getByRole("button", { name: "Gửi đề xuất" }).click(); await expect(page.getByRole("alert").filter({ hasText: "30 phút" })).toBeVisible(); expect(sent).toHaveLength(0);
  await page.getByLabel("Kết thúc khung 1").fill("2030-01-01T09:30"); await page.getByRole("button", { name: "Gửi đề xuất" }).click();
  await expect.poll(() => sent.length).toBe(1); expect(new Date(sent[0].startAt).toISOString()).toBe("2030-01-01T02:00:00.000Z");
  await page.screenshot({ path: "../docs/api/screenshots/ut06-hr-slots.png", fullPage: true });
});
test("HR calendar ignores delayed old week and fetches all pages", async ({ page }) => {
  await auth(page, "HR");
  let firstWeek: string | undefined;
  let releaseOld!: () => void, oldArrived!: () => void, oldFinished!: () => void;
  const hold = new Promise<void>(resolve => { releaseOld = resolve; });
  const arrived = new Promise<void>(resolve => { oldArrived = resolve; });
  const finished = new Promise<void>(resolve => { oldFinished = resolve; });
  await page.route("**/api/v1/**", async route => {
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    const url = new URL(route.request().url());
    if (url.pathname.endsWith("/companies/me/interviews")) {
      const pageNo = Number(url.searchParams.get("page") || 1), from = url.searchParams.get("from")!;
      if (!firstWeek) firstWeek = from;
      const old = from === firstWeek;
      if (old) { oldArrived(); await hold; }
      const startAt = new Date(new Date(from).getTime() + 9 * 3600000).toISOString();
      const items = Array.from({ length: pageNo === 1 ? 100 : 1 }, (_, i) => ({ schedule: { ...schedule, id: `${from}-${pageNo}-${i}`, startAt, endAt: new Date(new Date(startAt).getTime() + 1800000).toISOString() }, candidateId: `c${i}`, candidateName: old ? `Old week ${i}` : pageNo === 2 ? "Last candidate" : `New week ${i}`, jobTitle: "Java", roundName: "Technical", interviewerEmail: "hr@test.local" }));
      try { await route.fulfill({ headers: cors, json: { items, total: 101, page: pageNo, size: 100 } }); }
      catch (error) { if (!old) throw error; } // The old request may have been aborted.
      finally { if (old) oldFinished(); }
      return;
    }
    return url.pathname.endsWith("/auth/me") ? route.fallback() : route.fulfill({ headers: cors, json: {} });
  });
  await page.goto("/hr/interviews"); await arrived;
  const oldRange = await page.getByTestId("calendar-range").innerText();
  await page.getByRole("button", { name: "Tuần sau" }).click();
  await expect(page.getByText("Last candidate", { exact: true })).toBeVisible();
  await expect(page.getByTestId("calendar-range")).not.toHaveText(oldRange);
  releaseOld(); await finished;
  await expect(page.getByText("Old week 0", { exact: true })).toHaveCount(0);
  await expect(page.getByText("New week 0", { exact: true })).toBeVisible();
  await expect(page.getByText("Last candidate", { exact: true })).toBeVisible();
  await page.screenshot({ path: "../docs/api/screenshots/ut06-hr-calendar.png", fullPage: false });
});
