"use client";
import { useEffect, useRef, useState } from "react";
import { Button } from "@/app/components/ui/button";
import { useAuthStore } from "@/hooks/use-auth-store";
import { contactService } from "@/services/contact.service";
import { ApiError, getErrorMessage } from "@/services/api";
import type { ContactAccess, ContactView } from "@/interface/contact";
import { RequestContactDialog } from "./request-contact-dialog";

export function CandidateContactPanel(props: { userId: string; jobId?: string }) {
  const token = useAuthStore(s => s.token);
  const user = useAuthStore(s => s.user);
  if (!token || user?.role !== "HR") return null;
  return <ContactPanel key={`${props.userId}:${user.companyId}:${token}`} {...props} />;
}
function ContactPanel({ userId, jobId }: { userId: string; jobId?: string }) {
  const [access, setAccess] = useState<ContactAccess | null>(null);
  const [contact, setContact] = useState<ContactView | null>(null);
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);
  const [busy, setBusy] = useState(false);
  const inFlight = useRef(false);
  const alive = useRef(true);
  useEffect(() => { alive.current = true; return () => { alive.current = false; }; }, []);
  useEffect(() => {
    const abort = new AbortController();
    contactService.status(userId, abort.signal).then(setAccess).catch(err => {
      if (!abort.signal.aborted) setError(getErrorMessage(err));
    });
    return () => abort.abort();
  }, [userId, revision]);
  function refresh() { setContact(null); setAccess(null); setError(""); setRevision(v => v + 1); }
  async function reveal() {
    if (inFlight.current) return;
    inFlight.current = true; setBusy(true); setContact(null); setError("");
    try { const result = await contactService.reveal(userId); if (alive.current) setContact(result); }
    catch (err) { if (alive.current) { setError(getErrorMessage(err)); if (err instanceof ApiError && err.status === 403) { setAccess(null); setRevision(v => v + 1); } } }
    finally { inFlight.current = false; if (alive.current) setBusy(false); }
  }
  return <section className="mt-4 space-y-3 rounded-xl border bg-card p-4 text-card-foreground" aria-label="Liên hệ ứng viên">
    {access && <p className="font-medium">{access.fullName}</p>}
    <p className="text-sm font-medium">{contact ? "Thông tin liên hệ" : "Liên hệ: đã ẩn"}</p>
    {error && <p role="alert" className="break-words text-sm text-destructive">{error}</p>}
    {!access && !error && <p role="status" className="text-sm text-muted-foreground">Đang kiểm tra quyền…</p>}
    {!access && error && <Button className="min-h-11" variant="outline" onClick={refresh}>Thử lại</Button>}
    {access?.canSeeContact ? <div className="space-y-3">
      {contact && <dl className="space-y-2 break-all text-sm"><div><dt className="text-muted-foreground">Email</dt><dd>{contact.email || "Chưa cập nhật"}</dd></div><div><dt className="text-muted-foreground">Điện thoại</dt><dd>{contact.phone || "Chưa cập nhật"}</dd></div></dl>}
      <Button className="min-h-11" onClick={reveal} disabled={busy}>{busy ? "Đang lấy liên hệ…" : contact ? "Xem lại" : "Xem liên hệ"}</Button>
      <p className="text-xs text-muted-foreground">Mỗi lần xem được ghi vào nhật ký để ứng viên tra cứu.</p>
    </div> : access && <div className="space-y-3">
      {access.requestStatus === "PENDING" ? <><p className="text-sm text-muted-foreground">Đang chờ STAFF duyệt</p><Button className="min-h-11" variant="outline" onClick={refresh}>Kiểm tra trạng thái</Button></> : <>
        {access.requestStatus === "REJECTED" && <p className="whitespace-pre-wrap break-words text-sm text-destructive">Yêu cầu bị từ chối: {access.latestRequest?.rejectionReason}</p>}
        <RequestContactDialog userId={userId} jobId={jobId} onChanged={refresh} />
      </>}
    </div>}
  </section>;
}
