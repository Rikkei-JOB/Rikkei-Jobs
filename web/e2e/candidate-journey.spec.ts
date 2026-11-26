import { test, expect, type Page } from "@playwright/test";
import { mkdir } from "node:fs/promises";
import { application, applicationId, signIn, userId } from "./ut01-fixtures";
import type { Offer } from "../src/interface/offer";
import { chooseOption } from "./select-menu";

const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "Authorization,Content-Type", "Access-Control-Allow-Methods": "GET,POST,OPTIONS" };
const baseOffer: Offer = { id: "offer-a", applicationId, position: "Frontend Developer", salary: 18000000, startDate: "2030-02-01", workLocation: "Hà Nội · Văn phòng Rikkei", note: "Tham gia xây dựng sản phẩm công nghệ cùng đội ngũ phát triển.\nThời gian làm việc: Thứ Hai – Thứ Sáu.", responseDeadline: "2030-01-10T02:00:00Z", status: "SENT", respondedAt: null, declineReason: null, createdAt: "2026-11-25T01:00:00Z" };
const slot = { id: "slot-a", roundId: "round-a", proposalId: "p1", startAt: "2030-01-05T02:00:00Z", endAt: "2030-01-05T03:00:00Z", status: "OPEN" };
const invite = { roundId: "round-a", roundName: "Phỏng vấn chuyên môn", jobTitle: "Frontend Developer", companyName: "Rikkei Software", interviewerEmail: "recruitment@rikkei.example", slots: [slot], schedule: null };

async function setup(page: Page, options: { mutation?: "error" | "network" | "conflict"; deadline?: string } = {}) {
  await signIn(page);
  let current = { ...baseOffer, responseDeadline: options.deadline ?? baseOffer.responseDeadline };
  let picked = false;
  let posts = 0;
  await page.route("**/api/v1/**", async route => {
    const req = route.request(), url = new URL(req.url()), path = url.pathname;
    const json = (body: unknown, status = 200) => route.fulfill({ headers: cors, status, json: body });
    if (req.method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    if (path.endsWith("/auth/me")) return json({ id: userId, fullName: "Nguyễn Văn An", email: "candidate@example.test", role: "CANDIDATE", companyId: null, mustChangePassword: false });
    if (path.endsWith("/notifications/unread-count")) return json({ unreadCount: 0 });
    if (path.endsWith("/notifications")) return json({ items: [], total: 0, page: 1, size: 10 });
    if (path.endsWith(`/me/applications/${applicationId}`)) return json({ ...application, status: "INTERVIEW", jobTitle: "Frontend Developer" });
    if (path.endsWith("/me/applications")) return json({ items: [{ ...application, status: url.searchParams.get("status") ?? "INTERVIEW", jobTitle: "Frontend Developer" }], total: 24, page: Number(url.searchParams.get("page") ?? 1), size: 20 });
    if (path.endsWith("/pick")) { picked = true; return json({}); }
    if (path.endsWith("/me/interviews")) return json({ items: [
      { ...invite, slots: picked ? [] : [slot], schedule: picked ? { ...slot, id: "schedule-a", slotId: slot.id, status: "SCHEDULED", meetUrl: "https://meet.example/room" } : null },
      { ...invite, roundId: "round-b", roundName: "Trao đổi văn hoá", slots: [], schedule: { ...slot, status: "SCHEDULED", meetUrl: "https://meet.example/culture" } },
      { ...invite, roundId: "round-c", roundName: "Lịch cũ", slots: [], schedule: { ...slot, startAt: "2020-01-01T02:00:00Z", endAt: "2020-01-01T03:00:00Z", status: "SCHEDULED", meetUrl: "https://meet.example/old" } },
    ], total: 3, page: 1, size: 100 });
    if (path.endsWith("/accept") || path.endsWith("/decline")) {
      posts++;
      if (options.mutation === "error") return json({ message: "Không thể phản hồi lúc này." }, 500);
      if (options.mutation === "conflict") { current = { ...current, status: "EXPIRED" }; return json({ message: "Offer đã hết hạn." }, 409); }
      current = { ...current, status: path.endsWith("/accept") ? "ACCEPTED" : "DECLINED", respondedAt: "2026-11-25T02:00:00Z", declineReason: req.postDataJSON()?.reason ?? null };
      if (options.mutation === "network") return route.abort("failed");
      return json(current);
    }
    if (path.endsWith("/me/offers/offer-a")) return json(current);
    if (path.endsWith("/me/offers")) return json({ items: [current], total: 12, page: Number(url.searchParams.get("page") ?? 1), size: 10 });
    return json({});
  });
  return { posts: () => posts };
}

test("application detail returns to filtered page and rejects external returnTo", async ({ page }) => {
  await setup(page);
  await page.goto("/candidate/applications?status=INTERVIEW&page=2");
  await page.getByRole("link", { name: "Xem tiến trình" }).click();
  await expect(page.getByRole("heading", { name: "Tài liệu đã nộp" })).toBeVisible();
  await page.getByRole("link", { name: "Ứng tuyển của tôi", exact: true }).click();
  await expect(page).toHaveURL(/status=INTERVIEW&page=2/);
  await page.goto(`/candidate/applications/${applicationId}?returnTo=https://evil.example`);
  await expect(page.getByRole("link", { name: "Ứng tuyển của tôi", exact: true })).toHaveAttribute("href", "/candidate/applications");
});

test("interviews distinguish upcoming, past appointment and picking without switching tab", async ({ page }) => {
  await setup(page);
  await page.goto("/candidate/interviews");
  await expect(page.getByRole("tab", { name: /Cần xử lý/ })).toHaveAttribute("aria-selected", "true");
  await page.getByRole("radio").check();
  await page.getByRole("button", { name: "Xác nhận khung giờ" }).click();
  await expect(page.getByRole("status").filter({ hasText: "Đã xác nhận khung giờ" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Xem lịch sắp diễn ra" })).toBeFocused();
  await expect(page.getByRole("tab", { name: /Cần xử lý/ })).toHaveAttribute("aria-selected", "true");
  await page.getByRole("tab", { name: /Sắp diễn ra/ }).click();
  await expect(page.getByRole("link", { name: "Vào phòng phỏng vấn" })).toHaveCount(2);
  await page.getByRole("tab", { name: /Lịch sử/ }).click();
  await expect(page.getByText("Đã qua giờ hẹn")).toBeVisible();
  await expect(page.getByRole("link", { name: "Vào phòng phỏng vấn" })).toHaveCount(0);
});

test("offer needs confirmation and returns focus to result after accepting", async ({ page }) => {
  const state = await setup(page);
  await page.goto("/candidate/offers/offer-a");
  await page.getByRole("button", { name: "Nhận offer", exact: true }).click();
  const dialog = page.getByRole("dialog");
  await expect(dialog.getByRole("heading", { name: "Xác nhận nhận offer" })).toBeVisible();
  expect(state.posts()).toBe(0);
  await dialog.getByRole("button", { name: "Xác nhận nhận offer", exact: true }).click();
  await expect(dialog).toHaveCount(0);
  await expect(page.getByText("Đã nhận", { exact: true })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Phản hồi offer" })).toBeFocused();
  expect(state.posts()).toBe(1);
});

test("decline preserves reason on error and reopening", async ({ page }) => {
  await setup(page, { mutation: "error" });
  await page.goto("/candidate/offers/offer-a");
  await expect(page.getByLabel("Lý do từ chối", { exact: true })).toHaveCount(0);
  await page.getByRole("button", { name: "Từ chối offer", exact: true }).click();
  await page.getByLabel("Lý do từ chối", { exact: true }).fill("Tôi đã chọn cơ hội phù hợp hơn.");
  await page.getByRole("button", { name: "Xác nhận từ chối", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText("Không thể phản hồi");
  await page.getByRole("button", { name: "Quay lại", exact: true }).click();
  await page.getByRole("button", { name: "Từ chối offer", exact: true }).click();
  await expect(page.getByLabel("Lý do từ chối", { exact: true })).toHaveValue("Tôi đã chọn cơ hội phù hợp hơn.");
});

test("network outcome must be checked before another response", async ({ page }) => {
  const state = await setup(page, { mutation: "network" });
  await page.goto("/candidate/offers/offer-a");
  await page.getByRole("button", { name: "Nhận offer", exact: true }).click();
  await page.getByRole("button", { name: "Xác nhận nhận offer", exact: true }).click();
  await expect(page.getByRole("button", { name: "Xác nhận nhận offer", exact: true })).toBeDisabled();
  await page.getByRole("button", { name: "Kiểm tra kết quả", exact: true }).click();
  await expect(page.getByText("Đã nhận", { exact: true })).toBeVisible();
  expect(state.posts()).toBe(1);
});

test("offline reconciliation preserves unknown outcome and dialog can still close", async ({ page, context }) => {
  await setup(page, { mutation: "network" });
  await page.goto("/candidate/offers/offer-a");
  await page.getByRole("button", { name: "Nhận offer", exact: true }).click();
  await page.getByRole("button", { name: "Xác nhận nhận offer", exact: true }).click();
  await expect(page.getByRole("button", { name: "Kiểm tra kết quả", exact: true })).toBeVisible();
  await context.setOffline(true);
  await page.getByRole("button", { name: "Kiểm tra kết quả", exact: true }).click();
  await expect(page.getByRole("button", { name: "Quay lại", exact: true })).toBeEnabled();
  await page.keyboard.press("Escape");
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Nhận offer", exact: true })).toBeDisabled();
  await context.setOffline(false);
});

test("ending a focused interview restores focus to the selected tab", async ({ page }) => {
  await page.clock.install({ time: new Date("2030-01-05T02:59:30Z") });
  await setup(page);
  await page.goto("/candidate/interviews?view=upcoming");
  await page.getByRole("link", { name: "Vào phòng phỏng vấn" }).focus();
  await page.clock.fastForward(61000);
  await expect(page.getByRole("tab", { name: /Sắp diễn ra/ })).toBeFocused();
  await expect(page.getByRole("tab", { name: /Sắp diễn ra/ })).toHaveAttribute("aria-selected", "true");
});

test("deadline locks confirmation while dialog is open", async ({ page }) => {
  await page.clock.install({ time: new Date("2029-01-01T00:00:00Z") });
  const state = await setup(page, { deadline: "2029-01-01T00:02:00Z" });
  await page.goto("/candidate/offers/offer-a");
  await page.getByRole("button", { name: "Nhận offer", exact: true }).click();
  await page.clock.fastForward(121000);
  await expect(page.getByRole("button", { name: "Xác nhận nhận offer", exact: true })).toBeDisabled();
  expect(state.posts()).toBe(0);
});

test("initial response arriving after deadline is reconciled immediately", async ({ page }) => {
  await page.clock.install({ time: new Date("2029-01-01T00:00:00Z") });
  await setup(page);
  let reads = 0;
  let release!: () => void;
  const hold = new Promise<void>(resolve => { release = resolve; });
  await page.route("**/api/v1/recruitment/me/offers/offer-a", async route => {
    reads++;
    if (reads === 1) await hold;
    return route.fulfill({ headers: cors, json: { ...baseOffer, responseDeadline: "2029-01-01T00:00:02Z" } });
  });
  await page.goto("/candidate/offers/offer-a");
  await expect.poll(() => reads).toBe(1);
  await page.clock.fastForward(4000); release();
  await expect(page.getByText("Đã hết hạn trả lời")).toBeVisible();
  await expect(page.getByRole("button", { name: "Nhận offer", exact: true })).toHaveCount(0);
  await expect.poll(() => reads).toBe(2);
});

test("late deadline GET cannot overwrite authoritative accepted offer", async ({ page }) => {
  await page.clock.install({ time: new Date("2029-01-01T00:00:00Z") });
  await setup(page);
  let reads = 0, posted = false;
  let releaseGet!: () => void, releasePost!: () => void;
  const getHold = new Promise<void>(resolve => { releaseGet = resolve; });
  const postHold = new Promise<void>(resolve => { releasePost = resolve; });
  await page.route("**/api/v1/recruitment/me/offers/offer-a", async route => {
    reads++;
    if (reads > 1) await getHold;
    try { await route.fulfill({ headers: cors, json: { ...baseOffer, responseDeadline: "2029-01-01T00:00:10Z" } }); } catch { /* request may be cancelled */ }
  });
  await page.route("**/api/v1/recruitment/me/offers/offer-a/accept", async route => {
    posted = true; await postHold;
    return route.fulfill({ headers: cors, json: { ...baseOffer, status: "ACCEPTED", responseDeadline: "2029-01-01T00:00:10Z" } });
  });
  await page.goto("/candidate/offers/offer-a");
  await page.getByRole("button", { name: "Nhận offer", exact: true }).click();
  await page.getByRole("button", { name: "Xác nhận nhận offer", exact: true }).click();
  await expect.poll(() => posted).toBe(true);
  await page.clock.fastForward(11000);
  await expect.poll(() => reads).toBe(2);
  releasePost(); await expect(page.getByText("Đã nhận", { exact: true })).toBeVisible();
  releaseGet();
  await page.waitForTimeout(300);
  await expect(page.getByText("Đã nhận", { exact: true })).toBeVisible();
});

test("email offer returns to its application and mobile does not overflow", async ({ page }) => {
  await setup(page);
  await page.setViewportSize({ width: 360, height: 800 });
  await page.goto("/candidate/offers/offer-a");
  await page.getByRole("link", { name: "Quay lại hồ sơ ứng tuyển", exact: true }).click();
  await expect(page).toHaveURL(new RegExp(`/candidate/applications/${applicationId}$`));
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});

test("cancelled and unsafe meeting links cannot be opened; obsolete slots are hidden", async ({ page }) => {
  await setup(page);
  await page.route("**/api/v1/recruitment/me/interviews*", route => route.fulfill({ headers: cors, json: { items: [
    { ...invite, roundId: "cancelled", roundName: "Lịch đã huỷ", slots: [{ ...slot, status: "CANCELLED" }, { ...slot, id: "past", startAt: "2020-01-01T00:00:00Z" }], schedule: { ...slot, status: "CANCELLED", meetUrl: "https://old.example" } },
    { ...invite, roundId: "unsafe", roundName: "Lịch chưa có link", slots: [], schedule: { ...slot, status: "SCHEDULED", meetUrl: "javascript:alert(1)" } },
  ], total: 2, page: 1, size: 100 } }));
  await page.goto("/candidate/interviews");
  await expect(page.getByText("Đã huỷ", { exact: true })).toBeVisible();
  await expect(page.getByRole("radio")).toHaveCount(0);
  await expect(page.getByRole("link", { name: "Vào phòng phỏng vấn" })).toHaveCount(0);
  await page.getByRole("tab", { name: /Sắp diễn ra/ }).click();
  await expect(page.getByText("Đường dẫn phòng phỏng vấn chưa được cập nhật.")).toBeVisible();
  await expect(page.getByRole("link", { name: "Vào phòng phỏng vấn" })).toHaveCount(0);
});

test("terminal application has no current step and long mobile content stays contained", async ({ page }) => {
  await setup(page);
  await page.route(`**/api/v1/recruitment/me/applications/${applicationId}`, route => route.fulfill({ headers: cors, json: { ...application, status: "REJECTED", canWithdraw: false, jobTitle: "Lập trình viên phát triển nền tảng công nghệ và ứng dụng doanh nghiệp", coverLetter: "NộiDungLiền".repeat(60), history: [...application.history, { id: "h2", fromStatus: "SCREENING", toStatus: "REJECTED", actorId: userId, note: null, createdAt: "2026-11-25T01:00:00Z" }] } }));
  await page.setViewportSize({ width: 360, height: 800 });
  await page.goto(`/candidate/applications/${applicationId}`);
  await expect(page.getByText("Đơn đã kết thúc: Không đạt.")).toBeVisible();
  await expect(page.locator('[aria-current="step"]')).toHaveCount(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});

test("filter errors hide stale application rows and can retry", async ({ page }) => {
  await setup(page);
  await page.route("**/api/v1/recruitment/me/applications?*", route => {
    if (new URL(route.request().url()).searchParams.get("status") === "REJECTED") return route.fulfill({ status: 400, headers: cors, json: { message: "Không tải được danh sách." } });
    return route.fallback();
  });
  await page.goto("/candidate/applications");
  await expect(page.getByRole("link", { name: "Xem tiến trình" })).toBeVisible();
  await chooseOption(page.getByLabel("Lọc theo trạng thái"), "REJECTED");
  // Lọc riêng thông báo lỗi: vùng thông báo chuyển trang của Next.js cũng có role="alert".
  await expect(page.getByRole("alert").filter({ hasText: "Không tải được danh sách" })).toBeVisible();
  await expect(page.getByRole("link", { name: "Xem tiến trình" })).toHaveCount(0);
  await chooseOption(page.getByLabel("Lọc theo trạng thái"), "INTERVIEW");
  await expect(page.getByRole("link", { name: "Xem tiến trình" })).toBeVisible();
});

test("offer context failure does not block response and Escape restores trigger focus", async ({ page }) => {
  await setup(page);
  await page.route(`**/api/v1/recruitment/me/applications/${applicationId}`, route => route.fulfill({ status: 404, headers: cors, json: { message: "Không tìm thấy hồ sơ." } }));
  await page.goto("/candidate/offers/offer-a");
  await expect(page.getByRole("link", { name: "Xem hồ sơ ứng tuyển" })).toBeVisible();
  await page.getByRole("button", { name: "Nhận offer", exact: true }).click();
  await page.keyboard.press("Escape");
  await expect(page.getByRole("button", { name: "Nhận offer", exact: true })).toBeFocused();
});

test("withdrawal conflict keeps dialog and reason after refreshing application", async ({ page }) => {
  await setup(page);
  await page.route(`**/api/v1/recruitment/me/applications/${applicationId}/withdraw`, route => route.fulfill({ status: 409, headers: cors, json: { message: "Hồ sơ vừa được cập nhật. Vui lòng kiểm tra lại." } }));
  await page.goto(`/candidate/applications/${applicationId}`);
  await page.getByRole("button", { name: "Rút đơn", exact: true }).click();
  await page.getByLabel("Lý do rút đơn").fill("Thay đổi kế hoạch nghề nghiệp");
  const refreshed = page.waitForResponse(r => r.url().endsWith(`/me/applications/${applicationId}`) && r.request().method() === "GET");
  await page.getByRole("button", { name: "Xác nhận rút đơn", exact: true }).click();
  await refreshed;
  await expect(page.getByRole("dialog")).toBeVisible();
  await expect(page.getByLabel("Lý do rút đơn")).toHaveValue("Thay đổi kế hoạch nghề nghiệp");
});

for (const viewport of [{ name: "desktop", width: 1440, height: 1000 }, { name: "tablet", width: 834, height: 1112 }, { name: "mobile", width: 390, height: 844 }]) {
  test(`journey screenshots ${viewport.name}`, async ({ page }) => {
    test.skip(process.env.JOURNEY_SCREENSHOTS !== "1", "Explicit fixture screenshots");
    await page.clock.install({ time: new Date("2030-01-03T02:00:00Z") });
    await setup(page); await page.setViewportSize(viewport);
    await mkdir("../docs/design/candidate-journey", { recursive: true });
    for (const [name, path] of [["applications", "/candidate/applications"], ["application-detail", `/candidate/applications/${applicationId}`], ["interviews-action", "/candidate/interviews"], ["interviews-upcoming", "/candidate/interviews?view=upcoming"], ["offer-detail", "/candidate/offers/offer-a"]]) {
      await page.goto(path); await expect(page.locator(".journey h1")).toBeVisible();
      await expect(page.locator(".journey [aria-busy=true]")).toHaveCount(0);
      await page.addStyleTag({ content: "nextjs-portal{display:none!important}" });
      await page.screenshot({ path: `../docs/design/candidate-journey/${name}-${viewport.name}.png`, fullPage: true });
    }
    await page.getByRole("button", { name: "Từ chối offer", exact: true }).click();
    await page.getByLabel("Lý do từ chối", { exact: true }).fill("Cảm ơn doanh nghiệp. Tôi đã chọn một cơ hội khác phù hợp hơn.");
    await expect(page.getByRole("button", { name: "Xác nhận từ chối", exact: true })).toBeEnabled();
    await page.screenshot({ path: `../docs/design/candidate-journey/offer-dialog-${viewport.name}.png`, fullPage: false });
  });
}
