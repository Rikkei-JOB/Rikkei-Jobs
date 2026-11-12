export type ApplicationStatus = "SUBMITTED" | "SCREENING" | "INTERVIEW" | "OFFER" | "HIRED" | "REJECTED" | "WITHDRAWN";

export interface ApplicationSummary {
  id: string; jobId: string; jobTitle: string; companyId: string; companyName: string;
  status: ApplicationStatus; createdAt: string; updatedAt: string; canWithdraw: boolean;
}
export interface ApplicationHistoryItem {
  id: string; fromStatus: ApplicationStatus | null; toStatus: ApplicationStatus;
  actorId: string; note: string | null; createdAt: string;
}
export interface ApplicationDetail extends ApplicationSummary {
  cvId: string; cvSnapshotUrl: string; coverLetter: string | null; history: ApplicationHistoryItem[];
}
export interface ApplicationPage { items: ApplicationSummary[]; total: number; page: number; size: number }
export interface ApplyPayload { cvId: string; coverLetter?: string }
export interface CvChoice { id: string; name: string; targetPosition: string | null }
