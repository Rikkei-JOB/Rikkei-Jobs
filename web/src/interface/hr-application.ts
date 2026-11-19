import type { ApplicationHistoryItem, ApplicationStatus } from "@/interface/application";
import type { Offer } from "@/interface/offer";
import type { InterviewSlot, InterviewSchedule } from "@/interface/interview";

export type RoundResult = "PENDING" | "PASS" | "FAIL";
export interface InterviewRound {
  id: string; applicationId: string; roundNo: number; name: string; interviewerEmail: string;
  scheduledAt: string | null; result: RoundResult; score: number | null; note: string | null; createdAt: string;
  slots?: InterviewSlot[]; schedule?: InterviewSchedule | null;
}
export interface RoundRevision {
  id: string; roundId: string; actorId: string; oldResult: RoundResult; newResult: RoundResult;
  oldScore: number | null; newScore: number | null; oldNote: string | null; newNote: string | null; createdAt: string;
}
export interface MatchSkill {
  skill: string; required: boolean; minScore: number; candidateScore: number;
}
export interface MatchScore {
  score: number; belowMinimum: boolean; met: MatchSkill[]; missing: MatchSkill[];
}
export interface HrApplicationSummary {
  id: string; jobId: string; jobTitle: string; companyId: string; companyName: string;
  userId: string; candidateName: string; status: ApplicationStatus; createdAt: string; updatedAt: string;
}
export interface HrApplicationPage { items: HrApplicationSummary[]; total: number; page: number; size: number }
export interface HrApplicationDetail {
  id: string; jobId: string; jobTitle: string; companyId: string; companyName: string;
  status: ApplicationStatus; createdAt: string; updatedAt: string;
  candidate: { userId: string; fullName: string; email: string; phone?: string | null };
  cvId: string; cvSnapshotUrl: string; coverLetter: string | null;
  match: MatchScore | null; rounds: InterviewRound[]; roundRevisions: RoundRevision[];
  history: ApplicationHistoryItem[]; allowedTransitions: ApplicationStatus[];
  offer: Offer | null;
  onboardConfirmedAt: string | null;
}
export interface MentorRoundItem {
  jobTitle: string; companyName: string; applicationStatus: ApplicationStatus; round: InterviewRound;
}
export interface MentorRoundPage { items: MentorRoundItem[]; total: number; page: number; size: number }
