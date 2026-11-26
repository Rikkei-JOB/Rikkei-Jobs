"use client";

import * as React from "react";
import { Camera, Trash2 } from "lucide-react";
import { Alert } from "@/app/components/ui/alert";
import { Button } from "@/app/components/ui/button";
import { qualificationService } from "@/services/profile-qualifications.service";
import { validateProfileFile } from "@/schemas/profile-qualifications.schema";
import { getErrorMessage } from "@/services/api";
import { ICON_DANGER } from "./display";

/** Ảnh đại diện tròn ở đầu thẻ hồ sơ: bấm biểu tượng máy ảnh để đổi ảnh. Thông báo hiện ở hàng riêng bên dưới. */
export function ProfileAvatar({ initialFileUrl, fallback }: { initialFileUrl: string | null; fallback: string }) {
  const [fileUrl, setFileUrl] = React.useState<string | null>(initialFileUrl);
  const [src, setSrc] = React.useState<string | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [busy, setBusy] = React.useState(false);
  const [saved, setSaved] = React.useState(false);
  const [reload, setReload] = React.useState(0);
  React.useEffect(() => {
    let ignore = false; let objectUrl: string | null = null;
    if (fileUrl) qualificationService.readFile(fileUrl).then(blob => {
      if (!ignore) { objectUrl = URL.createObjectURL(blob); setSrc(objectUrl); }
    }).catch(err => { if (!ignore) setError(getErrorMessage(err, "Không thể tải ảnh đại diện.")); });
    return () => { ignore = true; if (objectUrl) URL.revokeObjectURL(objectUrl); };
  }, [fileUrl, reload]);
  async function upload(file: File) {
    setSaved(false); setError(null);
    const invalid = validateProfileFile(file, true);
    if (invalid) { setError(invalid); return; }
    setBusy(true);
    try {
      const url = await qualificationService.upload(file);
      const profile = await qualificationService.saveAvatar(url); setSrc(null); setFileUrl(profile.avatarUrl); setSaved(true);
    } catch (err) { setError(getErrorMessage(err, "Không thể cập nhật ảnh đại diện.")); }
    finally { setBusy(false); }
  }
  async function remove() {
    if (!window.confirm("Xoá ảnh đại diện?")) return;
    setBusy(true); setSaved(false); setError(null);
    try { await qualificationService.saveAvatar(null); setFileUrl(null); setSrc(null); setSaved(true); }
    catch (err) { setError(getErrorMessage(err, "Không thể xoá ảnh đại diện.")); }
    finally { setBusy(false); }
  }
  return <>
    <div className="flex items-end gap-2">
      <div className="relative">
        <div className="flex size-24 items-center justify-center overflow-hidden rounded-full bg-[#FFF1F2] text-2xl font-semibold text-[#B4232D] ring-4 ring-white">
          {src ? <picture><img src={src} alt="Ảnh đại diện của bạn" className="size-24 object-cover" /></picture> : <span aria-hidden>{fallback}</span>}
        </div>
        <label htmlFor="profile-avatar" title="Đổi ảnh đại diện (JPG hoặc PNG, tối đa 5MB)"
          className={`absolute -right-2 -bottom-1 flex size-11 cursor-pointer items-center justify-center rounded-full border border-[#E2E8F0] bg-white text-[#475569] shadow-[0_2px_8px_rgb(15_23_42/0.08)] hover:bg-[#FFF1F2] hover:text-[#B4232D] [&:has(+input:focus-visible)]:outline-2 [&:has(+input:focus-visible)]:outline-offset-2 [&:has(+input:focus-visible)]:outline-[#B4232D] ${busy ? "pointer-events-none opacity-50" : ""}`}>
          <Camera className="size-5" aria-hidden />
        </label>
        <input id="profile-avatar" type="file" accept="image/jpeg,image/png" aria-label="Chọn ảnh đại diện" className="sr-only" disabled={busy} onChange={event => {
          const file = event.target.files?.[0]; if (file) void upload(file); event.target.value = "";
        }} />
      </div>
      {fileUrl ? <Button variant="ghost" className={ICON_DANGER} aria-label="Xoá ảnh đại diện" onClick={() => void remove()} disabled={busy}><Trash2 /></Button> : null}
    </div>
    {busy || error || saved ? <div className="order-last basis-full space-y-2">
      {busy ? <p role="status" className="text-xs text-muted-foreground">Đang cập nhật ảnh đại diện...</p> : null}
      {error ? <div className="space-y-2"><Alert variant="destructive">{error}</Alert>{fileUrl ? <Button onClick={() => { setError(null); setReload(v => v + 1); }}>Tải lại ảnh</Button> : null}</div> : null}
      {saved ? <Alert variant="success">Đã cập nhật ảnh đại diện</Alert> : null}
    </div> : null}
  </>;
}
