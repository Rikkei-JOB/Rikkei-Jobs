export interface InterviewSlot { id: string; roundId: string; proposalId: string; startAt: string; endAt: string; status: "OPEN" | "PICKED" | "CANCELLED" }
export interface InterviewSchedule { id: string; roundId: string; slotId: string; startAt: string; endAt: string; meetUrl: string | null; status: "SCHEDULED" | "DONE" | "CANCELLED" }
export interface InterviewProposal { proposalId: string; roundId: string; slots: InterviewSlot[] }
export interface InterviewInvitation { roundId: string; roundName: string; jobTitle: string; companyName: string; interviewerEmail: string; slots: InterviewSlot[]; schedule: InterviewSchedule | null }
export interface InterviewCalendarItem { schedule: InterviewSchedule; candidateId: string; candidateName: string; jobTitle: string; roundName: string; interviewerEmail: string }
export interface InterviewPage<T> { items: T[]; total: number; page: number; size: number }
export interface SlotInput { startAt: string; endAt: string }
