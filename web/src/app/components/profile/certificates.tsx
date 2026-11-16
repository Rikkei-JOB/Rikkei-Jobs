"use client";

import * as React from "react";
import { useForm } from "react-hook-form";
import { Card, CardContent, CardHeader } from "@/app/components/ui/card";
import { Button } from "@/app/components/ui/button";
import { Alert } from "@/app/components/ui/alert";
import { Input } from "@/app/components/ui/input";
import { FormField } from "@/app/components/auth/form-field";
import { zodResolver } from "@/helper/zod-resolver";
import { certificateSchema, certificatePayload, validateProfileFile, type CertificateValues } from "@/schemas/profile-qualifications.schema";
import { qualificationService } from "@/services/profile-qualifications.service";
import { getErrorMessage } from "@/services/api";
import type { Certificate } from "@/interface/profile-qualifications";

export function Certificates() {
  const [items, setItems] = React.useState<Certificate[] | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [reload, setReload] = React.useState(0);
  const [editing, setEditing] = React.useState<Certificate | null | undefined>(undefined);
  const [busy, setBusy] = React.useState<string | null>(null);
  React.useEffect(() => {
    let ignore = false;
    qualificationService.certificates().then(data => { if (!ignore) { setItems(data); setError(null); } })
      .catch(err => { if (!ignore) setError(getErrorMessage(err, "Không thể tải chứng chỉ.")); });
    return () => { ignore = true; };
  }, [reload]);
  async function remove(item: Certificate) {
    if (!window.confirm(`Xoá chứng chỉ ${item.name}?`)) return;
    setBusy(item.id); setError(null);
    try { await qualificationService.deleteCertificate(item.id); setItems(old => old?.filter(e => e.id !== item.id) ?? []); }
    catch (err) { setError(getErrorMessage(err, "Không thể xoá chứng chỉ.")); }
    finally { setBusy(null); }
  }
  async function download(item: Certificate) {
    if (!item.fileUrl) return;
    setBusy(item.id); setError(null);
    try {
      const blob = await qualificationService.readFile(item.fileUrl);
      const url = URL.createObjectURL(blob); const link = document.createElement("a");
      link.href = url; link.download = `chung-chi.${blob.type === "application/pdf" ? "pdf" : blob.type === "image/png" ? "png" : "jpg"}`;
      link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (err) { setError(getErrorMessage(err, "Không thể tải tệp chứng chỉ.")); }
    finally { setBusy(null); }
  }
  return <Card>
    <CardHeader><div className="flex flex-wrap items-center justify-between gap-3"><h2 className="text-lg font-semibold">Chứng chỉ</h2>
      <Button onClick={() => setEditing(null)} disabled={editing !== undefined || items === null}>Thêm chứng chỉ</Button></div></CardHeader>
    <CardContent className="space-y-4">
      {error ? <div className="space-y-2"><Alert variant="destructive">{error}</Alert>{items === null ? <Button onClick={() => { setError(null); setReload(v => v + 1); }}>Thử lại chứng chỉ</Button> : null}</div> : null}
      {items === null && !error ? <p role="status">Đang tải chứng chỉ...</p> : null}
      {editing !== undefined ? <CertificateForm key={editing?.id ?? "new"} item={editing} onCancel={() => setEditing(undefined)} onSaved={item => {
        setItems(old => editing ? (old ?? []).map(e => e.id === item.id ? item : e) : [...old ?? [], item]); setEditing(undefined);
      }} /> : null}
      {items?.length === 0 && editing === undefined ? <p className="text-sm text-muted-foreground">Chưa có chứng chỉ.</p> : null}
      {items?.map(item => <article key={item.id} className="rounded-lg border p-3 space-y-2">
        <h3 className="font-medium break-words">{item.name}</h3><p className="text-sm break-words">{item.issuer}</p>
        <p className="text-xs text-muted-foreground">Cấp ngày {item.issuedAt}{item.expiresAt ? ` · Hết hạn ${item.expiresAt}` : ""}</p>
        <div className="flex flex-wrap gap-2">
          {item.fileUrl ? <Button variant="outline" aria-label={`Tải tệp ${item.name}`} disabled={busy !== null} onClick={() => void download(item)}>Tải tệp</Button> : null}
          <Button variant="outline" aria-label={`Sửa chứng chỉ ${item.name}`} disabled={editing !== undefined || busy !== null} onClick={() => setEditing(item)}>Sửa</Button>
          <Button variant="destructive" aria-label={`Xoá chứng chỉ ${item.name}`} disabled={editing !== undefined || busy !== null} onClick={() => void remove(item)}>Xoá</Button>
        </div>
      </article>)}
    </CardContent>
  </Card>;
}

function CertificateForm({ item, onCancel, onSaved }: { item: Certificate | null; onCancel: () => void; onSaved: (item: Certificate) => void }) {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<CertificateValues>({
    resolver: zodResolver(certificateSchema), defaultValues: { name: item?.name ?? "", issuer: item?.issuer ?? "", issuedAt: item?.issuedAt ?? "", expiresAt: item?.expiresAt ?? "" },
  });
  const [error, setError] = React.useState<string | null>(null);
  const [file, setFile] = React.useState<File | null>(null);
  const [attachment, setAttachment] = React.useState<string | null>(item?.fileUrl ?? null);
  async function submit(values: CertificateValues) {
    setError(null);
    if (file) {
      const invalid = validateProfileFile(file);
      if (invalid) { setError(invalid); return; }
    }
    try {
      const fileUrl = file ? await qualificationService.upload(file) : attachment;
      // Keep a successful upload for retry if saving the certificate itself fails.
      setAttachment(fileUrl); setFile(null);
      onSaved(await qualificationService.saveCertificate(item?.id ?? null, certificatePayload(values, fileUrl)));
    } catch (err) { setError(getErrorMessage(err, "Không thể lưu chứng chỉ.")); }
  }
  return <form onSubmit={handleSubmit(submit)} noValidate className="rounded-lg border p-3 space-y-4">
    <fieldset disabled={isSubmitting} className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Tên chứng chỉ" htmlFor="certificate-name" error={errors.name?.message}><Input id="certificate-name" required maxLength={255} {...register("name")} /></FormField>
        <FormField label="Đơn vị cấp" htmlFor="certificate-issuer" error={errors.issuer?.message}><Input id="certificate-issuer" required maxLength={255} {...register("issuer")} /></FormField>
        <FormField label="Ngày cấp" htmlFor="certificate-issued" error={errors.issuedAt?.message}><Input id="certificate-issued" type="date" required {...register("issuedAt")} /></FormField>
        <FormField label="Ngày hết hạn" htmlFor="certificate-expires" error={errors.expiresAt?.message}><Input id="certificate-expires" type="date" {...register("expiresAt")} /></FormField>
      </div>
      <FormField label="Tệp chứng chỉ" htmlFor="certificate-file"><Input id="certificate-file" type="file" accept="application/pdf,image/jpeg,image/png" onChange={e => { setFile(e.target.files?.[0] ?? null); setError(null); }} /></FormField>
      <p className="text-xs text-muted-foreground">PDF, JPG hoặc PNG, tối đa 5MB. Tệp chỉ dành cho bạn.</p>
      {attachment && !file ? <div className="flex flex-wrap items-center gap-2 text-sm"><span>Đã có tệp đính kèm</span><Button type="button" variant="outline" onClick={() => setAttachment(null)}>Bỏ tệp đính kèm</Button></div> : null}
      {error ? <Alert variant="destructive">{error}</Alert> : null}
      <div className="flex gap-2"><Button type="submit">{isSubmitting ? "Đang lưu..." : "Lưu chứng chỉ"}</Button><Button type="button" variant="outline" onClick={onCancel}>Huỷ</Button></div>
    </fieldset>
  </form>;
}
