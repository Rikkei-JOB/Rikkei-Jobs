import { expect, test } from "@playwright/test";
import { mkdir } from "node:fs/promises";
import { join } from "node:path";
import { deflateSync } from "node:zlib";
import type { User } from "../src/interface/auth";
import type { PortfolioProject } from "../src/interface/portfolio";

/** PNG đơn sắc 64x36 hợp lệ để ảnh bìa hiển thị thật. */
function png(r: number, g: number, b: number): Buffer {
  const w = 64, h = 36;
  const crcTable = Array.from({ length: 256 }, (_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
  const crc = (buf: Buffer) => { let c = 0xffffffff; for (const byte of buf) c = crcTable[(c ^ byte) & 0xff] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
  const chunk = (type: string, data: Buffer) => {
    const body = Buffer.concat([Buffer.from(type), data]);
    const out = Buffer.alloc(body.length + 8); out.writeUInt32BE(data.length, 0); body.copy(out, 4); out.writeUInt32BE(crc(body), body.length + 4);
    return out;
  };
  const header = Buffer.alloc(13); header.writeUInt32BE(w, 0); header.writeUInt32BE(h, 4); header[8] = 8; header[9] = 2;
  const row = Buffer.concat([Buffer.from([0]), Buffer.alloc(w * 3).map((_, i) => [r, g, b][i % 3])]);
  return Buffer.concat([Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]), chunk("IHDR", header),
    chunk("IDAT", deflateSync(Buffer.concat(Array(h).fill(row)))), chunk("IEND", Buffer.alloc(0))]);
}

test("ứng viên thêm dự án có 2 ảnh, IOC không xoá được, sắp thứ tự được giữ sau khi tải lại", async ({ page, request }, info) => {
  test.skip(process.env.HS03_LIVE !== "1", "Chạy với stack dev đã bật PROFILE_DEMO_DATA_ENABLED=true.");
  test.setTimeout(120_000);
  const base = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";
  const ticket = await request.post(base + "/auth/dev/sso-ticket", { data: { lmsUserId: "lms-sv-001" } });
  const sso = await request.post(base + "/auth/sso", { data: { ticket: (await ticket.json()).ticket } });
  const session = await sso.json() as { accessToken: string; user: User };
  const headers = { Authorization: "Bearer " + session.accessToken };
  // Bắt đầu từ trạng thái chỉ còn dữ liệu seed: xoá dự án cá nhân của lần chạy trước.
  for (const p of await (await request.get(base + "/profiles/me/portfolio", { headers })).json() as PortfolioProject[]) {
    if (!p.verified) await request.delete(base + "/profiles/me/portfolio/" + p.id, { headers });
  }
  await page.addInitScript(s => localStorage.setItem("rikkei-jobs-auth",
    JSON.stringify({ state: { token: s.accessToken, user: s.user }, version: 0 })), session);
  await page.goto("/candidate/profile");
  const section = page.locator("[data-slot='card']").filter({ has: page.getByRole("heading", { name: "Portfolio dự án" }) });
  const ioc = section.getByTestId("portfolio-card").filter({ hasText: "Thực tập IOC – đã xác thực" });
  await expect(ioc).toBeVisible();
  await expect(ioc.getByRole("button", { name: /^Xoá dự án/ })).toHaveCount(0);

  const directory = process.env.HS03_SCREENSHOT_DIR ?? info.outputDir;
  await mkdir(directory, { recursive: true });
  const style = "nextjs-portal { display: none !important; }";

  await section.getByRole("button", { name: "Thêm dự án", exact: true }).click();
  const dialog = page.getByRole("dialog", { name: "Thêm dự án" });
  const name = "Shop thời trang (dự án cá nhân)";
  await dialog.getByLabel("Tên dự án").fill(name);
  await dialog.getByLabel("Vai trò").fill("Fullstack Developer");
  await dialog.getByRole("textbox", { name: /Công nghệ/ }).fill("Next.js, Spring Boot, PostgreSQL");
  await dialog.getByLabel("Link mã nguồn").fill("abc");
  await dialog.getByLabel("Mô tả").fill("Website bán hàng có giỏ hàng, thanh toán và trang quản trị.");
  await dialog.getByRole("button", { name: "Lưu dự án" }).click();
  await expect(dialog.getByText("Phải là URL http(s) hợp lệ")).toBeVisible();
  await dialog.getByLabel("Link mã nguồn").fill("https://github.com/example/fashion-shop");
  await dialog.locator("#portfolio-images").setInputFiles([
    { name: "home.png", mimeType: "image/png", buffer: png(37, 99, 235) },
    { name: "cart.png", mimeType: "image/png", buffer: png(234, 88, 12) },
  ]);
  await expect(dialog.getByRole("img", { name: /^Xem trước/ })).toHaveCount(2);
  await page.screenshot({ path: join(directory, "form-preview.png"), style });
  await dialog.getByRole("button", { name: "Lưu dự án" }).click();

  await expect(dialog).toBeHidden();
  const card = section.getByTestId("portfolio-card").filter({ hasText: name });
  await expect(card.getByText("2 ảnh")).toBeVisible();
  await expect(card.getByRole("img", { name: `Ảnh bìa dự án ${name}` })).toBeVisible();

  await card.getByRole("button", { name: `Đưa ${name} lên trước` }).click();
  await expect(section.getByTestId("portfolio-card").first()).toContainText(name);
  await page.reload();
  await expect(section.getByTestId("portfolio-card").first()).toContainText(name);
  await expect(section.getByRole("img", { name: `Ảnh bìa dự án ${name}` })).toBeVisible();
  await section.screenshot({ path: join(directory, "portfolio-desktop.png"), style });

  // Bấm thẻ mở chi tiết (chỉ đọc), từ đó mới chuyển sang sửa.
  await section.getByTestId("portfolio-card").filter({ hasText: name }).getByRole("heading", { name }).click();
  const detail = page.getByRole("dialog", { name });
  await expect(detail.getByRole("button", { name: "Xem ảnh 2" })).toBeVisible();
  await expect(detail.getByRole("textbox")).toHaveCount(0);
  await page.screenshot({ path: join(directory, "detail.png"), style });
  await detail.getByRole("button", { name: "Sửa dự án" }).click();
  const edit = page.getByRole("dialog", { name: "Sửa dự án" });
  await expect(edit.getByLabel("Tên dự án")).toHaveValue(name);
  await edit.getByRole("button", { name: "Huỷ" }).click();
  await expect(edit).toBeHidden();

  await page.setViewportSize({ width: 390, height: 900 });
  await section.screenshot({ path: join(directory, "portfolio-mobile.png"), style });
});
