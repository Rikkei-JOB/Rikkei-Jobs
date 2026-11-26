"use client";

import { useState, type FormEvent } from "react";
import { Alert } from "@/app/components/ui/alert";
import { Button } from "@/app/components/ui/button";
import { Input } from "@/app/components/ui/input";
import { useSkillCatalog } from "@/hooks/use-skill-catalog";
import { ApiError, getErrorMessage } from "@/services/api";
import { profileReviewService, type ReviewPreview, type ReviewEdit } from "@/services/profile-review.service";
import { SelectMenu } from "@/app/components/ui/select-menu";

interface EditorProps {
  preview: ReviewPreview;
  disabled: boolean;
  onBusy: (busy: boolean) => void;
  onStale: () => void;
  onSaved: () => Promise<unknown>;
}
const fields = { headline: "Tiêu đề", summary: "Giới thiệu", selfSkills: "Kỹ năng tự khai" };

export function ReviewEditor(props: EditorProps) {
  const [field, setField] = useState<ReviewEdit["field"]>("headline");
  const [saving, setSaving] = useState(false);
  function busy(value: boolean) {
    setSaving(value);
    props.onBusy(value);
  }
  return <section className="space-y-3 rounded-lg border bg-card p-4">
    <h2 className="font-semibold">Chỉnh sửa thông tin tự khai</h2>
    <label className="block space-y-1 text-sm">Trường cần chỉnh sửa
      <SelectMenu aria-label="Trường cần chỉnh sửa" value={field} disabled={saving || props.disabled}
        onValueChange={value => setField(value as ReviewEdit["field"])}
        options={Object.entries(fields).map(([key, title]) => ({ value: key, label: title }))} />
    </label>
    <EditForm key={field} {...props} field={field} disabled={props.disabled || saving} onBusy={busy} />
  </section>;
}

function EditForm({ preview, disabled, onBusy, onSaved, onStale, field }: EditorProps & { field: ReviewEdit["field"] }) {
  const [text, setText] = useState(field === "selfSkills" ? "" : preview[field] ?? "");
  const [rows, setRows] = useState(() => preview.skills.flatMap(({ skill, selfScore }) =>
    selfScore === null ? [] : [{ skillId: skill.id, score: String(selfScore) }]));
  const [reason, setReason] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const catalog = useSkillCatalog(field === "selfSkills");
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (disabled || !reason.trim()) return;
    onBusy(true);
    setError("");
    setSuccess(false);
    try {
      const change: ReviewEdit = field === "selfSkills"
        ? { field, value: rows.map(row => ({ skillId: row.skillId, selfScore: Number(row.score) })), reason: reason.trim() }
        : { field, value: text.trim(), reason: reason.trim() };
      await profileReviewService.edit(preview.userId, change);
      await onSaved();
      setReason("");
      setSuccess(true);
    } catch (cause) {
      if (cause instanceof ApiError && [404, 409].includes(cause.status)) onStale();
      else setError(getErrorMessage(cause));
    } finally { onBusy(false); }
  }
  return <form onSubmit={submit} onChange={() => { setSuccess(false); setError(""); }}>
    <fieldset disabled={disabled} className="space-y-3">
      {field !== "selfSkills" ?
        <label className="block space-y-1 text-sm">Giá trị mới
          <textarea className="block w-full rounded-md border p-2" rows={field === "summary" ? 4 : 1}
            value={text} maxLength={field === "summary" ? 5000 : 255} onChange={event => setText(event.target.value)} />
        </label> : catalog.isError ?
          <Alert variant="destructive">{getErrorMessage(catalog.error)} <Button type="button" onClick={() => catalog.refetch()}>Thử lại</Button></Alert>
          : catalog.isPending ? <p role="status">Đang tải danh mục kỹ năng...</p> : <>
            {rows.map((row, index) => <div key={index} className="grid items-end gap-2 sm:grid-cols-[1fr_8rem_auto]">
              <label className="space-y-1 text-sm">Kỹ năng {index + 1}
                <SelectMenu aria-label={`Kỹ năng ${index + 1}`} required value={row.skillId} placeholder="Chọn kỹ năng"
                  onValueChange={value => setRows(rows.map((item, at) => at === index ? { ...item, skillId: value } : item))}
                  options={catalog.data.map(skill => ({ value: skill.id, label: skill.name,
                    disabled: rows.some((item, at) => at !== index && item.skillId === skill.id) }))} />
              </label>
              <label className="space-y-1 text-sm">Điểm tự khai {index + 1}
                <Input type="number" required min={0} max={100} step="any" value={row.score}
                  onChange={event => setRows(rows.map((item, at) => at === index ? { ...item, score: event.target.value } : item))} />
              </label>
              <Button type="button" variant="outline" aria-label={`Bỏ kỹ năng ${index + 1}`}
                onClick={() => { setRows(rows.filter((_, at) => at !== index)); setSuccess(false); }}>Bỏ</Button>
            </div>)}
            <Button type="button" variant="outline" disabled={rows.length >= catalog.data.length}
              onClick={() => { setRows([...rows, { skillId: "", score: "" }]); setSuccess(false); }}>Thêm kỹ năng tự khai</Button>
          </>}
      <label className="block space-y-1 text-sm">Lý do chỉnh sửa
        <Input value={reason} required onChange={event => setReason(event.target.value)} />
      </label>
      <Button type="submit" disabled={disabled || !reason.trim() || (field === "selfSkills" && !catalog.isSuccess)}>Lưu chỉnh sửa</Button>
      {error && <Alert variant="destructive">{error}</Alert>}
      {success && <Alert variant="success">Đã lưu chỉnh sửa.</Alert>}
    </fieldset>
  </form>;
}
