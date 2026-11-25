import type { Page } from "@playwright/test";
import { apiFixtures, signIn, jobId } from "./ut01-fixtures";

export const closedJobId = "10000000-0000-0000-0000-000000000002";
export const careerJobs = [
  { jobId, companyId: "50000000-0000-0000-0000-000000000001", jobTitle: "Lập trình viên Frontend React", companyName: "Rikkei Software", deadline: "2027-12-31T17:00:00Z", acceptingApplications: true },
  { jobId: closedJobId, companyId: "50000000-0000-0000-0000-000000000002", jobTitle: "Chuyên viên phát triển nền tảng công nghệ và trải nghiệm sản phẩm số cho doanh nghiệp", companyName: "Công ty Công nghệ Đặng và Cộng sự", deadline: "2026-01-01T17:00:00Z", acceptingApplications: false },
  { jobId: "10000000-0000-0000-0000-000000000003", companyId: "50000000-0000-0000-0000-000000000003", jobTitle: "Java Backend Developer", companyName: "Rikkei Solutions", deadline: "2027-11-30T17:00:00Z", acceptingApplications: true },
];

export const jobsApiPattern = /\/candidate-career-fixture\/jobs(?:\/[^/?]+)?(?:\?.*)?$/;
export const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "Authorization,Content-Type", "Access-Control-Allow-Methods": "GET,POST,OPTIONS" };

export async function careerFixtures(page: Page) {
  await signIn(page);
  await apiFixtures(page);
  await page.route(jobsApiPattern, route => {
    if (route.request().method() === "OPTIONS") return route.fulfill({ status: 204, headers: cors });
    const id = new URL(route.request().url()).pathname.split("/").at(-1);
    const job = careerJobs.find(item => item.jobId === id);
    return route.fulfill({ headers: cors, json: job ?? careerJobs });
  });
}
