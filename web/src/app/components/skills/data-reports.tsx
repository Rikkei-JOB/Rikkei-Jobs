"use client";

import * as React from "react";
import { Flag } from "lucide-react";
import { Alert } from "@/app/components/ui/alert";
import { Button } from "@/app/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/app/components/ui/card";
import { ReportStatusBadge, errorText, formatDateTime, textareaClass } from "@/app/components/admin/admin-format";
import { useCreateDataReport, useMyDataReports } from "@/hooks/use-admin";
import type { DataReport } from "@/interface/admin";

/** Báo cáo gần nhất của mỗi bằng chứng (danh sách đã sắp mới nhất trước). */
export type ReportsByEvidence = Map<string, DataReport>;

export function useReportsByEvidence(enabled: boolean): ReportsByEvidence | null {
  const { data } = useMyDataReports(enabled);
  return React.useMemo(() => {
    if (!enabled) return null;
    const map: ReportsByEvidence = new Map();
    for (const report of data ?? []) {
      if (report.evidenceId && !map.has(report.evidenceId)) map.set(report.evidenceId, report);
    }
    return map;
  }, [data, enabled]);
}

/** Nút "Báo dữ liệu sai" dưới một bằng chứng; đã báo thì hiện trạng thái và kết quả xử lý. */
export function EvidenceReport({ evidenceId, reports }: { evidenceId: string; reports: ReportsByEvidence }) {
  const latest = reports.get(evidenceId);
  const create = useCreateDataReport();
  const [open, setOpen] = React.useState(false);
  const [content, setContent] = React.useState("");

  if (latest?.status === "OPEN") {
    return (
      <p className="mt-2 flex items-center gap-1.5 text-[11px] text-muted-foreground">
        <ReportStatusBadge status="OPEN" /> Đã báo sai lúc {formatDateTime(latest.createdAt)}, đang chờ quản trị xử lý.
      </p>
    );
  }

  function submit(e: React.FormEvent) {
    e.preventDefault();
    create.mutate(
      { evidenceId, content: content.trim() },
      { onSuccess: () => { setOpen(false); setContent(""); } },
    );
  }

  return (
    <div className="mt-2 space-y-2">
      {latest ? (
        <p className="text-[11px]">
          <ReportStatusBadge status={latest.status} /> <span className="text-muted-foreground">Kết quả: </span>
          {latest.resolution}
        </p>
      ) : null}
      {open ? (
        <form onSubmit={submit} className="space-y-2">
          <label htmlFor={`report-${evidenceId}`} className="text-[11px] font-medium">Dữ liệu sai thế nào?</label>
          <textarea id={`report-${evidenceId}`} className={textareaClass} rows={2} maxLength={2000} required autoFocus
            value={content} onChange={(e) => setContent(e.target.value)}
            placeholder="Ví dụ: Điểm thi đúng là 9, không phải 7." />
          {create.error ? <p role="alert" className="text-[11px] text-destructive">{errorText(create.error)}</p> : null}
          <div className="flex gap-2">
            <Button type="submit" size="xs" disabled={create.isPending}>{create.isPending ? "Đang gửi..." : "Gửi báo cáo"}</Button>
            <Button type="button" variant="ghost" size="xs" onClick={() => setOpen(false)}>Huỷ</Button>
          </div>
        </form>
      ) : (
        <Button type="button" variant="ghost" size="xs" className="text-muted-foreground" onClick={() => setOpen(true)}>
          <Flag />
          Báo dữ liệu sai
        </Button>
      )}
    </div>
  );
}

/** Danh sách báo cáo của ứng viên kèm kết quả xử lý. */
export function MyDataReports() {
  const { data, error } = useMyDataReports();
  if (error) return <Alert variant="destructive">{errorText(error)}</Alert>;
  if (!data || data.length === 0) return null;
  return (
    <Card>
      <CardHeader>
        <CardTitle>Báo cáo dữ liệu sai của tôi</CardTitle>
        <CardDescription>Kết quả quản trị xử lý các bằng chứng bạn đã báo sai.</CardDescription>
      </CardHeader>
      <CardContent>
        <ul className="divide-y divide-border/60 text-xs">
          {data.map((report) => (
            <li key={report.id} className="space-y-1 py-2.5">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <span className="font-medium">{report.evidenceTitle} · {report.skillName}</span>
                <ReportStatusBadge status={report.status} />
              </div>
              <p className="text-muted-foreground">Bạn báo ({formatDateTime(report.createdAt)}): {report.content}</p>
              {report.status !== "OPEN" ? (
                <p>Kết quả ({formatDateTime(report.resolvedAt)}): {report.resolution}</p>
              ) : null}
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  );
}
