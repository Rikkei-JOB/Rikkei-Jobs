"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import { Button } from "@/app/components/ui/button";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import type { Offer } from "@/interface/offer";
import { formatOfferSalary, offerStatusLabel } from "@/interface/offer";
import { ApiError, getErrorMessage } from "@/services/api";
import { offerService } from "@/services/offer.service";

function remaining(deadline: string, now: number) {
  const seconds = Math.max(0, Math.ceil((new Date(deadline).getTime() - now) / 1000));
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const rest = seconds % 60;
  return `${days} ngày ${hours} giờ ${minutes} phút ${rest} giây`;
}

export function CandidateOfferDetail({ id }: { id: string }) {
  const allowed = useAuthGuard({ roles: ["CANDIDATE"] });
  const [offer, setOffer] = useState<Offer | null>(null);
  const [error, setError] = useState("");
  const [mutationError, setMutationError] = useState("");
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [now, setNow] = useState(() => Date.now());
  const [version, setVersion] = useState(0);
  const expiredReloaded = useRef<string | null>(null);
  const reload = useCallback(() => setVersion(value => value + 1), []);

  useEffect(() => {
    if (!allowed) return;
    let active = true;
    offerService.detail(id).then(value => { if (active) { setOffer(value); setError(""); } })
      .catch(cause => { if (active) setError(cause instanceof ApiError && cause.status === 404 ? "Không tìm thấy offer." : getErrorMessage(cause)); });
    return () => { active = false; };
  }, [allowed, id, version]);
  useEffect(() => { const timer = window.setInterval(() => setNow(Date.now()), 1000); return () => window.clearInterval(timer); }, []);
  useEffect(() => {
    if (offer?.status === "SENT" && new Date(offer.responseDeadline).getTime() <= now && expiredReloaded.current !== offer.id) {
      expiredReloaded.current = offer.id;
      reload();
    }
  }, [offer, now, reload]);

  async function respond(action: "accept" | "decline") {
    if (action === "decline" && !reason.trim()) { setMutationError("Vui lòng nhập lý do từ chối."); return; }
    setBusy(true); setMutationError("");
    try {
      const updated = action === "accept" ? await offerService.accept(id) : await offerService.decline(id, reason.trim());
      setOffer(updated);
    } catch (cause) {
      setMutationError(getErrorMessage(cause));
      if (cause instanceof ApiError && cause.status === 409) reload();
    } finally { setBusy(false); }
  }

  if (!allowed) return null;
  const expired = offer ? new Date(offer.responseDeadline).getTime() <= now : false;
  return <main className="mx-auto max-w-3xl space-y-5">
    <Link href="/candidate/offers" className="text-sm text-primary underline">← Danh sách offer</Link>
    {error && <p role="alert" className="rounded-lg border border-destructive/30 p-4">{error}</p>}
    {!offer && !error && <p role="status">Đang tải offer…</p>}
    {offer && <section className="space-y-4 rounded-xl border bg-background p-5 sm:p-7">
      <div className="flex flex-wrap items-start justify-between gap-3"><h1 className="text-2xl font-semibold">{offer.position}</h1><span>{offerStatusLabel[offer.status]}</span></div>
      <dl className="grid gap-3 text-sm sm:grid-cols-2">
        <div><dt className="text-muted-foreground">Lương</dt><dd className="font-medium">{formatOfferSalary(offer.salary)}</dd></div>
        <div><dt className="text-muted-foreground">Ngày bắt đầu</dt><dd>{offer.startDate}</dd></div>
        <div><dt className="text-muted-foreground">Địa điểm làm việc</dt><dd>{offer.workLocation}</dd></div>
        <div><dt className="text-muted-foreground">Hạn trả lời</dt><dd>{new Date(offer.responseDeadline).toLocaleString("vi-VN")}</dd></div>
      </dl>
      {offer.note && <p className="whitespace-pre-wrap text-sm">{offer.note}</p>}
      {offer.status === "SENT" && <p role="timer" className="text-sm">{expired ? "Đã hết hạn trả lời" : `Còn ${remaining(offer.responseDeadline, now)}`}</p>}
      {mutationError && <p role="alert" className="text-sm text-destructive">{mutationError}</p>}
      {offer.status === "SENT" && !expired && <div className="space-y-3 border-t pt-4">
        <Button disabled={busy} onClick={() => respond("accept")}>Nhận offer</Button>
        <label className="block text-sm">Lý do từ chối<textarea className="mt-1 w-full rounded-lg border bg-background px-3 py-2" required maxLength={2000} value={reason} onChange={event => setReason(event.target.value)} /></label>
        <Button variant="outline" disabled={busy || !reason.trim()} onClick={() => respond("decline")}>Từ chối offer</Button>
      </div>}
      {offer.status === "DECLINED" && offer.declineReason && <p className="text-sm">Lý do từ chối: {offer.declineReason}</p>}
      {offer.status === "ACCEPTED" && <Link href="/candidate/profile" className="inline-block text-sm text-primary underline">Xem kinh nghiệm trên hồ sơ</Link>}
    </section>}
  </main>;
}
