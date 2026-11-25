"use client";

import { useState, type FormEvent } from "react";
import { Button } from "@/app/components/ui/button";
import type { HrApplicationDetail } from "@/interface/hr-application";
import { formatOfferSalary, offerStatusLabel } from "@/interface/offer";
import { ApiError, getErrorMessage } from "@/services/api";
import { hrApplicationService } from "@/services/hr-application.service";

export function OfferPanel({ application, onChanged }: { application: HrApplicationDetail; onChanged: () => void }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [position, setPosition] = useState(application.jobTitle);
  const [salary, setSalary] = useState("");
  const [startDate, setStartDate] = useState("");
  const [workLocation, setWorkLocation] = useState("");
  const [responseDeadline, setResponseDeadline] = useState("");
  const [note, setNote] = useState("");
  const [locallyConfirmedAt, setLocallyConfirmedAt] = useState<string | null>(null);
  const rounds = [...application.rounds].sort((a, b) => a.roundNo - b.roundNo);
  const eligible = application.status === "INTERVIEW" && rounds.length > 0 &&
    !rounds.some(round => round.result === "PENDING") && rounds[rounds.length - 1].result === "PASS";

  async function send(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const amount = Number(salary.replaceAll(".", ""));
    if (!Number.isSafeInteger(amount) || amount <= 0) { setError("Lương phải là số nguyên VND lớn hơn 0."); return; }
    if (new Date(responseDeadline).getTime() <= Date.now()) { setError("Hạn trả lời phải ở tương lai."); return; }
    setBusy(true); setError("");
    try {
      await hrApplicationService.sendOffer(application.id, {
        position: position.trim(), salary: amount, startDate, workLocation: workLocation.trim(),
        responseDeadline: new Date(responseDeadline).toISOString(), note: note.trim() || null,
      });
      onChanged();
    } catch (reason) {
      setError(getErrorMessage(reason));
      if (reason instanceof ApiError && reason.status === 409) onChanged();
    } finally { setBusy(false); }
  }

  async function confirmOnboard() {
    setBusy(true); setError("");
    try { const record = await hrApplicationService.confirmOnboard(application.id); setLocallyConfirmedAt(record.confirmedAt); onChanged(); }
    catch (reason) { setError(getErrorMessage(reason)); if (reason instanceof ApiError && reason.status === 409) onChanged(); }
    finally { setBusy(false); }
  }

  if (!eligible && !application.offer) return <section className="rounded-xl border bg-background p-5"><h2 className="text-lg font-semibold">Chưa có offer</h2><p className="mt-2 text-sm text-muted-foreground">Có thể gửi offer khi hồ sơ ở bước phỏng vấn, tất cả vòng đã có kết quả và vòng cuối đạt.</p></section>;
  const confirmedAt = application.onboardConfirmedAt ?? locallyConfirmedAt;
  return <section className="rounded-xl border bg-background p-5">
    <h2 className="text-lg font-semibold">Offer</h2>
    {error && <p role="alert" className="mt-3 text-sm text-destructive">{error}</p>}
    {application.offer ? <div className="mt-4 space-y-2 text-sm">
      <p><strong>Trạng thái:</strong> {offerStatusLabel[application.offer.status]}</p>
      <p><strong>Vị trí:</strong> {application.offer.position}</p>
      <p><strong>Lương:</strong> {formatOfferSalary(application.offer.salary)}</p>
      <p><strong>Ngày bắt đầu:</strong> {application.offer.startDate}</p>
      <p><strong>Địa điểm:</strong> {application.offer.workLocation}</p>
      <p><strong>Hạn trả lời:</strong> {new Date(application.offer.responseDeadline).toLocaleString("vi-VN")}</p>
      {application.offer.respondedAt && <p><strong>Đã phản hồi:</strong> {new Date(application.offer.respondedAt).toLocaleString("vi-VN")}</p>}
      {application.offer.note && <p><strong>Ghi chú:</strong> {application.offer.note}</p>}
      {application.offer.declineReason && <p><strong>Lý do từ chối:</strong> {application.offer.declineReason}</p>}
      {application.offer.status === "ACCEPTED" && (confirmedAt
        ? <p><strong>Đã xác nhận đi làm:</strong> {new Date(confirmedAt).toLocaleString("vi-VN")}</p>
        : <Button disabled={busy} onClick={confirmOnboard}>Xác nhận đã đi làm</Button>)}
    </div> : <form onSubmit={send} className="mt-4 grid gap-4 sm:grid-cols-2">
      <label className="block text-sm">Vị trí offer<input className="mt-1 w-full rounded-lg border bg-background px-3 py-2" required maxLength={255} value={position} onChange={e => setPosition(e.target.value)} /></label>
      <label className="block text-sm">Lương gross/tháng (VND)<input className="mt-1 w-full rounded-lg border bg-background px-3 py-2" type="text" inputMode="numeric" required value={salary} onChange={e => {
        const digits = e.target.value.replace(/[.,\s]/g, "");
        if (/^\d{0,18}$/.test(digits)) setSalary(digits.replace(/\B(?=(\d{3})+(?!\d))/g, "."));
      }} /></label>
      <label className="block text-sm">Ngày bắt đầu<input className="mt-1 w-full rounded-lg border bg-background px-3 py-2" type="date" required value={startDate} onChange={e => setStartDate(e.target.value)} /></label>
      <label className="block text-sm">Địa điểm làm việc<input className="mt-1 w-full rounded-lg border bg-background px-3 py-2" required maxLength={500} value={workLocation} onChange={e => setWorkLocation(e.target.value)} /></label>
      <label className="block text-sm">Hạn trả lời<input className="mt-1 w-full rounded-lg border bg-background px-3 py-2" type="datetime-local" required value={responseDeadline} onChange={e => setResponseDeadline(e.target.value)} /></label>
      <label className="block text-sm">Ghi chú<textarea className="mt-1 w-full rounded-lg border bg-background px-3 py-2" maxLength={2000} value={note} onChange={e => setNote(e.target.value)} /></label>
      <div className="sm:col-span-2"><Button type="submit" disabled={busy}>{busy ? "Đang gửi…" : "Gửi offer"}</Button></div>
    </form>}
  </section>;
}
