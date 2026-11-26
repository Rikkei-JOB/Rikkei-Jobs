import { expect, test } from "@playwright/test";
import { mkdir } from "node:fs/promises";
import { join } from "node:path";
import type { User } from "../src/interface/auth";

test("ứng viên gửi, sửa sau khi bị trả lại, công khai rồi rút", async ({ page, request }, info) => {
  test.skip(process.env.HS05_LIVE !== "1", "Chạy với stack dev và database nghiệm thu riêng.");
  test.setTimeout(120_000);
  const base = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";
  async function sso(lmsUserId: string) {
    const ticket = await request.post(base + "/auth/dev/sso-ticket", { data: { lmsUserId } });
    expect(ticket.status()).toBe(200);
    const response = await request.post(base + "/auth/sso", { data: { ticket: (await ticket.json()).ticket } });
    expect(response.status()).toBe(200);
    return await response.json() as { accessToken: string; user: User };
  }
  const candidate = await sso("lms-sv-001");
  const staff = await sso("lms-st-001");
  const hr = await request.post(base + "/auth/login", { data: { email: "hr1@rikkeijobs.test", password: "Test@123" } });
  expect(hr.status()).toBe(200);
  const hrHeaders = { Authorization: "Bearer " + (await hr.json()).accessToken };
  const headers = { Authorization: "Bearer " + staff.accessToken };
  const decision = base + "/staff/profiles/" + candidate.user.id;
  const skills = base + "/profiles/" + candidate.user.id + "/skills";
  await page.addInitScript(session => localStorage.setItem("rikkei-jobs-auth",
    JSON.stringify({ state: { token: session.accessToken, user: session.user }, version: 0 })), candidate);
  await page.goto("/candidate/profile");
  const panel = page.getByRole("region", { name: "Công khai hồ sơ" });
  const submit = panel.getByRole("button", { name: "Gửi duyệt", exact: true });
  await expect(submit).toBeVisible();
  await expect(submit).toBeDisabled();
  expect((await request.post(decision + "/approve", { headers })).status()).toBe(409);
  expect((await request.get(skills, { headers: hrHeaders })).status()).toBe(404);
  const directory = process.env.HS05_SCREENSHOT_DIR ?? info.outputDir;
  await mkdir(directory, { recursive: true });
  async function capture(name: string) {
    await panel.screenshot({ path: join(directory, name + ".png"), style: "nextjs-portal { display: none !important; }" });
  }
  await capture("private-desktop");
  await panel.getByRole("checkbox").check();
  await submit.click();
  await expect(panel.getByRole("status")).toContainText("Chờ duyệt");
  await expect(panel.getByRole("button", { name: "Rút yêu cầu" })).toBeEnabled();
  await capture("pending-desktop");
  const before = await request.get(base + "/staff/profile-reviews?status=PENDING", { headers });
  expect((await before.json()).items.some((p: {userId: string}) => p.userId === candidate.user.id)).toBeTruthy();
  expect((await request.post(decision + "/reject", { headers, data: {} })).status()).toBe(400);
  const reason = "Vui lòng bổ sung mục tiêu nghề nghiệp.";
  expect((await request.post(decision + "/reject", { headers, data: { reason } })).status()).toBe(200);
  // The stale pending button gets 409 and must reload the latest decision and history.
  await panel.getByRole("button", { name: "Rút yêu cầu" }).click();
  await expect(panel.getByRole("status")).toContainText("Đã trả lại");
  await expect(panel.getByText(reason, { exact: true }).first()).toBeVisible();
  await page.reload();
  await expect(panel.getByRole("button", { name: "Gửi lại" })).toBeDisabled();
  await page.setViewportSize({ width: 390, height: 844 });
  await capture("rejected-mobile");
  const summary = page.getByLabel("Giới thiệu bản thân");
  const target = "Mong muốn phát triển ứng dụng backend Java.";
  await summary.fill(await summary.inputValue() === target ? "Mục tiêu nghề nghiệp: lập trình viên Java." : target);
  await page.getByRole("button", { name: "Lưu thông tin" }).click();
  await expect(page.getByText("Đã lưu thông tin", { exact: false }).first()).toBeVisible();
  await panel.getByRole("checkbox").check();
  await panel.getByRole("button", { name: "Gửi lại" }).click();
  await expect(panel.getByRole("status")).toContainText("Chờ duyệt");
  expect((await request.post(decision + "/approve", { headers })).status()).toBe(200);
  expect((await request.get(skills, { headers: hrHeaders })).status()).toBe(200);
  await page.reload();
  await expect(panel.getByRole("status")).toContainText("Đã công khai");
  await capture("public-mobile");
  await panel.getByRole("button", { name: "Rút công khai" }).click();
  await expect(submit).toBeDisabled();
  expect((await request.get(skills, { headers: hrHeaders })).status()).toBe(404);
  const history = await request.get(base + "/profiles/me/review-history", {
    headers: { Authorization: "Bearer " + candidate.accessToken },
  });
  const recent = (await history.json()).slice(0, 5);
  expect(recent.map((e: {action: string}) => e.action)).toEqual(["WITHDRAW", "APPROVE", "SUBMIT", "REJECT", "SUBMIT"]);
  expect(recent.map((e: {actorId: string}) => e.actorId)).toEqual([
    candidate.user.id, staff.user.id, candidate.user.id, staff.user.id, candidate.user.id,
  ]);
  for (const e of recent) expect(Date.parse(e.createdAt)).toBeGreaterThan(0);
});
