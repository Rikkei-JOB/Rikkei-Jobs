"use client";

import { useQuery } from "@tanstack/react-query";
import { useAuthStore } from "@/hooks/use-auth-store";
import { jobService } from "@/services/job.service";
import type { JobSummary } from "@/interface/job";
import { Button } from "@/app/components/ui/button";
import { SelectMenu } from "@/app/components/ui/select-menu";

export function HrJobFilter({ value, onChange }: { value: string; onChange(value: string): void }) {
  const userId = useAuthStore(state => state.user?.id);
  const token = useAuthStore(state => state.token);
  const jobs = useQuery({
    queryKey: ["hr-job-options", userId, token],
    queryFn: async ({ signal }) => {
      const items: JobSummary[] = [];
      for (let page = 1; ; page++) {
        const result = await jobService.listMine({ page, size: 100 }, signal);
        items.push(...result.items);
        if (items.length >= result.total) return [...new Map(items.map(item => [item.id, item])).values()];
        if (!result.items.length) throw new Error("Danh sách vị trí đã thay đổi. Vui lòng tải lại.");
      }
    },
  });
  const missing = Boolean(value && jobs.data && !jobs.data.some(job => job.id === value));
  const selected = useQuery({ queryKey: ["hr-job-option", userId, token, value], queryFn: ({ signal }) => jobService.get(value, signal), enabled: missing });
  const options = jobs.data ?? [];
  const fallback = value && !options.some(job => job.id === value);
  return <div className="min-w-0 flex-1">
    <label htmlFor="hr-job-filter">Vị trí tuyển dụng</label>
    <SelectMenu id="hr-job-filter" value={value} onValueChange={onChange} aria-describedby={jobs.isError || selected.isError ? "hr-job-filter-error" : undefined}
      options={[
        { value: "", label: jobs.isPending ? "Đang tải vị trí…" : "Tất cả vị trí" },
        ...(fallback ? [{ value, label: selected.data?.title ?? (jobs.isError || selected.isError ? "Vị trí đã chọn (chưa tải được tên)" : "Vị trí đã chọn (đang tra tên)") }] : []),
        ...options.map(job => ({ value: job.id, label: job.title })),
      ]} />
    {(jobs.isError || selected.isError) && <div id="hr-job-filter-error" role="alert" className="mt-2 text-sm text-destructive">Không tải được {jobs.isError ? "danh sách vị trí" : "tên vị trí đã chọn"}. Bộ lọc hiện tại vẫn được giữ.
      <Button type="button" variant="ghost" onClick={() => jobs.isError ? jobs.refetch() : selected.refetch()}>Tải lại vị trí</Button>
    </div>}
  </div>;
}
