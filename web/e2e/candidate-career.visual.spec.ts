import { test, expect } from "@playwright/test";
import { mkdir } from "node:fs/promises";
import path from "node:path";
import { careerFixtures, careerJobs, closedJobId, cors, jobsApiPattern } from "./candidate-career-fixtures";
import { jobId } from "./ut01-fixtures";

const viewports = [
  { name: "desktop", width: 1440, height: 1000 },
  { name: "tablet", width: 834, height: 1112 },
  { name: "mobile", width: 390, height: 844 },
];

for (const viewport of viewports) {
  test(`capture ${viewport.name} candidate flow with API fixtures`, async ({ page }) => {
    await page.setViewportSize(viewport);
    await careerFixtures(page);
    const output = path.resolve("../docs/design/candidate-local");
    await mkdir(output, { recursive: true });
    async function capture(state: string) {
      await page.evaluate(() => document.fonts.ready);
      await page.addStyleTag({ content: "nextjs-portal { display: none !important; }" });
      await page.evaluate(width => {
        let label = document.getElementById("fixture-capture-label");
        if (!label) { label = document.createElement("div"); label.id = "fixture-capture-label"; document.body.append(label); }
        label.textContent = `API fixture · ${width}px`;
        label.style.cssText = "position:fixed;top:8px;left:50%;transform:translateX(-50%);z-index:1000;background:#18233a;color:white;padding:3px 8px;border-radius:4px;font:11px/16px sans-serif;pointer-events:none";
      }, viewport.width);
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
      await page.screenshot({ path: path.join(output, `${viewport.name}-${state}.png`), animations: "disabled" });
      await page.screenshot({ path: path.join(output, `${viewport.name}-${state}-full.png`), fullPage: true, animations: "disabled" });
    }

    await page.goto("/candidate/jobs");
    await expect(page.getByRole("article")).toHaveCount(careerJobs.length);
    await capture("01-list");
    await page.goto(`/candidate/jobs/${jobId}`);
    await expect(page.getByRole("heading", { name: "Chuẩn bị ứng tuyển" })).toBeVisible();
    await capture("02-detail");
    await page.getByRole("button", { name: "Ứng tuyển", exact: true }).click();
    await expect(page.getByLabel("Chọn CV")).toBeVisible();
    await capture("03-apply");
    await page.keyboard.press("Escape");

    await page.goto("/candidate/jobs?q=khong-co-ket-qua");
    await expect(page.getByText("Không tìm thấy vị trí phù hợp")).toBeVisible();
    await capture("04-empty");
    await page.goto(`/candidate/jobs/${closedJobId}`);
    await expect(page.getByRole("button", { name: "Đã đóng nhận hồ sơ", exact: true })).toBeDisabled();
    await capture("05-closed");

    await page.route("**/profile/cvs", route => route.fulfill({ headers: cors, json: [] }));
    await page.goto(`/candidate/jobs/${jobId}`);
    await page.getByRole("button", { name: "Ứng tuyển", exact: true }).click();
    await expect(page.getByText("Bạn chưa có CV để ứng tuyển.")).toBeVisible();
    await capture("06-no-cv");
    await page.keyboard.press("Escape");

    await page.route(jobsApiPattern, route => route.fulfill({ status: 503, headers: cors, json: { code: "UNAVAILABLE", message: "Chưa kết nối được dữ liệu việc làm. Vui lòng thử lại." } }));
    await page.goto("/candidate/jobs");
    await expect(page.getByRole("button", { name: "Thử lại", exact: true })).toBeVisible();
    await capture("07-error");
  });
}
