import { test,expect } from "@playwright/test";
import { contactFixtures,candidateId } from "./ut03-fixtures";
test("locked HR requests contact and does not reveal automatically",async({page})=>{
  const state=await contactFixtures(page);
  await page.goto("/hr/candidates/"+candidateId+"/contact");
  await expect(page.getByText("Liên hệ: đã ẩn",{exact:true})).toBeVisible();
  expect(state.views).toBe(0);
  await page.getByRole("button",{name:"Xin mở liên hệ",exact:true}).click();
  await page.getByLabel("JD liên quan").selectOption({label:"Java Developer"});
  await page.getByLabel("Lý do xin mở liên hệ").fill("Cần trao đổi vị trí Java");
  await page.getByRole("button",{name:"Gửi yêu cầu",exact:true}).click();
  await expect(page.getByText("Đang chờ STAFF duyệt")).toBeVisible();
  expect(state.views).toBe(0);
});
test("eligible HR only reveals on click and each new read is explicit",async({page})=>{
  const state=await contactFixtures(page,"HR",true);
  await page.goto("/hr/candidates/"+candidateId+"/contact");
  await expect(page.getByRole("button",{name:"Xem liên hệ",exact:true})).toBeVisible();
  expect(state.views).toBe(0);
  await page.getByRole("button",{name:"Xem liên hệ",exact:true}).click();
  await expect(page.getByText("contact-only@example.test",{exact:true})).toBeVisible();
  expect(state.views).toBe(1);
  await page.getByRole("button",{name:"Xem lại",exact:true}).click();
  await expect.poll(()=>state.views).toBe(2);
  const persisted=await page.evaluate(()=>localStorage.getItem("rikkei-jobs-auth"));
  expect(persisted).not.toContain("contact-only@example.test");
});
