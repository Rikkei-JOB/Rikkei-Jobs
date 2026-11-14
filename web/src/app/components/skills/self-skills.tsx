"use client";

import * as React from "react";
import { Card, CardContent, CardHeader } from "@/app/components/ui/card";
import { Button } from "@/app/components/ui/button";
import { Alert } from "@/app/components/ui/alert";
import { Input } from "@/app/components/ui/input";
import { FormField } from "@/app/components/auth/form-field";
import { skillService } from "@/services/skill.service";
import { getErrorMessage } from "@/services/api";
import type { Skill, SkillProfileItem } from "@/interface/skill";

type Row = { skillId: string; score: string };
type RowError = { skillId?: string; score?: string };
const SELECT_CLASS = "h-7 w-full min-w-0 rounded-md border border-input bg-input/20 px-2 py-1 text-sm outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/30 disabled:opacity-50 md:text-xs/relaxed";
const toRows = (skills: SkillProfileItem[]): Row[] => skills.filter(item => item.selfScore !== null)
  .map(item => ({ skillId: item.skill.id, score: String(item.selfScore) }));

export function SelfSkills({ onSaved }: { onSaved: () => void }) {
  const [catalog, setCatalog] = React.useState<Skill[] | null>(null);
  const [rows, setRows] = React.useState<Row[]>([]);
  const [errors, setErrors] = React.useState<RowError[]>([]);
  const [loadError, setLoadError] = React.useState<string | null>(null);
  const [saveError, setSaveError] = React.useState<string | null>(null);
  const [saving, setSaving] = React.useState(false);
  const [saved, setSaved] = React.useState(false);
  const [reload, setReload] = React.useState(0);
  React.useEffect(() => {
    let ignore = false;
    Promise.all([skillService.getCatalog(), skillService.getOwnSkills()]).then(([skills, own]) => {
      if (!ignore) { setCatalog(skills); setRows(toRows(own)); setLoadError(null); }
    }).catch(error => { if (!ignore) setLoadError(getErrorMessage(error, "Không thể tải kỹ năng tự đánh giá.")); });
    return () => { ignore = true; };
  }, [reload]);

  function change(next: Row[]) { setRows(next); setErrors([]); setSaveError(null); setSaved(false); }
  async function submit(event: React.FormEvent) {
    event.preventDefault();
    const selected = new Set<string>();
    const nextErrors = rows.map(row => {
      const result: RowError = {};
      if (!row.skillId || !catalog?.some(skill => skill.id === row.skillId)) result.skillId = "Vui lòng chọn kỹ năng";
      else if (selected.has(row.skillId)) result.skillId = "Mỗi kỹ năng chỉ được chọn một lần";
      selected.add(row.skillId);
      const score = Number(row.score);
      const [coefficient, exponent = "0"] = String(score).split("e");
      const decimals = Math.max(0, (coefficient.split(".")[1]?.length ?? 0) - Number(exponent));
      if (row.score.trim() === "") result.score = "Vui lòng nhập điểm";
      else if (!Number.isFinite(score) || score < 0 || score > 100) result.score = "Điểm phải từ 0 đến 100";
      else if (decimals > 6) result.score = "Điểm có tối đa 6 chữ số thập phân";
      return result;
    });
    setErrors(nextErrors); setSaved(false); setSaveError(null);
    if (nextErrors.some(error => error.skillId || error.score)) return;
    setSaving(true);
    try {
      const updated = await skillService.saveSelfSkills(rows.map(row => ({ skillId: row.skillId, selfScore: Number(row.score) })));
      setRows(toRows(updated)); setSaved(true); onSaved();
    } catch (error) { setSaveError(getErrorMessage(error, "Không thể lưu kỹ năng tự khai.")); }
    finally { setSaving(false); }
  }

  return <Card>
    <CardHeader>
      <h2 className="text-lg font-semibold">Kỹ năng tự đánh giá</h2>
      <p className="text-xs text-muted-foreground">Điểm tự khai từ 0–100, không làm thay đổi điểm xác minh từ LMS hoặc các nguồn khác.</p>
    </CardHeader>
    <CardContent className="space-y-4">
      {loadError ? <div className="space-y-2"><Alert variant="destructive">{loadError}</Alert>
        <Button onClick={() => { setLoadError(null); setReload(value => value + 1); }}>Thử lại kỹ năng</Button></div>
        : catalog === null ? <p role="status">Đang tải kỹ năng tự đánh giá...</p>
        : <form noValidate onSubmit={submit} className="space-y-4">
          <fieldset disabled={saving} className="space-y-4">
            {catalog.length === 0 ? <p className="text-sm text-muted-foreground">Chưa có danh mục kỹ năng.</p>
              : rows.length === 0 ? <p className="text-sm text-muted-foreground">Chưa chọn kỹ năng tự khai. Lưu danh sách trống để bỏ toàn bộ kỹ năng tự khai.</p> : null}
            {rows.map((row, index) => <div key={index} className="grid gap-3 rounded-lg border p-3 sm:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_auto] sm:items-start">
              <FormField label={`Kỹ năng ${index + 1}`} htmlFor={`self-skill-${index}`} error={errors[index]?.skillId}>
                <select id={`self-skill-${index}`} className={SELECT_CLASS} value={row.skillId} aria-invalid={Boolean(errors[index]?.skillId)}
                  aria-describedby={errors[index]?.skillId ? `self-skill-${index}-error` : undefined}
                  onChange={event => change(rows.map((item, at) => at === index ? { ...item, skillId: event.target.value } : item))}>
                  <option value="">Chọn kỹ năng</option>
                  {catalog.map(skill => <option key={skill.id} value={skill.id} disabled={rows.some((item, at) => at !== index && item.skillId === skill.id)}>{skill.name} · {skill.group}</option>)}
                </select>
              </FormField>
              <FormField label={`Điểm tự đánh giá ${index + 1}`} htmlFor={`self-score-${index}`} error={errors[index]?.score}>
                <Input id={`self-score-${index}`} type="number" min={0} max={100} step={1} value={row.score} aria-invalid={Boolean(errors[index]?.score)}
                  aria-describedby={errors[index]?.score ? `self-score-${index}-error` : undefined}
                  onChange={event => change(rows.map((item, at) => at === index ? { ...item, score: event.target.value } : item))} />
              </FormField>
              <Button type="button" variant="outline" className="sm:mt-6" aria-label={`Bỏ kỹ năng ${index + 1}`} onClick={() => change(rows.filter((_, at) => at !== index))}>Bỏ</Button>
            </div>)}
            <div className="flex flex-wrap gap-2">
              <Button type="button" variant="outline" disabled={catalog.length === 0 || rows.length >= catalog.length} onClick={() => change([...rows, { skillId: "", score: "" }])}>Thêm kỹ năng tự khai</Button>
              <Button type="submit" disabled={catalog.length === 0}>{saving ? "Đang lưu..." : "Lưu kỹ năng tự khai"}</Button>
            </div>
          </fieldset>
          {saveError ? <Alert variant="destructive">{saveError}</Alert> : null}
          {saved ? <Alert variant="success">Đã lưu kỹ năng tự khai</Alert> : null}
        </form>}
    </CardContent>
  </Card>;
}
