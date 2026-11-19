"use client";

import * as React from "react";
import { ArrowDown, ArrowUp, Pencil, Plus, Trash2, X } from "lucide-react";
import { Alert } from "@/app/components/ui/alert";
import { Button } from "@/app/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/app/components/ui/card";
import { Input } from "@/app/components/ui/input";
import { useDeleteTemplate, useSaveTemplate, useTemplates } from "@/hooks/use-admin";
import type { EvaluationTemplate } from "@/interface/admin";
import { Field } from "./skills-tab";
import { errorText, formatDateTime, textareaClass } from "./admin-format";

interface CriterionRow {
  key: number;
  name: string;
  description: string;
  weight: string;
  maxScore: string;
}

let rowKey = 0;
const blankRow = (): CriterionRow => ({ key: ++rowKey, name: "", description: "", weight: "", maxScore: "10" });

/** Tab Mẫu phiếu: mẫu phiếu đánh giá kèm tiêu chí (DG-01 dùng). */
export function TemplatesTab() {
  const { data: templates, error, isPending } = useTemplates();
  const [editing, setEditing] = React.useState<EvaluationTemplate | "new" | null>(null);
  const [notice, setNotice] = React.useState<string | null>(null);

  return (
    <div className="space-y-4">
      {notice ? <Alert variant="success">{notice}</Alert> : null}
      {editing ? (
        <TemplateForm
          key={editing === "new" ? "new" : editing.id}
          template={editing === "new" ? null : editing}
          onClose={(message) => {
            setEditing(null);
            if (message) setNotice(message);
          }}
        />
      ) : (
        <Button size="sm" onClick={() => { setNotice(null); setEditing("new"); }}>
          <Plus />
          Thêm mẫu phiếu
        </Button>
      )}

      {error ? <Alert variant="destructive">{errorText(error)}</Alert> : null}
      {isPending ? <p className="text-xs text-muted-foreground">Đang tải...</p> : null}
      {templates && templates.length === 0 ? <p className="text-xs text-muted-foreground">Chưa có mẫu phiếu.</p> : null}

      <div className="grid gap-4 lg:grid-cols-2">
        {templates?.map((template) => (
          <Card key={template.id}>
            <CardHeader>
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0">
                  <CardTitle>{template.name}</CardTitle>
                  <CardDescription>
                    {template.active ? "Đang dùng" : "Đã tắt"} · cập nhật {formatDateTime(template.updatedAt)}
                  </CardDescription>
                </div>
                <Button variant="ghost" size="sm" onClick={() => { setNotice(null); setEditing(template); }}>
                  <Pencil />
                  Sửa
                </Button>
              </div>
            </CardHeader>
            <CardContent className="space-y-2">
              {template.description ? <p className="text-xs text-muted-foreground">{template.description}</p> : null}
              <ol className="space-y-1 text-xs">
                {template.criteria.map((c) => (
                  <li key={c.id} className="flex justify-between gap-2 border-b border-border/60 py-1">
                    <span>{c.name}</span>
                    <span className="whitespace-nowrap text-muted-foreground">{c.weight}% · thang {c.maxScore}</span>
                  </li>
                ))}
              </ol>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}

function TemplateForm({ template, onClose }: { template: EvaluationTemplate | null; onClose: (message?: string) => void }) {
  const save = useSaveTemplate();
  const remove = useDeleteTemplate();
  const [name, setName] = React.useState(template?.name ?? "");
  const [description, setDescription] = React.useState(template?.description ?? "");
  const [active, setActive] = React.useState(template?.active ?? true);
  const [reason, setReason] = React.useState("");
  const [rows, setRows] = React.useState<CriterionRow[]>(
    template
      ? template.criteria.map((c) => ({ key: ++rowKey, name: c.name, description: c.description ?? "",
          weight: String(c.weight), maxScore: String(c.maxScore) }))
      : [blankRow()],
  );
  const busy = save.isPending || remove.isPending;
  const totalWeight = rows.reduce((sum, r) => sum + (Number(r.weight) || 0), 0);

  const update = (key: number, patch: Partial<CriterionRow>) =>
    setRows((prev) => prev.map((r) => (r.key === key ? { ...r, ...patch } : r)));
  const move = (index: number, delta: number) =>
    setRows((prev) => {
      const next = [...prev];
      const [row] = next.splice(index, 1);
      next.splice(index + delta, 0, row);
      return next;
    });

  function submit(e: React.FormEvent) {
    e.preventDefault();
    save.mutate(
      {
        id: template?.id ?? null,
        payload: {
          name: name.trim(),
          description: description.trim() || null,
          active,
          reason: reason.trim(),
          criteria: rows.map((r) => ({ name: r.name.trim(), description: r.description.trim() || null,
            weight: Number(r.weight), maxScore: Number(r.maxScore) })),
        },
      },
      { onSuccess: (saved) => onClose(`Đã lưu mẫu phiếu ${saved.name}.`) },
    );
  }

  function del() {
    if (!template || !window.confirm(`Xoá mẫu phiếu ${template.name}?`)) return;
    remove.mutate({ id: template.id, reason: reason.trim() }, { onSuccess: () => onClose(`Đã xoá mẫu phiếu ${template.name}.`) });
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between gap-2">
          <CardTitle>{template ? `Sửa mẫu phiếu: ${template.name}` : "Thêm mẫu phiếu"}</CardTitle>
          <Button variant="ghost" size="sm" onClick={() => onClose()} aria-label="Đóng">
            <X />
          </Button>
        </div>
        <CardDescription>Lưu sẽ thay toàn bộ danh sách tiêu chí. Phiếu đã chấm giữ tiêu chí lúc chấm.</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={submit} className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Tên mẫu" id="template-name">
              <Input id="template-name" value={name} onChange={(e) => setName(e.target.value)} required maxLength={255} />
            </Field>
            <label className="flex items-center gap-2 self-end pb-1.5 text-xs">
              <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
              Đang dùng (mentor chọn được)
            </label>
            <div className="space-y-1 sm:col-span-2">
              <label htmlFor="template-description" className="text-xs/relaxed font-medium">Mô tả</label>
              <textarea id="template-description" className={textareaClass} rows={2} value={description}
                maxLength={2000} onChange={(e) => setDescription(e.target.value)} />
            </div>
          </div>

          <fieldset className="space-y-2">
            <legend className="text-xs font-medium">
              Tiêu chí <span className="font-normal text-muted-foreground">(tổng trọng số hiện tại {Number(totalWeight.toFixed(2))}%)</span>
            </legend>
            {rows.map((row, index) => (
              <div key={row.key} className="grid gap-2 rounded-md border border-border/60 p-2 sm:grid-cols-[1fr_6rem_6rem_auto]">
                <Input aria-label={`Tên tiêu chí ${index + 1}`} placeholder="Tên tiêu chí" value={row.name}
                  onChange={(e) => update(row.key, { name: e.target.value })} required maxLength={255} />
                <Input aria-label={`Trọng số tiêu chí ${index + 1}`} type="number" min={0.01} max={100} step="0.01"
                  placeholder="Trọng số %" value={row.weight} onChange={(e) => update(row.key, { weight: e.target.value })} required />
                <Input aria-label={`Thang điểm tiêu chí ${index + 1}`} type="number" min={1} max={100} step={1}
                  placeholder="Thang" value={row.maxScore} onChange={(e) => update(row.key, { maxScore: e.target.value })} required />
                <div className="flex gap-1">
                  <Button type="button" variant="ghost" size="icon" aria-label="Lên" disabled={index === 0} onClick={() => move(index, -1)}>
                    <ArrowUp />
                  </Button>
                  <Button type="button" variant="ghost" size="icon" aria-label="Xuống" disabled={index === rows.length - 1}
                    onClick={() => move(index, 1)}>
                    <ArrowDown />
                  </Button>
                  <Button type="button" variant="ghost" size="icon" aria-label="Bỏ tiêu chí" disabled={rows.length === 1}
                    onClick={() => setRows((prev) => prev.filter((r) => r.key !== row.key))}>
                    <Trash2 />
                  </Button>
                </div>
                <Input aria-label={`Mô tả tiêu chí ${index + 1}`} placeholder="Mô tả (không bắt buộc)" value={row.description}
                  onChange={(e) => update(row.key, { description: e.target.value })} className="sm:col-span-4" maxLength={2000} />
              </div>
            ))}
            <Button type="button" variant="outline" size="sm" disabled={rows.length >= 50} onClick={() => setRows((prev) => [...prev, blankRow()])}>
              <Plus />
              Thêm tiêu chí
            </Button>
          </fieldset>

          <Field label="Lý do (không bắt buộc)" id="template-reason">
            <Input id="template-reason" value={reason} maxLength={500} onChange={(e) => setReason(e.target.value)} />
          </Field>
          {save.error || remove.error ? <Alert variant="destructive">{errorText(save.error ?? remove.error)}</Alert> : null}
          <div className="flex flex-wrap gap-2">
            <Button type="submit" size="sm" disabled={busy}>{save.isPending ? "Đang lưu..." : "Lưu mẫu phiếu"}</Button>
            {template ? (
              <Button type="button" variant="destructive" size="sm" disabled={busy} onClick={del}>
                <Trash2 />
                Xoá
              </Button>
            ) : null}
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
