import { api } from "@/services/api";

export interface CandidateJob {
  jobId: string;
  companyId: string;
  jobTitle: string;
  companyName: string;
  deadline: string;
  acceptingApplications: boolean;
}

const jobsApi = (process.env.NEXT_PUBLIC_JOB_API_PATH ?? "").replace(/\/$/, "");

export const candidateJobService = {
  async list(): Promise<CandidateJob[]> {
    const { data } = await api.get<CandidateJob[]>(jobsApi);
    return data;
  },
  async get(jobId: string): Promise<CandidateJob> {
    const { data } = await api.get<CandidateJob>(`${jobsApi}/${encodeURIComponent(jobId)}`);
    return data;
  },
};
