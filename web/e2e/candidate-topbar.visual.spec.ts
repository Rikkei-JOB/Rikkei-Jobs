import { test, expect } from "@playwright/test";
import { mkdir } from "node:fs/promises";
import path from "node:path";
import { careerFixtures } from "./candidate-career-fixtures";

for (const viewport of [{ name: "desktop", width: 1440, height: 1000 }, { name: "tablet", width: 834, height: 1112 }, { name: "mobile", width: 390, height: 844 }]) {
  test(`candidate topbar and inbox ${viewport.name}`, async ({ page }) => {
    await page.setViewportSize(viewport);
    await careerFixtures(page);
    const items = [
      { id: "90000000-0000-0000-0000-000000000001", type: "OFFER", title: "Bạn có offer mới", body: "Frontend Developer tại Rikkei Software: Offer.", href: "/candidate/offers/90000000-0000-0000-0000-000000000002", createdAt: "2026-11-24T02:30:00Z", readAt: null },
      { id: "90000000-0000-0000-0000-000000000003", type: "INTERVIEW_INVITATION", title: "Bạn có lời mời phỏng vấn", body: "Phỏng vấn chuyên môn tại Rikkei Software — chọn khung giờ phù hợp.", href: "/candidate/interviews", createdAt: "2026-11-23T08:00:00Z", readAt: null },
      { id: "90000000-0000-0000-0000-000000000004", type: "APPLICATION_STATUS", title: "Cập nhật trạng thái ứng tuyển", body: "Java Backend Developer tại Rikkei Solutions: Sàng lọc.", href: "/candidate/applications", createdAt: "2026-11-22T03:00:00Z", readAt: "2026-11-22T04:00:00Z" },
    ];
    await page.route("**/recruitment/me/notifications**", route => route.fulfill({ headers: { "Access-Control-Allow-Origin": "*" }, json: { items, unreadCount: 2, total: 3, page: 1, size: 10 } }));
    const output = path.resolve("../docs/design/candidate-topbar");
    await mkdir(output, { recursive: true });
    async function capture(name: string) {
      await page.evaluate(() => document.fonts.ready);
      await page.addStyleTag({ content: "nextjs-portal { display: none !important; }" });
      await page.evaluate(() => {
        if (document.getElementById("fixture-label")) return;
        const label = document.createElement("div"); label.id = "fixture-label"; label.textContent = "API fixture";
        label.style.cssText = "position:fixed;bottom:8px;right:8px;z-index:1000;background:#18233a;color:white;font:10px/16px sans-serif;padding:2px 6px;border-radius:4px;pointer-events:none";
        document.body.append(label);
      });
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
      await page.screenshot({ path: path.join(output, `${viewport.name}-${name}.png`), animations: "disabled" });
    }
    await page.goto("/candidate/jobs");
    await expect(page.getByRole("article")).toHaveCount(3);
    await expect(page.getByRole("button", { name: "Thông báo, 2 chưa đọc", exact: true })).toBeVisible();
    await capture("01-topbar");
    await page.getByRole("button", { name: "Thông báo, 2 chưa đọc", exact: true }).click();
    await expect(page.getByRole("dialog", { name: "Thông báo", exact: true })).toContainText("Bạn có offer mới");
    await capture("02-notifications");
    await page.keyboard.press("Escape");
    if (viewport.name === "desktop") await page.getByRole("button", { name: "Menu ứng tuyển", exact: true }).click();
    else await page.getByRole("button", { name: "Mở menu", exact: true }).click();
    await capture("03-navigation");
    await page.keyboard.press("Escape");
    await page.setViewportSize({ width: 360, height: 640 });
    await page.getByRole("button", { name: "Thông báo, 2 chưa đọc", exact: true }).click();
    const box = await page.getByRole("dialog", { name: "Thông báo", exact: true }).boundingBox();
    expect(box!.x).toBeGreaterThanOrEqual(0);
    expect(box!.x + box!.width).toBeLessThanOrEqual(360);
    expect(box!.y + box!.height).toBeLessThanOrEqual(640);
  });
}
