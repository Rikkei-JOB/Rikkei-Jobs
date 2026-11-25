import { test, expect } from "@playwright/test";
import { careerFixtures } from "./candidate-career-fixtures";

const id = "80000000-0000-0000-0000-000000000001";
const first = { id, type: "APPLICATION_STATUS", title: "Hồ sơ đang được sàng lọc", body: "Rikkei Software đã cập nhật hồ sơ của bạn.", href: "/candidate/applications", createdAt: "2026-11-24T02:00:00Z", readAt: null as string | null };

test.beforeEach(async ({ page }) => { await careerFixtures(page); });

test("candidate uses horizontal navigation, reads a notification and updates the badge", async ({ page }) => {
  let readAt: string | null = null;
  await page.route("**/recruitment/me/notifications**", route => {
    const isWrite = route.request().method() === "POST";
    if (isWrite) { readAt = "2026-11-24T02:01:00Z"; return route.fulfill({ status: 204, headers: { "Access-Control-Allow-Origin": "*" } }); }
    return route.fulfill({ headers: { "Access-Control-Allow-Origin": "*" }, json: { items: [{ ...first, readAt }], total: 1, page: 1, size: 10, unreadCount: readAt ? 0 : 1 } });
  });
  await page.goto("/candidate/jobs");
  await expect(page.getByRole("navigation", { name: "Menu ứng viên" })).toBeVisible();
  await expect(page.getByRole("navigation", { name: "Menu chính", exact: true })).toHaveCount(0);
  const bell = page.getByRole("button", { name: "Thông báo, 1 chưa đọc", exact: true });
  await bell.click();
  const panel = page.getByRole("dialog", { name: "Thông báo", exact: true });
  await expect(panel.getByText(first.title)).toBeVisible();
  await panel.getByRole("button", { name: "Đánh dấu đã đọc", exact: true }).click();
  await expect(page.getByRole("button", { name: "Thông báo, 0 chưa đọc", exact: true })).toBeVisible();
  await page.keyboard.press("Escape");
  await page.getByRole("button", { name: "Menu ứng tuyển", exact: true }).click();
  await expect(page.getByRole("menuitem", { name: "Phỏng vấn", exact: true })).toBeVisible();
});

test("mobile menu and bell fit the viewport and close with Escape", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.route("**/recruitment/me/notifications**", route => route.fulfill({ headers: { "Access-Control-Allow-Origin": "*" }, json: { items: [], total: 0, page: 1, size: 10, unreadCount: 0 } }));
  await page.goto("/candidate/jobs");
  const menu = page.getByRole("button", { name: "Mở menu", exact: true });
  await menu.click();
  await expect(page.getByRole("link", { name: "Lộ trình", exact: true })).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(menu).toBeFocused();
  await page.getByRole("button", { name: "Thông báo, 0 chưa đọc", exact: true }).click();
  await expect(page.getByText("Bạn chưa có thông báo")).toBeVisible();
  const box = await page.getByRole("dialog", { name: "Thông báo", exact: true }).boundingBox();
  expect(box!.x).toBeGreaterThanOrEqual(0);
  expect(box!.x + box!.width).toBeLessThanOrEqual(390);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});

test("notification pagination preserves the global unread count and read-all persists", async ({ page }) => {
  let unreadCount = 11;
  await page.route("**/recruitment/me/notifications**", route => {
    if (route.request().method() === "POST") { unreadCount = 0; return route.fulfill({ status: 204, headers: { "Access-Control-Allow-Origin": "*" } }); }
    const pageNumber = Number(new URL(route.request().url()).searchParams.get("page") ?? 1);
    return route.fulfill({ headers: { "Access-Control-Allow-Origin": "*" }, json: {
      items: [{ ...first, id: `${id}-${pageNumber}`, title: `Cập nhật trang ${pageNumber}`, readAt: unreadCount ? null : "2026-11-24T03:00:00Z" }],
      total: 11, unreadCount, page: pageNumber, size: 10,
    } });
  });
  await page.goto("/candidate/jobs");
  await page.getByRole("button", { name: "Thông báo, 11 chưa đọc", exact: true }).click();
  await page.getByRole("button", { name: "Trang thông báo tiếp" }).click();
  await expect(page.getByText("Cập nhật trang 2", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Thông báo, 11 chưa đọc", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Đánh dấu tất cả đã đọc", exact: true }).click();
  await expect(page.getByRole("button", { name: "Thông báo, 0 chưa đọc", exact: true })).toBeVisible();
  await page.reload();
  await page.getByRole("button", { name: "Thông báo, 0 chưa đọc", exact: true }).click();
  await expect(page.getByText("Cập nhật trang 1", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Đánh dấu tất cả đã đọc", exact: true })).toBeDisabled();
});

test("failed read keeps the badge and allows retry before navigation", async ({ page }) => {
  let fail = true, unreadCount = 1;
  await page.route("**/recruitment/me/notifications**", route => {
    const headers = { "Access-Control-Allow-Origin": "*" };
    if (route.request().method() === "POST") {
      if (fail) return route.fulfill({ status: 503, headers, json: { code: "UNAVAILABLE", message: "Chưa thể đánh dấu đã đọc." } });
      unreadCount = 0; return route.fulfill({ status: 204, headers });
    }
    return route.fulfill({ headers, json: { items: [first], total: 1, page: 1, size: 10, unreadCount } });
  });
  await page.goto("/candidate/jobs");
  await page.getByRole("button", { name: "Thông báo, 1 chưa đọc", exact: true }).click();
  const notification = page.getByRole("button").filter({ hasText: first.title });
  await notification.click();
  await expect(page.getByRole("dialog", { name: "Thông báo", exact: true }).getByRole("alert")).toContainText("Chưa thể đánh dấu đã đọc.");
  await expect(page).toHaveURL(/\/candidate\/jobs$/);
  await expect(page.getByRole("button", { name: "Thông báo, 1 chưa đọc", exact: true })).toBeVisible();
  fail = false;
  await notification.click();
  await expect(page).toHaveURL(/\/candidate\/applications$/);
});

test("notification API failure is distinct from an empty inbox and can be retried", async ({ page }) => {
  let fail = true;
  await page.route("**/recruitment/me/notifications**", route => route.fulfill({
    status: fail ? 503 : 200, headers: { "Access-Control-Allow-Origin": "*" },
    json: fail ? { code: "UNAVAILABLE", message: "Không tải được thông báo." } : { items: [], total: 0, page: 1, size: 10, unreadCount: 0 },
  }));
  await page.goto("/candidate/jobs");
  await page.getByRole("button", { name: "Thông báo", exact: true }).click();
  const panel = page.getByRole("dialog", { name: "Thông báo", exact: true });
  await expect(panel.getByRole("alert")).toContainText("Không tải được thông báo.");
  await expect(page.getByText("Bạn chưa có thông báo")).toHaveCount(0);
  fail = false;
  await panel.getByRole("button", { name: "Thử lại", exact: true }).click();
  await expect(page.getByText("Bạn chưa có thông báo")).toBeVisible();
});

test("a delayed read cannot navigate after logout or leak into the next candidate session", async ({ page }) => {
  let release!: () => void, started!: () => void, finished!: () => void;
  const pending = new Promise<void>(resolve => { release = resolve; });
  const readStarted = new Promise<void>(resolve => { started = resolve; });
  const readFinished = new Promise<void>(resolve => { finished = resolve; });
  await page.route("**/recruitment/me/notifications**", async route => {
    const headers = { "Access-Control-Allow-Origin": "*" };
    if (route.request().method() === "POST") {
      started(); await pending;
      try { await route.fulfill({ status: 204, headers }); } finally { finished(); }
      return;
    }
    const nextCandidate = route.request().headers().authorization === "Bearer next-candidate-token";
    await route.fulfill({ headers, json: { items: nextCandidate ? [] : [first], total: nextCandidate ? 0 : 1, page: 1, size: 10, unreadCount: nextCandidate ? 0 : 1 } });
  });
  try {
    await page.goto("/candidate/jobs");
    await page.getByRole("button", { name: "Thông báo, 1 chưa đọc", exact: true }).click();
    await page.getByRole("button").filter({ hasText: first.title }).click();
    await readStarted;
    await page.getByRole("button", { name: "Tài khoản ứng viên", exact: true }).click();
    await page.getByRole("menuitem", { name: "Đăng xuất", exact: true }).click();
    await expect(page).toHaveURL(/\/login$/);
    const lateNavigations: string[] = [];
    page.on("framenavigated", frame => { if (frame === page.mainFrame()) lateNavigations.push(frame.url()); });
    release(); await readFinished;
    await page.waitForTimeout(250); // Allow an erroneous async router.push to surface after the response.
    expect(lateNavigations.some(url => url.includes("/candidate/applications"))).toBe(false);
    const nextUser = { id: "70000000-0000-0000-0000-000000000002", fullName: "Ứng viên tiếp theo", email: "next@example.test", role: "CANDIDATE", companyId: null, mustChangePassword: false };
    await page.addInitScript(user => localStorage.setItem("rikkei-jobs-auth", JSON.stringify({ state: { token: "next-candidate-token", user }, version: 0 })), nextUser);
    await page.route("**/auth/me", route => route.fulfill({ headers: { "Access-Control-Allow-Origin": "*" }, json: nextUser }));
    await page.goto("/candidate/jobs");
    await page.getByRole("button", { name: "Thông báo, 0 chưa đọc", exact: true }).click();
    await expect(page.getByText("Bạn chưa có thông báo")).toBeVisible();
    await expect(page.getByText(first.title, { exact: true })).toHaveCount(0);
  } finally { release(); }
});
