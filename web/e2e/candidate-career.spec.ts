import { test, expect } from "@playwright/test";
import { careerFixtures, closedJobId, jobsApiPattern, cors } from "./candidate-career-fixtures";
import { jobId, cvId } from "./ut01-fixtures";
import { chooseOption, expectOption } from "./select-menu";

test.beforeEach(async ({ page }) => { await careerFixtures(page); });

test("loading keeps the search available until job data arrives", async ({ page }) => {
  let release!: () => void;
  const pending = new Promise<void>(resolve => { release = resolve; });
  await page.route(jobsApiPattern, async route => { await pending; await route.fallback(); });
  try {
    await page.goto("/candidate/jobs");
    await expect(page.getByRole("status")).toContainText("Đang tải việc làm");
    await expect(page.getByLabel("Vị trí hoặc công ty")).toBeVisible();
    release();
    await expect(page.getByRole("article")).toHaveCount(3);
  } finally { release(); }
});

test("search normalizes Vietnamese accents and restores filters through history and refresh", async ({ page }) => {
  await page.goto("/candidate/jobs?source=profile");
  await page.getByLabel("Vị trí hoặc công ty").fill("dang");
  await page.getByRole("button", { name: "Tìm kiếm", exact: true }).click();
  await expect(page.getByRole("article")).toHaveCount(1);
  await expect(page.getByRole("article")).toContainText("Công ty Công nghệ Đặng và Cộng sự");
  await page.getByLabel("Trạng thái nhận hồ sơ").selectOption("open");
  await page.getByRole("button", { name: "Tìm kiếm", exact: true }).click();
  await expect(page.getByText("Không tìm thấy vị trí phù hợp")).toBeVisible();
  await page.reload();
  await expect(page.getByLabel("Vị trí hoặc công ty")).toHaveValue("dang");
  await expect(page.getByLabel("Trạng thái nhận hồ sơ")).toHaveValue("open");
  await page.goBack();
  await expect(page.getByLabel("Trạng thái nhận hồ sơ")).toHaveValue("all");
  await expect(page.getByRole("article")).toHaveCount(1);
  await page.goForward();
  await expect(page.getByLabel("Trạng thái nhận hồ sơ")).toHaveValue("open");
  await page.getByRole("button", { name: "Xoá bộ lọc", exact: true }).click();
  await expect(page).toHaveURL(/\/candidate\/jobs\?source=profile$/);
  await expect(page.getByLabel("Vị trí hoặc công ty")).toHaveValue("");
  await expect(page.getByRole("article")).toHaveCount(3);
});

test("opens job detail and submits the chosen CV", async ({ page }) => {
  await page.goto("/candidate/jobs");
  await page.getByRole("link", { name: "Lập trình viên Frontend React", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Chuẩn bị ứng tuyển" })).toBeVisible();
  const trigger = page.getByRole("button", { name: "Ứng tuyển", exact: true });
  await trigger.click();
  await expect(page.getByRole("dialog")).toContainText("Lập trình viên Frontend React");
  await page.keyboard.press("Escape");
  await expect(trigger).toBeFocused();
  await trigger.click();
  await chooseOption(page.getByLabel("Chọn CV"), cvId);
  await page.getByLabel("Thư giới thiệu", { exact: true }).fill("Mong được đồng hành cùng đội ngũ.");
  const request = page.waitForRequest(r => r.method() === "POST" && r.url().endsWith(`/jobs/${jobId}/apply`));
  await page.getByRole("button", { name: "Gửi ứng tuyển", exact: true }).click();
  expect((await request).postDataJSON()).toEqual({ cvId, coverLetter: "Mong được đồng hành cùng đội ngũ." });
  await expect(page).toHaveURL(/\/candidate\/applications\/20000000/, { timeout: 15_000 });
});

test("closed job explains why applying is unavailable", async ({ page }) => {
  await page.goto(`/candidate/jobs/${closedJobId}`);
  await expect(page.getByRole("button", { name: "Đã đóng nhận hồ sơ", exact: true })).toBeDisabled();
  await expect(page.getByRole("heading", { name: "Chuẩn bị ứng tuyển" })).toBeVisible();
});

test("list error can be retried without losing the search form", async ({ page }) => {
  let failed = true;
  await page.route(jobsApiPattern, route => failed
    ? route.fulfill({ status: 503, headers: cors, json: { code: "UNAVAILABLE", message: "Chưa thể tải việc làm." } })
    : route.fallback());
  await page.goto("/candidate/jobs?q=React");
  await expect(page.getByRole("alert").filter({ hasText: "Chưa thể tải việc làm." })).toBeVisible();
  await expect(page.getByLabel("Vị trí hoặc công ty")).toHaveValue("React");
  failed = false;
  await page.getByRole("button", { name: "Thử lại", exact: true }).click();
  await expect(page.getByRole("article")).toHaveCount(1);
});

test("mobile detail action and application popup fit a narrow and short screen", async ({ page }) => {
  await page.setViewportSize({ width: 360, height: 640 });
  await page.goto(`/candidate/jobs/${jobId}`);
  const trigger = page.getByRole("button", { name: "Ứng tuyển", exact: true });
  await expect(page.locator("[data-mobile-apply]")).toBeVisible();
  await expect(trigger).toBeInViewport();
  await trigger.click();
  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  await expect(page.locator("[data-mobile-apply]")).toBeHidden();
  await page.getByLabel("Thư giới thiệu", { exact: true }).fill("Ứng tuyển từ điện thoại.");
  await page.getByRole("button", { name: "Gửi ứng tuyển", exact: true }).scrollIntoViewIfNeeded();
  const box = await dialog.boundingBox();
  expect(box!.x).toBeGreaterThanOrEqual(0);
  expect(box!.x + box!.width).toBeLessThanOrEqual(360);
  expect(box!.height).toBeLessThanOrEqual(640);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.keyboard.press("Escape");
  await expect(page.locator("[data-mobile-apply]")).toBeVisible();
  await expect(trigger).toBeFocused();
});

test("CV loading can be retried while preserving the introduction", async ({ page }) => {
  let failed = true;
  await page.route("**/profile/cvs", route => failed
    ? route.fulfill({ status: 503, headers: cors, json: { code: "UNAVAILABLE", message: "Chưa thể tải CV." } })
    : route.fallback());
  await page.goto(`/candidate/jobs/${jobId}`);
  await page.getByRole("button", { name: "Ứng tuyển", exact: true }).click();
  const dialog = page.getByRole("dialog");
  await expect(dialog.getByRole("alert")).toContainText("Chưa thể tải CV.");
  await page.getByLabel("Thư giới thiệu", { exact: true }).fill("Giữ lại phần giới thiệu này.");
  await expect(dialog.getByRole("button", { name: "Gửi ứng tuyển", exact: true })).toBeDisabled();
  failed = false;
  await page.getByRole("button", { name: "Tải lại CV", exact: true }).click();
  await chooseOption(page.getByLabel("Chọn CV"), cvId);
  await expect(page.getByLabel("Thư giới thiệu", { exact: true })).toHaveValue("Giữ lại phần giới thiệu này.");
  await expect(dialog.getByRole("button", { name: "Gửi ứng tuyển", exact: true })).toBeEnabled();
});

test("reopening refreshes CVs before allowing submission with a new selection", async ({ page }) => {
  let reads = 0, release!: () => void, started!: () => void;
  const pending = new Promise<void>(resolve => { release = resolve; });
  const refreshing = new Promise<void>(resolve => { started = resolve; });
  await page.route("**/profile/cvs", async route => {
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    if (++reads === 1) return route.fallback();
    started(); await pending;
    await route.fulfill({ headers: cors, json: [{ id: "40000000-0000-0000-0000-000000000002", name: "CV mới", targetPosition: "React Developer" }] });
  });
  try {
    await page.goto(`/candidate/jobs/${jobId}`);
    const trigger = page.getByRole("button", { name: "Ứng tuyển", exact: true });
    await trigger.click();
    await chooseOption(page.getByLabel("Chọn CV"), cvId);
    await page.keyboard.press("Escape");
    await trigger.click();
    await refreshing;
    const send = page.getByRole("button", { name: "Gửi ứng tuyển", exact: true });
    await expect(send).toBeDisabled();
    release();
    await expectOption(page.getByLabel("Chọn CV"), "");
    await expect(send).toBeDisabled();
    await chooseOption(page.getByLabel("Chọn CV"), "40000000-0000-0000-0000-000000000002");
    await expect(send).toBeEnabled();
  } finally { release(); }
});

test("uncertain submission blocks resending until the result is reconciled", async ({ page }) => {
  let attempts = 0;
  await page.route(`**/recruitment/jobs/${jobId}/apply`, route => { attempts++; return route.abort("failed"); });
  await page.goto(`/candidate/jobs/${jobId}`);
  await page.getByRole("button", { name: "Ứng tuyển", exact: true }).click();
  await chooseOption(page.getByLabel("Chọn CV"), cvId);
  await page.getByLabel("Thư giới thiệu", { exact: true }).fill("Giữ nguyên khi chưa rõ kết quả.");
  await page.getByRole("button", { name: "Gửi ứng tuyển", exact: true }).click();
  await expect(page.getByRole("button", { name: "Gửi ứng tuyển", exact: true })).toBeDisabled();
  await expect(page.getByLabel("Thư giới thiệu", { exact: true })).toHaveValue("Giữ nguyên khi chưa rõ kết quả.");
  await page.getByRole("button", { name: "Kiểm tra kết quả", exact: true }).click();
  await expect(page).toHaveURL(/\/candidate\/applications\/20000000/, { timeout: 15_000 });
  expect(attempts).toBe(1);
});
