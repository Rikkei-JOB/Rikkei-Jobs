import { test,expect } from "@playwright/test";
import { contactFixtures } from "./ut03-fixtures";
test("STAFF reviews requests and opens filtered access logs",async({page})=>{
  const state=await contactFixtures(page,"STAFF");
  await page.goto("/staff/contact-requests");
  await expect(page.getByRole("heading",{name:"Yêu cầu mở liên hệ",exact:true})).toBeVisible();
  await page.getByRole("button",{name:"Duyệt",exact:true}).click();
  await expect.poll(()=>state.allowed).toBe(true);
  await expect(page.getByRole("article").getByText("Đã duyệt",{exact:true})).toBeVisible();
  await page.goto("/staff/contact-access-logs");
  await page.getByLabel("Từ ngày").fill("2026-11-26");
  await page.getByLabel("Đến ngày").fill("2026-11-26");
  const request=page.waitForRequest(r=>r.url().includes("/staff/contact-access-logs?")&&r.url().includes("from="));
  await page.getByRole("button",{name:"Áp dụng bộ lọc"}).click();
  expect((await request).url()).toContain("2026-11-25T17");
  await expect(page.getByRole("cell",{name:/Công ty A/})).toBeVisible();
});
test("STAFF rejection requires reason and candidate sees companies",async({page})=>{
  await contactFixtures(page,"STAFF");
  await page.goto("/staff/contact-requests");
  await page.getByRole("button",{name:"Từ chối",exact:true}).click();
  await page.getByLabel("Lý do từ chối").fill("Chưa phù hợp");
  await page.getByRole("button",{name:"Xác nhận từ chối"}).click();
  await expect(page.getByRole("article").getByText("Đã từ chối",{exact:true})).toBeVisible();
});
test("candidate access history shows company and access source",async({page})=>{
  await contactFixtures(page,"CANDIDATE");
  await page.goto("/candidate/contact-access-logs");
  await expect(page.getByRole("heading",{name:"Công ty đã xem liên hệ",exact:true})).toBeVisible();
  await expect(page.getByText("Công ty A",{exact:true})).toBeVisible();
  await expect(page.getByText("Yêu cầu được duyệt",{exact:true})).toBeVisible();
});
