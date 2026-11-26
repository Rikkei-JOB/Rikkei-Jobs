"use client";
import { Dialog } from "@base-ui/react/dialog";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { Button } from "@/app/components/ui/button";
import { SelectMenu } from "@/app/components/ui/select-menu";
import { jobService } from "@/services/job.service";
import { contactService } from "@/services/contact.service";
import { ApiError, getErrorMessage } from "@/services/api";
import type { JobSummary } from "@/interface/job";

export function RequestContactDialog({ userId, jobId, onChanged }: { userId: string; jobId?: string; onChanged(): void }) {
  const [open, setOpen] = useState(false);
  return <Dialog.Root open={open} onOpenChange={setOpen}>
    <Dialog.Trigger render={<Button className="min-h-11" variant="outline" />}>Xin mở liên hệ</Dialog.Trigger>
    <Dialog.Portal><Dialog.Backdrop className="fixed inset-0 z-50 bg-black/45" />
      <Dialog.Popup className="fixed left-1/2 top-1/2 z-50 max-h-[90dvh] w-[calc(100%-2rem)] max-w-lg -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-2xl border bg-background p-6 shadow-xl">
        <Dialog.Title className="text-xl font-semibold">Xin mở liên hệ</Dialog.Title>
        <Dialog.Description className="mt-2 text-sm text-muted-foreground">Chọn JD và giải thích nhu cầu tuyển dụng để STAFF xem xét.</Dialog.Description>
        <RequestForm userId={userId} jobId={jobId} onDone={() => { setOpen(false); onChanged(); }} />
      </Dialog.Popup>
    </Dialog.Portal>
  </Dialog.Root>;
}
function RequestForm({ userId, jobId, onDone }: { userId: string; jobId?: string; onDone(): void }) {
  const [jobs, setJobs] = useState<JobSummary[]>([]);
  const [jobPage, setJobPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [selected, setSelected] = useState(jobId ?? "");
  const [reason, setReason] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(!jobId);
  const [busy, setBusy] = useState(false);
  const submitting = useRef(false);
  const alive = useRef(true);
  useEffect(() => { alive.current = true; return () => { alive.current = false; }; }, []);
  useEffect(() => {
    if (jobId) return;
    const abort = new AbortController();
    jobService.listMine({ page: jobPage, size: 100 }, abort.signal).then(result => {
      setJobs(result.items); setTotal(result.total); setLoading(false);
    }).catch(err => { if (!abort.signal.aborted) { setError(getErrorMessage(err)); setLoading(false); } });
    return () => abort.abort();
  }, [jobId, jobPage]);
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (submitting.current) return;
    if (!selected || !reason.trim() || reason.trim().length > 2000) { setError("Chọn JD và nhập lý do từ 1 đến 2000 ký tự."); return; }
    submitting.current = true; setBusy(true); setError("");
    try { await contactService.request(userId, { jobId: selected, reason: reason.trim() }); if (alive.current) onDone(); }
    catch (err) { if (alive.current) { if (err instanceof ApiError && err.status === 409) onDone(); else setError(getErrorMessage(err)); } }
    finally { submitting.current = false; if (alive.current) setBusy(false); }
  }
  return <form onSubmit={submit} className="mt-5 space-y-4">
    {!jobId && <div><label className="block text-sm font-medium" htmlFor="contact-job">JD liên quan</label>
      <SelectMenu id="contact-job" required className="ui-select-trigger mt-2" value={selected} disabled={loading || busy} onValueChange={setSelected}
        placeholder={loading ? "Đang tải JD…" : "Chọn JD của công ty"} options={jobs.map(job => ({ value: job.id, label: job.title }))} />{total > 100 && <div className="mt-2 flex items-center gap-3"><Button className="min-h-11" type="button" variant="outline" disabled={jobPage === 1 || busy} onClick={() => { setLoading(true); setSelected(""); setJobPage(p => p - 1); }}>JD trước</Button><span>Trang {jobPage}</span><Button className="min-h-11" type="button" variant="outline" disabled={jobPage * 100 >= total || busy} onClick={() => { setLoading(true); setSelected(""); setJobPage(p => p + 1); }}>JD tiếp</Button></div>}
      {!loading && jobs.length === 0 && <p className="mt-2 text-sm">Công ty chưa có JD để gửi yêu cầu.</p>}
    </div>}
    <div><label htmlFor="contact-reason" className="block text-sm font-medium">Lý do xin mở liên hệ</label>
      <textarea id="contact-reason" required maxLength={2000} rows={4} value={reason} onChange={e => setReason(e.target.value)} disabled={busy} className="mt-2 w-full rounded-lg border p-3 text-base" />
      <p className="text-xs text-muted-foreground">{reason.length}/2000 ký tự</p></div>
    {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
    <div className="flex flex-wrap justify-end gap-3"><Dialog.Close disabled={busy} className="min-h-11 rounded-lg border px-4">Hủy</Dialog.Close><Button className="min-h-11" type="submit" disabled={busy || loading || !selected || !reason.trim()}>{busy ? "Đang gửi…" : "Gửi yêu cầu"}</Button></div>
  </form>;
}
