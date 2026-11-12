import { type Page } from "@playwright/test";

export const jobId = "10000000-0000-0000-0000-000000000001";
export const applicationId = "20000000-0000-0000-0000-000000000001";
export const userId = "30000000-0000-0000-0000-000000000001";
export const cvId = "40000000-0000-0000-0000-000000000001";
const candidateUser = {
  id: userId, fullName: "Nguyễn Văn An", email: "candidate@example.test", role: "CANDIDATE", companyId: null, mustChangePassword: false,
};
export const application = {
  id: applicationId, jobId, jobTitle: "Java Developer", companyId: "company-a", companyName: "Rikkei Software",
  status: "SUBMITTED", createdAt: "2026-11-12T01:00:00Z", updatedAt: "2026-11-12T01:00:00Z", canWithdraw: true,
  cvId, cvSnapshotUrl: `/api/v1/recruitment/applications/${applicationId}/cv`, coverLetter: "Xin chào",
  history: [{ id: "history-1", fromStatus: null, toStatus: "SUBMITTED", actorId: userId, note: null, createdAt: "2026-11-12T01:00:00Z" }],
};

export async function signIn(page: Page) {
  await page.addInitScript((user) => {
    localStorage.setItem("rikkei-jobs-auth", JSON.stringify({ state: { token: "test-token", user }, version: 0 }));
  }, candidateUser);
}

export async function apiFixtures(page: Page) {
  await page.route("**/api/v1/**", async (route) => {
    const url = new URL(route.request().url());
    const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "Authorization,Content-Type", "Access-Control-Allow-Methods": "GET,POST,OPTIONS" };
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    let body: unknown = {};
    if (url.pathname.endsWith("/auth/me")) body = candidateUser;
    else if (url.pathname.endsWith("/profile/cvs")) body = [
      { id: cvId, name: "CV Backend", targetPosition: "Java Developer" },
      { id: "40000000-0000-0000-0000-000000000002", name: "CV Frontend", targetPosition: "React Developer" },
    ];
    else if (url.pathname.endsWith(`/jobs/${jobId}/apply`)) return route.fulfill({ status: 201, headers: cors, json: application });
    else if (url.pathname.endsWith(`/me/applications/${applicationId}`)) body = application;
    else if (url.pathname.endsWith("/me/applications")) body = { items: [application], total: 1, page: 1, size: 20 };
    return route.fulfill({ headers: cors, json: body });
  });
}
