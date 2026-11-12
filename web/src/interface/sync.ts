/** Kiểu dữ liệu màn "Đồng bộ dữ liệu" (NT-05), khớp SyncDtos của profile-service. */

export type SyncTrigger = "NIGHTLY" | "ON_OPEN" | "MANUAL";
export type SyncStatus = "SUCCESS" | "FAILED";
export type ExternalSource = "LMS" | "EXAM" | "PM";
export type OverrideField = "score" | "title";

export interface SyncCounts {
  fetched: number;
  inserted: number;
  updated: number;
  unchanged: number;
  skipped: number;
}

export interface SyncRun {
  id: string;
  trigger: SyncTrigger;
  userId: string | null;
  candidateName: string | null;
  lmsUserId: string | null;
  status: SyncStatus;
  startedAt: string;
  finishedAt: string;
  error: string | null;
  counts: SyncCounts | null;
  triggeredBy: string | null;
}

export interface CandidateSync {
  userId: string;
  fullName: string;
  email: string;
  lmsUserId: string | null;
  lastRun: SyncRun | null;
}

export interface OverrideEntry {
  field: OverrideField;
  value: string;
  reason: string;
  staffId: string;
  createdAt: string;
}

export interface ExternalRecord {
  id: string;
  source: ExternalSource;
  sourceKey: string | null;
  sourceCode: string;
  title: string;
  score: number;
  scoreMax: number;
  recordedAt: string;
  syncedAt: string | null;
  verified: boolean;
  payload: Record<string, unknown> | null;
  overriddenFields: OverrideField[];
  overrides: OverrideEntry[];
}

export interface OverridePayload {
  field: OverrideField;
  value: string;
  reason: string;
}

export interface Page<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

export interface SyncRunQuery {
  status?: SyncStatus;
  userId?: string;
  page: number;
  size: number;
}
