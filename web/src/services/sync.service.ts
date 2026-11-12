import { api } from "@/services/api";
import type {
  CandidateSync,
  ExternalRecord,
  OverridePayload,
  Page,
  SyncRun,
  SyncRunQuery,
} from "@/interface/sync";

/** API đồng bộ dữ liệu cho cán bộ (STAFF), profile-service qua gateway /api/v1/staff/... */
export const syncService = {
  listRuns: async ({ status, userId, page, size }: SyncRunQuery) =>
    (await api.get<Page<SyncRun>>("/staff/sync-runs", { params: { status, userId, page, size } })).data,

  searchCandidates: async (q: string) =>
    (await api.get<CandidateSync[]>("/staff/sync/candidates", { params: { q: q || undefined } })).data,

  /** Mock lỗi vẫn trả 200 với status = FAILED và error. */
  syncUser: async (userId: string) => (await api.post<SyncRun>(`/staff/sync/${userId}`)).data,

  runNightly: async () => (await api.post<{ message: string }>("/staff/sync/nightly")).data,

  listRecords: async (userId: string) =>
    (await api.get<ExternalRecord[]>("/staff/external-records", { params: { userId } })).data,

  override: async (recordId: string, payload: OverridePayload) =>
    (await api.put<ExternalRecord>(`/staff/external-records/${recordId}/override`, payload)).data,
};
