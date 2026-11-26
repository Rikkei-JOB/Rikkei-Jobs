"use client";

import * as React from "react";
import { useForm } from "react-hook-form";
import { Languages as LanguagesIcon, Pencil, Plus, Trash2 } from "lucide-react";
import { ADD_TILE, BADGE, BRAND_SOFT, ICON_ACTION, ICON_DANGER, ITEM_CARD, ITEM_TITLE, META, PRIMARY_CTA, SECONDARY_CTA, SECTION_TITLE, TONE } from "./display";
import { Card, CardContent, CardHeader } from "@/app/components/ui/card";
import { Button } from "@/app/components/ui/button";
import { Alert } from "@/app/components/ui/alert";
import { Input } from "@/app/components/ui/input";
import { FormDialog } from "@/app/components/profile/form-dialog";
import { FormField } from "@/app/components/auth/form-field";
import { zodResolver } from "@/helper/zod-resolver";
import { languageSchema, languagePayload, type LanguageValues } from "@/schemas/profile-qualifications.schema";
import { qualificationService } from "@/services/profile-qualifications.service";
import { getErrorMessage } from "@/services/api";
import { CEFR_LEVELS, type CefrLevel, type Language } from "@/interface/profile-qualifications";

/** Tên tiếng Việt của từng bậc CEFR. */
const CEFR_LABEL: Record<CefrLevel, string> = {
  A1: "Sơ cấp", A2: "Sơ trung cấp", B1: "Trung cấp", B2: "Trung cao cấp", C1: "Cao cấp", C2: "Thành thạo",
};
const SELECT_CLASS = "h-9 w-full min-w-0 rounded-md border border-input bg-input/20 px-2 py-1 text-sm outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/30 disabled:opacity-50";

export function Languages() {
  const [items, setItems] = React.useState<Language[] | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [reload, setReload] = React.useState(0);
  const [editing, setEditing] = React.useState<Language | null | undefined>(undefined);
  const [busy, setBusy] = React.useState<string | null>(null);
  React.useEffect(() => {
    let ignore = false;
    qualificationService.languages().then(data => { if (!ignore) { setItems(data); setError(null); } })
      .catch(err => { if (!ignore) setError(getErrorMessage(err, "Không thể tải ngoại ngữ.")); });
    return () => { ignore = true; };
  }, [reload]);
  async function remove(item: Language) {
    if (!window.confirm(`Xoá ngoại ngữ ${item.name}?`)) return;
    setBusy(item.id); setError(null);
    try { await qualificationService.deleteLanguage(item.id); setItems(old => old?.filter(e => e.id !== item.id) ?? []); }
    catch (err) { setError(getErrorMessage(err, "Không thể xoá ngoại ngữ.")); }
    finally { setBusy(null); }
  }
  return <Card>
    <CardHeader><div className="flex flex-wrap items-center justify-between gap-3">
      <h2 className={SECTION_TITLE}>Ngoại ngữ{items?.length ? <span className="font-normal text-[#64748B]"> · {items.length}</span> : null}</h2>
      <Button className={PRIMARY_CTA} onClick={() => setEditing(null)} disabled={items === null}><Plus aria-hidden />Thêm ngoại ngữ</Button></div></CardHeader>
    <CardContent className="space-y-4">
      {error ? <div className="space-y-2"><Alert variant="destructive">{error}</Alert>{items === null ? <Button onClick={() => { setError(null); setReload(v => v + 1); }}>Thử lại ngoại ngữ</Button> : null}</div> : null}
      {items === null && !error ? <p role="status">Đang tải ngoại ngữ...</p> : null}
      {editing !== undefined ? <FormDialog title={editing ? "Sửa ngoại ngữ" : "Thêm ngoại ngữ"} onClose={() => setEditing(undefined)}><LanguageForm key={editing?.id ?? "new"} item={editing} onCancel={() => setEditing(undefined)} onSaved={() => {
        setEditing(undefined); setReload(v => v + 1); // tải lại để giữ thứ tự theo trình độ
      }} /></FormDialog> : null}
      {items?.length === 0 ? <button type="button" onClick={() => setEditing(null)} className={ADD_TILE}>
        <LanguagesIcon className="size-6" aria-hidden /><span className="text-sm font-medium">Chưa có ngoại ngữ</span><span className="text-xs">Thêm ngoại ngữ và trình độ theo khung CEFR (A1–C2).</span>
      </button> : null}
      {items?.length ? <ul className="grid grid-cols-[repeat(auto-fill,minmax(min(100%,300px),1fr))] gap-4">{items.map(item =>
        <li key={item.id}
          onClick={e => { if (busy === null && !(e.target as HTMLElement).closest("button,a")) setEditing(item); }}
          className={`flex cursor-pointer items-start gap-3 p-4 ${ITEM_CARD} ${busy === item.id ? "opacity-50" : ""}`}>
          <div className={`flex size-11 shrink-0 items-center justify-center rounded-lg text-sm font-semibold ${BRAND_SOFT}`} aria-hidden>{item.level}</div>
          <div className="min-w-0 flex-1">
            <h3 className={ITEM_TITLE}>{item.name}</h3>
            <p className="mt-1"><span className={`${BADGE} ${TONE.neutral} whitespace-nowrap`}>{item.level} · {CEFR_LABEL[item.level]}</span></p>
            {item.note ? <p className={`mt-1 break-words ${META}`}>{item.note}</p> : null}
          </div>
          <div className="flex shrink-0">
            <Button variant="ghost" className={ICON_ACTION} aria-label={`Sửa ngoại ngữ ${item.name}`} disabled={busy !== null} onClick={() => setEditing(item)}><Pencil /></Button>
            <Button variant="ghost" className={ICON_DANGER} aria-label={`Xoá ngoại ngữ ${item.name}`} disabled={busy !== null} onClick={() => void remove(item)}><Trash2 /></Button>
          </div>
        </li>)}</ul> : null}
    </CardContent>
  </Card>;
}

function LanguageForm({ item, onCancel, onSaved }: { item: Language | null; onCancel: () => void; onSaved: (item: Language) => void }) {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LanguageValues>({
    resolver: zodResolver(languageSchema), defaultValues: { name: item?.name ?? "", level: item?.level ?? "", note: item?.note ?? "" },
  });
  const [error, setError] = React.useState<string | null>(null);
  async function submit(values: LanguageValues) {
    setError(null);
    try { onSaved(await qualificationService.saveLanguage(item?.id ?? null, languagePayload(values))); }
    catch (err) { setError(getErrorMessage(err, "Không thể lưu ngoại ngữ.")); }
  }
  return <form onSubmit={handleSubmit(submit)} noValidate className="space-y-4">
    <fieldset disabled={isSubmitting} className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Ngoại ngữ" htmlFor="language-name" error={errors.name?.message}><Input id="language-name" required maxLength={100} placeholder="Tiếng Anh" {...register("name")} /></FormField>
        <FormField label="Trình độ (CEFR)" htmlFor="language-level" error={errors.level?.message}>
          <select id="language-level" required className={SELECT_CLASS} aria-invalid={Boolean(errors.level)} {...register("level")}>
            <option value="">Chọn trình độ</option>
            {CEFR_LEVELS.map(level => <option key={level} value={level}>{level} · {CEFR_LABEL[level]}</option>)}
          </select>
        </FormField>
      </div>
      <FormField label="Ghi chú" htmlFor="language-note" error={errors.note?.message}><Input id="language-note" maxLength={255} placeholder="IELTS 7.0, JLPT N2..." {...register("note")} /></FormField>
      {error ? <Alert variant="destructive">{error}</Alert> : null}
      <div className="flex gap-2"><Button type="submit" className={PRIMARY_CTA}>{isSubmitting ? "Đang lưu..." : "Lưu ngoại ngữ"}</Button><Button type="button" variant="outline" className={SECONDARY_CTA} onClick={onCancel}>Huỷ</Button></div>
    </fieldset>
  </form>;
}
