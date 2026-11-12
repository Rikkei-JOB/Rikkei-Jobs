"use client";

import * as React from "react";
import { Alert } from "@/app/components/ui/alert";
import { Button } from "@/app/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/app/components/ui/card";
import { useSyncRuns } from "@/hooks/use-sync";
import { getErrorMessage } from "@/services/api";
import type { SyncStatus } from "@/interface/sync";
import { StatusBadge, TRIGGER_LABEL, describeCounts, formatDateTime } from "./sync-format";
import { cn } from "cn";

const PAGE_SIZE = 10;
const FILTERS: { label: string; value: SyncStatus | undefined }[] = [
  { label: "Tất cả", value: undefined },
  { label: "Thành công", value: "SUCCESS" },
  { label: "Lỗi", value: "FAILED" },
];

/** Bảng các lần đồng bộ, mới nhất trước; lọc theo trạng thái và (tuỳ chọn) theo ứng viên đang chọn. */
export function SyncRunTable({ candidate }: { candidate: { userId: string; fullName: string } | null }) {
  const [status, setStatus] = React.useState<SyncStatus | undefined>(undefined);
  const [onlyCandidate, setOnlyCandidate] = React.useState(false);
  const [page, setPage] = React.useState(1);
  const userId = onlyCandidate && candidate ? candidate.userId : undefined;
  const { data, error, isPending, isFetching, refetch } = useSyncRuns({ status, userId, page, size: PAGE_SIZE });
  const totalPages = data ? Math.max(1, Math.ceil(data.total / PAGE_SIZE)) : 1;

  function changeFilter(next: () => void) {
    next();
    setPage(1);
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Lịch sử đồng bộ</CardTitle>
        <CardDescription>Mỗi dòng là một lần đồng bộ một ứng viên: job đêm, khi ứng viên mở hồ sơ hoặc cán bộ bấm đồng bộ lại.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="flex flex-wrap items-center gap-2">
          <div role="group" aria-label="Lọc theo trạng thái" className="inline-flex rounded-md border border-border p-0.5">
            {FILTERS.map((filter) => (
              <button
                key={filter.label}
                type="button"
                aria-pressed={status === filter.value}
                onClick={() => changeFilter(() => setStatus(filter.value))}
                className={cn(
                  "rounded px-2 py-0.5 text-xs font-medium",
                  status === filter.value ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:text-foreground",
                )}
              >
                {filter.label}
              </button>
            ))}
          </div>
          {candidate ? (
            <label className="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
              <input
                type="checkbox"
                checked={onlyCandidate}
                onChange={(e) => changeFilter(() => setOnlyCandidate(e.target.checked))}
              />
              Chỉ {candidate.fullName}
            </label>
          ) : null}
          <Button variant="ghost" size="sm" className="ml-auto" onClick={() => refetch()} disabled={isFetching}>
            {isFetching ? "Đang tải..." : "Làm mới"}
          </Button>
        </div>

        {error ? <Alert variant="destructive">{getErrorMessage(error)}</Alert> : null}

        <div className="overflow-x-auto">
          <table className="w-full min-w-[720px] text-left text-xs">
            <thead className="border-b border-border text-muted-foreground">
              <tr>
                <th className="py-2 pr-3 font-medium">Thời điểm</th>
                <th className="py-2 pr-3 font-medium">Kiểu</th>
                <th className="py-2 pr-3 font-medium">Ứng viên</th>
                <th className="py-2 pr-3 font-medium">Trạng thái</th>
                <th className="py-2 pr-3 font-medium">Số bản ghi</th>
                <th className="py-2 font-medium">Lỗi</th>
              </tr>
            </thead>
            <tbody>
              {isPending ? (
                <tr>
                  <td colSpan={6} className="py-4 text-center text-muted-foreground">Đang tải...</td>
                </tr>
              ) : data && data.items.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-4 text-center text-muted-foreground">Chưa có lần đồng bộ nào.</td>
                </tr>
              ) : (
                data?.items.map((run) => (
                  <tr key={run.id} className="border-b border-border/60 align-top last:border-0">
                    <td className="py-2 pr-3 whitespace-nowrap">{formatDateTime(run.startedAt)}</td>
                    <td className="py-2 pr-3 whitespace-nowrap">{TRIGGER_LABEL[run.trigger]}</td>
                    <td className="py-2 pr-3">
                      {run.candidateName ?? "—"}
                      {run.lmsUserId ? <div className="text-muted-foreground">{run.lmsUserId}</div> : null}
                    </td>
                    <td className="py-2 pr-3"><StatusBadge status={run.status} /></td>
                    <td className="py-2 pr-3 whitespace-nowrap">{describeCounts(run.counts)}</td>
                    <td className="py-2 text-destructive">{run.error ?? ""}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="flex items-center justify-between text-xs text-muted-foreground">
          <span>{data ? `${data.total} lần chạy` : ""}</span>
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" disabled={page <= 1} onClick={() => setPage((p) => p - 1)}>
              Trước
            </Button>
            <span>
              Trang {page}/{totalPages}
            </span>
            <Button variant="outline" size="sm" disabled={page >= totalPages} onClick={() => setPage((p) => p + 1)}>
              Sau
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
