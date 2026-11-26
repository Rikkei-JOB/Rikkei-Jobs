import { expect, test, type Page } from "@playwright/test";
import { mkdir } from "node:fs/promises";
import { join } from "node:path";
import { chooseOption } from "./select-menu";

async function signIn(page: Page, role: string) {
  const user = { id: "reviewer", fullName: "Phạm Minh", role, mustChangePassword: false, companyId: null };
  await page.addInitScript(user => localStorage.setItem("rikkei-jobs-auth",
    JSON.stringify({ state: { token: "test-token", user }, version: 0 })), user);
  return user;
}

test("STAFF sửa giới thiệu, trả lại hồ sơ rồi duyệt hồ sơ kế tiếp", async ({ page }, info) => {
  const user = await signIn(page, "STAFF");
  const rows = ["Nguyễn Thu Hà", "Trần Minh An"].map((fullName, i) => ({
    userId: "06000000-0000-4000-8000-00000000000" + (i + 1), fullName, classCode: "HS06-A",
    courseName: "Java 2026", consentAt: "2026-11-" + (18 + i) + "T02:00:00Z", visibility: "PENDING", rejectCount: i === 0 ? 1 : 0,
  }));
  const skill = { id: "10000000-0000-4000-8000-000000000001", name: "Java", code: "JAVA", group: "Backend" };
  let summary = "Mong muốn phát triển ứng dụng quản lý tuyển dụng.";
  let decisionCount = 0;
  await page.route("**/api/v1/**", async route => {
    const req = route.request();
    const url = new URL(req.url());
    const headers = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "Authorization,Content-Type",
      "Access-Control-Allow-Methods": "GET,PUT,POST,OPTIONS" };
    const ok = (json: unknown) => route.fulfill({ headers, json });
    if (req.method() === "OPTIONS") return route.fulfill({ status: 204, headers });
    if (url.pathname.endsWith("/auth/me")) return ok(user);
    if (url.pathname.endsWith("/staff/profile-reviews")) {
      expect(url.searchParams.get("status")).toBe("PENDING");
      if (decisionCount) expect(url.searchParams.get("classCode")).toBe("HS06-A");
      return ok({ items: rows, page: 1, size: 20, total: rows.length });
    }
    if (url.pathname.endsWith("/skills")) return ok([skill]);
    const row = rows.find(item => url.pathname.includes(item.userId));
    if (row && url.pathname.endsWith("/preview")) return ok({ ...row, headline: "Lập trình viên Java", summary,
      skills: [{ skill, verifiedScore: 85, selfScore: 80, level: "GOOD", evidences: [] }] });
    if (row && url.pathname.endsWith("/self-declared")) {
      expect(req.postDataJSON()).toEqual({ field: "summary", value: "Phát triển ứng dụng Java.", reason: "Chuẩn hóa giới thiệu." });
      summary = req.postDataJSON().value;
      return route.fulfill({ status: 204, headers });
    }
    if (row && /\/(reject|approve)$/.test(url.pathname)) {
      if (!decisionCount) {
        expect(url.pathname.endsWith("/reject")).toBeTruthy();
        expect(req.postDataJSON().reason).toBe("Bổ sung minh chứng.");
      } else expect(url.pathname.endsWith("/approve")).toBeTruthy();
      rows.splice(rows.indexOf(row), 1);
      decisionCount++;
      return ok({ visibility: decisionCount === 1 ? "REJECTED" : "PUBLIC" });
    }
    return ok({});
  });
  const directory = process.env.HS06_SCREENSHOT_DIR ?? info.outputDir;
  await mkdir(directory, { recursive: true });
  async function capture(name: string) {
    await page.screenshot({ path: join(directory, name + ".png"), fullPage: true, style: "nextjs-portal { display: none !important; }" });
  }

  await page.goto("/staff/profile-reviews");
  await page.getByLabel("Lớp", { exact: true }).fill("HS06-A");
  await page.getByRole("button", { name: "Lọc hồ sơ" }).click();
  await expect(page).toHaveURL(/classCode=HS06-A/);
  const first = page.getByRole("link", { name: "Nguyễn Thu Hà" });
  await expect(first).toBeVisible();
  await capture("hang-cho");
  await first.click();
  await expect(page.getByRole("heading", { name: "Nguyễn Thu Hà" })).toBeVisible();
  await expect(page.getByRole("region", { name: "Bản xem trước của HR" }).getByText("Java", { exact: true }).first()).toBeVisible();
  await capture("xem-truoc");
  await chooseOption(page.getByLabel("Trường cần chỉnh sửa"), "summary");
  await page.getByLabel("Giá trị mới").fill("Phát triển ứng dụng Java.");
  await expect(page.getByRole("button", { name: "Lưu chỉnh sửa" })).toBeDisabled();
  await page.getByLabel("Lý do chỉnh sửa").fill("Chuẩn hóa giới thiệu.");
  await page.getByRole("button", { name: "Lưu chỉnh sửa" }).click();
  await expect(page.getByText("Phát triển ứng dụng Java.", { exact: true }).first()).toBeVisible();
  await expect(page.getByRole("button", { name: "Trả lại hồ sơ" })).toBeDisabled();
  await page.getByLabel("Lý do trả lại").fill("Bổ sung minh chứng.");
  await page.getByRole("button", { name: "Trả lại hồ sơ" }).click();
  await expect(page.getByRole("heading", { name: "Trần Minh An" })).toBeVisible();
  await page.getByRole("button", { name: "Duyệt hồ sơ" }).click();
  await expect(page.getByText(/Không có hồ sơ chờ duyệt/)).toBeVisible();
  expect(decisionCount).toBe(2);
});

for (const role of ["HR", "CANDIDATE"]) {
  test(role + " mở URL trực tiếp thấy 403", async ({ page }) => {
    const user = await signIn(page, role);
    let staffRequests = 0;
    await page.route("**/api/v1/**", route => {
      if (route.request().url().includes("/staff/")) staffRequests++;
      return route.fulfill({ json: user, headers: { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "*" } });
    });
    for (const path of ["/staff/profile-reviews", "/staff/profile-reviews/06000000-0000-4000-8000-000000000001"]) {
      await page.goto(path);
      await expect(page.getByText(/403 · Không có quyền truy cập/)).toBeVisible();
    }
    expect(staffRequests).toBe(0);
  });
}
