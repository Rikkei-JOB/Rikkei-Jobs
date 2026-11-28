"use client";

import { useMemo, useState, type ReactNode } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { AlertCircle, AlertTriangle, ArrowLeft, ArrowRight, Ban, Briefcase, Building2, CalendarClock, CheckCircle2, CircleDollarSign, Layers, MapPin, Undo2, Users, XCircle } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { LEVEL_LABEL, TYPE_LABEL, WORK_MODE_LABEL, formatDate } from "@/app/components/job/job-labels";
import { CompanyMark } from "@/app/components/company/company-mark";
import { useNow } from "@/hooks/use-now";
import { useReviewStaffJob, useStaffJob } from "@/hooks/use-staff-jobs";
import type { StaffJobDetail } from "@/interface/staff-job";
import { ApiError, getErrorMessage } from "@/services/api";
import { formatSalary, jobBoardService } from "@/services/job-board.service";
import { staffJobService } from "@/services/staff-job.service";
import { JobsBase, STATUS_INFO, daysLeft, deadlineText, formatDateTime, timeAgo, useJobsBase } from "./staff-job-utils";
import "./staff-jobs.css";

const QUICK_REASONS = [
  "Mô tả công việc còn sơ sài, cần ghi rõ nhiệm vụ hằng ngày.",
  "Mức lương chưa rõ ràng hoặc chưa đúng đơn vị.",
  "Yêu cầu kỹ năng chưa khớp với cấp bậc tuyển dụng.",
  "Hạn nộp hồ sơ quá ngắn, cần ít nhất 7 ngày.",
  "Nội dung có thông tin liên hệ ngoài nền tảng.",
];

/** Giới hạn lý do khớp backend (ReasonRules): bắt buộc, cắt khoảng trắng hai đầu rồi tối đa 1000 ký tự. */
const REASON_MAX = 1000;

/** Lỗi theo trường `errors.reason` của backend (400 VALIDATION_ERROR), nếu có. */
function reasonFieldError(error: unknown): string | null {
  if (!(error instanceof ApiError) || !error.errors || typeof error.errors !== "object") return null;
  const message = (error.errors as Record<string, unknown>).reason;
  return typeof message === "string" ? message : null;
}

function RichText({ text }: { text: string | null }) {
  if (!text?.trim()) return <p className="sjr-missing">Chưa có nội dung.</p>;
  const lines = text.split(/\r?\n/).map(line => line.trim()).filter(Boolean);
  const bullet = /^[-•*]\s*/;
  if (lines.every(line => bullet.test(line))) return <ul>{lines.map((line, i) => <li key={i}>{line.replace(bullet, "")}</li>)}</ul>;
  return <p className="whitespace-pre-line">{text.trim()}</p>;
}

export function StaffJobReview({ id, base = "/staff/jobs" }: { id: string; base?: string }) {
  return <JobsBase.Provider value={base}><StaffJobReviewContent id={id} /></JobsBase.Provider>;
}

function StaffJobReviewContent({ id }: { id: string }) {
  const { data, error, isPending, refetch } = useStaffJob(id);
  if (error) {
    const missing = error instanceof ApiError && error.status === 404;
    return <div className="sjr space-y-4"><BackLink /><div role="alert" className="sjr-empty"><AlertCircle aria-hidden="true" />
      <h2>{missing ? "Không tìm thấy JD" : "Chưa thể tải JD"}</h2><p>{missing ? "JD không tồn tại hoặc vẫn là bản nháp của doanh nghiệp." : getErrorMessage(error)}</p>
      {!missing && <Button variant="outline" onClick={() => refetch()}>Thử lại</Button>}</div></div>;
  }
  if (isPending) return <div className="sjr space-y-4" role="status" aria-label="Đang tải"><BackLink /><div className="h-48 rounded-2xl bg-muted motion-safe:animate-pulse" /><div className="h-96 rounded-2xl bg-muted motion-safe:animate-pulse" /></div>;
  return <ReviewView detail={data} />;
}

function BackLink() {
  return <Link href={useJobsBase()} className="sjr-back"><ArrowLeft aria-hidden="true" />Danh sách JD</Link>;
}

function ReviewView({ detail }: { detail: StaffJobDetail }) {
  const { job, company } = detail;
  const now = useNow();
  const catalog = useQuery({ queryKey: ["job-board", "skills"], queryFn: jobBoardService.skillCatalog, staleTime: 10 * 60_000 });
  const names = useMemo(() => new Map((catalog.data ?? []).map(skill => [skill.id, skill.name])), [catalog.data]);
  const deadline = deadlineText(job.deadline, now);
  const status = job.status === "DRAFT" ? "PENDING" : job.status;

  const facts = [
    job.level && { icon: Layers, text: LEVEL_LABEL[job.level] },
    job.type && { icon: Briefcase, text: TYPE_LABEL[job.type] + (job.workMode ? ` · ${WORK_MODE_LABEL[job.workMode]}` : "") },
    (job.address || job.city) && { icon: MapPin, text: job.address ?? job.city },
    job.quantity != null && { icon: Users, text: `Số lượng: ${job.quantity}` },
  ].filter(Boolean) as { icon: typeof Layers; text: string }[];

  return <div className="sjr space-y-5">
    <BackLink />
    <section className="sjr-hero">
      <div className="sjr-hero-company">
        <CompanyMark name={company?.name ?? "?"} logoUrl={company?.logoUrl} size={48} />
        <div className="min-w-0"><p className="sjr-company"><Building2 aria-hidden="true" />{company?.name ?? "Không rõ công ty"}{company?.industry ? ` · ${company.industry}` : ""}</p>
          <span className="sjr-badge" data-tone={STATUS_INFO[status].tone}>{STATUS_INFO[status].label}</span></div>
      </div>
      <h1>{job.title}</h1>
      {job.position && job.position !== job.title && <p className="sjr-lead">Vị trí: {job.position}</p>}
      <div className="sjr-facts">
        {facts.map(fact => <span key={fact.text}><fact.icon aria-hidden="true" />{fact.text}</span>)}
        <span className="sjr-salary"><CircleDollarSign aria-hidden="true" />{formatSalary(job)}</span>
        <span className="sjr-deadline" data-tone={deadline.tone}><CalendarClock aria-hidden="true" />{job.deadline ? `Hạn nộp ${formatDate(job.deadline)} · ${deadline.text}` : deadline.text}</span>
      </div>
      <p className="sjr-meta">Doanh nghiệp gửi {timeAgo(job.updatedAt, now)} ({formatDateTime(job.updatedAt)})</p>
    </section>

    {company?.status === "LOCKED" && <p className="sjr-alert" role="status"><AlertTriangle aria-hidden="true" />Công ty đang bị khoá: JD được duyệt vẫn không hiển thị cho ứng viên cho tới khi công ty được mở khoá.</p>}

    <div className="sjr-grid">
      <div className="space-y-5 min-w-0">
        <section className="sjr-panel"><h2>Mô tả công việc</h2><RichText text={job.description} /></section>
        <section className="sjr-panel"><h2>Yêu cầu công việc</h2><RichText text={job.requirements} /></section>
        <section className="sjr-panel"><h2>Quyền lợi</h2><RichText text={job.benefits} /></section>
        <section className="sjr-panel">
          <h2>Kỹ năng yêu cầu <small>{job.skills.length} kỹ năng</small></h2>
          {job.skills.length ? <ul className="sjr-skills">{job.skills.map(skill => <li key={skill.skillId}>
            <span className="sjr-skill-name">{names.get(skill.skillId) ?? "Kỹ năng"}</span>
            <span className="sjr-skill-kind" data-required={skill.required || undefined}>{skill.required ? "Bắt buộc" : "Ưu tiên"}</span>
            <span className="sjr-skill-bar" aria-label={`Điểm tối thiểu ${skill.minScore}`}><i style={{ width: `${skill.minScore}%` }} /></span>
            <span className="sjr-skill-score">{skill.minScore}</span>
          </li>)}</ul> : <p className="sjr-missing">JD chưa khai báo kỹ năng.</p>}
        </section>
      </div>
      <aside className="sjr-aside"><DecisionPanel detail={detail} now={now} /></aside>
    </div>
  </div>;
}

function DecisionPanel({ detail, now }: { detail: StaffJobDetail; now: number }) {
  const { job, company } = detail;
  const router = useRouter();
  const base = useJobsBase();
  const review = useReviewStaffJob(job.id);
  const [mode, setMode] = useState<"idle" | "confirm-approve" | "reject" | "close">("idle");
  const [reason, setReason] = useState("");
  const [opening, setOpening] = useState(false);
  // Thao tác vừa làm xong trong phiên này; dùng để quyết định có mời mở JD chờ duyệt tiếp theo hay không
  // (không dùng review.isSuccess vì mở rồi huỷ một ô xác nhận khác sẽ reset mutation).
  const [justDid, setJustDid] = useState<"approve" | "reject" | "close" | null>(null);
  // Lỗi 400 theo trường hiện ngay dưới ô lý do; lỗi khác (409 INVALID_JOB_STATUS, JOB_EXPIRED, CONFLICT) hiện ở trên.
  const fieldError = reasonFieldError(review.error);

  const expired = job.deadline ? daysLeft(job.deadline, now) < 0 : false;
  const checks = [
    { ok: Boolean(job.description && job.description.trim().length >= 50), label: "Mô tả công việc đủ chi tiết (từ 50 ký tự)" },
    { ok: job.skills.some(skill => skill.required), label: "Có ít nhất một kỹ năng bắt buộc" },
    { ok: Boolean(job.deadline) && daysLeft(job.deadline!, now) >= 7, label: "Hạn nộp còn từ 7 ngày" },
    { ok: job.salaryMin != null || job.salaryMax != null, label: "Có thông tin mức lương" },
    { ok: company?.status !== "LOCKED", label: "Công ty đang hoạt động" },
  ];

  /** Mở một ô xác nhận: xoá lý do cũ và lỗi của lần gửi trước để hộp mới luôn sạch. */
  function startMode(next: "confirm-approve" | "reject" | "close") {
    setReason("");
    review.reset();
    setMode(next);
  }

  /** Gửi quyết định một lần; thành công thì đóng ô nhập và nhớ thao tác vừa làm. */
  function submit(action: { type: "approve" } | { type: "reject"; reason: string } | { type: "close"; reason: string }) {
    if (review.isPending) return;
    review.mutate(action, {
      onSuccess: () => {
        setReason("");
        setMode("idle");
        setJustDid(action.type);
      },
    });
  }

  async function openNext() {
    setOpening(true);
    try {
      const next = await staffJobService.list({ status: "PENDING", page: 1, size: 1 });
      router.push(next.items[0] ? `${base}/${next.items[0].id}` : base);
    } catch { router.push(base); }
  }

  const errorLine = review.isError && !fieldError
    ? <p role="alert" className="sjr-error"><AlertCircle aria-hidden="true" />{getErrorMessage(review.error)}</p>
    : null;

  /** Ô lý do bắt buộc dùng cho Trả lại và Đóng JD: đếm ký tự, lỗi theo trường ngay dưới ô. */
  const reasonField = (id: string, label: string, placeholder: string, quick?: ReactNode) => <>
    <label className="sjr-label" htmlFor={id}>
      <span className="sjr-label-name">{label} <span className="sjr-required">*</span></span>
      <span>{reason.length}/{REASON_MAX}</span>
    </label>
    {quick}
    <textarea id={id} rows={4} maxLength={REASON_MAX} required value={reason} onChange={event => setReason(event.target.value)}
      disabled={review.isPending} aria-invalid={fieldError ? true : undefined} aria-describedby={fieldError ? `${id}-error` : undefined}
      placeholder={placeholder} className="sjr-textarea" />
    {fieldError && <p id={`${id}-error`} role="alert" className="sjr-error"><AlertCircle aria-hidden="true" />{fieldError}</p>}
  </>;

  if (job.status !== "PENDING") {
    const reviewed = job.reviewedAt ? formatDateTime(job.reviewedAt) : null;
    return <section className="sjr-panel sjr-decision" aria-label="Kết quả duyệt">
      <h2>Kết quả duyệt</h2>
      {job.status === "REJECTED" ? <div className="sjr-result" data-tone="rejected"><XCircle aria-hidden="true" /><div><strong>Đã trả lại</strong>{reviewed && <span>{reviewed}</span>}</div></div>
        : <div className="sjr-result" data-tone="approved"><CheckCircle2 aria-hidden="true" /><div><strong>{job.status === "CLOSED" ? "Đã duyệt, JD đã đóng" : "Đã duyệt, đang hiển thị"}</strong>{reviewed && <span>{reviewed}</span>}</div></div>}
      {job.rejectReason && <blockquote className="sjr-reason">{job.rejectReason}</blockquote>}

      {errorLine}

      {job.status === "APPROVED" && (mode === "close" ? <div className="space-y-3">
        {reasonField("close-reason", "Lý do đóng JD", "Nêu rõ vì sao JD phải ngừng tuyển, ví dụ nội dung vi phạm quy định…")}
        <div className="flex gap-2">
          <Button variant="outline" className="flex-1" disabled={review.isPending} onClick={() => setMode("idle")}>Huỷ</Button>
          <Button className="flex-1 sjr-btn-danger" disabled={!reason.trim() || review.isPending}
            onClick={() => submit({ type: "close", reason: reason.trim() })}><Ban aria-hidden="true" />{review.isPending ? "Đang đóng…" : "Đóng JD"}</Button>
        </div>
      </div> : <div className="grid gap-2">
        <Button variant="outline" className="sjr-btn-outline-danger" onClick={() => startMode("close")}><Ban aria-hidden="true" />Đóng JD</Button>
        <p className="sjr-hint">Đóng JD khi nội dung vi phạm: JD ngừng hiển thị cho ứng viên và không thể mở lại — HR cũng không sửa hay gửi duyệt lại JD này được nữa, muốn tuyển tiếp phải tạo JD mới. Lý do được ghi vào lịch sử doanh nghiệp.</p>
      </div>)}

      {justDid && justDid !== "close" && <Button className="w-full" disabled={opening} onClick={openNext}>{opening ? "Đang mở…" : "Mở JD chờ duyệt tiếp theo"}<ArrowRight aria-hidden="true" /></Button>}
      <Link href={base} className="sjr-back justify-center">Về danh sách</Link>
    </section>;
  }

  return <section className="sjr-panel sjr-decision" aria-label="Quyết định duyệt">
    <h2>Quyết định duyệt</h2>
    <ul className="sjr-checks">{checks.map(check => <li key={check.label} data-ok={check.ok || undefined}>
      {check.ok ? <CheckCircle2 aria-hidden="true" /> : <AlertTriangle aria-hidden="true" />}<span>{check.label}</span></li>)}</ul>
    <p className="sjr-hint">Danh sách tự kiểm tra chỉ để tham khảo; bạn vẫn quyết định duyệt hay trả lại.</p>

    {errorLine}

    {mode === "reject" ? <div className="space-y-3">
      {reasonField("reject-reason", "Lý do trả lại", "Nêu rõ điểm cần sửa để HR chỉnh lại và gửi duyệt lần nữa…",
        <div className="sjr-quick">{QUICK_REASONS.map(text => <button key={text} type="button" onClick={() => setReason(current => current ? `${current}\n${text}` : text)}>{text}</button>)}</div>)}
      <div className="flex gap-2">
        <Button variant="outline" className="flex-1" disabled={review.isPending} onClick={() => setMode("idle")}>Huỷ</Button>
        <Button className="flex-1 sjr-btn-danger" disabled={!reason.trim() || review.isPending}
          onClick={() => submit({ type: "reject", reason: reason.trim() })}><Undo2 aria-hidden="true" />{review.isPending ? "Đang gửi…" : "Trả lại JD"}</Button>
      </div>
    </div> : mode === "confirm-approve" ? <div className="space-y-3">
      <p className="sjr-confirm">Duyệt JD này? JD sẽ hiển thị ngay trên trang việc làm cho ứng viên.</p>
      <div className="flex gap-2">
        <Button variant="outline" className="flex-1" disabled={review.isPending} onClick={() => setMode("idle")}>Huỷ</Button>
        <Button className="flex-1 sjr-btn-success" disabled={review.isPending} onClick={() => submit({ type: "approve" })}>
          <CheckCircle2 aria-hidden="true" />{review.isPending ? "Đang duyệt…" : "Xác nhận duyệt"}</Button>
      </div>
    </div> : <div className="grid gap-2">
      <Button className="sjr-btn-success" disabled={expired} onClick={() => startMode("confirm-approve")}><CheckCircle2 aria-hidden="true" />Duyệt JD</Button>
      {expired && <p className="sjr-hint">Hạn nộp đã qua nên không thể duyệt; hãy trả lại để HR cập nhật hạn nộp.</p>}
      <Button variant="outline" className="sjr-btn-outline-danger" onClick={() => startMode("reject")}><Undo2 aria-hidden="true" />Trả lại cho doanh nghiệp</Button>
    </div>}
  </section>;
}
