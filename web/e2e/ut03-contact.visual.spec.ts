import { test,expect } from "@playwright/test";
import { resolve } from "node:path";
import { mkdir } from "node:fs/promises";
import { contactFixtures,candidateId } from "./ut03-fixtures";
for (const width of [1440,768,390]) {
  for (const [role,path,name] of [
    ["HR","/hr/candidates/"+candidateId+"/contact","hr"],
    ["STAFF","/staff/contact-requests","staff-requests"],
    ["STAFF","/staff/contact-access-logs","staff-logs"],
    ["CANDIDATE","/candidate/contact-access-logs","candidate-logs"],
  ]) test(name+" at "+width,async({page})=>{
    await page.setViewportSize({width,height:900});
    await contactFixtures(page,role);
    await page.goto(path);
    await expect(page.getByRole("heading",{level:1})).toBeVisible();
    await expect(page.getByText("Đang tải…",{exact:true})).toHaveCount(0);
    expect(await page.evaluate(()=>document.documentElement.scrollWidth)).toBeLessThanOrEqual(width);
    const dir=resolve(__dirname,"../../docs/api/screenshots/ut03");await mkdir(dir,{recursive:true});
    await page.screenshot({path:resolve(dir,name+"-"+width+".png"),fullPage:true});
  });
}
