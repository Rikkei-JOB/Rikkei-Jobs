"use client";

import { Dialog } from "@base-ui/react/dialog";
import Link from "next/link";
import { useEffect, useRef, useState, type FormEvent, type RefObject } from "react";
import { FileCheck2, X } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { applicationLinks } from "@/config/application-status";
import type { ApplicationDetail, CvChoice } from "@/interface/application";
import { applicationService } from "@/services/application.service";
import { listApplicationCvs } from "@/services/application-cv.service";
import { ApiError, getErrorMessage } from "@/services/api";
import "./career.css";

interface Props {
  jobId: string; open: boolean; onOpenChange(open: boolean): void; onApplied(application: ApplicationDetail): void;
  jobTitle?: string; companyName?: string; returnFocus?: RefObject<HTMLElement | null>;
}
export function ApplyDialog({ jobId, open, onOpenChange, onApplied, jobTitle, companyName, returnFocus }: Props) {
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
      <Dialog.Backdrop className="career-dialog-backdrop" />
      <Dialog.Popup className="career career-dialog" finalFocus={returnFocus}>
        <div className="career-dialog-heading">
          <div className="career-dialog-title-row">
            <div><p className="career-eyebrow">HỒ SƠ ỨNG TUYỂN</p><Dialog.Title className="text-xl font-semibold">Ứng tuyển vị trí này</Dialog.Title></div>
            <Dialog.Close disabled={busy} aria-label="Đóng hộp ứng tuyển" className="career-dialog-close"><X /></Dialog.Close>
          </div>
          <Dialog.Description className="career-description">Chọn CV phù hợp và gửi lời giới thiệu đến nhà tuyển dụng.</Dialog.Description>
          {jobTitle && <div className="career-dialog-context"><strong>{jobTitle}</strong>{companyName && <span>{companyName}</span>}</div>}
        </div>
        <form onSubmit={submit}>
          <div className="career-dialog-body">
          {loading ? <p role="status">Đang tải CV…</p> : loadFailed ? <Button className="career-secondary" type="button" variant="outline" onClick={() => { setLoading(true); setLoadVersion(v => v + 1); }}>Tải lại CV</Button>
            : choices.length === 0 ? <div className="career-dialog-empty"><p>Bạn chưa có CV để ứng tuyển.</p><Link href={applicationLinks.cvs} className="career-text-link">Tạo CV</Link></div>
            : <div><label htmlFor="application-cv">Chọn CV</label>
              <select id="application-cv" required value={cvId} onChange={e => setCvId(e.target.value)} disabled={busy}>
                <option value="">Chọn một CV</option>{choices.map(cv => <option key={cv.id} value={cv.id}>{cv.name}{cv.targetPosition ? ` — ${cv.targetPosition}` : ""}</option>)}
              </select><Link href={applicationLinks.cvs} className="career-text-link">Quản lý CV của tôi</Link>
            </div>}
          <div><label htmlFor="application-letter">Thư giới thiệu</label>
            <textarea id="application-letter" rows={5} maxLength={5000} value={coverLetter} onChange={e => setCoverLetter(e.target.value)} disabled={busy} aria-describedby="application-letter-help" placeholder="Giới thiệu ngắn về bạn và lý do phù hợp với vị trí…" />
            <p id="application-letter-help" className="career-field-help text-right">{coverLetter.length}/5.000 ký tự · Không bắt buộc</p>
          </div>
          <p className="career-snapshot-note"><FileCheck2 aria-hidden="true" /><span>CV sẽ được lưu thành bản PDF tại thời điểm nộp. Các chỉnh sửa CV sau đó không thay đổi bản đã gửi.</span></p>
          {error && <div role="alert" className="career-dialog-error"><p>{error}</p><Link href="/candidate/applications" className="career-text-link">Xem ứng tuyển của tôi</Link></div>}
          {uncertain && <div className="space-y-2"><p>Chưa xác định được kết quả gửi. Kiểm tra kết quả trước khi gửi lại.</p><Button className="career-secondary" type="button" variant="outline" disabled={busy} onClick={reconcile}>Kiểm tra kết quả</Button></div>}
          </div>
          <div className="career-dialog-footer">
            <Dialog.Close disabled={busy} className="career-secondary">Huỷ</Dialog.Close>
            <Button type="submit" disabled={busy || loading || loadFailed || !cvId || uncertain} className="career-primary">{busy ? "Đang xử lý…" : "Gửi ứng tuyển"}</Button>
          </div>
        </form>
      </Dialog.Popup>
    </Dialog.Portal>
  </Dialog.Root>;
}
