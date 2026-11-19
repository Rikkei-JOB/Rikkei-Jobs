import { test, expect, type Page, type Route } from "@playwright/test";
import type { MatchScore } from "../src/interface/hr-application";
const cors={ "Access-Control-Allow-Origin":"*", "Access-Control-Allow-Headers":"Authorization,Content-Type", "Access-Control-Allow-Methods":"GET,POST,OPTIONS" };
const stamp="2026-11-20T00:00:00Z";
function score(value=78, below=false): MatchScore { return { score:value, belowMinimum:below, met:[], missing:[{skill:"Java",required:true,minScore:80,candidateScore:value}] }; }
function detail(id: string, match: MatchScore|null, matchStatus?: string) {
  return {id,jobId:"job",jobTitle:`Java ${id}`,companyId:"company",companyName:"Company",status:"INTERVIEW",createdAt:stamp,updatedAt:stamp,candidate:{userId:`candidate-${id}`,fullName:`Candidate ${id}`,email:"candidate@test.local"},cvId:"cv",cvSnapshotUrl:"cv",coverLetter:null,match,...(matchStatus?{matchStatus}:{}),rounds:[{id:`round-${id}`,applicationId:id,roundNo:1,name:"Kỹ thuật",interviewerEmail:"hr@test.local",scheduledAt:null,result:"PASS",score:80,note:null,createdAt:stamp}],roundRevisions:[],history:[],allowedTransitions:["REJECTED"],offer:null,onboardConfirmedAt:null};
}
async function fixture(page: Page, role: "HR"|"STAFF"|"ADMIN"="HR", initial: MatchScore|null=null, initialStatus?: string) {
  const user={id:"viewer-old",role,fullName:"Viewer",email:"viewer@test.local",companyId:role==="HR"?"company":null,mustChangePassword:false};
  await page.addInitScript(user=>localStorage.setItem("rikkei-jobs-auth",JSON.stringify({state:{token:"token-old",user},version:0})),user);
  const state={user,initial,initialStatus,calls:0,detailCalls:0,retry:async(route: Route)=>route.fulfill({headers:cors,json:{matchStatus:"AVAILABLE",match:score()}})};
  await page.route("**/api/v1/**",async route=>{
    if(route.request().method()==="OPTIONS")return route.fulfill({status:204,headers:cors});
    const path=new URL(route.request().url()).pathname;
    if(path.endsWith("/auth/me"))return route.fulfill({headers:cors,json:state.user});
    if(path.endsWith("/auth/login")){state.user={...state.user,id:"viewer-new",fullName:"New Viewer"};return route.fulfill({headers:cors,json:{accessToken:"token-new",user:state.user,mustChangePassword:false}});}
    if(path.endsWith("/match")){state.calls++;return state.retry(route);}
    const id=path.match(/\/applications\/(app-[ab])$/)?.[1];
    if(id){state.detailCalls++;return route.fulfill({headers:cors,json:detail(id,id==="app-b"?score(88):state.initial,id==="app-b"?"AVAILABLE":state.initialStatus)});}
    if(path.endsWith("/hr/applications"))return route.fulfill({headers:cors,json:{items:[{id:"app-b",jobId:"job",jobTitle:"Java app-b",companyId:"company",companyName:"Company",userId:"candidate-b",candidateName:"Candidate app-b",status:"INTERVIEW",createdAt:stamp,updatedAt:stamp}],total:1,page:1,size:20}});
    return route.fulfill({headers:cors,json:{}});
  });
  return state;
}
test("score zero and belowMinimum are advisory; missing metadata remains compatible",async({page})=>{
  const state=await fixture(page,"HR",score(0,true));
  await page.goto("/hr/recruitment/app-a");
  await expect(page.getByText("Điểm phù hợp chỉ mang tính tham khảo")).toBeVisible();
  await expect(page.getByRole("img",{name:"Điểm phù hợp 0%"})).toBeVisible();
  await expect(page.getByText("Thiếu kỹ năng bắt buộc tối thiểu")).toBeVisible();
  await expect(page.getByRole("button",{name:"Gửi offer",exact:true})).toBeEnabled();
  await expect(page.getByRole("button",{name:"Chuyển sang Không đạt"})).toBeEnabled();expect(state.calls).toBe(0);
  await page.screenshot({path:"../docs/api/screenshots/ut02-match-available.png",fullPage:true});
});
test("empty skill lists are available; ADMIN never shows matching or retry",async({page})=>{
  const state=await fixture(page,"STAFF",{score:0,belowMinimum:false,met:[],missing:[]},"AVAILABLE");
  await page.goto("/staff/applications/app-a");await expect(page.getByText("JD chưa khai báo kỹ năng")).toBeVisible();expect(state.calls).toBe(0);
});
test("ADMIN does not display the score block",async({page})=>{
  const state=await fixture(page,"ADMIN",null,"NOT_REQUESTED");await page.goto("/admin/applications/app-a");
  await expect(page.getByText("Candidate app-a",{exact:true})).toBeVisible();await expect(page.getByRole("heading",{name:"Điểm khớp với JD"})).toHaveCount(0);expect(state.calls).toBe(0);
});
test("retry updates only match and preserves unsaved fields; double click sends once",async({page})=>{
  const state=await fixture(page,"HR",null,"UNAVAILABLE");let release!:()=>void;const wait=new Promise<void>(resolve=>{release=resolve;});
  state.retry=async route=>{await wait;await route.fulfill({headers:cors,json:{matchStatus:"AVAILABLE",match:score(78)}});};
  await page.goto("/hr/recruitment/app-a");await expect(page.getByText(/Tạm thời không lấy được điểm phù hợp/)).toBeVisible();
  await page.getByLabel("Ghi chú chuyển bước").fill("Đang cân nhắc");await page.getByLabel("Tên vòng",{exact:true}).fill("Vòng mới");await page.getByLabel("Lương gross/tháng (VND)").fill("12000000");
  await page.getByRole("button",{name:"Thử lại",exact:true}).dblclick();await expect.poll(()=>state.calls).toBe(1);
  await expect(page.getByRole("button",{name:"Gửi offer",exact:true})).toBeEnabled();release();
  await expect(page.getByRole("img",{name:"Điểm phù hợp 78%"})).toBeVisible();
  await expect(page.getByLabel("Ghi chú chuyển bước")).toHaveValue("Đang cân nhắc");await expect(page.getByLabel("Tên vòng",{exact:true})).toHaveValue("Vòng mới");await expect(page.getByLabel("Lương gross/tháng (VND)")).toHaveValue("12.000.000");expect(state.detailCalls).toBe(1);
  await page.setViewportSize({width:390,height:844});
  await page.evaluate(()=>{window.getSelection()?.removeAllRanges();window.scrollTo(0,0);});
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
  await page.screenshot({path:"../docs/api/screenshots/ut02-match-mobile.png",fullPage:true});
});
test("retry failure clears old score and preserves permission error",async({page})=>{
  const state=await fixture(page,"HR",score(78),"AVAILABLE");
  state.retry=async route=>route.fulfill({status:403,headers:cors,json:{code:"FORBIDDEN",message:"Bạn không có quyền xem điểm."}});
  await page.goto("/hr/recruitment/app-a");await expect(page.getByRole("img",{name:"Điểm phù hợp 78%"})).toBeVisible();await page.getByRole("button",{name:"Tải lại điểm",exact:true}).click();
  await expect(page.getByText("Bạn không có quyền xem điểm.")).toBeVisible();await expect(page.getByRole("img",{name:/Điểm phù hợp/})).toHaveCount(0);
  await expect(page.getByRole("button",{name:"Gửi offer",exact:true})).toBeEnabled();
  await page.screenshot({path:"../docs/api/screenshots/ut02-match-unavailable.png",fullPage:true});
  state.initial=score(86);state.initialStatus="AVAILABLE";
  await page.getByRole("button",{name:"Lưu kết quả",exact:true}).click();
  await expect(page.getByRole("img",{name:"Điểm phù hợp 86%"})).toBeVisible();
  await expect(page.getByText("Bạn không có quyền xem điểm.")).toHaveCount(0);
});
test("late retry for old application cannot replace new application's score",async({page})=>{
  const state=await fixture(page,"HR",null,"UNAVAILABLE");let release!:()=>void,finished!:()=>void;const hold=new Promise<void>(r=>{release=r;}),done=new Promise<void>(r=>{finished=r;});
  state.retry=async route=>{await hold;try{await route.fulfill({headers:cors,json:{matchStatus:"AVAILABLE",match:score(55)}});}catch{}finally{finished();}};
  await page.goto("/hr/recruitment/app-a");await page.getByRole("button",{name:"Thử lại",exact:true}).click();await expect.poll(()=>state.calls).toBe(1);
  await page.getByRole("link",{name:"← Danh sách đơn ứng tuyển"}).click();await page.getByRole("link").filter({hasText:"Candidate app-b"}).click();
  await expect(page.getByRole("img",{name:"Điểm phù hợp 88%"})).toBeVisible();release();await done;
  await expect(page.getByRole("img",{name:"Điểm phù hợp 88%"})).toBeVisible();await expect(page.getByRole("img",{name:"Điểm phù hợp 55%"})).toHaveCount(0);
});
test("late retry from previous login cannot affect the next viewer",async({page})=>{
  const state=await fixture(page,"HR",null,"UNAVAILABLE");let release!:()=>void,finished!:()=>void;const hold=new Promise<void>(r=>{release=r;}),done=new Promise<void>(r=>{finished=r;});
  state.retry=async route=>{await hold;try{await route.fulfill({headers:cors,json:{matchStatus:"AVAILABLE",match:score(55)}});}catch{}finally{finished();}};
  await page.goto("/hr/recruitment/app-a");await page.getByRole("button",{name:"Thử lại",exact:true}).click();await expect.poll(()=>state.calls).toBe(1);
  await page.getByRole("button",{name:"Đăng xuất"}).click();await page.getByRole("tab",{name:"Doanh nghiệp",exact:true}).click();await page.getByLabel("Email",{exact:true}).fill("new@test.local");await page.getByLabel("Mật khẩu",{exact:true}).fill("Password123!");await page.getByRole("button",{name:"Đăng nhập",exact:true}).click();
  await page.getByRole("navigation",{name:"Menu chính"}).getByRole("link",{name:"Tuyển dụng",exact:true}).click();await page.getByRole("link").filter({hasText:"Candidate app-b"}).click();await expect(page.getByRole("img",{name:"Điểm phù hợp 88%"})).toBeVisible();
  release();await done;await expect(page.getByRole("img",{name:"Điểm phù hợp 55%"})).toHaveCount(0);
});
