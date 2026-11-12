import { test, expect } from "@playwright/test";
import { signIn, apiFixtures, jobId, cvId } from "./ut01-fixtures";

test("selects a CV and submits the introduction once", async ({ page }) => {
  await signIn(page); await apiFixtures(page);
  await page.goto(`/candidate/applications/new?jobId=${jobId}`);
  await expect(page.getByRole("dialog")).toBeVisible();
  await page.getByLabel("Chọn CV").selectOption(cvId);
  await page.getByLabel("Thư giới thiệu").fill("Tôi muốn ứng tuyển vị trí Java.");
  const request = page.waitForRequest(r => r.method() === "POST" && r.url().endsWith(`/jobs/${jobId}/apply`));
  await page.getByRole("button", { name: "Gửi ứng tuyển", exact: true }).click();
  expect((await request).postDataJSON()).toEqual({ cvId, coverLetter: "Tôi muốn ứng tuyển vị trí Java." });
  await expect(page).toHaveURL(/\/candidate\/applications\/20000000/);
  await expect(page.getByRole("heading", { name: "Java Developer" })).toBeVisible();
});

test("empty CV list gives a way to create a CV", async ({ page }) => {
  await signIn(page); await apiFixtures(page);
  await page.route("**/api/v1/profile/cvs", r => r.fulfill({ headers: { "Access-Control-Allow-Origin": "*" }, json: [] }));
  await page.goto(`/candidate/applications/new?jobId=${jobId}`);
  await expect(page.getByText("Bạn chưa có CV để ứng tuyển.")).toBeVisible();
  await expect(page.getByRole("link", { name: "Tạo CV" })).toHaveAttribute("href", "/candidate/cvs");
  await expect(page.getByRole("button", { name: "Gửi ứng tuyển", exact: true })).toBeDisabled();
});

test("requires a candidate session", async ({ page }) => {
  await page.goto(`/candidate/applications/new?jobId=${jobId}`);
  await expect(page).toHaveURL(/\/login/);
});

test("closed JD keeps the introduction and does not report success", async ({ page }) => {
  await signIn(page); await apiFixtures(page);
  await page.route(`**/api/v1/recruitment/jobs/${jobId}/apply`, route => route.fulfill({ status: 409,
    headers: { "Access-Control-Allow-Origin": "*" }, json: { code: "CONFLICT", message: "Vị trí hiện không nhận hồ sơ ứng tuyển." } }));
  await page.goto(`/candidate/applications/new?jobId=${jobId}`);
  await page.getByLabel("Chọn CV").selectOption(cvId);
  await page.getByLabel("Thư giới thiệu").fill("Giữ lại nội dung này.");
  await page.getByRole("button", { name: "Gửi ứng tuyển", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText("Vị trí hiện không nhận hồ sơ ứng tuyển.");
  await expect(page.getByLabel("Thư giới thiệu")).toHaveValue("Giữ lại nội dung này.");
  await expect(page.getByRole("dialog")).toBeVisible();
});

test("mobile apply dialog fits the viewport and can be dismissed by keyboard", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await signIn(page); await apiFixtures(page);
  await page.goto(`/candidate/applications/new?jobId=${jobId}`);
  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  const box = await dialog.boundingBox();
  expect(box?.width).toBeLessThanOrEqual(390);
  expect(box?.x).toBeGreaterThanOrEqual(0);
  await page.keyboard.press("Escape");
  await expect(page).toHaveURL(/\/candidate\/applications$/);
});
