"use client";

import * as React from "react";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { Alert } from "@/app/components/ui/alert";
import { Button } from "@/app/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/app/components/ui/card";
import { Input } from "@/app/components/ui/input";
import { Label } from "@/app/components/ui/label";
import { useAdminSkills, useCreateSkill, useDeleteSkill, useUpdateSkill } from "@/hooks/use-admin";
import type { AdminSkill } from "@/interface/admin";
import { cn } from "cn";
import { errorText, fieldError } from "./admin-format";

/** Tab Kỹ năng: thêm, sửa tên/nhóm, bật/tắt, xoá (kỹ năng đã dùng chỉ bị tắt). */
export function SkillsTab() {
  const { data: skills, error, isPending } = useAdminSkills();
  const [query, setQuery] = React.useState("");
  const [editing, setEditing] = React.useState<string | null>(null);
  const [notice, setNotice] = React.useState<string | null>(null);

  const keyword = query.trim().toLowerCase();
  const shown = (skills ?? []).filter(
    (s) => !keyword || [s.code, s.name, s.group].some((v) => v.toLowerCase().includes(keyword)),
  );

  return (
    <div className="space-y-4">
      <CreateSkillForm onCreated={(skill) => setNotice(`Đã thêm kỹ năng ${skill.name}. Kỹ năng mới hiện ngay ở form JD và hồ sơ.`)} />

      <Card>
        <CardHeader>
          <CardTitle>Danh mục kỹ năng</CardTitle>
          <CardDescription>
            Kỹ năng đã có bằng chứng hoặc ánh xạ nguồn không xoá được, chỉ tắt: không chọn mới được nhưng dữ liệu cũ giữ nguyên.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {notice ? <Alert variant="success">{notice}</Alert> : null}
          {error ? <Alert variant="destructive">{errorText(error)}</Alert> : null}
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Tìm theo mã, tên hoặc nhóm"
            aria-label="Tìm kỹ năng"
            className="max-w-xs"
          />
          {isPending ? <p className="text-xs text-muted-foreground">Đang tải...</p> : null}
          <div className="overflow-x-auto">
            <table className="w-full min-w-[640px] text-left text-xs">
              <thead className="border-b border-border text-muted-foreground">
                <tr>
                  <th className="py-2 pr-3 font-medium">Mã</th>
                  <th className="py-2 pr-3 font-medium">Tên</th>
                  <th className="py-2 pr-3 font-medium">Nhóm</th>
                  <th className="py-2 pr-3 font-medium">Trạng thái</th>
                  <th className="py-2 font-medium" />
                </tr>
              </thead>
              <tbody>
                {shown.map((skill) => (
                  <React.Fragment key={skill.id}>
                    <tr className={cn("border-b border-border/60 align-top", !skill.active && "text-muted-foreground")}>
                      <td className="py-2 pr-3 font-mono">{skill.code}</td>
                      <td className="py-2 pr-3">{skill.name}</td>
                      <td className="py-2 pr-3">{skill.group}</td>
                      <td className="py-2 pr-3 whitespace-nowrap">
                        {skill.active ? "Đang dùng" : "Đã tắt"}
                        {skill.inUse ? <span className="ml-1 text-muted-foreground">· có dữ liệu</span> : null}
                      </td>
                      <td className="py-2 text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          aria-expanded={editing === skill.id}
                          onClick={() => setEditing(editing === skill.id ? null : skill.id)}
                        >
                          <Pencil />
                          Sửa
                        </Button>
                      </td>
                    </tr>
                    {editing === skill.id ? (
                      <tr className="border-b border-border/60 bg-muted/30">
                        <td colSpan={5} className="p-3">
                          <EditSkillForm
                            skill={skill}
                            onDone={(message) => {
                              setEditing(null);
                              setNotice(message);
                            }}
                          />
                        </td>
                      </tr>
                    ) : null}
                  </React.Fragment>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

function CreateSkillForm({ onCreated }: { onCreated: (skill: AdminSkill) => void }) {
  const create = useCreateSkill();
  const [form, setForm] = React.useState({ code: "", name: "", group: "", reason: "" });
  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((prev) => ({ ...prev, [key]: e.target.value }));

  function submit(e: React.FormEvent) {
    e.preventDefault();
    create.mutate(
      { code: form.code.trim(), name: form.name.trim(), group: form.group.trim(), reason: form.reason.trim() },
      {
        onSuccess: (skill) => {
          setForm({ code: "", name: "", group: "", reason: "" });
          onCreated(skill);
        },
      },
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Thêm kỹ năng</CardTitle>
        <CardDescription>Mã viết hoa không dấu (ví dụ KOTLIN), không đổi được sau khi tạo.</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={submit} className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <Field label="Mã" id="skill-code" error={fieldError(create.error, "code")}>
            <Input id="skill-code" value={form.code} onChange={set("code")} placeholder="KOTLIN" required />
          </Field>
          <Field label="Tên" id="skill-name" error={fieldError(create.error, "name")}>
            <Input id="skill-name" value={form.name} onChange={set("name")} placeholder="Kotlin" required />
          </Field>
          <Field label="Nhóm" id="skill-group" error={fieldError(create.error, "group")}>
            <Input id="skill-group" value={form.group} onChange={set("group")} placeholder="Backend" required />
          </Field>
          <Field label="Lý do (không bắt buộc)" id="skill-reason">
            <Input id="skill-reason" value={form.reason} onChange={set("reason")} maxLength={500} />
          </Field>
          <div className="flex items-center gap-3 sm:col-span-2 lg:col-span-4">
            <Button type="submit" size="sm" disabled={create.isPending}>
              <Plus />
              {create.isPending ? "Đang thêm..." : "Thêm kỹ năng"}
            </Button>
            {create.error && !fieldError(create.error, "code") && !fieldError(create.error, "name") ? (
              <span role="alert" className="text-xs text-destructive">{errorText(create.error)}</span>
            ) : null}
          </div>
        </form>
      </CardContent>
    </Card>
  );
}

function EditSkillForm({ skill, onDone }: { skill: AdminSkill; onDone: (message: string) => void }) {
  const update = useUpdateSkill();
  const remove = useDeleteSkill();
  const [form, setForm] = React.useState({ name: skill.name, group: skill.group, active: skill.active, reason: "" });
  const busy = update.isPending || remove.isPending;

  function save(e: React.FormEvent) {
    e.preventDefault();
    update.mutate(
      { id: skill.id, payload: { name: form.name.trim(), group: form.group.trim(), active: form.active, reason: form.reason.trim() } },
      { onSuccess: (saved) => onDone(`Đã lưu kỹ năng ${saved.name}.`) },
    );
  }

  function del() {
    const question = skill.inUse
      ? `Kỹ năng ${skill.name} đã có dữ liệu nên sẽ chỉ bị tắt. Tiếp tục?`
      : `Xoá hẳn kỹ năng ${skill.name}?`;
    if (!window.confirm(question)) return;
    remove.mutate({ id: skill.id, reason: form.reason.trim() }, { onSuccess: (result) => onDone(result.message) });
  }

  return (
    <form onSubmit={save} className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
      <Field label="Tên" id={`name-${skill.id}`} error={fieldError(update.error, "name")}>
        <Input id={`name-${skill.id}`} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
      </Field>
      <Field label="Nhóm" id={`group-${skill.id}`} error={fieldError(update.error, "group")}>
        <Input id={`group-${skill.id}`} value={form.group} onChange={(e) => setForm({ ...form, group: e.target.value })} required />
      </Field>
      <Field label="Lý do" id={`reason-${skill.id}`}>
        <Input id={`reason-${skill.id}`} value={form.reason} maxLength={500}
          onChange={(e) => setForm({ ...form, reason: e.target.value })} />
      </Field>
      <label className="flex items-center gap-2 self-end pb-1.5 text-xs">
        <input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
        Đang dùng
      </label>
      <div className="flex flex-wrap items-center gap-2 sm:col-span-2 lg:col-span-4">
        <Button type="submit" size="sm" disabled={busy}>{update.isPending ? "Đang lưu..." : "Lưu"}</Button>
        <Button type="button" variant="destructive" size="sm" disabled={busy} onClick={del}>
          <Trash2 />
          {skill.inUse ? "Tắt (đã có dữ liệu)" : "Xoá"}
        </Button>
        {update.error || remove.error ? (
          <span role="alert" className="text-xs text-destructive">{errorText(update.error ?? remove.error)}</span>
        ) : null}
      </div>
    </form>
  );
}

export function Field({ label, id, error, children }: { label: string; id: string; error?: string; children: React.ReactNode }) {
  return (
    <div className="space-y-1">
      <Label htmlFor={id}>{label}</Label>
      {children}
      {error ? <p className="text-[11px] text-destructive">{error}</p> : null}
    </div>
  );
}
