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
import { TaskBanner } from "@/app/components/ui/task-banner";

/**
 * Màn "Đồng bộ dữ liệu" của cán bộ (NT-05).
 */
export default function StaffSyncPage() {
  const [selected, setSelected] = React.useState<CandidateSync | null>(null);
  const nightly = useRunNightly();

  return (
    <div className="ptit-page max-w-6xl">
      <TaskBanner
        eyebrow="Tích hợp LMS · Khảo thí · Thực tập"
        title="Đồng bộ dữ liệu"
        description="Điểm học tập (LMS), điểm thi (Khảo thí) và kết quả thực tập (lms-pm) làm bằng chứng cho hồ sơ năng lực. Job tự chạy hằng đêm (mặc định 01:00)."
        illustration="applications"
        compact
      >
        <Button variant="outline" onClick={() => nightly.mutate()} disabled={nightly.isPending}>
          <Moon aria-hidden="true" />
          Chạy job đêm ngay
        </Button>
      </TaskBanner>

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
