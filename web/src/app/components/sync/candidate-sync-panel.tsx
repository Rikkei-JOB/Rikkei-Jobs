"use client";

import * as React from "react";
import { RefreshCw, Search } from "lucide-react";
import { Alert } from "@/app/components/ui/alert";
import { Button } from "@/app/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/app/components/ui/card";
import { Input } from "@/app/components/ui/input";
import { useSyncCandidates, useSyncUser } from "@/hooks/use-sync";
import { getErrorMessage } from "@/services/api";
import type { CandidateSync, SyncRun } from "@/interface/sync";
import { StatusBadge, describeCounts, formatDateTime } from "./sync-format";
import { cn } from "cn";

function useDebounced<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = React.useState(value);
  React.useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(timer);
  }, [value, delayMs]);
  return debounced;
}

/** Tìm ứng viên, bấm "Đồng bộ lại" và xem kết quả ngay (lỗi mock hiện rõ lý do). */
export function CandidateSyncPanel({
  selectedId,
  onSelect,
}: {
  selectedId: string | null;
  onSelect: (candidate: CandidateSync) => void;
}) {
  const [query, setQuery] = React.useState("");
  const debounced = useDebounced(query.trim(), 300);
  const { data: candidates, error, isPending } = useSyncCandidates(debounced);
  const syncUser = useSyncUser();
  const [results, setResults] = React.useState<Record<string, SyncRun | { failure: string }>>({});

  function sync(candidate: CandidateSync) {
    syncUser.mutate(candidate.userId, {
      onSuccess: (run) => setResults((prev) => ({ ...prev, [candidate.userId]: run })),
      onError: (e) => setResults((prev) => ({ ...prev, [candidate.userId]: { failure: getErrorMessage(e) } })),
    });
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Ứng viên</CardTitle>
        <CardDescription>Tìm theo tên, email hoặc mã LMS. Đồng bộ lại lấy điểm học tập, thi và thực tập mới nhất từ hệ thống ngoài.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="relative">
          <Search className="pointer-events-none absolute top-1/2 left-2 size-3.5 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Ví dụ: Nguyễn Văn An, candidate1@..., lms-sv-001"
            aria-label="Tìm ứng viên"
            className="pl-7"
          />
        </div>

        {error ? <Alert variant="destructive">{getErrorMessage(error)}</Alert> : null}
        {isPending ? <p className="text-xs text-muted-foreground">Đang tải...</p> : null}
        {candidates && candidates.length === 0 ? (
          <p className="text-xs text-muted-foreground">Không tìm thấy ứng viên.</p>
        ) : null}

        <ul className="divide-y divide-border/60">
          {candidates?.map((candidate) => {
            const result = results[candidate.userId];
            const running = syncUser.isPending && syncUser.variables === candidate.userId;
            const lastRun = result && "status" in result ? result : candidate.lastRun;
            return (
              <li
                key={candidate.userId}
                className={cn("space-y-2 py-2.5", selectedId === candidate.userId && "rounded-md bg-muted/40 px-2")}
              >
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div className="min-w-0">
                    <div className="text-sm font-medium">{candidate.fullName}</div>
                    <div className="text-xs text-muted-foreground">
                      {candidate.email} · {candidate.lmsUserId ?? "chưa liên kết LMS"}
                    </div>
                    <div className="mt-1 flex items-center gap-1.5 text-xs text-muted-foreground">
                      {lastRun ? (
                        <>
                          <StatusBadge status={lastRun.status} />
                          <span>Lần cuối {formatDateTime(lastRun.startedAt)}</span>
                        </>
                      ) : (
                        <span>Chưa đồng bộ lần nào</span>
                      )}
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" onClick={() => onSelect(candidate)}>
                      Xem bản ghi
                    </Button>
                    <Button size="sm" onClick={() => sync(candidate)} disabled={!candidate.lmsUserId || running}>
                      <RefreshCw className={cn(running && "animate-spin")} />
                      {running ? "Đang đồng bộ..." : "Đồng bộ lại"}
                    </Button>
                  </div>
                </div>
                {result ? <SyncResult result={result} /> : null}
              </li>
            );
          })}
        </ul>
      </CardContent>
    </Card>
  );
}

function SyncResult({ result }: { result: SyncRun | { failure: string } }) {
  if ("failure" in result) {
    return <Alert variant="destructive">Không đồng bộ được: {result.failure}</Alert>;
  }
  if (result.status === "FAILED") {
    return (
      <Alert variant="destructive">
        Đồng bộ thất bại: {result.error}. Dữ liệu cũ được giữ nguyên.
      </Alert>
    );
  }
  return <Alert variant="success">Đồng bộ thành công: {describeCounts(result.counts)}.</Alert>;
}
