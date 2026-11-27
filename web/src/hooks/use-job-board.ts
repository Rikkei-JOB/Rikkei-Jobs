"use client";

import { useCallback, useSyncExternalStore } from "react";
import { keepPreviousData, useInfiniteQuery, useQuery, useQueryClient } from "@tanstack/react-query";
import { jobBoardService, type BoardFilters } from "@/services/job-board.service";

export const JOB_PAGE_SIZE = 20;

/** Tin công khai giống nhau với mọi người dùng nên khoá cache không gắn id người dùng. */
export const jobBoardKeys = {
  skills: ["job-board", "skills"] as const,
  search: (filters: BoardFilters, companyId: string | undefined, size: number) => ["job-board", "search", filters, companyId ?? null, size] as const,
  job: (id: string) => ["job-board", "job", id] as const,
  companies: ["job-board", "companies"] as const,
  company: (id: string) => ["job-board", "company", id] as const,
};

function useSkillCatalog() {
  return useQuery({ queryKey: jobBoardKeys.skills, queryFn: jobBoardService.skillCatalog, staleTime: 10 * 60_000 });
}

/** Tìm tin theo bộ lọc, tải thêm từng trang; giữ kết quả cũ trong lúc đổi bộ lọc để danh sách không nháy. */
export function useBoardSearch(filters: BoardFilters, options: { companyId?: string; size?: number } = {}) {
  const { data: catalog } = useSkillCatalog();
  const size = options.size ?? JOB_PAGE_SIZE;
  return useInfiniteQuery({
    queryKey: jobBoardKeys.search(filters, options.companyId, size),
    queryFn: ({ pageParam }) => jobBoardService.search(filters, pageParam, size, catalog ?? [], options.companyId),
    initialPageParam: 1,
    getNextPageParam: last => (last.page * last.size < last.total ? last.page + 1 : undefined),
    enabled: catalog !== undefined,
    placeholderData: keepPreviousData,
    staleTime: 60_000,
  });
}

export function useBoardJob(id: string | undefined) {
  const { data: catalog } = useSkillCatalog();
  return useQuery({
    queryKey: jobBoardKeys.job(id ?? ""),
    queryFn: () => jobBoardService.get(id!, catalog ?? []),
    enabled: Boolean(id) && catalog !== undefined,
    staleTime: 60_000,
  });
}

/** Tải trước chi tiết khi rê chuột vào thẻ, để bấm vào là hiện ngay. */
export function usePrefetchBoardJob() {
  const client = useQueryClient();
  return useCallback((id: string) => {
    const catalog = client.getQueryData<Awaited<ReturnType<typeof jobBoardService.skillCatalog>>>(jobBoardKeys.skills);
    if (!catalog) return;
    void client.prefetchQuery({ queryKey: jobBoardKeys.job(id), queryFn: () => jobBoardService.get(id, catalog), staleTime: 60_000 });
  }, [client]);
}

export function useBoardCompanies() {
  return useQuery({ queryKey: jobBoardKeys.companies, queryFn: jobBoardService.companies, staleTime: 60_000 });
}

export function useBoardCompany(id: string) {
  return useQuery({ queryKey: jobBoardKeys.company(id), queryFn: () => jobBoardService.company(id), staleTime: 60_000 });
}

/* Việc làm đã lưu: chỉ lưu trên trình duyệt này cho tới khi có API lưu tin. */
const SAVED_KEY = "rikkei-jobs:saved-jobs";
const listeners = new Set<() => void>();
let cachedRaw: string | null | undefined;
let cachedIds: string[] = [];

function readSaved(): string[] {
  let raw: string | null = null;
  try { raw = window.localStorage.getItem(SAVED_KEY); } catch { raw = null; }
  if (raw !== cachedRaw) {
    cachedRaw = raw;
    try { cachedIds = raw ? (JSON.parse(raw) as unknown[]).filter((id): id is string => typeof id === "string") : []; } catch { cachedIds = []; }
  }
  return cachedIds;
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  window.addEventListener("storage", listener);
  return () => { listeners.delete(listener); window.removeEventListener("storage", listener); };
}

const NO_IDS: string[] = [];

export function useSavedJobs() {
  const ids = useSyncExternalStore(subscribe, readSaved, () => NO_IDS);
  const toggle = useCallback((id: string) => {
    const current = readSaved();
    const next = current.includes(id) ? current.filter(saved => saved !== id) : [id, ...current];
    try { window.localStorage.setItem(SAVED_KEY, JSON.stringify(next)); } catch { /* chế độ riêng tư: bỏ qua */ }
    listeners.forEach(listener => listener());
  }, []);
  return { ids, isSaved: (id: string) => ids.includes(id), toggle };
}
