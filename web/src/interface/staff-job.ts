import type { CompanyStatus } from "@/interface/company";
import type { Job, JobLevel, JobStatus } from "@/interface/job";

/** DN-07: trạng thái cán bộ thấy (không có DRAFT). */
export type StaffJobStatus = Exclude<JobStatus, "DRAFT">;

/** Một dòng GET /staff/jobs. submittedAt = lần HR gửi duyệt hoặc sửa gần nhất. */
export interface StaffJobItem {
  id: string;
  title: string;
  level: JobLevel | null;
  status: StaffJobStatus;
  deadline: string | null;
  submittedAt: string;
  reviewedAt: string | null;
  requiredSkillCount: number;
  rejectReason: string | null;
  companyId: string;
  companyName: string | null;
  companyStatus: CompanyStatus | null;
}

export interface StaffJobSummary { total: number; byStatus: Record<StaffJobStatus, number> }

export interface StaffJobDetail {
  job: Job;
  reviewedBy: string | null;
  company: { id: string; name: string; status: CompanyStatus; industry: string | null; size: string | null; city: string | null; logoUrl: string | null } | null;
}
