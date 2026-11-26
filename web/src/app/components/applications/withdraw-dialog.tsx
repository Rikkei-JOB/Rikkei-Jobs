"use client";

import { Dialog } from "@base-ui/react/dialog";
import { useRef, useState, type FormEvent } from "react";
import { AlertTriangle } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import type { ApplicationDetail } from "@/interface/application";
import { applicationService } from "@/services/application.service";
import { ApiError, getErrorMessage } from "@/services/api";

export function WithdrawDialog({
  id,
  onWithdrawn,
  onConflict,
}: {
  id: string;
  onWithdrawn(a: ApplicationDetail): void;
  onConflict(): void;
}) {
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const submitting = useRef(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (submitting.current || !reason.trim()) return;
    submitting.current = true;
    setBusy(true);
    setError("");
    try {
      onWithdrawn(await applicationService.withdraw(id, { reason: reason.trim() }));
      setOpen(false);
    } catch (err) {
      setError(getErrorMessage(err));
      if (err instanceof ApiError && err.status === 409) onConflict();
    } finally {
      submitting.current = false;
      setBusy(false);
    }
  }

  return (
    <Dialog.Root
      open={open}
      onOpenChange={(value) => {
        if (!busy) setOpen(value);
      }}
    >
      <Dialog.Trigger render={<Button variant="destructive" />}>Rút đơn</Dialog.Trigger>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-[1px]" />
        <Dialog.Popup className="fixed top-1/2 left-1/2 z-50 w-[calc(100%-2rem)] max-w-lg -translate-x-1/2 -translate-y-1/2 overflow-hidden rounded-2xl border border-border bg-white p-6 shadow-[0_24px_80px_rgb(15_23_42/0.20)]">
          <div className="mb-3 flex size-11 items-center justify-center rounded-xl border border-[#FECACA] bg-[#FEF2F2] text-destructive">
            <AlertTriangle className="size-5" aria-hidden="true" />
          </div>
          <Dialog.Title className="text-xl font-bold text-heading">Rút đơn ứng tuyển</Dialog.Title>
          <Dialog.Description className="mt-1.5 mb-5 text-sm leading-relaxed text-muted-foreground">
            Đơn hiện tại sẽ kết thúc. Bạn vẫn có thể nộp đơn mới nếu vị trí đang nhận hồ sơ.
          </Dialog.Description>
          <form onSubmit={submit} className="space-y-4">
            <div>
              <label htmlFor="withdraw-reason" className="block text-sm font-semibold text-heading">
                Lý do rút đơn
              </label>
              <textarea
                id="withdraw-reason"
                required
                maxLength={1000}
                rows={4}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                disabled={busy}
                placeholder="Chia sẻ ngắn gọn lý do bạn rút đơn..."
                className="mt-2 w-full rounded-xl border border-input bg-white p-3 text-base text-foreground outline-none focus:border-ring focus:ring-2 focus:ring-ring/20 md:text-sm"
              />
            </div>
            {error && (
              <p role="alert" className="text-sm font-medium text-destructive">
                {error}
              </p>
            )}
            <div className="flex justify-end gap-3 pt-2">
              <Dialog.Close
                disabled={busy}
                className="inline-flex min-h-10 cursor-pointer items-center justify-center rounded-lg border border-border bg-[#F1F5F9] px-4 py-2 text-sm font-semibold text-heading hover:bg-[#E2E8F0]"
              >
                Giữ đơn
              </Dialog.Close>
              <Button
                type="submit"
                disabled={busy || !reason.trim()}
                className="bg-[#DA251D] text-white hover:bg-[#B4232D]"
              >
                {busy ? "Đang xử lý…" : "Xác nhận rút đơn"}
              </Button>
            </div>
          </form>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
