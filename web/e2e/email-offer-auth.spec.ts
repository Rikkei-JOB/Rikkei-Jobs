import { test, expect, type Page } from "@playwright/test";
import { application, applicationId, userId } from "./ut01-fixtures";

const path = "/candidate/offers/email-offer";
const user = { id: userId, fullName: "Nguyễn Văn An", email: "candidate@example.test", role: "CANDIDATE", companyId: null, mustChangePassword: false };
const offer = { id: "email-offer", applicationId, position: "Java Developer", salary: 12000000, startDate: "2030-02-01", workLocation: "Hà Nội", note: null, responseDeadline: "2030-01-03T00:00:00Z", status: "SENT", respondedAt: null, declineReason: null, createdAt: "2026-11-25T00:00:00Z" };
const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "Authorization,Content-Type", "Access-Control-Allow-Methods": "GET,POST,OPTIONS" };

async function fixtures(page: Page, options: { initial?: "candidate" | "other" | "hr"; forced?: boolean; expirePost?: boolean; ssoOther?: boolean } = {}) {
  let actor = options.initial === "hr" ? { ...user, id: "hr-user", role: "HR", companyId: "company" as string | null } : options.initial === "other" ? { ...user, id: "other-candidate" } : { ...user };
  let currentOffer = { ...offer };
  let posts = 0, reads = 0;
  if (options.initial) await page.addInitScript(initialUser => {
    if (!sessionStorage.getItem("email-fixture-initialised")) {
      sessionStorage.setItem("email-fixture-initialised", "1");
      localStorage.setItem("rikkei-jobs-auth", JSON.stringify({ state: { token: "old-token", user: initialUser }, version: 0 }));
    }
  }, actor);
  await page.route("**/api/v1/**", route => {
    const req = route.request(), url = new URL(req.url());
    const json = (body: unknown, status = 200) => route.fulfill({ headers: cors, status, json: body });
    if (req.method() === "OPTIONS") return route.fulfill({ headers: cors, status: 204 });
    if (url.pathname.endsWith("/auth/sso")) {
      actor = { ...user, id: options.ssoOther ? "other-candidate" : user.id, mustChangePassword: Boolean(options.forced) };
      return json({ accessToken: options.ssoOther ? "other-token" : "new-token", user: actor });
    }
    if (url.pathname.endsWith("/auth/change-password")) { actor = { ...user }; return json({ accessToken: "changed-token", message: "Đã đổi mật khẩu" }); }
    if (url.pathname.endsWith("/auth/me")) return json(req.headers().authorization === "Bearer other-token" ? { ...user, id: "other-candidate" } : actor);
    if (url.pathname.endsWith("/me/offers/email-offer/accept")) {
      posts++; currentOffer = { ...currentOffer, status: "ACCEPTED" };
      return options.expirePost && posts === 1 ? json({ message: "Phiên đăng nhập đã hết hạn." }, 401) : json(currentOffer);
    }
    if (url.pathname.endsWith("/me/offers/email-offer")) { reads++; return actor.id === user.id && req.headers().authorization !== "Bearer other-token" ? json(currentOffer) : json({ message: "Không tìm thấy offer." }, 404); }
    if (url.pathname.endsWith("/me/offers")) return json({ items: [currentOffer], total: 1, page: 1, size: 10 });
    if (url.pathname.endsWith(`/me/applications/${applicationId}`)) return json({ ...application, status: "OFFER", canWithdraw: false });
    if (url.pathname.endsWith("/me/applications")) return json({ items: [], total: 0, page: 1, size: 20 });
    if (url.pathname.endsWith("/me/notifications")) return json({ items: [], unreadCount: 0, total: 0, page: 1, size: 10 });
    return json({});
  });
  return { reads: () => reads, posts: () => posts };
}

async function sso(page: Page) { await page.goto("/sso-callback?ticket=email-fixture"); }

test("email link requires login and SSO returns to the offer without responding", async ({ page }) => {
  const state = await fixtures(page);
  await page.goto(path);
  await expect(page).toHaveURL(/\/login$/);
  expect(state.reads()).toBe(0);
  await sso(page);
  await expect(page).toHaveURL(new RegExp(`${path}$`));
  await expect(page.getByRole("button", { name: "Nhận offer", exact: true })).toBeVisible();
  expect(state.posts()).toBe(0);
});

test("offer menu is removed, old list redirects and detail returns to its application", async ({ page }) => {
  await fixtures(page, { initial: "candidate" });
  await page.goto("/candidate/offers");
  await expect(page).toHaveURL(/\/candidate\/applications$/);
  await page.getByRole("button", { name: "Menu ứng tuyển", exact: true }).click();
  await expect(page.getByRole("menuitem", { name: "Offer", exact: true })).toHaveCount(0);
  await page.goto(path + "?returnTo=/candidate/offers?page=2");
  await expect(page.getByRole("link", { name: "Danh sách offer", exact: true })).toHaveCount(0);
  await expect(page.getByRole("link", { name: "Quay lại hồ sơ ứng tuyển", exact: true })).toHaveAttribute("href", `/candidate/applications/${applicationId}`);
});

test("forced password change preserves offer destination", async ({ page }) => {
  await fixtures(page, { forced: true });
  await page.goto(path); await expect(page).toHaveURL(/\/login$/);
  await sso(page); await expect(page).toHaveURL(/\/change-password$/);
  await page.getByLabel("Mật khẩu hiện tại", { exact: true }).fill("Password123");
  await page.getByLabel("Mật khẩu mới", { exact: true }).fill("NewPassword456");
  await page.getByLabel("Nhập lại mật khẩu mới", { exact: true }).fill("NewPassword456");
  await page.getByRole("button", { name: "Đổi mật khẩu", exact: true }).click();
  await expect(page).toHaveURL(new RegExp(`${path}$`));
  await expect(page.getByRole("button", { name: "Nhận offer", exact: true })).toBeVisible();
});

for (const initial of ["other", "hr"] as const) test(`wrong account ${initial} can switch without losing offer destination`, async ({ page }) => {
  await fixtures(page, { initial });
  await page.goto(path);
  await expect(page.getByText("Java Developer", { exact: true })).toHaveCount(0);
  await page.getByRole("button", { name: "Đăng nhập tài khoản khác", exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
  await sso(page);
  await expect(page).toHaveURL(new RegExp(`${path}$`));
  await expect(page.getByRole("button", { name: "Nhận offer", exact: true })).toBeVisible();
});

test("401 during acceptance returns through login and reads result without replaying POST", async ({ page }) => {
  const state = await fixtures(page, { initial: "candidate", expirePost: true });
  await page.goto(path);
  await page.getByRole("button", { name: "Nhận offer", exact: true }).click();
  await page.getByRole("button", { name: "Xác nhận nhận offer", exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
  await sso(page);
  await expect(page).toHaveURL(new RegExp(`${path}$`));
  await expect(page.getByText("Đã nhận", { exact: true })).toBeVisible();
  expect(state.posts()).toBe(1);
});

test("decline reason limit matches backend 1000 characters", async ({ page }) => {
  await fixtures(page, { initial: "candidate" }); await page.goto(path);
  await page.getByRole("button", { name: "Từ chối offer", exact: true }).click();
  await expect(page.getByLabel("Lý do từ chối", { exact: true })).toHaveAttribute("maxlength", "1000");
});

test("switching account in another tab removes the previous offer and its open dialog", async ({ page, context }) => {
  const state = await fixtures(page, { initial: "candidate" });
  await page.goto(path);
  await page.getByRole("button", { name: "Nhận offer", exact: true }).click();
  const otherTab = await context.newPage();
  await fixtures(otherTab, { ssoOther: true }); await sso(otherTab);
  await page.bringToFront();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await expect(page.getByRole("heading", { name: "Java Developer", exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Đăng nhập tài khoản khác", exact: true })).toBeVisible();
  expect(state.posts()).toBe(0);
});

for (const status of [401, 403]) test(`late ${status} from an older session does not invalidate the new session`, async ({ page, context }) => {
  await fixtures(page, { initial: "candidate" });
  let arrived = false, release!: () => void;
  const hold = new Promise<void>(resolve => { release = resolve; });
  await page.route(`**/api/v1/recruitment/me/applications/${applicationId}`, async route => {
    arrived = true; await hold;
    await route.fulfill({ headers: cors, status, json: { code: status === 403 ? "MUST_CHANGE_PASSWORD" : "UNAUTHORIZED", message: "Old session error" } });
  });
  await page.goto(path);
  await expect.poll(() => arrived).toBe(true);
  const oldResponse = page.waitForResponse(r => r.url().endsWith(`/me/applications/${applicationId}`));
  const otherTab = await context.newPage();
  await fixtures(otherTab, { ssoOther: true }); await sso(otherTab);
  await expect.poll(() => otherTab.evaluate(() => JSON.parse(localStorage.getItem("rikkei-jobs-auth")!).state.token)).toBe("other-token");
  await page.bringToFront();
  // Assert the live tab picked up the new session before releasing A's pending read.
  await expect(page.getByRole("button", { name: "Đăng nhập tài khoản khác", exact: true })).toBeVisible();
  release(); await oldResponse;
  await page.waitForTimeout(300);
  await expect(page).toHaveURL(new RegExp(`${path}$`));
  const persisted = await page.evaluate(() => JSON.parse(localStorage.getItem("rikkei-jobs-auth")!).state);
  expect(persisted.token).toBe("other-token");
  expect(persisted.user.mustChangePassword).toBe(false);
});

for (const saved of ["https://evil.example", "//evil.example", "/candidate/\\evil", "/candidate/%2f%2fevil", "/candidate/../hr/jobs", "/hr/jobs"]) test(`rejects unsafe or wrong-role destination ${saved}`, async ({ page }) => {
  await fixtures(page);
  await page.goto("/login");
  await page.evaluate(value => sessionStorage.setItem("rikkei-jobs-next-path", value), saved);
  await sso(page);
  // Ứng viên về trang chủ "/" (ROLE_LANDING); đích không an toàn hoặc của vai trò khác bị bỏ qua.
  await expect(page).toHaveURL(/^https?:\/\/[^/]+\/(?:candidate(?:\/profile)?)?$/);
});
