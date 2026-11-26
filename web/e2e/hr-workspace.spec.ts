import { test, expect, type Page } from "@playwright/test";
import { chooseOption, expectOption } from "./select-menu";

const appId = "20000000-0000-0000-0000-000000000002";
const jobId = "50000000-0000-0000-0000-000000000002";
const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "Authorization,Content-Type", "Access-Control-Allow-Methods": "GET,POST,PUT,OPTIONS" };

async function fixture(page: Page) {
  let status = "SCREENING";
  const user = { id: "hr-workspace", role: "HR", fullName: "Nguyễn Hoàng Minh Anh", companyId: "company-1", mustChangePassword: false, email: "hr@example.test" };
  await page.addInitScript(user => localStorage.setItem("rikkei-jobs-auth", JSON.stringify({ state: { token: "test-token", user }, version: 0 })), user);
  await page.route("**/api/v1/**", route => {
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    const url = new URL(route.request().url());
    const path = url.pathname;
    const json = (data: unknown) => route.fulfill({ headers: cors, json: data });
    if (path.endsWith("/auth/me")) return json(user);
    if (path.endsWith(`/applications/${appId}/transition`)) { status = route.request().postDataJSON().toStatus; return json({ id: appId, status }); }
    if (path.endsWith("/companies/me/jobs")) {
      const page = Number(url.searchParams.get("page") ?? 1);
      return json({ items: page === 1 ? [{ id: "job-first", title: "Frontend Developer", status: "APPROVED", level: "JUNIOR", deadline: "2026-12-30", updatedAt: "2026-11-25T00:00:00Z" }] : [{ id: jobId, title: "Java Developer", status: "APPROVED", level: "JUNIOR", deadline: "2026-12-30", updatedAt: "2026-11-25T00:00:00Z" }], total: 2, page, size: 1 });
    }
    if (path.endsWith(`/jobs/${jobId}`)) return json({ id: jobId, title: "Java Developer" });
    if (path.endsWith("/hr/applications")) return json({ items: [{ id: appId, candidateName: "Trần Thị Mai Anh", jobTitle: "Java Developer", companyName: "Rikkei Technology", status, createdAt: "2026-11-25T02:00:00Z" }], total: 1, page: 1, size: 20 });
    if (path.endsWith(`/applications/${appId}`)) return json({ id: appId, jobId, jobTitle: "Java Developer", companyId: "company-1", companyName: "Rikkei Technology", status, createdAt: "2026-11-25T02:00:00Z", candidate: { userId: "candidate-1", fullName: "Trần Thị Mai Anh", email: "mai.anh@example.test", phone: "0901234567" }, coverLetter: "Em mong muốn phát triển kỹ năng Java và đóng góp cho đội ngũ sản phẩm.", match: null, matchStatus: "UNAVAILABLE", rounds: [], roundRevisions: [], history: [{ id: "h1", toStatus: "SUBMITTED", createdAt: "2026-11-25T02:00:00Z" }], allowedTransitions: ["INTERVIEW", "REJECTED"] });
    if (path.endsWith("/companies/me")) return json({ id: "company-1", name: "Rikkei Technology", taxCode: "0101234567", logoUrl: null, status: "ACTIVE", industry: "Công nghệ thông tin", city: "Hà Nội", website: "https://example.test", size: "201-1000", description: "Phát triển sản phẩm công nghệ và kết nối nhân tài.", address: "Hà Nội", createdAt: "2026-11-25T02:00:00Z", updatedAt: "2026-11-25T02:00:00Z" });
    if (path.includes("/interviews")) {
      const start = Date.parse(url.searchParams.get("from") ?? "2026-11-23T00:00:00+07:00") + 9 * 3600000;
      const items = ["Trần Thị Mai Anh", "Nguyễn Đức Minh"].map((name, i) => ({ candidateName: name, candidateId: `candidate-${i}`, jobTitle: "Java Developer", roundName: "Phỏng vấn kỹ thuật", interviewerEmail: "hr@example.test", schedule: { id: `schedule-${i}`, roundId: `round-${i}`, slotId: `slot-${i}`, startAt: new Date(start + i * 86400000).toISOString(), endAt: new Date(start + i * 86400000 + 3600000).toISOString(), meetUrl: null, status: "SCHEDULED" } }));
      return json({ items, total: items.length, page: 1, size: 100 });
    }
    return json({});
  });
}

test("HR desktop top bar and account menu preserve role destinations", async ({ page }) => {
  await fixture(page);
  const notifications: string[] = [];
  page.on("request", request => { if (request.url().includes("/me/notifications")) notifications.push(request.url()); });
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.goto("/hr/recruitment");
  const nav = page.getByRole("navigation", { name: "Menu HR", exact: true });
  await expect(nav.getByRole("link", { name: "Hồ sơ ứng tuyển" })).toHaveAttribute("aria-current", "page");
  await expect(nav.getByRole("link", { name: "Tin tuyển dụng" })).toBeVisible();
  await page.getByRole("button", { name: "Tài khoản HR" }).click();
  await expect(page.getByRole("menuitem", { name: "Thông tin công ty" })).toHaveAttribute("href", "/hr/company");
  await expect(page.getByRole("menuitem", { name: "Đổi mật khẩu" })).toHaveAttribute("href", "/change-password");
  await page.keyboard.press("Escape");
  await expect(page.getByRole("button", { name: "Tài khoản HR" })).toBeFocused();
  expect(notifications).toEqual([]);
});

test("HR position filter loads later JD pages and preserves the list on return", async ({ page }) => {
  await fixture(page);
  await page.goto("/hr/recruitment");
  await chooseOption(page.getByLabel("Vị trí tuyển dụng", { exact: true }), jobId);
  await expect(page).toHaveURL(new RegExp(`jobId=${jobId}`));
  await page.getByRole("link", { name: /Xem hồ sơ.*Trần Thị Mai Anh/ }).click();
  await expect(page.getByRole("heading", { level: 1, name: "Trần Thị Mai Anh" })).toBeVisible();
  await page.getByLabel("Ghi chú chuyển bước").fill("Mời trao đổi kỹ thuật");
  await page.getByRole("tab", { name: /^Phỏng vấn/ }).click();
  await expect(page).toHaveURL(/tab=interviews/);
  await page.getByRole("tab", { name: /^Hồ sơ/ }).click();
  await expect(page.getByLabel("Ghi chú chuyển bước")).toHaveValue("Mời trao đổi kỹ thuật");
  await page.getByRole("link", { name: /Danh sách đơn ứng tuyển/ }).click();
  await expectOption(page.getByLabel("Vị trí tuyển dụng", { exact: true }), jobId);
  await page.getByRole("button", { name: "Xoá bộ lọc" }).click();
  await expectOption(page.getByLabel("Vị trí tuyển dụng", { exact: true }), "");
});

test("HR mobile navigation dismisses on route change and fits 360px", async ({ page }) => {
  await fixture(page);
  await page.setViewportSize({ width: 360, height: 780 });
  await page.goto("/hr/recruitment");
  await page.getByRole("button", { name: "Mở menu HR" }).click();
  await page.getByRole("navigation", { name: "Menu HR di động" }).getByRole("link", { name: "Tin tuyển dụng" }).click();
  await expect(page).toHaveURL(/\/hr\/jobs/);
  await expect(page.getByRole("navigation", { name: "Menu HR di động" })).toHaveCount(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
});

test("HR returning from a workflow mutation refreshes the list immediately", async ({ page }) => {
  await fixture(page);
  await page.goto("/hr/recruitment");
  await page.getByRole("link", { name: /Xem hồ sơ.*Trần Thị Mai Anh/ }).click();
  await page.getByRole("button", { name: "Chuyển sang Phỏng vấn", exact: true }).click();
  await expect(page.getByText("Phỏng vấn", { exact: true }).first()).toBeVisible();
  await page.getByRole("link", { name: "Danh sách đơn ứng tuyển", exact: true }).click();
  await expect(page.getByRole("cell", { name: "Phỏng vấn", exact: true })).toBeVisible();
});

test("HR ignores an external list return destination", async ({ page }) => {
  await fixture(page);
  await page.goto(`/hr/recruitment/${appId}?returnTo=https%3A%2F%2Fexample.test`);
  await expect(page.getByRole("link", { name: /Danh sách đơn ứng tuyển/ })).toHaveAttribute("href", "/hr/recruitment");
});

test("HR keeps a selected job absent from list pages and resolves its title", async ({ page }) => {
  await fixture(page);
  await page.route("**/api/v1/jobs/job-archived", route => route.fulfill({ headers: cors, json: { id: "job-archived", title: "Backend Engineer (đã đóng)" } }));
  await page.goto("/hr/recruitment?jobId=job-archived&status=SCREENING");
  const select = page.getByLabel("Vị trí tuyển dụng", { exact: true });
  await expectOption(select, "job-archived");
  await expect(select).toContainText("Backend Engineer (đã đóng)");
  await page.reload();
  await expectOption(select, "job-archived");
});

test("HR filter failure offers retry without showing old rows as current", async ({ page }) => {
  await fixture(page);
  let failed = true;
  await page.route("**/api/v1/recruitment/hr/applications?**", route => {
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    if (new URL(route.request().url()).searchParams.get("status") === "REJECTED" && failed) return route.fulfill({ status: 503, headers: cors, json: { message: "Không tải được hồ sơ" } });
    return route.fallback();
  });
  await page.goto("/hr/recruitment");
  await expect(page.getByRole("link", { name: /Xem hồ sơ.*Trần Thị Mai Anh/ })).toBeVisible();
  await chooseOption(page.getByLabel("Trạng thái", { exact: true }), "REJECTED");
  await expect(page.getByRole("alert").filter({ hasText: "Không tải được hồ sơ" })).toBeVisible();
  await expect(page.getByRole("link", { name: /Xem hồ sơ.*Trần Thị Mai Anh/ })).toHaveCount(0);
  failed = false;
  await page.getByRole("button", { name: "Thử lại", exact: true }).click();
  await expect(page.getByRole("link", { name: /Xem hồ sơ.*Trần Thị Mai Anh/ })).toBeVisible();
  await page.goBack();
  await expectOption(page.getByLabel("Trạng thái", { exact: true }), "");
});

test("HR calendar has a mobile agenda and a desktop list switch", async ({ page }) => {
  await fixture(page);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/hr/interviews");
  await expect(page.getByRole("button", { name: "Danh sách", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Danh sách", exact: true }).click();
  await expect(page.getByRole("button", { name: "Danh sách", exact: true })).toHaveAttribute("aria-pressed", "true");
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await expect(page.getByText("Trần Thị Mai Anh", { exact: true })).toBeVisible();
  const grid = page.getByRole("region", { name: "Lịch trong tuần" }).locator(":scope > div");
  expect(await grid.evaluate(element => element.scrollWidth <= element.clientWidth)).toBe(true);
});

for (const [name, width, height] of [["desktop", 1440, 1000], ["tablet", 834, 1112], ["mobile", 390, 844]] as const) {
  test(`HR screenshots ${name} (API fixtures)`, async ({ page }) => {
    test.skip(!process.env.HR_SCREENSHOTS, "Explicit visual capture only");
    await fixture(page);
    await page.setViewportSize({ width, height });
    for (const [screen, path] of [["applications", "/hr/recruitment"], ["detail", `/hr/recruitment/${appId}`], ["jobs", "/hr/jobs"], ["company", "/hr/company"], ["calendar", "/hr/interviews"]]) {
      await page.goto(path);
      await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
      await expect(page.getByText(/Đang tải/)).toHaveCount(0);
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
      await page.screenshot({ path: `../docs/design/hr-workspace/${screen}-${name}.png`, fullPage: true, style: "nextjs-portal { display: none !important; }" });
    }
  });
}
