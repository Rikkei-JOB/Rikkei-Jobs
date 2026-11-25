"use client";

import * as React from "react";
import { useForm } from "react-hook-form";
import { GraduationCap, Pencil, Plus, Trash2 } from "lucide-react";
import { HOVER_ACTIONS, initials, monthYear, toneOf } from "./display";
import { Card, CardContent, CardHeader } from "@/app/components/ui/card";
import { Button } from "@/app/components/ui/button";
import { Alert } from "@/app/components/ui/alert";
import { Input } from "@/app/components/ui/input";
import { FormDialog } from "@/app/components/profile/form-dialog";
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
  // Mốc mới nhất lên đầu dòng thời gian.
  const sorted = items ? [...items].sort((a, b) => b.fromDate.localeCompare(a.fromDate)) : null;
  return <Card>
    <CardHeader><div className="flex flex-wrap items-center justify-between gap-3">
      <h2 className="text-lg font-semibold">Học vấn{items?.length ? <span className="font-normal text-muted-foreground"> · {items.length}</span> : null}</h2>
      <Button onClick={() => setEditing(null)} disabled={items === null}><Plus aria-hidden />Thêm học vấn</Button></div></CardHeader>
    <CardContent className="space-y-4">
      {error ? <div className="space-y-2"><Alert variant="destructive">{error}</Alert>{items === null ? <Button onClick={() => { setError(null); setReload(v => v + 1); }}>Thử lại học vấn</Button> : null}</div> : null}
      {items === null && !error ? <p role="status">Đang tải học vấn...</p> : null}
      {editing !== undefined ? <FormDialog title={editing ? "Sửa học vấn" : "Thêm học vấn"} onClose={() => setEditing(undefined)}><EducationForm key={editing?.id ?? "new"} item={editing} onCancel={() => setEditing(undefined)} onSaved={item => {
        setItems(old => editing ? (old ?? []).map(e => e.id === item.id ? item : e) : [...old ?? [], item]); setEditing(undefined);
      }} /></FormDialog> : null}
      {items?.length === 0 ? <button type="button" onClick={() => setEditing(null)} className="flex w-full flex-col items-center gap-1 rounded-xl border border-dashed p-6 text-muted-foreground transition-colors hover:border-foreground/40 hover:text-foreground">
        <GraduationCap className="size-6" aria-hidden /><span className="text-sm">Chưa có thông tin học vấn</span><span className="text-xs">Thêm trường bạn đã và đang học.</span>
      </button> : null}
      {sorted?.length ? <ol>{sorted.map((item, index) => <li key={item.id}
        onClick={e => { if (deleting === null && !(e.target as HTMLElement).closest("button,a")) setEditing(item); }}
        className={`group flex cursor-pointer gap-3 rounded-xl p-3 transition-colors hover:bg-muted/60 ${deleting === item.id ? "opacity-50" : ""}`}>
        <div className="flex flex-col items-center">
          <div className={`flex size-10 shrink-0 items-center justify-center rounded-lg text-xs font-semibold ${toneOf(item.school)}`} aria-hidden>{schoolInitials(item.school)}</div>
          {index < sorted.length - 1 ? <div className="mt-2 w-px flex-1 bg-border" aria-hidden /> : null}
        </div>
        <div className="min-w-0 flex-1 pb-1">
          <div className="flex items-start justify-between gap-2">
            <div className="min-w-0">
              <h3 className="text-sm font-semibold break-words">{item.school}</h3>
              <p className="mt-0.5 text-xs text-muted-foreground break-words">{[item.major, `${monthYear(item.fromDate)} – ${item.toDate ? monthYear(item.toDate) : "nay"}`].filter(Boolean).join(" · ")}</p>
            </div>
            <div className={HOVER_ACTIONS}>
              <Button variant="outline" size="icon-sm" aria-label={`Sửa học vấn ${item.school}`} disabled={deleting !== null} onClick={() => setEditing(item)}><Pencil /></Button>
              <Button variant="outline" size="icon-sm" className="text-destructive" aria-label={`Xoá học vấn ${item.school}`} disabled={deleting !== null} onClick={() => void remove(item)}><Trash2 /></Button>
            </div>
          </div>
          <div className="mt-2 flex flex-wrap gap-1.5">
            {item.gpa !== null ? <span className="rounded-full bg-emerald-50 px-2 py-0.5 text-[0.6875rem] font-medium text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300">GPA {item.gpa} / 10</span> : null}
            {item.toDate ? null : <span className="rounded-full bg-sky-50 px-2 py-0.5 text-[0.6875rem] text-sky-700 dark:bg-sky-950 dark:text-sky-300">Đang học</span>}
          </div>
          {item.note ? <p className="mt-2 text-xs whitespace-pre-wrap break-words text-muted-foreground">{item.note}</p> : null}
        </div>
      </li>)}</ol> : null}
    </CardContent>
  </Card>;
}

// Bỏ các từ chung như "Đại học", "THPT Chuyên" để chữ viết tắt ra "BK" thay vì "ĐH".
const SCHOOL_WORDS = /^(trường|đại|học|cao|đẳng|thpt|thcs|chuyên|university|of|college|-)$/i;
function schoolInitials(school: string) {
  return initials(school.split(/\s+/).filter(w => !SCHOOL_WORDS.test(w)).join(" ") || school);
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
  return <form onSubmit={handleSubmit(submit)} noValidate className="space-y-4">
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
