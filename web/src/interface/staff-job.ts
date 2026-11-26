import type { JobStatus } from "@/interface/job";
import type { PublicJobDetail } from "@/interface/public-job";

/** Công ty rút gọn trong danh sách JD của cán bộ (CompanyRef); name null khi không tra được công ty. */
export interface CompanyRef {
  id: string;
  name: string | null;
}

/** Cán bộ đã xử lý JD (StaffRef); fullName null khi không tra được identity-service. */
export interface StaffRef {
  id: string;
  fullName: string | null;
}

/** Một dòng trong GET /staff/jobs (StaffJobItem). deadline dạng yyyy-MM-dd; submittedAt là ISO instant (lần cập nhật cuối). */
export interface StaffJobItem {
  id: string;
  title: string;
  status: JobStatus;
  deadline: string | null;
  submittedAt: string;
  company: CompanyRef;
}

/**
 * GET /staff/jobs/{id} (StaffJobDetail): nội dung như ứng viên thấy cộng thông tin duyệt.
 * job.publishedAt chỉ có nghĩa khi JD đang APPROVED.
 */
export interface StaffJobDetail {
  job: PublicJobDetail;
  status: JobStatus;
  rejectReason: string | null;
  reviewedAt: string | null;
  reviewedBy: StaffRef | null;
  companyLocked: boolean;
}

export interface StaffJobListParams {
  status?: JobStatus;
  companyId?: string;
  page: number;
  size?: number;
}
