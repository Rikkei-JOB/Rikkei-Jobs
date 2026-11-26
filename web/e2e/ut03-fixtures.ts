import { type Page } from "@playwright/test";
export const candidateId="30000000-0000-0000-0000-000000000003";
export const jobId="10000000-0000-0000-0000-000000000003";
export const companyId="50000000-0000-0000-0000-000000000003";
export const requestId="60000000-0000-0000-0000-000000000003";
export async function contactFixtures(page: Page, role="HR", allowed=false) {
  const user={id: role==="CANDIDATE"?candidateId:"70000000-0000-0000-0000-000000000003",fullName:"Người dùng UT-03",email:"viewer@test.local",role,companyId:role==="HR"?companyId:null,mustChangePassword:false};
  await page.addInitScript(user=>localStorage.setItem("rikkei-jobs-auth",JSON.stringify({state:{token:"ut03-token",user},version:0})),user);
  const state={allowed,status:null as string|null,views:0};
  await page.route("**/api/v1/**",async route=>{
    const url=new URL(route.request().url()), path=url.pathname,method=route.request().method();
    const headers={"Access-Control-Allow-Origin":"*","Access-Control-Allow-Headers":"Authorization,Content-Type","Access-Control-Allow-Methods":"GET,POST,OPTIONS"};
    if(method==="OPTIONS")return route.fulfill({status:204,headers});
    let body:unknown={};
    const request={id:requestId,companyId,companyName:"Công ty A",userId:candidateId,candidateName:"Ứng viên UT-03",jobId,reason:"Cần trao đổi vị trí Java",status:state.status??"PENDING",rejectionReason:state.status==="REJECTED"?"Chưa phù hợp":null,createdBy:user.id,createdAt:"2026-11-26T01:00:00Z",reviewedBy:null,reviewedAt:null};
    if(path.endsWith("/auth/me"))body=user;
    else if(path.endsWith("/companies/me/jobs"))body={items:[{id:jobId,title:"Java Developer",status:"APPROVED"}],page:1,size:20,total:1};
    else if(path.endsWith("/contact-access"))body={userId:candidateId,fullName:"Ứng viên UT-03",canSeeContact:state.allowed,via:state.allowed?"APPROVED_REQUEST":null,requestStatus:state.status,latestRequest:state.status?request:null};
    else if(path.endsWith("/contact") && method==="GET"){state.views++;body={email:"contact-only@example.test",phone:"0901234567"};}
    else if(path.endsWith("/contact-requests")&&method==="POST"){state.status="PENDING";return route.fulfill({status:201,headers,json:{...request,status:"PENDING"}});}
    else if(path.endsWith("/approve")){state.status="APPROVED";state.allowed=true;body={...request,status:"APPROVED"};}
    else if(path.endsWith("/reject")){state.status="REJECTED";body={...request,status:"REJECTED"};}
    else if(path.endsWith("/staff/contact-requests"))body={items:[request],total:1,page:1,size:20};
    else if(path.endsWith("/contact-access-logs"))body={items:[{id:"log-1",companyId,companyName:"Công ty A",hrId:user.id,hrName:"HR A",userId:candidateId,candidateName:"Ứng viên UT-03",via:"APPROVED_REQUEST",createdAt:"2026-11-26T01:00:00Z"}],page:1,size:20,total:1};
    else if(path.endsWith("/notifications"))body={items:[],total:0,page:1,size:20,unreadCount:0};
    return route.fulfill({headers,json:body});
  });
  return state;
}
