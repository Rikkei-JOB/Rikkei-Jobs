import { test, expect, type Page } from "@playwright/test";
import { mkdir } from "node:fs/promises";
import { application } from "./ut01-fixtures";

const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "Authorization,Content-Type", "Access-Control-Allow-Methods": "GET,OPTIONS" };
const viewports = [{ name: "desktop", width: 1440, height: 1000 }, { name: "tablet", width: 834, height: 1112 }, { name: "mobile", width: 390, height: 844 }];

async function fixture(page: Page, role: "CANDIDATE" | "HR") {
  const user = { id: `banner-${role}`, fullName: role === "HR" ? "Nguyễn Hoàng Minh Anh" : "Nguyễn Văn An", role, email: "preview@example.test", companyId: role === "HR" ? "company-1" : null, mustChangePassword: false };
  await page.clock.install({ time: new Date("2030-01-03T02:00:00Z") });
  await page.addInitScript(user => localStorage.setItem("rikkei-jobs-auth", JSON.stringify({ state: { token: "preview-token", user }, version: 0 })), user);
  await page.route("**/api/v1/**", route => {
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    const path = new URL(route.request().url()).pathname;
    const json = (body: unknown) => route.fulfill({ headers: cors, json: body });
    if (path.endsWith("/auth/me")) return json(user);
    if (path.endsWith("/unread-count")) return json({ unreadCount: 0 });
    if (path.endsWith("/companies/me")) return json({ id: "company-1", name: "Rikkei Technology" });
    if (path.endsWith("/companies/me/jobs")) return json({ items: [], total: 0, page: 1, size: 20 });
    if (path.endsWith("/applications")) return json({ items: [{ ...application, candidateName: "Trần Thị Mai Anh", jobTitle: "Frontend Developer", companyName: "Rikkei Technology", status: "INTERVIEW" }], total: 1, page: 1, size: 20 });
    if (path.endsWith("/interviews")) {
      const schedule = { id: "schedule-1", roundId: "round-1", slotId: "slot-1", startAt: "2030-01-04T02:00:00Z", endAt: "2030-01-04T03:00:00Z", status: "SCHEDULED", meetUrl: null };
      return json({ items: [{ candidateName: "Trần Thị Mai Anh", candidateId: "candidate-1", roundId: "round-1", roundName: "Phỏng vấn chuyên môn", jobTitle: "Frontend Developer", companyName: "Rikkei Technology", interviewerEmail: "recruitment@example.test", slots: [], schedule }], total: 1, page: 1, size: 100 });
    }
    return json({ items: [], total: 0, page: 1, size: 10 });
  });
}

for (const role of ["CANDIDATE", "HR"] as const) for (const kind of ["applications", "interviews"]) {
  test(`task banner remains visible during loading, empty and error: ${role} ${kind}`, async ({ page }) => {
    test.skip(process.env.TASK_BANNER_SCREENSHOTS !== "1", "Explicit banner state checks");
    await fixture(page, role);
    await page.setViewportSize({ width: 360, height: 844 });
    let state = "loading";
    let release!: () => void;
    const ready = new Promise<void>(resolve => { release = resolve; });
    await page.route("**/api/v1/**", async route => {
      if (route.request().method() === "OPTIONS" || !new URL(route.request().url()).pathname.endsWith(`/${kind}`)) return route.fallback();
      if (state === "loading") await ready;
      return route.fulfill({ headers: cors, status: state === "error" ? 500 : 200, json: state === "error" ? { message: "Không thể tải dữ liệu." } : { items: [], total: 0, page: 1, size: 20 } });
    });
    const path = role === "HR" ? `/hr/${kind === "applications" ? "recruitment" : kind}` : `/candidate/${kind}`;
    await page.goto(path);
    await expect(page.locator("main h1")).toBeVisible();
    await expect(page.getByText(/Đang tải (đơn ứng tuyển|hồ sơ|lời mời|lịch)…/)).toBeVisible();
    state = "empty"; release();
    await expect(page.getByText(/Chưa có đơn ứng tuyển phù hợp|Không có lịch cần xử lý|Chưa có hồ sơ phù hợp|Tuần này chưa có lịch phù hợp/)).toBeVisible();
    await expect(page.locator("main h1")).toBeVisible();
    state = "error";
    await page.reload();
    await expect(page.getByRole("button", { name: "Thử lại", exact: true })).toBeVisible();
    await expect(page.locator("main h1")).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  });
}

for (const viewport of viewports) {
  for (const role of ["CANDIDATE", "HR"] as const) {
    test(`task banners ${role} ${viewport.name}`, async ({ page }) => {
      test.skip(process.env.TASK_BANNER_SCREENSHOTS !== "1", "Explicit banner fixture screenshots");
      await fixture(page, role);
      await page.setViewportSize(viewport);
      const phase = process.env.TASK_BANNER_PHASE === "before" ? "before" : "after";
      const folder = `../docs/design/task-banners/${phase}`;
      await mkdir(folder, { recursive: true });
      for (const kind of ["applications", "interviews"]) {
        const path = role === "HR" ? `/hr/${kind === "applications" ? "recruitment" : "interviews"}` : `/candidate/${kind}${kind === "interviews" ? "?view=upcoming" : ""}`;
        await page.goto(path);
        await expect(page.locator("main h1")).toHaveCount(1);
        await expect(page.locator("main h1")).toBeVisible();
        const content = role === "HR" ? "Trần Thị Mai Anh" : kind === "interviews" ? "Phỏng vấn chuyên môn" : "Frontend Developer";
        await expect(page.getByText(content, { exact: true }).filter({ visible: true }).first()).toBeVisible();
        await page.evaluate(() => document.fonts.ready);
        await page.addStyleTag({ content: "nextjs-portal{display:none!important}" });
        expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
        await page.screenshot({ path: `${folder}/${role.toLowerCase()}-${kind}-${viewport.name}.png`, fullPage: true });
        if (role === "HR" && kind === "interviews") {
          await page.getByRole("button", { name: "Danh sách", exact: true }).focus();
          await page.keyboard.press("Enter");
          await expect(page.getByRole("button", { name: "Danh sách", exact: true })).toHaveAttribute("aria-pressed", "true");
          await page.getByRole("button", { name: "Tuần", exact: true }).click();
          await expect(page.getByRole("button", { name: "Tuần", exact: true })).toHaveAttribute("aria-pressed", "true");
        }
        if (role === "CANDIDATE" && kind === "applications") await expect(page.getByRole("link", { name: "Tìm việc làm mới" })).toHaveAttribute("href", "/candidate/jobs");
        await page.setViewportSize({ width: 360, height: 844 });
        expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
        await page.setViewportSize(viewport);
      }
    });
  }
}
