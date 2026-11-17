"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { syncService } from "@/services/sync.service";
import type { OverridePayload, SyncRunQuery } from "@/interface/sync";

export const syncKeys = {
  all: ["sync"] as const,
  runs: (query: SyncRunQuery) => ["sync", "runs", query] as const,
  candidates: (q: string) => ["sync", "candidates", q] as const,
  records: (userId: string) => ["sync", "records", userId] as const,
};

export function useSyncRuns(query: SyncRunQuery) {
  return useQuery({
    queryKey: syncKeys.runs(query),
    queryFn: () => syncService.listRuns(query),
    placeholderData: keepPreviousData,
  });
}

export function useSyncCandidates(q: string) {
  return useQuery({ queryKey: syncKeys.candidates(q), queryFn: () => syncService.searchCandidates(q) });
}

export function useExternalRecords(userId: string | null) {
  return useQuery({
    queryKey: syncKeys.records(userId ?? ""),
    queryFn: () => syncService.listRecords(userId as string),
    enabled: Boolean(userId),
  });
}

/** Đồng bộ lại một ứng viên; xong thì làm mới bảng lần chạy, danh sách ứng viên và bản ghi. */
export function useSyncUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: syncService.syncUser,
    onSettled: () => queryClient.invalidateQueries({ queryKey: syncKeys.all }),
  });
}

export function useRunNightly() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: syncService.runNightly,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: syncKeys.all }),
  });
}

export function useOverrideRecord(userId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ recordId, payload }: { recordId: string; payload: OverridePayload }) =>
      syncService.override(recordId, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: syncKeys.records(userId) }),
  });
}
