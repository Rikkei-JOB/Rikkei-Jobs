"use client";

import { Dialog } from "@base-ui/react/dialog";
import { useRef, useState, type FormEvent } from "react";
import { Button } from "@/app/components/ui/button";
import type { ApplicationDetail } from "@/interface/application";
import { applicationService } from "@/services/application.service";
import { ApiError, getErrorMessage } from "@/services/api";

export function WithdrawDialog({ id, onWithdrawn, onConflict }: { id: string; onWithdrawn(a: ApplicationDetail): void; onConflict(): void }) {
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const submitting = useRef(false);
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (submitting.current || !reason.trim()) return;
    submitting.current = true; setBusy(true); setError("");
    try { onWithdrawn(await applicationService.withdraw(id, { reason: reason.trim() })); setOpen(false); }
    catch (err) { setError(getErrorMessage(err)); if (err instanceof ApiError && err.status === 409) onConflict(); }
    finally { submitting.current = false; setBusy(false); }
  }
  return <Dialog.Root open={open} onOpenChange={value => { if (!busy) setOpen(value); }}>
    <Dialog.Trigger render={<Button variant="outline" className="h-10 border-destructive/30 px-4 text-destructive" />}>Rút đơn</Dialog.Trigger>
    <Dialog.Portal>
      <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/45" />
      <Dialog.Popup className="fixed left-1/2 top-1/2 z-50 w-[calc(100%-2rem)] max-w-lg -translate-x-1/2 -translate-y-1/2 rounded-2xl border bg-background p-6 shadow-xl">
        <Dialog.Title className="text-xl font-semibold">Rút đơn ứng tuyển</Dialog.Title>
        <Dialog.Description className="mb-5 mt-2 text-sm text-muted-foreground">Đơn hiện tại sẽ kết thúc. Bạn có thể nộp đơn mới nếu vị trí vẫn nhận hồ sơ.</Dialog.Description>
        <form onSubmit={submit} className="space-y-4">
          <label htmlFor="withdraw-reason" className="block text-sm font-medium">Lý do rút đơn</label>
          <textarea id="withdraw-reason" required maxLength={1000} rows={4} value={reason} onChange={e => setReason(e.target.value)} disabled={busy} className="w-full rounded-lg border p-3 text-sm" />
          {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
          <div className="flex justify-end gap-3">
            <Dialog.Close disabled={busy} className="rounded-lg border px-4 py-2 text-sm">Giữ đơn</Dialog.Close>
            <Button type="submit" variant="destructive" disabled={busy || !reason.trim()} className="h-10 px-4">{busy ? "Đang xử lý…" : "Xác nhận rút đơn"}</Button>
          </div>
        </form>
      </Dialog.Popup>
    </Dialog.Portal>
  </Dialog.Root>;
}
