"use client";

import * as React from "react";
import { useForm } from "react-hook-form";
import { Award, Download, FileText, Paperclip, Pencil, Plus, Trash2, Upload } from "lucide-react";
import { localToday } from "@/schemas/candidate-profile.schema";
import { ADD_TILE, BADGE, BRAND_SOFT, ICON_ACTION, ICON_DANGER, ITEM_CARD, ITEM_TITLE, META, PRIMARY_CTA, SECONDARY_CTA, SECTION_TITLE, TONE, monthYear } from "./display";
import { Card, CardContent, CardHeader } from "@/app/components/ui/card";
import { Button } from "@/app/components/ui/button";
import { Alert } from "@/app/components/ui/alert";
import { Input } from "@/app/components/ui/input";
import { FormDialog } from "@/app/components/profile/form-dialog";
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
  const today = localToday();
  return <Card>
    <CardHeader><div className="flex flex-wrap items-center justify-between gap-3">
      <h2 className={SECTION_TITLE}>Chứng chỉ{items?.length ? <span className="font-normal text-[#64748B]"> · {items.length}</span> : null}</h2>
      <Button className={PRIMARY_CTA} onClick={() => setEditing(null)} disabled={items === null}><Plus aria-hidden />Thêm chứng chỉ</Button></div></CardHeader>
    <CardContent className="space-y-4">
      {error ? <div className="space-y-2"><Alert variant="destructive">{error}</Alert>{items === null ? <Button onClick={() => { setError(null); setReload(v => v + 1); }}>Thử lại chứng chỉ</Button> : null}</div> : null}
      {items === null && !error ? <p role="status">Đang tải chứng chỉ...</p> : null}
      {editing !== undefined ? <FormDialog title={editing ? "Sửa chứng chỉ" : "Thêm chứng chỉ"} onClose={() => setEditing(undefined)}><CertificateForm key={editing?.id ?? "new"} item={editing} onCancel={() => setEditing(undefined)} onSaved={item => {
        setItems(old => editing ? (old ?? []).map(e => e.id === item.id ? item : e) : [...old ?? [], item]); setEditing(undefined);
      }} /></FormDialog> : null}
      {items?.length === 0 ? <button type="button" onClick={() => setEditing(null)} className={ADD_TILE}>
        <Award className="size-6" aria-hidden /><span className="text-sm font-medium">Chưa có chứng chỉ</span><span className="text-xs">Thêm chứng chỉ ngoại ngữ, chuyên môn bạn đã có.</span>
      </button> : null}
      {items?.length ? <ul className="grid grid-cols-[repeat(auto-fill,minmax(min(100%,300px),1fr))] gap-4">{items.map(item => {
        const expiry = expiryStatus(item.expiresAt, today);
        return <li key={item.id}
          onClick={e => { if (busy === null && !(e.target as HTMLElement).closest("button,a")) setEditing(item); }}
          className={`flex cursor-pointer flex-col gap-3 p-4 ${ITEM_CARD} ${busy === item.id ? "opacity-50" : ""}`}>
          <div className="flex items-start justify-between gap-2">
            <div className={`flex size-11 shrink-0 items-center justify-center rounded-lg ${BRAND_SOFT}`} aria-hidden>
              {item.fileType === "application/pdf" ? <FileText className="size-5" /> : <Award className="size-5" />}
            </div>
            <div className="flex shrink-0">
              {item.fileUrl ? <Button variant="ghost" className={ICON_ACTION} aria-label={`Tải tệp ${item.name}`} disabled={busy !== null} onClick={() => void download(item)}><Download /></Button> : null}
              <Button variant="ghost" className={ICON_ACTION} aria-label={`Sửa chứng chỉ ${item.name}`} disabled={busy !== null} onClick={() => setEditing(item)}><Pencil /></Button>
              <Button variant="ghost" className={ICON_DANGER} aria-label={`Xoá chứng chỉ ${item.name}`} disabled={busy !== null} onClick={() => void remove(item)}><Trash2 /></Button>
            </div>
          </div>
          <div className="min-w-0">
            <h3 className={ITEM_TITLE}>{item.name}</h3>
            <p className={`mt-0.5 break-words ${META}`}>{item.issuer} · Cấp {monthYear(item.issuedAt)}</p>
          </div>
          <div className="mt-auto flex flex-wrap gap-1.5">
            <span className={`${BADGE} ${expiry.className}`}>{expiry.label}</span>
            {item.fileUrl ? <span className={`${BADGE} ${TONE.neutral}`}><Paperclip className="size-3.5" aria-hidden />Có tệp</span> : null}
          </div>
        </li>;
      })}</ul> : null}
    </CardContent>
  </Card>;
}

/** Số ngày trước hạn bắt đầu báo "sắp hết hạn". */
const EXPIRY_WARNING_DAYS = 90;
const DAY_MS = 86_400_000;

/** So sánh ngày dạng ISO "yyyy-mm-dd" với hôm nay theo giờ máy. */
function expiryStatus(expiresAt: string | null, today: string) {
  if (!expiresAt) return { label: "Không thời hạn", className: TONE.success };
  if (expiresAt < today) return { label: `Đã hết hạn ${monthYear(expiresAt)}`, className: TONE.danger };
  const days = Math.round((Date.parse(expiresAt) - Date.parse(today)) / DAY_MS);
  if (days <= EXPIRY_WARNING_DAYS) return { label: `Sắp hết hạn · còn ${days} ngày`, className: TONE.warning };
  return { label: `Còn hạn đến ${monthYear(expiresAt)}`, className: TONE.success };
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
  return <form onSubmit={handleSubmit(submit)} noValidate className="space-y-4">
    <fieldset disabled={isSubmitting} className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Tên chứng chỉ" htmlFor="certificate-name" error={errors.name?.message}><Input id="certificate-name" required maxLength={255} {...register("name")} /></FormField>
        <FormField label="Đơn vị cấp" htmlFor="certificate-issuer" error={errors.issuer?.message}><Input id="certificate-issuer" required maxLength={255} {...register("issuer")} /></FormField>
        <FormField label="Ngày cấp" htmlFor="certificate-issued" error={errors.issuedAt?.message}><Input id="certificate-issued" type="date" required {...register("issuedAt")} /></FormField>
        <FormField label="Ngày hết hạn" htmlFor="certificate-expires" error={errors.expiresAt?.message}><Input id="certificate-expires" type="date" {...register("expiresAt")} /></FormField>
      </div>
      <FormField label="Tệp chứng chỉ" htmlFor="certificate-file">
        {/* Input trong suốt phủ cả khung nên vừa bấm chọn vừa kéo thả tệp vào được. */}
        <div className="relative flex min-h-11 items-center gap-3 rounded-lg border border-dashed border-[#64748B] p-3 text-sm transition-colors duration-200 hover:border-[#B4232D] focus-within:outline-2 focus-within:outline-offset-2 focus-within:outline-[#B4232D]">
          {file ? <FileText className="size-5 shrink-0 text-[#B4232D]" aria-hidden /> : <Upload className="size-5 shrink-0 text-muted-foreground" aria-hidden />}
          <span className="min-w-0 flex-1 truncate">{file ? file.name : attachment ? "Đã có tệp đính kèm · kéo thả hoặc bấm để đổi" : "Kéo thả tệp vào đây hoặc bấm để chọn"}</span>
          <input id="certificate-file" type="file" accept="application/pdf,image/jpeg,image/png" className="absolute inset-0 cursor-pointer opacity-0"
            onChange={e => { setFile(e.target.files?.[0] ?? null); setError(null); }} />
        </div>
      </FormField>
      <p className="text-xs text-muted-foreground">PDF, JPG hoặc PNG, tối đa 5MB. Tệp chỉ dành cho bạn.</p>
      {attachment && !file ? <Button type="button" variant="outline" onClick={() => setAttachment(null)}>Bỏ tệp đính kèm</Button> : null}
      {error ? <Alert variant="destructive">{error}</Alert> : null}
      <div className="flex gap-2"><Button type="submit" className={PRIMARY_CTA}>{isSubmitting ? "Đang lưu..." : "Lưu chứng chỉ"}</Button><Button type="button" variant="outline" className={SECONDARY_CTA} onClick={onCancel}>Huỷ</Button></div>
    </fieldset>
  </form>;
}
