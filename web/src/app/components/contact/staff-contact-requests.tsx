"use client";
import { Dialog } from "@base-ui/react/dialog";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { Button } from "@/app/components/ui/button";
import { SelectMenu } from "@/app/components/ui/select-menu";
import { TaskBanner } from "@/app/components/ui/task-banner";
import { contactService } from "@/services/contact.service";
import { ApiError, getErrorMessage } from "@/services/api";
import type { ContactPage, ContactRequest, ContactRequestStatus } from "@/interface/contact";
import { contactDate, statusLabels, ContactPagination } from "./contact-history-shared";

export function StaffContactRequests() {
  const [status, setStatus] = useState<ContactRequestStatus | "">("PENDING");
  const [page, setPage] = useState(1);
  const [revision, setRevision] = useState(0);
  const [data, setData] = useState<ContactPage<ContactRequest> | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState<string | null>(null);
  const [reject, setReject] = useState<ContactRequest | null>(null);
  const [reason, setReason] = useState("");
  const lock = useRef(false);
  const alive = useRef(true);
  useEffect(() => { alive.current = true; return () => { alive.current = false; }; }, []);
  useEffect(() => {
    const abort = new AbortController();
    contactService.listRequests({ status: status || undefined, page }, abort.signal).then(setData)
      .catch(err => { if (!abort.signal.aborted) setError(getErrorMessage(err)); });
    return () => abort.abort();
  }, [status, page, revision]);
  function refresh() { setData(null); setRevision(v => v + 1); }
  async function review(request: ContactRequest, approved: boolean) {
    if (lock.current || (!approved && !reason.trim())) return;
    lock.current = true; setBusy(request.id); setError("");
    try {
      if (approved) await contactService.approve(request.id); else await contactService.reject(request.id, reason.trim());
      if (alive.current) { setReject(null); refresh(); }
    } catch (err) { if (alive.current) { setError(getErrorMessage(err)); if (err instanceof ApiError && err.status === 409) { setReject(null); refresh(); } } }
    finally { lock.current = false; if (alive.current) setBusy(null); }
  }
  function rejectSubmit(e: FormEvent) { e.preventDefault(); if (reject) void review(reject, false); }
  return <div className="w-full space-y-5">
    <TaskBanner compact eyebrow="Bảo mật liên hệ" title="Yêu cầu mở liên hệ" description="Xem nhu cầu tuyển dụng và quyết định cấp quyền cho công ty." illustration="company" />
    <div className="max-w-xs"><label htmlFor="contact-status" className="block text-sm font-medium">Trạng thái</label>
      <SelectMenu id="contact-status" className="ui-select-trigger mt-2" value={status} onValueChange={value => { setStatus(value as ContactRequestStatus | ""); setPage(1); setData(null); setError(""); }}
        options={[{ value: "", label: "Tất cả" }, ...Object.entries(statusLabels).map(([value, label]) => ({ value, label }))]} />
    </div>
    {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
    {!data && !error && <p role="status">Đang tải…</p>}
    {!data && error && <Button className="min-h-11" variant="outline" onClick={() => { setError(""); refresh(); }}>Thử lại</Button>}
    {data && <><div className="space-y-4">{data.items.length === 0 && <p className="rounded-xl border bg-card p-6 text-muted-foreground">Không có yêu cầu phù hợp.</p>}
      {data.items.map(request => <article key={request.id} className="rounded-xl border bg-card p-5 text-card-foreground">
        <div className="flex flex-wrap items-start justify-between gap-3"><div className="min-w-0 break-words"><h2 className="text-lg font-semibold">{request.candidateName || request.userId}</h2><p className="mt-1 text-sm">{request.companyName || request.companyId}</p></div><span className="rounded-full bg-muted px-3 py-1 text-sm">{statusLabels[request.status]}</span></div>
        <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-2"><div className="min-w-0"><dt className="text-muted-foreground">JD liên quan</dt><dd className="break-all">{request.jobId}</dd></div><div><dt className="text-muted-foreground">Ngày yêu cầu</dt><dd>{contactDate(request.createdAt)}</dd></div></dl>
        <p className="mt-4 text-sm font-medium">Lý do xin mở</p><p className="mt-1 whitespace-pre-wrap break-words text-sm">{request.reason}</p>
        {request.rejectionReason && <p className="mt-3 whitespace-pre-wrap break-words text-sm text-destructive">Lý do từ chối: {request.rejectionReason}</p>}
        {request.reviewedAt && <p className="mt-3 break-all text-xs text-muted-foreground">Xử lý lúc {contactDate(request.reviewedAt)} · {request.reviewedBy}</p>}
        {request.status === "PENDING" && <div className="mt-5 flex flex-wrap gap-3"><Button className="min-h-11" disabled={!!busy} onClick={() => review(request,true)}>{busy === request.id ? "Đang xử lý…" : "Duyệt"}</Button><Button className="min-h-11" variant="outline" disabled={!!busy} onClick={() => { setReason(""); setReject(request); setError(""); }}>Từ chối</Button></div>}
      </article>)}</div><ContactPagination {...data} onPage={next => { setPage(next); setData(null); }} /></>}
    <Dialog.Root open={!!reject} onOpenChange={open => { if (!open && !busy) setReject(null); }}><Dialog.Portal><Dialog.Backdrop className="fixed inset-0 z-50 bg-black/45" /><Dialog.Popup className="fixed left-1/2 top-1/2 z-50 w-[calc(100%-2rem)] max-w-lg -translate-x-1/2 -translate-y-1/2 rounded-2xl border bg-background p-6 shadow-xl">
      <Dialog.Title className="text-xl font-semibold">Từ chối yêu cầu</Dialog.Title><Dialog.Description className="mt-2 text-sm text-muted-foreground">HR sẽ nhận được lý do để hiểu quyết định của bạn.</Dialog.Description>
      <form onSubmit={rejectSubmit} className="mt-5 space-y-4"><label htmlFor="reject-contact-reason" className="block text-sm font-medium">Lý do từ chối</label><textarea id="reject-contact-reason" required maxLength={2000} rows={4} className="w-full rounded-lg border p-3 text-base" value={reason} onChange={e => setReason(e.target.value)} disabled={!!busy} />
        {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
        <div className="flex flex-wrap justify-end gap-3"><Dialog.Close disabled={!!busy} className="min-h-11 rounded-lg border px-4">Hủy</Dialog.Close><Button className="min-h-11" type="submit" variant="destructive" disabled={!!busy || !reason.trim()}>Xác nhận từ chối</Button></div>
      </form>
    </Dialog.Popup></Dialog.Portal></Dialog.Root>
  </div>;
}
