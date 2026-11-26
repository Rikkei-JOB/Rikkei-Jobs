export type ContactVia = "APPLICATION" | "APPROVED_REQUEST";
export type ContactRequestStatus = "PENDING" | "APPROVED" | "REJECTED";
export interface ContactView { email: string | null; phone: string | null }
export interface ContactRequest {
  id: string; companyId: string; companyName: string | null; userId: string; candidateName: string | null;
  jobId: string; reason: string; status: ContactRequestStatus; rejectionReason: string | null;
  createdBy: string; createdAt: string; reviewedBy: string | null; reviewedAt: string | null;
}
export interface ContactAccess {
  userId: string; fullName: string; canSeeContact: boolean; via: ContactVia | null;
  requestStatus: ContactRequestStatus | null; latestRequest: ContactRequest | null;
}
export interface ContactLog {
  id: string; companyId: string; companyName: string; hrId: string; hrName: string | null;
  userId: string; candidateName: string | null; via: ContactVia; createdAt: string;
}
export interface ContactPage<T> { items: T[]; total: number; page: number; size: number }
export interface ContactLogFilters { companyId?: string; userId?: string; from?: string; to?: string; page?: number; size?: number }
