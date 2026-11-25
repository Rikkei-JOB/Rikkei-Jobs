export type JobLevel = "INTERN" | "FRESHER" | "JUNIOR" | "MIDDLE" | "SENIOR";
export type JobType = "FULLTIME" | "PARTTIME" | "INTERNSHIP";
export type WorkMode = "ONSITE" | "REMOTE" | "HYBRID";
export type JobStatus = "DRAFT" | "PENDING" | "APPROVED" | "REJECTED" | "CLOSED";

/** Kỹ năng yêu cầu của JD; minScore nguyên 1-100. */
export interface JobSkill {
  skillId: string;
  required: boolean;
  minScore: number;
}

/** JobResponse của company-service (GET/POST/PUT /jobs...). Ngày: chuỗi ISO (deadline dạng yyyy-MM-dd). */
export interface Job {
  id: string;
  companyId: string;
  title: string;
  position: string | null;
  level: JobLevel | null;
  type: JobType | null;
  quantity: number | null;
  city: string | null;
  address: string | null;
  workMode: WorkMode | null;
  salaryMin: number | null;
  salaryMax: number | null;
  description: string | null;
  requirements: string | null;
  benefits: string | null;
  deadline: string | null;
  status: JobStatus;
  rejectReason: string | null;
  reviewedAt: string | null;
  createdAt: string;
  updatedAt: string;
  skills: JobSkill[];
}

/** Một dòng trong GET /companies/me/jobs. */
export interface JobSummary {
  id: string;
  title: string;
  level: JobLevel | null;
  status: JobStatus;
  deadline: string | null;
  updatedAt: string;
  requiredSkillCount: number;
  rejectReason: string | null;
}

/** Body POST /jobs và PUT /jobs/{id} (JobRequest): chỉ title bắt buộc khi lưu nháp, trường trống gửi null. */
export interface JobPayload {
  title: string;
  position: string | null;
  level: JobLevel | null;
  type: JobType | null;
  quantity: number | null;
  city: string | null;
  address: string | null;
  workMode: WorkMode | null;
  salaryMin: number | null;
  salaryMax: number | null;
  description: string | null;
  requirements: string | null;
  benefits: string | null;
  deadline: string | null;
  skills: JobSkill[];
}

/** GET /companies/me/jobs/summary: số JD theo trạng thái cho các thẻ ở đầu danh sách. */
export interface JobStatusSummary {
  total: number;
  byStatus: Record<JobStatus, number>;
  expiringSoon: number;
}

export interface JobListParams {
  status?: JobStatus;
  page: number;
  size?: number;
}
