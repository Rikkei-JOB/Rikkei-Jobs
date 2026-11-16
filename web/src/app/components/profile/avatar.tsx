"use client";

import * as React from "react";
import { Card, CardContent, CardHeader } from "@/app/components/ui/card";
import { Alert } from "@/app/components/ui/alert";
import { Button } from "@/app/components/ui/button";
import { Input } from "@/app/components/ui/input";
import { FormField } from "@/app/components/auth/form-field";
import { qualificationService } from "@/services/profile-qualifications.service";
import { validateProfileFile } from "@/schemas/profile-qualifications.schema";
import { getErrorMessage } from "@/services/api";

export function ProfileAvatar({ initialFileUrl }: { initialFileUrl: string | null }) {
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
  return <Card>
    <CardHeader><h2 className="text-lg font-semibold">Ảnh đại diện</h2></CardHeader>
    <CardContent className="space-y-3">
      {src ? <picture><img src={src} alt="Ảnh đại diện của bạn" className="size-24 rounded-full object-cover ring-1 ring-border" /></picture> : null}
      <FormField label="Chọn ảnh đại diện" htmlFor="profile-avatar"><Input id="profile-avatar" type="file" accept="image/jpeg,image/png" disabled={busy} onChange={event => {
        const file = event.target.files?.[0]; if (file) void upload(file); event.target.value = "";
      }} /></FormField>
      <p className="text-xs text-muted-foreground">JPG hoặc PNG, tối đa 5MB. Ảnh được lưu riêng tư.</p>
      {fileUrl ? <Button variant="outline" onClick={() => void remove()} disabled={busy}>Xoá ảnh đại diện</Button> : null}
      {busy ? <p role="status">Đang cập nhật ảnh đại diện...</p> : null}
      {error ? <div className="space-y-2"><Alert variant="destructive">{error}</Alert>{fileUrl ? <Button onClick={() => { setError(null); setReload(v => v + 1); }}>Tải lại ảnh</Button> : null}</div> : null}
      {saved ? <Alert variant="success">Đã cập nhật ảnh đại diện</Alert> : null}
    </CardContent>
  </Card>;
}
