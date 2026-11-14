import { test, expect, type Page } from "@playwright/test";
import { signIn, userId } from "./ut01-fixtures";

const java = { id: "00000000-0000-0000-0001-000000000001", code: "JAVA", name: "Java", group: "Backend" };
const english = { id: "00000000-0000-0000-0001-000000000002", code: "ENGLISH", name: "Tiếng Anh", group: "Soft skills" };
const profile = { userId, fullName: "Nguyễn Văn An", email: "an@example.test", program: "Kỹ sư", classCode: "KS25", courseName: "K25", avatarUrl: null, visibility: "PRIVATE" };
const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "Authorization,Content-Type", "Access-Control-Allow-Methods": "GET,PUT,OPTIONS" };
type SelfSkill = { skillId: string; selfScore: number };
async function setup(page: Page, initial: SelfSkill[] = [], failSave = false, failLoad = false) {
  await signIn(page);
  let self = initial; let rejectSave = failSave; let rejectLoad = failLoad;
  const writes: SelfSkill[][] = [];
  const auth: string[] = [];
  const skills = () => [java, english].filter(skill => skill === java || self.some(s => s.skillId === skill.id)).map(skill => ({
    skill, verifiedScore: skill === java ? 80 : null, level: skill === java ? "GOOD" : null,
    selfScore: self.find(s => s.skillId === skill.id)?.selfScore ?? null,
    evidences: [
      ...(skill === java ? [{ source: "LMS", title: "LMS Java", score: 80, recordedAt: "2026-11-01T00:00:00Z", verified: true }] : []),
      ...self.filter(s => s.skillId === skill.id).map(s => ({ source: "SELF", title: "Tự khai", score: s.selfScore, recordedAt: "2026-11-14T00:00:00Z", verified: false })),
    ],
  }));
  await page.route("**/api/v1/**", async route => {
    const request = route.request(); const path = new URL(request.url()).pathname;
    if (request.method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    if (path.endsWith("/auth/me")) return route.fulfill({ headers: cors, json: { id: userId, fullName: profile.fullName, email: profile.email, role: "CANDIDATE", companyId: null, mustChangePassword: false } });
    if (path.endsWith("/profiles/me")) return route.fulfill({ headers: cors, json: profile });
    if (path === "/api/v1/skills") return route.fulfill(rejectLoad ? { status: 503, headers: cors, json: { code: "EXTERNAL_UNAVAILABLE", message: "Danh mục tạm thời không sẵn sàng" } } : { headers: cors, json: [java, english] });
    if (path.endsWith("/profiles/me/skills")) return route.fulfill({ headers: cors, json: skills() });
    if (path.endsWith("/profiles/me/self-skills")) {
      auth.push(request.headers().authorization ?? ""); writes.push(request.postDataJSON());
      if (rejectSave) return route.fulfill({ status: 503, headers: cors, json: { code: "EXTERNAL_UNAVAILABLE", message: "Chưa lưu được kỹ năng" } });
      self = request.postDataJSON(); return route.fulfill({ headers: cors, json: skills() });
    }
    return route.fulfill({ headers: cors, json: [] });
  });
  return { writes, auth, recover: () => { rejectSave = false; rejectLoad = false; } };
}

test("adds edits and clears self skills while refreshing HS-01 and preserving verified score", async ({ page }) => {
  const state = await setup(page); await page.goto("/candidate/profile");
  await expect(page.getByRole("heading", { name: "Kỹ năng tự đánh giá", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Thêm kỹ năng tự khai" }).click();
  await page.getByLabel("Kỹ năng 1", { exact: true }).selectOption(english.id);
  await page.getByLabel("Điểm tự đánh giá 1", { exact: true }).fill("65");
  await page.getByRole("button", { name: "Lưu kỹ năng tự khai" }).click();
  await expect(page.getByRole("status").filter({ hasText: "Đã lưu kỹ năng tự khai" })).toBeVisible();
  const skillProfile = page.getByRole("region", { name: "Skill Profile" });
  await expect(skillProfile.getByText("Tiếng Anh", { exact: true })).toBeVisible();
  await expect(skillProfile.getByText("65.0", { exact: true })).toBeVisible();
  await expect(skillProfile.locator('[data-slot="card"]').filter({ has: page.getByText("Tiếng Anh", { exact: true }) }).getByText("Tự khai", { exact: true })).toBeVisible();
  await expect(skillProfile.getByText("80.0", { exact: true })).toBeVisible();
  await page.getByLabel("Điểm tự đánh giá 1", { exact: true }).fill("75");
  await page.getByRole("button", { name: "Lưu kỹ năng tự khai" }).click();
  await expect(skillProfile.getByText("75.0", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Bỏ kỹ năng 1", exact: true }).click();
  await page.getByRole("button", { name: "Lưu kỹ năng tự khai" }).click();
  await expect(skillProfile.getByText("Tiếng Anh", { exact: true })).toHaveCount(0);
  await expect(skillProfile.getByText("80.0", { exact: true })).toBeVisible();
  expect(state.writes).toEqual([[{ skillId: english.id, selfScore: 65 }], [{ skillId: english.id, selfScore: 75 }], []]);
  expect(state.auth).toEqual(["Bearer test-token", "Bearer test-token", "Bearer test-token"]);
  await page.reload(); await expect(page.getByLabel("Kỹ năng 1", { exact: true })).toHaveCount(0);
});

test("rejects invalid scores and prevents duplicate selection before saving", async ({ page }) => {
  const state = await setup(page, [{ skillId: english.id, selfScore: 50 }]); await page.goto("/candidate/profile");
  await page.getByLabel("Điểm tự đánh giá 1", { exact: true }).fill("101");
  await page.getByRole("button", { name: "Lưu kỹ năng tự khai" }).click();
  await expect(page.getByText("Điểm phải từ 0 đến 100", { exact: true })).toBeVisible();
  await page.getByLabel("Điểm tự đánh giá 1", { exact: true }).fill("1.1234567");
  await page.getByRole("button", { name: "Lưu kỹ năng tự khai" }).click();
  await expect(page.getByText("Điểm có tối đa 6 chữ số thập phân", { exact: true })).toBeVisible();
  await page.getByLabel("Điểm tự đánh giá 1", { exact: true }).fill("");
  await page.getByRole("button", { name: "Lưu kỹ năng tự khai" }).click();
  await expect(page.getByText("Vui lòng nhập điểm", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Thêm kỹ năng tự khai" }).click();
  await expect(page.getByLabel("Kỹ năng 2", { exact: true }).locator(`option[value="${english.id}"]`)).toBeDisabled();
  expect(state.writes).toEqual([]);
});

test("keeps draft after failed save and retries successfully", async ({ page }) => {
  const state = await setup(page, [{ skillId: english.id, selfScore: 50 }], true); await page.goto("/candidate/profile");
  await page.getByLabel("Điểm tự đánh giá 1", { exact: true }).fill("72");
  await page.getByRole("button", { name: "Lưu kỹ năng tự khai" }).click();
  await expect(page.getByRole("alert").filter({ hasText: "Chưa lưu được kỹ năng" })).toBeVisible();
  await expect(page.getByLabel("Điểm tự đánh giá 1", { exact: true })).toHaveValue("72");
  state.recover(); await page.getByRole("button", { name: "Lưu kỹ năng tự khai" }).click();
  await expect(page.getByRole("status").filter({ hasText: "Đã lưu kỹ năng tự khai" })).toBeVisible();
  expect(state.writes).toHaveLength(2);
});

test("retries catalogue loading without replacing existing form sections", async ({ page }) => {
  const state = await setup(page, [], false, true); await page.goto("/candidate/profile");
  await expect(page.getByRole("alert").filter({ hasText: "Danh mục tạm thời không sẵn sàng" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Thông tin cá nhân" })).toBeVisible();
  state.recover(); await page.getByRole("button", { name: "Thử lại kỹ năng" }).click();
  await page.getByRole("button", { name: "Thêm kỹ năng tự khai" }).click();
  await expect(page.getByLabel("Kỹ năng 1", { exact: true })).toBeVisible();
});

test("viewing another profile remains read-only without self-assessment controls", async ({ page }) => {
  await setup(page);
  await page.route("**/api/v1/profiles/*/skills", async route => route.fulfill({ headers: cors, json: [{
    skill: english, verifiedScore: null, selfScore: 65, level: null,
    evidences: [{ source: "SELF", title: "Tự khai", score: 65, recordedAt: "2026-11-14T00:00:00Z", verified: false }],
  }] }));
  await page.goto("/profile/90000000-0000-0000-0000-000000000001");
  await expect(page.getByText("Tiếng Anh", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Lưu kỹ năng tự khai" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Tính lại chỉ số" })).toHaveCount(0);
});

test("mixed verified and self skill shows both declarations without replacing verified score", async ({ page }) => {
  await setup(page, [{ skillId: java.id, selfScore: 60 }]); await page.goto("/candidate/profile");
  const javaCard = page.getByRole("region", { name: "Skill Profile" }).locator('[data-slot="card"]').filter({ has: page.getByText("Java", { exact: true }) });
  await expect(page.getByRole("region", { name: "Skill Profile" }).getByText("Tự khai", { exact: true }).first().locator("../..")).toContainText("1");
  await expect(javaCard.getByText("Tự khai", { exact: true })).toBeVisible();
  await expect(javaCard.getByText("80.0", { exact: true })).toBeVisible();
  await expect(javaCard.getByText("Điểm tự đánh giá: 60.0 / 100", { exact: true })).toBeVisible();
  await page.getByLabel("Điểm tự đánh giá 1", { exact: true }).fill("75");
  await page.getByRole("button", { name: "Lưu kỹ năng tự khai" }).click();
  await expect(javaCard.getByText("Điểm tự đánh giá: 75.0 / 100", { exact: true })).toBeVisible();
  await expect(javaCard.getByText("80.0", { exact: true })).toBeVisible();
});
