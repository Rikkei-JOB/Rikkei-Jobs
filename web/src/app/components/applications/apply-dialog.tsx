"use client";

import { Dialog } from "@base-ui/react/dialog";
import Link from "next/link";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { X } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { applicationLinks } from "@/config/application-status";
import type { ApplicationDetail, CvChoice } from "@/interface/application";
import { applicationService } from "@/services/application.service";
import { listApplicationCvs } from "@/services/application-cv.service";
import { ApiError, getErrorMessage } from "@/services/api";

interface Props { jobId: string; open: boolean; onOpenChange(open: boolean): void; onApplied(application: ApplicationDetail): void }
export function ApplyDialog({ jobId, open, onOpenChange, onApplied }: Props) {
  const [choices, setChoices] = useState<CvChoice[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [loadVersion, setLoadVersion] = useState(0);
  const [cvId, setCvId] = useState("");
  const [coverLetter, setCoverLetter] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [uncertain, setUncertain] = useState(false);
  const submitting = useRef(false);
  useEffect(() => {
    if (!open) return;
    let alive = true;
    listApplicationCvs().then(items => {
      if (alive) { setChoices(items); setLoadFailed(false); setError(""); }
    }).catch(err => {
      if (alive) { setLoadFailed(true); setError(getErrorMessage(err, "Không tải được danh sách CV.")); }
    }).finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [open, loadVersion]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (submitting.current || !cvId || uncertain) return;
    submitting.current = true; setBusy(true); setError("");
    try {
      const result = await applicationService.apply(jobId, { cvId, coverLetter });
      onApplied(result);
    } catch (err) {
      setError(getErrorMessage(err, "Không gửi được đơn. Vui lòng thử lại."));
      if (err instanceof ApiError && err.status === 0) setUncertain(true);
    } finally { submitting.current = false; setBusy(false); }
  }
  async function reconcile() {
    if (submitting.current) return;
    submitting.current = true; setBusy(true);
    try {
      let page = 1;
      while (true) {
        const result = await applicationService.list({ page, size: 100 });
        const existing = result.items.find(a => a.jobId === jobId && a.status !== "REJECTED" && a.status !== "WITHDRAWN");
        if (existing) { onApplied(await applicationService.detail(existing.id)); return; }
        if (page * 100 >= result.total) break;
        page++;
      }
      setUncertain(false); setError("Chưa thấy đơn đang xử lý cho vị trí này. Bạn có thể gửi lại.");
    } catch (err) { setError(getErrorMessage(err)); }
    finally { submitting.current = false; setBusy(false); }
  }
  return <Dialog.Root open={open} onOpenChange={value => { if (!busy) onOpenChange(value); }}>
    <Dialog.Portal>
      <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/45" />
      <Dialog.Popup className="fixed left-1/2 top-1/2 z-50 max-h-[90dvh] w-[calc(100%-2rem)] max-w-xl -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-2xl border bg-background p-6 shadow-xl">
        <div className="flex items-center justify-between gap-4">
          <Dialog.Title className="text-xl font-semibold">Ứng tuyển vị trí này</Dialog.Title>
          <Dialog.Close disabled={busy} aria-label="Đóng hộp ứng tuyển" className="rounded-md p-2 hover:bg-muted"><X className="h-4 w-4" /></Dialog.Close>
        </div>
        <Dialog.Description className="mb-6 mt-2 text-sm text-muted-foreground">Chọn CV phù hợp và gửi lời giới thiệu đến nhà tuyển dụng.</Dialog.Description>
        <form onSubmit={submit} className="space-y-5">
          {loading ? <p role="status">Đang tải CV…</p> : loadFailed ? <Button type="button" variant="outline" onClick={() => { setLoading(true); setLoadVersion(v => v + 1); }}>Tải lại CV</Button>
            : choices.length === 0 ? <div className="rounded-lg bg-muted p-4 text-sm"><p>Bạn chưa có CV để ứng tuyển.</p><Link href={applicationLinks.cvs} className="mt-2 inline-block font-medium text-primary underline">Tạo CV</Link></div>
            : <div className="space-y-2"><label htmlFor="application-cv" className="text-sm font-medium">Chọn CV</label>
              <select id="application-cv" required value={cvId} onChange={e => setCvId(e.target.value)} disabled={busy} className="h-11 w-full rounded-lg border bg-background px-3 text-sm">
                <option value="">Chọn một CV</option>{choices.map(cv => <option key={cv.id} value={cv.id}>{cv.name}{cv.targetPosition ? ` — ${cv.targetPosition}` : ""}</option>)}
              </select><Link href={applicationLinks.cvs} className="text-xs text-primary underline">Quản lý CV của tôi</Link>
            </div>}
          <div className="space-y-2"><label htmlFor="application-letter" className="text-sm font-medium">Thư giới thiệu</label>
            <textarea id="application-letter" rows={5} maxLength={5000} value={coverLetter} onChange={e => setCoverLetter(e.target.value)} disabled={busy} placeholder="Giới thiệu ngắn về bạn và lý do phù hợp với vị trí…" className="w-full rounded-lg border bg-background p-3 text-sm" />
            <p className="text-right text-xs text-muted-foreground">{coverLetter.length}/5.000 ký tự · Không bắt buộc</p>
          </div>
          <p className="rounded-lg bg-muted/60 p-3 text-xs leading-relaxed text-muted-foreground">CV sẽ được lưu thành bản PDF tại thời điểm nộp. Các chỉnh sửa CV sau đó không thay đổi bản đã gửi.</p>
          {error && <div role="alert" className="space-y-2 text-sm text-destructive"><p>{error}</p><Link href="/candidate/applications" className="underline">Xem ứng tuyển của tôi</Link></div>}
          {uncertain && <div className="space-y-2 text-sm"><p>Chưa xác định được kết quả gửi. Kiểm tra kết quả trước khi gửi lại.</p><Button type="button" variant="outline" disabled={busy} onClick={reconcile}>Kiểm tra kết quả</Button></div>}
          <div className="flex justify-end gap-3 border-t pt-4">
            <Dialog.Close disabled={busy} className="rounded-lg border px-4 py-2 text-sm">Huỷ</Dialog.Close>
            <Button type="submit" disabled={busy || loading || loadFailed || !cvId || uncertain} className="h-10 px-5">{busy ? "Đang xử lý…" : "Gửi ứng tuyển"}</Button>
          </div>
        </form>
      </Dialog.Popup>
    </Dialog.Portal>
  </Dialog.Root>;
}
