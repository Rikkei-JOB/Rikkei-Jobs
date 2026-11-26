import { test, expect } from "@playwright/test";
import { mkdir } from "node:fs/promises";
import path from "node:path";
import { careerFixtures } from "./candidate-career-fixtures";

for (const viewport of [{ name: "desktop", width: 1440, height: 1000 }, { name: "tablet", width: 834, height: 1112 }, { name: "mobile", width: 390, height: 844 }]) {
  test(`technology banner ${viewport.name}`, async ({ page }) => {
    await page.setViewportSize(viewport);
    await careerFixtures(page);
    await page.route("**/recruitment/me/notifications**", route => route.fulfill({ headers: { "Access-Control-Allow-Origin": "*" }, json: { items: [], unreadCount: 0, total: 0, page: 1, size: 10 } }));
    await page.goto("/candidate/jobs");
    await expect(page.getByRole("article")).toHaveCount(3);
    await expect(page.getByRole("heading", { level: 1 })).toHaveText("Bước tiếp theo cho sự nghiệp của bạn");
    await expect(page.getByRole("link", { name: "Hoàn thiện hồ sơ", exact: true })).toHaveAttribute("href", "/candidate/profile");
    await page.evaluate(() => document.fonts.ready);
    await page.addStyleTag({ content: "nextjs-portal { display: none !important; }" });
    await page.evaluate(() => {
      const label = document.createElement("span"); label.textContent = "API fixture";
      label.style.cssText = "position:fixed;bottom:8px;right:8px;z-index:1000;background:#18233a;color:white;font:10px/16px sans-serif;padding:2px 6px;border-radius:4px";
      document.body.append(label);
    });
    const output = path.resolve("../docs/design/candidate-banner");
    await mkdir(output, { recursive: true });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.screenshot({ path: path.join(output, `${viewport.name}.png`), animations: "disabled" });
    await page.locator(".career-banner").screenshot({ path: path.join(output, `${viewport.name}-banner.png`), animations: "disabled" });
    await page.setViewportSize({ width: 360, height: 640 });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  });
}
