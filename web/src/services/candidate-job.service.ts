import { api } from "@/services/api";

export interface CandidateJob {
  jobId: string;
  companyId: string;
  jobTitle: string;
  companyName: string;
  deadline: string;
  acceptingApplications: boolean;
}

/**
 * TẠM THỜI (trước DN-07 Task 7 / DN-04): ngoài mock (`http://localhost:9000/jobs`, trả mảng CandidateJob),
 * service này đọc được cả API thật của company-service (`NEXT_PUBLIC_JOB_API_PATH=/jobs` qua gateway),
 * vốn trả `{items: PublicJobSummary[]}` và `PublicJobDetail`. API thật chỉ trả JD đang hiện với ứng viên
 * (APPROVED, còn hạn, công ty hoạt động), nên luôn nhận hồ sơ. Sẽ thay bằng giao diện chuẩn ở DN-04.
 */
interface RealJob {
  id: string;
  title: string;
  deadline: string;
  company: { id: string; name: string };
}

const jobsApi = (process.env.NEXT_PUBLIC_JOB_API_PATH ?? "").replace(/\/$/, "");

function fromReal(job: RealJob): CandidateJob {
  return {
    jobId: job.id,
    companyId: job.company.id,
    jobTitle: job.title,
    companyName: job.company.name,
    deadline: job.deadline,
    acceptingApplications: true,
  };
}

function isReal(job: CandidateJob | RealJob): job is RealJob {
  return "id" in job && "company" in job;
}

export const candidateJobService = {
  async list(): Promise<CandidateJob[]> {
    const { data } = await api.get<CandidateJob[] | { items: RealJob[] }>(jobsApi, { params: { size: 100 } });
    return Array.isArray(data) ? data : data.items.map(fromReal);
  },
  async get(jobId: string): Promise<CandidateJob> {
    const { data } = await api.get<CandidateJob | RealJob>(`${jobsApi}/${encodeURIComponent(jobId)}`);
    return isReal(data) ? fromReal(data) : data;
  },
};
