import type { JobLevel, JobSkill, JobType, WorkMode } from "@/interface/job";

/** Công ty rút gọn trong danh sách JD công khai (PublicCompanyRef). */
export interface PublicCompanyRef {
  id: string;
  name: string;
  logoUrl: string | null;
}

/** Công ty trong chi tiết JD công khai (PublicCompanyInfo). */
export interface PublicCompanyInfo extends PublicCompanyRef {
  industry: string | null;
  size: string | null;
  city: string | null;
  website: string | null;
  description: string | null;
}

/** Một dòng trong GET /jobs (PublicJobSummary). deadline dạng yyyy-MM-dd, publishedAt là ISO instant. */
export interface PublicJobSummary {
  id: string;
  title: string;
  level: JobLevel | null;
  type: JobType | null;
  workMode: WorkMode | null;
  city: string | null;
  salaryMin: number | null;
  salaryMax: number | null;
  deadline: string | null;
  publishedAt: string | null;
  company: PublicCompanyRef;
}

/** GET /jobs/{id} (PublicJobDetail): không có status, rejectReason, người duyệt. */
export interface PublicJobDetail {
  id: string;
  title: string;
  position: string | null;
  level: JobLevel | null;
  type: JobType | null;
  workMode: WorkMode | null;
  quantity: number | null;
  city: string | null;
  address: string | null;
  salaryMin: number | null;
  salaryMax: number | null;
  deadline: string | null;
  publishedAt: string | null;
  description: string | null;
  requirements: string | null;
  benefits: string | null;
  skills: JobSkill[];
  company: PublicCompanyInfo;
}
