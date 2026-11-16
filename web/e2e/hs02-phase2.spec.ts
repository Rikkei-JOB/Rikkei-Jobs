import { test, expect, type Page } from "@playwright/test";
import { signIn, userId } from "./ut01-fixtures";

const profile = { userId, fullName: "Nguyễn Văn An", email: "an@example.test", dob: null, gender: null, phone: null,
  address: null, avatarUrl: null, headline: null, summary: null, program: "Kỹ sư", classCode: "KS25", courseName: "K25", visibility: "PRIVATE", updatedAt: "2026-11-13T01:00:00Z" };
const fileUrl = "/api/v1/files/50000000-0000-0000-0000-000000000001";
const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "Authorization,Content-Type", "Access-Control-Allow-Methods": "GET,POST,PUT,DELETE,OPTIONS" };
async function setup(page: Page, rejectUpload = false) {
  await signIn(page);
  const records: Record<string, Record<string, unknown>[]> = { educations: [], certificates: [] };
  const writes: string[] = [];
  const authenticatedDownloads: string[] = [];
  await page.route("**/api/v1/**", async route => {
    const request = route.request(); const method = request.method(); const path = new URL(request.url()).pathname;
    if (method === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    if (path.endsWith("/auth/me")) return route.fulfill({ headers: cors, json: { id: userId, fullName: profile.fullName, email: profile.email, role: "CANDIDATE", companyId: null, mustChangePassword: false } });
    if (path.endsWith("/profiles/me")) return route.fulfill({ headers: cors, json: profile });
    if (path.endsWith("/files") && method === "POST") {
      writes.push("upload");
      expect(request.headers()["content-type"]).toMatch(/^multipart\/form-data; boundary=/);
      return route.fulfill({ status: rejectUpload ? 400 : 201, headers: cors, json: rejectUpload ? { code: "INVALID_FILE_TYPE", message: "Loại tệp không hợp lệ" } : { url: fileUrl } });
    }
    if (path === fileUrl) {
      authenticatedDownloads.push(request.headers().authorization ?? "");
      return route.fulfill({ headers: { ...cors, "Content-Type": "image/png" }, body: Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aXmUAAAAASUVORK5CYII=", "base64") });
    }
    if (path.endsWith("/profiles/me/avatar")) return route.fulfill({ headers: cors, json: { ...profile, avatarUrl: request.postDataJSON().fileUrl } });
    const match = path.match(/\/profiles\/me\/(educations|certificates)(?:\/([^/]+))?$/);
    if (match) {
      const [, kind, id] = match;
      if (method === "GET") return route.fulfill({ headers: cors, json: records[kind] });
      writes.push(kind);
      if (method === "DELETE") { records[kind] = records[kind].filter(item => item.id !== id); return route.fulfill({ status: 204, headers: cors }); }
      const item = { id: id ?? `${kind}-1`, ...request.postDataJSON() };
      if (method === "POST") records[kind].push(item); else records[kind] = records[kind].map(old => old.id === id ? item : old);
      return route.fulfill({ status: method === "POST" ? 201 : 200, headers: cors, json: item });
    }
    return route.fulfill({ headers: cors, json: [] });
  });
  return { writes, authenticatedDownloads };
}

test("creates edits and deletes education with date and GPA validation", async ({ page }) => {
  const state = await setup(page); await page.goto("/candidate/profile");
  await page.getByRole("button", { name: "Thêm học vấn", exact: true }).click();
  await page.getByLabel("Trường học").fill("Đại học A");
  await page.getByLabel("Từ ngày").fill("2020-09-01");
  await page.getByLabel("Đến ngày").fill("2019-01-01");
  await page.getByLabel("GPA (0–10)").fill("11");
  await page.getByRole("button", { name: "Lưu học vấn", exact: true }).click();
  await expect(page.getByText("Ngày kết thúc phải từ ngày bắt đầu trở đi")).toBeVisible();
  await expect(page.getByText("GPA phải từ 0 đến 10")).toBeVisible();
  expect(state.writes).toEqual([]);
  await page.getByLabel("Đến ngày").fill("2024-06-01"); await page.getByLabel("GPA (0–10)").fill("8.5");
  await page.getByRole("button", { name: "Lưu học vấn", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Đại học A", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Sửa học vấn Đại học A" }).click();
  await page.getByLabel("Trường học").fill("Đại học B"); await page.getByRole("button", { name: "Lưu học vấn", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Đại học B", exact: true })).toBeVisible();
  page.once("dialog", dialog => dialog.accept()); await page.getByRole("button", { name: "Xoá học vấn Đại học B" }).click();
  await expect(page.getByRole("heading", { name: "Đại học B", exact: true })).toHaveCount(0);
});

test("uploads a certificate, downloads it with authentication, edits and deletes it", async ({ page }) => {
  const state = await setup(page); await page.goto("/candidate/profile");
  await page.getByRole("button", { name: "Thêm chứng chỉ", exact: true }).click();
  await page.getByLabel("Tên chứng chỉ").fill("AWS"); await page.getByLabel("Đơn vị cấp").fill("Amazon");
  await page.getByLabel("Ngày cấp").fill("2024-01-01");
  await page.getByLabel("Tệp chứng chỉ").setInputFiles({ name: "certificate.pdf", mimeType: "application/pdf", buffer: Buffer.from("%PDF-1.7 sample") });
  await page.getByRole("button", { name: "Lưu chứng chỉ", exact: true }).click();
  await expect(page.getByRole("heading", { name: "AWS", exact: true })).toBeVisible();
  const download = page.waitForEvent("download"); await page.getByRole("button", { name: "Tải tệp AWS" }).click(); await download;
  expect(state.authenticatedDownloads).toEqual(["Bearer test-token"]);
  await page.getByRole("button", { name: "Sửa chứng chỉ AWS" }).click();
  await page.getByLabel("Tên chứng chỉ").fill("AWS mới"); await page.getByRole("button", { name: "Lưu chứng chỉ", exact: true }).click();
  await expect(page.getByRole("heading", { name: "AWS mới", exact: true })).toBeVisible();
  page.once("dialog", dialog => dialog.accept()); await page.getByRole("button", { name: "Xoá chứng chỉ AWS mới" }).click();
  await expect(page.getByRole("heading", { name: "AWS mới", exact: true })).toHaveCount(0);
});

test("does not save a certificate after an upload error and retains input", async ({ page }) => {
  const state = await setup(page, true); await page.goto("/candidate/profile");
  await page.getByRole("button", { name: "Thêm chứng chỉ", exact: true }).click();
  await page.getByLabel("Tên chứng chỉ").fill("My certificate"); await page.getByLabel("Đơn vị cấp").fill("Issuer");
  await page.getByLabel("Ngày cấp").fill("2024-01-01");
  await page.getByLabel("Tệp chứng chỉ").setInputFiles({ name: "fake.pdf", mimeType: "application/pdf", buffer: Buffer.from("MZ EXE renamed") });
  await page.getByRole("button", { name: "Lưu chứng chỉ", exact: true }).click();
  await expect(page.getByRole("alert").filter({ hasText: "Loại tệp không hợp lệ" })).toBeVisible();
  await expect(page.getByLabel("Tên chứng chỉ")).toHaveValue("My certificate");
  expect(state.writes).toEqual(["upload"]);
});

test("uploads an avatar and reads the image with Bearer authentication", async ({ page }) => {
  const state = await setup(page); await page.goto("/candidate/profile");
  await page.getByLabel("Chọn ảnh đại diện").setInputFiles({ name: "avatar.png", mimeType: "image/png", buffer: Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aXmUAAAAASUVORK5CYII=", "base64") });
  await expect(page.getByAltText("Ảnh đại diện của bạn")).toBeVisible();
  await expect(page.getByRole("status").filter({ hasText: "Đã cập nhật ảnh đại diện" })).toBeVisible();
  expect(state.authenticatedDownloads).toEqual(["Bearer test-token"]);
});
