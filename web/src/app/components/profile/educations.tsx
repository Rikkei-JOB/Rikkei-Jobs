"use client";

import * as React from "react";
import { useForm } from "react-hook-form";
import { Card, CardContent, CardHeader } from "@/app/components/ui/card";
import { Button } from "@/app/components/ui/button";
import { Alert } from "@/app/components/ui/alert";
import { Input } from "@/app/components/ui/input";
import { FormField } from "@/app/components/auth/form-field";
import { zodResolver } from "@/helper/zod-resolver";
import { educationSchema, educationPayload, type EducationValues } from "@/schemas/profile-qualifications.schema";
import { qualificationService } from "@/services/profile-qualifications.service";
import { getErrorMessage } from "@/services/api";
import type { Education } from "@/interface/profile-qualifications";

export function Educations() {
  const [items, setItems] = React.useState<Education[] | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [reload, setReload] = React.useState(0);
  const [editing, setEditing] = React.useState<Education | null | undefined>(undefined);
  const [deleting, setDeleting] = React.useState<string | null>(null);
  React.useEffect(() => {
    let ignore = false;
    qualificationService.educations().then(data => { if (!ignore) { setItems(data); setError(null); } })
      .catch(err => { if (!ignore) setError(getErrorMessage(err, "Không thể tải học vấn.")); });
    return () => { ignore = true; };
  }, [reload]);
  async function remove(item: Education) {
    if (!window.confirm(`Xoá học vấn tại ${item.school}?`)) return;
    setDeleting(item.id); setError(null);
    try { await qualificationService.deleteEducation(item.id); setItems(old => old?.filter(e => e.id !== item.id) ?? []); }
    catch (err) { setError(getErrorMessage(err, "Không thể xoá học vấn.")); }
    finally { setDeleting(null); }
  }
  return <Card>
    <CardHeader><div className="flex flex-wrap items-center justify-between gap-3"><h2 className="text-lg font-semibold">Học vấn</h2>
      <Button onClick={() => setEditing(null)} disabled={editing !== undefined || items === null}>Thêm học vấn</Button></div></CardHeader>
    <CardContent className="space-y-4">
      {error ? <div className="space-y-2"><Alert variant="destructive">{error}</Alert>{items === null ? <Button onClick={() => { setError(null); setReload(v => v + 1); }}>Thử lại học vấn</Button> : null}</div> : null}
      {items === null && !error ? <p role="status">Đang tải học vấn...</p> : null}
      {editing !== undefined ? <EducationForm key={editing?.id ?? "new"} item={editing} onCancel={() => setEditing(undefined)} onSaved={item => {
        setItems(old => editing ? (old ?? []).map(e => e.id === item.id ? item : e) : [...old ?? [], item]); setEditing(undefined);
      }} /> : null}
      {items?.length === 0 && editing === undefined ? <p className="text-sm text-muted-foreground">Chưa có thông tin học vấn.</p> : null}
      {items?.map(item => <article key={item.id} className="rounded-lg border p-3 space-y-2">
        <h3 className="font-medium break-words">{item.school}</h3>
        {item.major ? <p className="text-sm break-words">{item.major}</p> : null}
        <p className="text-xs text-muted-foreground">{item.fromDate} — {item.toDate ?? "Đang học"}{item.gpa !== null ? ` · GPA: ${item.gpa}/10` : ""}</p>
        {item.note ? <p className="whitespace-pre-wrap break-words text-sm">{item.note}</p> : null}
        <div className="flex gap-2"><Button variant="outline" aria-label={`Sửa học vấn ${item.school}`} disabled={editing !== undefined || deleting !== null} onClick={() => setEditing(item)}>Sửa</Button>
          <Button variant="destructive" aria-label={`Xoá học vấn ${item.school}`} disabled={editing !== undefined || deleting !== null} onClick={() => void remove(item)}>{deleting === item.id ? "Đang xoá..." : "Xoá"}</Button></div>
      </article>)}
    </CardContent>
  </Card>;
}

function EducationForm({ item, onCancel, onSaved }: { item: Education | null; onCancel: () => void; onSaved: (item: Education) => void }) {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<EducationValues>({
    resolver: zodResolver(educationSchema), defaultValues: { school: item?.school ?? "", major: item?.major ?? "", fromDate: item?.fromDate ?? "", toDate: item?.toDate ?? "", gpa: item?.gpa?.toString() ?? "", note: item?.note ?? "" },
  });
  const [error, setError] = React.useState<string | null>(null);
  async function submit(values: EducationValues) {
    setError(null);
    try { onSaved(await qualificationService.saveEducation(item?.id ?? null, educationPayload(values))); }
    catch (err) { setError(getErrorMessage(err, "Không thể lưu học vấn.")); }
  }
  return <form onSubmit={handleSubmit(submit)} noValidate className="rounded-lg border p-3 space-y-4">
    <fieldset disabled={isSubmitting} className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Trường học" htmlFor="education-school" error={errors.school?.message}><Input id="education-school" required maxLength={255} aria-invalid={Boolean(errors.school)} {...register("school")} /></FormField>
        <FormField label="Chuyên ngành" htmlFor="education-major" error={errors.major?.message}><Input id="education-major" maxLength={255} {...register("major")} /></FormField>
        <FormField label="Từ ngày" htmlFor="education-from" error={errors.fromDate?.message}><Input id="education-from" type="date" required {...register("fromDate")} /></FormField>
        <FormField label="Đến ngày" htmlFor="education-to" error={errors.toDate?.message}><Input id="education-to" type="date" {...register("toDate")} /></FormField>
        <FormField label="GPA (0–10)" htmlFor="education-gpa" error={errors.gpa?.message}><Input id="education-gpa" type="number" step="any" min={0} max={10} {...register("gpa")} /></FormField>
      </div>
      <FormField label="Ghi chú học vấn" htmlFor="education-note" error={errors.note?.message}><textarea id="education-note" rows={3} maxLength={5000} className="w-full min-w-0 rounded-md border border-input p-2 text-sm" {...register("note")} /></FormField>
      {error ? <Alert variant="destructive">{error}</Alert> : null}
      <div className="flex gap-2"><Button type="submit">{isSubmitting ? "Đang lưu..." : "Lưu học vấn"}</Button><Button type="button" variant="outline" onClick={onCancel}>Huỷ</Button></div>
    </fieldset>
  </form>;
}
