"use client";

import * as React from "react";
import { Alert } from "@/app/components/ui/alert";
import { Button } from "@/app/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/app/components/ui/card";
import { Input } from "@/app/components/ui/input";
import { useDataReports, useResolveReport, useUpdateEvidence } from "@/hooks/use-admin";
import type { AdminEvidence, DataReport, DataReportStatus } from "@/interface/admin";
import { Field } from "./skills-tab";
import { REPORT_STATUS_LABEL, ReportStatusBadge, errorText, fieldError, formatDateTime, selectClass, textareaClass } from "./admin-format";

const PAGE_SIZE = 10;

/** Tab Báo sai: ứng viên báo bằng chứng sai; ADMIN sửa bằng chứng (bắt buộc lý do) rồi đóng báo cáo kèm kết quả. */
export function ReportsTab() {
  const [status, setStatus] = React.useState<DataReportStatus | "">("OPEN");
  const [page, setPage] = React.useState(1);
  const { data, error, isPending, isFetching } = useDataReports(status || undefined, page, PAGE_SIZE);
  const pages = data ? Math.max(1, Math.ceil(data.total / PAGE_SIZE)) : 1;

  return (
    <Card>
      <CardHeader>
        <CardTitle>Báo dữ liệu sai</CardTitle>
        <CardDescription>Ứng viên thấy kết quả xử lý ngay trong hồ sơ của mình.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="flex flex-wrap items-center gap-2">
          <label htmlFor="report-status" className="text-xs">Trạng thái</label>
          <select id="report-status" className={selectClass} value={status}
            onChange={(e) => { setStatus(e.target.value as DataReportStatus | ""); setPage(1); }}>
            <option value="">Tất cả</option>
            {(Object.keys(REPORT_STATUS_LABEL) as DataReportStatus[]).map((s) => (
              <option key={s} value={s}>{REPORT_STATUS_LABEL[s]}</option>
            ))}
          </select>
          {data ? <span className="text-xs text-muted-foreground">{data.total} báo cáo</span> : null}
        </div>
        {error ? <Alert variant="destructive">{errorText(error)}</Alert> : null}
        {isPending ? <p className="text-xs text-muted-foreground">Đang tải...</p> : null}
        {data && data.items.length === 0 ? <p className="text-xs text-muted-foreground">Không có báo cáo.</p> : null}

        <ul className="space-y-3" aria-busy={isFetching}>
          {data?.items.map((report) => <ReportItem key={report.id} report={report} />)}
        </ul>

        {pages > 1 ? (
          <div className="flex items-center justify-end gap-2 text-xs">
            <Button variant="outline" size="sm" disabled={page <= 1} onClick={() => setPage(page - 1)}>Trước</Button>
            <span>Trang {page}/{pages}</span>
            <Button variant="outline" size="sm" disabled={page >= pages} onClick={() => setPage(page + 1)}>Sau</Button>
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}

function ReportItem({ report }: { report: DataReport }) {
  return (
    <li className="space-y-2 rounded-md border border-border/60 p-3 text-xs">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="min-w-0">
          <div className="font-medium">{report.candidateName ?? report.userId}</div>
          <div className="text-muted-foreground">{report.candidateEmail} · gửi {formatDateTime(report.createdAt)}</div>
        </div>
        <ReportStatusBadge status={report.status} />
      </div>
      <div>
        <span className="text-muted-foreground">Bằng chứng: </span>
        {report.evidenceTitle} · {report.skillName}
        {report.evidence ? (
          <span className="text-muted-foreground"> (hiện tại: &quot;{report.evidence.title}&quot;, {report.evidence.score}/100)</span>
        ) : report.evidenceId === null ? (
          <span className="text-muted-foreground"> (bằng chứng đã bị xoá)</span>
        ) : null}
      </div>
      <blockquote className="border-l-2 border-amber-400 pl-2 whitespace-pre-line">{report.content}</blockquote>
      {report.status === "OPEN" ? (
        <div className="grid gap-3 lg:grid-cols-2">
          {report.evidence ? <EvidenceEditForm evidence={report.evidence} /> : null}
          <ResolveForm report={report} />
        </div>
      ) : (
        <p>
          <span className="text-muted-foreground">Kết quả ({formatDateTime(report.resolvedAt)}): </span>
          {report.resolution}
        </p>
      )}
    </li>
  );
}

function EvidenceEditForm({ evidence }: { evidence: AdminEvidence }) {
  const update = useUpdateEvidence();
  const [title, setTitle] = React.useState(evidence.title);
  const [score, setScore] = React.useState(String(evidence.score));
  const [reason, setReason] = React.useState("");
  const [saved, setSaved] = React.useState(false);
  const id = evidence.id;

  function submit(e: React.FormEvent) {
    e.preventDefault();
    setSaved(false);
    const nextScore = Number(score);
    update.mutate(
      {
        id,
        payload: {
          title: title.trim() !== evidence.title ? title.trim() : undefined,
          score: nextScore !== Number(evidence.score) ? nextScore : undefined,
          reason: reason.trim(),
        },
      },
      { onSuccess: () => { setSaved(true); setReason(""); } },
    );
  }

  return (
    <form onSubmit={submit} className="space-y-2 rounded-md bg-muted/30 p-2">
      <p className="font-medium">Sửa bằng chứng</p>
      {evidence.fromExternalRecord ? (
        <p className="text-[11px] text-muted-foreground">
          Bằng chứng từ nguồn ngoài: sửa ở bản ghi gốc, giữ qua các lần đồng bộ và áp cho mọi kỹ năng ánh xạ từ bản ghi đó.
        </p>
      ) : null}
      <div className="grid gap-2 sm:grid-cols-[1fr_6rem]">
        <Field label="Tên" id={`ev-title-${id}`} error={fieldError(update.error, "title")}>
          <Input id={`ev-title-${id}`} value={title} maxLength={255} onChange={(e) => setTitle(e.target.value)} />
        </Field>
        <Field label="Điểm (0–100)" id={`ev-score-${id}`} error={fieldError(update.error, "score")}>
          <Input id={`ev-score-${id}`} type="number" min={0} max={100} step="0.01" value={score}
            onChange={(e) => setScore(e.target.value)} />
        </Field>
      </div>
      <Field label="Lý do sửa (bắt buộc)" id={`ev-reason-${id}`} error={fieldError(update.error, "reason")}>
        <Input id={`ev-reason-${id}`} value={reason} maxLength={500} onChange={(e) => setReason(e.target.value)} />
      </Field>
      {update.error && !fieldError(update.error, "reason") ? (
        <p role="alert" className="text-destructive">{errorText(update.error)}</p>
      ) : null}
      {saved ? <p role="status" className="text-emerald-700 dark:text-emerald-400">Đã sửa bằng chứng, điểm kỹ năng đã tính lại.</p> : null}
      <Button type="submit" size="sm" variant="outline" disabled={update.isPending}>
        {update.isPending ? "Đang lưu..." : "Lưu bằng chứng"}
      </Button>
    </form>
  );
}

function ResolveForm({ report }: { report: DataReport }) {
  const resolve = useResolveReport();
  const [status, setStatus] = React.useState<"RESOLVED" | "REJECTED">("RESOLVED");
  const [resolution, setResolution] = React.useState("");

  function submit(e: React.FormEvent) {
    e.preventDefault();
    resolve.mutate({ id: report.id, status, resolution: resolution.trim() });
  }

  return (
    <form onSubmit={submit} className="space-y-2 rounded-md bg-muted/30 p-2">
      <p className="font-medium">Xử lý báo cáo</p>
      <div className="flex flex-wrap gap-3">
        {(["RESOLVED", "REJECTED"] as const).map((value) => (
          <label key={value} className="flex items-center gap-1.5">
            <input type="radio" name={`status-${report.id}`} value={value} checked={status === value}
              onChange={() => setStatus(value)} />
            {value === "RESOLVED" ? "Đã xử lý" : "Từ chối"}
          </label>
        ))}
      </div>
      <div className="space-y-1">
        <label htmlFor={`resolution-${report.id}`} className="font-medium">Kết quả gửi ứng viên</label>
        <textarea id={`resolution-${report.id}`} className={textareaClass} rows={2} maxLength={2000} required
          value={resolution} onChange={(e) => setResolution(e.target.value)}
          placeholder="Ví dụ: Đã sửa điểm thành 90 theo kết quả phúc khảo." />
      </div>
      {resolve.error ? <p role="alert" className="text-destructive">{errorText(resolve.error)}</p> : null}
      <Button type="submit" size="sm" disabled={resolve.isPending}>
        {resolve.isPending ? "Đang lưu..." : "Đóng báo cáo"}
      </Button>
    </form>
  );
}
