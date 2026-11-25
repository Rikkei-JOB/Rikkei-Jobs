"use client";

import Link from "next/link";
import { useRef, useState, type FormEvent } from "react";
import { Dialog } from "@base-ui/react/dialog";
import { Button } from "@/app/components/ui/button";
import type { Offer } from "@/interface/offer";
import { formatOfferSalary } from "@/interface/offer";
import { ApiError, getErrorMessage } from "@/services/api";
import { offerService } from "@/services/offer.service";
import { formatApplicationDate } from "@/config/application-status";

export function OfferResponseDialog({ offer, now, disabled, onUpdated, refresh }: { offer: Offer; now: number; disabled: boolean; onUpdated(value: Offer): Promise<void>; refresh(): Promise<Offer> }) {
  const [action, setAction] = useState<"accept" | "decline" | null>(null);
  const [reason, setReason] = useState(""), [error, setError] = useState(""), [busy, setBusy] = useState(false), [uncertain, setUncertain] = useState(false);
  const inFlight = useRef(false), heading = useRef<HTMLHeadingElement>(null);
  const acceptTrigger = useRef<HTMLButtonElement>(null), declineTrigger = useRef<HTMLButtonElement>(null);
  const lastAction = useRef<"accept" | "decline">("accept");
  const canRespond = offer.status === "SENT" && new Date(offer.responseDeadline).getTime() > now;
  const title = action === "decline" ? "Xác nhận từ chối" : "Xác nhận nhận offer";
  function open(value: "accept" | "decline") { lastAction.current = value; setAction(value); setError(""); }
  async function reconcile() {
    if (inFlight.current) return;
    if (!navigator.onLine) { setError("Bạn đang ngoại tuyến. Kết nối mạng rồi kiểm tra lại kết quả."); return; }
    inFlight.current = true; setBusy(true);
    try {
      const updated = await refresh(); await onUpdated(updated); setUncertain(false);
      if (updated.status !== "SENT" || new Date(updated.responseDeadline).getTime() <= Date.now()) { setAction(null); setError(""); }
      else setError("Đã kiểm tra: offer vẫn chờ phản hồi. Bạn có thể gửi lại.");
    } catch (cause) { setError(getErrorMessage(cause)); }
    finally { inFlight.current = false; setBusy(false); }
  }
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!action || inFlight.current || uncertain || disabled || offer.status !== "SENT" || new Date(offer.responseDeadline).getTime() <= Date.now()) return;
    if (action === "decline" && !reason.trim()) { setError("Vui lòng nhập lý do từ chối."); return; }
    inFlight.current = true; setBusy(true); setError("");
    try {
      const updated = action === "accept" ? await offerService.accept(offer.id) : await offerService.decline(offer.id, reason.trim());
      await onUpdated(updated); setAction(null);
    } catch (cause) {
      setError(getErrorMessage(cause));
      if (cause instanceof ApiError && cause.status === 0) setUncertain(true);
      if (cause instanceof ApiError && cause.status === 409) {
        try { const updated = await refresh(); await onUpdated(updated); if (updated.status !== "SENT" || new Date(updated.responseDeadline).getTime() <= Date.now()) setAction(null); }
        catch { setUncertain(true); }
      }
    } finally { setBusy(false); inFlight.current = false; }
  }
  return <>
    <h2 ref={heading} tabIndex={-1}>Phản hồi offer</h2>
    {canRespond ? <><p className="journey-muted mb-4">Đọc kỹ thông tin thư mời trước khi xác nhận quyết định của bạn.</p><div className="journey-actions"><Button ref={acceptTrigger} disabled={disabled || busy || uncertain} onClick={() => open("accept")}>Nhận offer</Button><Button ref={declineTrigger} variant="outline" disabled={disabled || busy || uncertain} onClick={() => open("decline")}>Từ chối offer</Button></div></> : <div className={offer.status === "ACCEPTED" ? "journey-success" : "journey-info"}><p>{offer.status === "ACCEPTED" ? "Bạn đã nhận offer này." : offer.status === "DECLINED" ? "Bạn đã từ chối offer này." : "Offer đã hết hạn phản hồi."}</p>{offer.respondedAt && <p className="journey-small">Đã phản hồi: {formatApplicationDate(offer.respondedAt)}</p>}{offer.status === "DECLINED" && offer.declineReason && <p className="journey-note">Lý do từ chối: {offer.declineReason}</p>}</div>}
    {offer.status === "ACCEPTED" && <Link href="/candidate/profile" className="journey-link mt-3">Xem kinh nghiệm trên hồ sơ →</Link>}
    {!action && error && <p role="alert" className="journey-error mt-4">{error}</p>}
    {!action && uncertain && <div className="journey-warning mt-4"><p>Chưa xác định được kết quả gửi. Kiểm tra kết quả trước khi gửi lại.</p><Button variant="outline" disabled={busy} onClick={reconcile}>Kiểm tra kết quả</Button></div>}
    <Dialog.Root open={action !== null} onOpenChange={value => { if (!value && !inFlight.current) setAction(null); }}>
      <Dialog.Portal><Dialog.Backdrop className="journey-dialog-backdrop" /><Dialog.Popup className="journey journey-dialog" finalFocus={() => {
        const trigger = lastAction.current === "accept" ? acceptTrigger.current : declineTrigger.current;
        return trigger && !trigger.disabled ? trigger : heading.current;
      }}>
        <div className="journey-dialog-header"><Dialog.Title className="text-xl font-semibold">{title}</Dialog.Title><Dialog.Description>{action === "decline" ? "Chia sẻ lý do để nhà tuyển dụng nhận được phản hồi của bạn." : "Vui lòng kiểm tra thông tin trước khi nhận thư mời này."}</Dialog.Description></div>
        <form onSubmit={submit} className="flex min-h-0 flex-col"><div className="journey-dialog-body"><div className="rounded-lg bg-slate-50 p-4"><h3>{offer.position}</h3><p className="mt-2">{formatOfferSalary(offer.salary)}</p><p className="journey-small journey-muted">Ngày bắt đầu: {offer.startDate}</p></div>
          {action === "decline" && <div><label htmlFor="offer-decline-reason">Lý do từ chối</label><textarea id="offer-decline-reason" required maxLength={1000} rows={4} value={reason} onChange={e => setReason(e.target.value)} disabled={busy} aria-describedby="offer-reason-help" /><p id="offer-reason-help" className="journey-small journey-muted text-right">{reason.length}/1.000 ký tự · Bắt buộc</p></div>}
          {!canRespond && <p className="journey-warning">Đã hết hạn trả lời hoặc offer không còn chờ phản hồi.</p>}
          {error && <p role="alert" className="journey-error">{error}</p>}
          {uncertain && <div className="journey-warning"><p>Chưa xác định được kết quả gửi. Kiểm tra kết quả trước khi gửi lại.</p><Button type="button" variant="outline" disabled={busy} onClick={reconcile}>Kiểm tra kết quả</Button></div>}
        </div><div className="journey-dialog-footer"><Dialog.Close disabled={busy} render={<Button type="button" variant="outline" />}>Quay lại</Dialog.Close><Button type="submit" disabled={busy || disabled || uncertain || !canRespond || (action === "decline" && !reason.trim())}>{busy ? "Đang xử lý…" : title}</Button></div></form>
      </Dialog.Popup></Dialog.Portal>
    </Dialog.Root>
  </>;
}
