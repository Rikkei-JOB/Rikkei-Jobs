import { test, expect, type Page } from "@playwright/test";
import { signIn, userId } from "./ut01-fixtures";

const profile = {
  userId, fullName: "Nguyễn Văn An", email: "candidate@example.test", dob: null, gender: null,
  phone: null, address: null, avatarUrl: null, headline: null, summary: null,
  program: "Kỹ sư phần mềm", classCode: "KS25-CNTT1", courseName: "K25", visibility: "PRIVATE",
  updatedAt: "2026-11-13T01:00:00Z",
};
const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "Authorization,Content-Type", "Access-Control-Allow-Methods": "GET,PUT,OPTIONS" };

async function fixtures(page: Page, failFirst = false) {
  await signIn(page);
  let unavailable = failFirst;
  await page.route("**/api/v1/**", async route => {
    const request = route.request();
    if (request.method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    const path = new URL(request.url()).pathname;
    if (path.endsWith("/auth/me")) return route.fulfill({ headers: cors, json: {
      id: userId, fullName: profile.fullName, email: profile.email, role: "CANDIDATE", companyId: null, mustChangePassword: false,
    } });
    if (path.endsWith("/profiles/me")) {
      if (request.method() === "GET" && unavailable) return route.fulfill({ status: 503, headers: cors, json: { code: "EXTERNAL_UNAVAILABLE", message: "LMS tạm thời không sẵn sàng" } });
      return route.fulfill({ headers: cors, json: request.method() === "PUT" ? { ...profile, ...request.postDataJSON() } : profile });
    }
    return route.fulfill({ headers: cors, json: [] });
  });
  return () => { unavailable = false; };
}

test("loads LMS fields read-only and saves only editable personal fields", async ({ page }) => {
  await fixtures(page);
  await page.goto("/candidate/profile");
  await expect(page.getByRole("heading", { name: "Thông tin cá nhân" })).toBeVisible();
  await expect(page.getByLabel("Hệ đào tạo")).toHaveValue(profile.program);
  await expect(page.getByLabel("Lớp")).toHaveAttribute("readonly", "");
  await expect(page.getByLabel("Khoá")).toHaveAttribute("readonly", "");
  await expect(page.getByText("Lấy từ LMS", { exact: true })).toBeVisible();
  await page.getByLabel("Họ và tên").fill("Nguyễn An");
  await page.getByLabel("Số điện thoại").fill("0912345678");
  await page.getByLabel("Ngày sinh").fill("2000-02-29");
  const saved = page.waitForRequest(r => r.method() === "PUT" && r.url().endsWith("/profiles/me"));
  await page.getByRole("button", { name: "Lưu thông tin" }).click();
  expect((await saved).postDataJSON()).toEqual({
    fullName: "Nguyễn An", email: profile.email, dob: "2000-02-29", gender: null,
    phone: "0912345678", address: null, headline: null, summary: null,
  });
  await expect(page.getByRole("status").filter({ hasText: "Đã lưu thông tin" })).toBeVisible();
});

test("rejects invalid personal details before calling the API", async ({ page }) => {
  await fixtures(page);
  let writes = 0;
  page.on("request", r => { if (r.method() === "PUT" && r.url().endsWith("/profiles/me")) writes++; });
  await page.goto("/candidate/profile");
  await page.getByLabel("Email", { exact: true }).fill("invalid-email");
  await page.getByLabel("Số điện thoại").fill("123");
  await page.getByLabel("Ngày sinh").fill("2099-01-01");
  await page.getByRole("button", { name: "Lưu thông tin" }).click();
  await expect(page.getByText("Email không hợp lệ", { exact: true })).toBeVisible();
  await expect(page.getByText("Số điện thoại phải gồm 10 số và bắt đầu bằng 0", { exact: true })).toBeVisible();
  await expect(page.getByText("Ngày sinh không được ở tương lai", { exact: true })).toBeVisible();
  expect(writes).toBe(0);
});

test("retries loading after LMS is unavailable", async ({ page }) => {
  const recover = await fixtures(page, true);
  await page.goto("/candidate/profile");
  await expect(page.getByRole("alert").filter({ hasText: "LMS tạm thời không sẵn sàng" })).toBeVisible();
  recover();
  await page.getByRole("button", { name: "Thử lại" }).click();
  await expect(page.getByLabel("Họ và tên")).toHaveValue(profile.fullName);
});

test("keeps unsaved input when saving fails", async ({ page }) => {
  await fixtures(page);
  await page.route("**/api/v1/profiles/me", async route => {
    if (route.request().method() !== "PUT") return route.fallback();
    return route.fulfill({ status: 503, headers: cors, json: { code: "EXTERNAL_UNAVAILABLE", message: "Không thể lưu lúc này" } });
  });
  await page.goto("/candidate/profile");
  await page.getByLabel("Giới thiệu bản thân").fill("Nội dung tôi đang chỉnh sửa");
  await page.getByRole("button", { name: "Lưu thông tin" }).click();
  await expect(page.getByRole("alert").filter({ hasText: "Không thể lưu lúc này" })).toBeVisible();
  await expect(page.getByLabel("Giới thiệu bản thân")).toHaveValue("Nội dung tôi đang chỉnh sửa");
  await expect(page.getByRole("button", { name: "Lưu thông tin" })).toBeEnabled();
});
