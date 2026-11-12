"use client";

import * as React from "react";
import { Moon } from "lucide-react";
import { Alert } from "@/app/components/ui/alert";
import { Button } from "@/app/components/ui/button";
import { CandidateSyncPanel } from "@/app/components/sync/candidate-sync-panel";
import { ExternalRecordsPanel } from "@/app/components/sync/external-records-panel";
import { SyncRunTable } from "@/app/components/sync/sync-run-table";
import { useRunNightly } from "@/hooks/use-sync";
import { getErrorMessage } from "@/services/api";
import type { CandidateSync } from "@/interface/sync";

/**
 * Màn "Đồng bộ dữ liệu" của cán bộ (NT-05). Vai trò STAFF do layout (app) kiểm theo đường dẫn /staff/...
 */
export default function StaffSyncPage() {
  const [selected, setSelected] = React.useState<CandidateSync | null>(null);
  const nightly = useRunNightly();

  return (
    <div className="mx-auto max-w-6xl space-y-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-heading text-lg font-semibold">Đồng bộ dữ liệu</h1>
          <p className="text-xs text-muted-foreground">
            Điểm học tập (LMS), điểm thi (Khảo thí) và kết quả thực tập (lms-pm) làm bằng chứng cho hồ sơ năng lực.
            Job tự chạy lúc 01:00 mỗi đêm.
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={() => nightly.mutate()} disabled={nightly.isPending}>
          <Moon />
          Chạy job đêm ngay
        </Button>
      </div>

      {nightly.isSuccess ? (
        <Alert variant="success">{nightly.data.message}. Kết quả từng ứng viên hiện ở bảng lịch sử, bấm Làm mới để xem.</Alert>
      ) : null}
      {nightly.error ? <Alert variant="destructive">{getErrorMessage(nightly.error)}</Alert> : null}

      <CandidateSyncPanel selectedId={selected?.userId ?? null} onSelect={setSelected} />
      {selected ? <ExternalRecordsPanel key={selected.userId} candidate={selected} /> : null}
      <SyncRunTable candidate={selected} />
    </div>
  );
}
