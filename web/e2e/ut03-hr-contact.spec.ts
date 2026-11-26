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
test("in-flight contact is not shown after moving to another candidate",async({page})=>{
  await contactFixtures(page,"HR",true);
  let started!:()=>void;
  const inFlight=new Promise<void>(resolve=>{started=resolve;});
  await page.route("**/api/v1/candidates/"+candidateId+"/contact",async route=>{
    if(route.request().method()==="OPTIONS")return route.fallback();
    started();
    await new Promise(resolve=>setTimeout(resolve,500));
    await route.fulfill({headers:{"Access-Control-Allow-Origin":"*"},json:{email:"stale-private@example.test",phone:"0900000000"}});
  });
  await page.goto("/hr/candidates/"+candidateId+"/contact");
  await page.getByRole("button",{name:"Xem liên hệ",exact:true}).click();
  await inFlight;
  await page.goto("/hr/candidates/30000000-0000-0000-0000-000000000004/contact");
  await expect(page.getByRole("button",{name:"Xem liên hệ",exact:true})).toBeVisible();
  await expect(page.getByText("stale-private@example.test")).toHaveCount(0);
});
test("failed reveal returns no cached contact and supports explicit retry",async({page})=>{
  await contactFixtures(page,"HR",true);
  await page.route("**/api/v1/candidates/"+candidateId+"/contact",route=>route.request().method()==="OPTIONS"?route.fallback():route.fulfill({status:503,headers:{"Access-Control-Allow-Origin":"*"},json:{code:"PROFILE_UNAVAILABLE",message:"Dịch vụ tạm thời không sẵn sàng"}}));
  await page.goto("/hr/candidates/"+candidateId+"/contact");
  await page.getByRole("button",{name:"Xem liên hệ",exact:true}).click();
  await expect(page.getByRole("region",{name:"Liên hệ ứng viên"}).getByRole("alert")).toContainText("Dịch vụ tạm thời không sẵn sàng");
  await expect(page.getByText("contact-only@example.test")).toHaveCount(0);
  await expect(page.getByRole("button",{name:"Xem liên hệ",exact:true})).toBeEnabled();
});
