"use client";

import { useMemo, useState, type FormEvent } from "react";
import { AlertCircle, Pencil, Plus, Search, Sparkles, X } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { TaskBanner } from "@/app/components/ui/task-banner";
import { useSaveSkill, useSkillCatalogAdmin } from "@/hooks/use-admin";
import type { Skill } from "@/interface/skill";
import { getErrorMessage } from "@/services/api";
import { normalizeText } from "@/services/job-board.service";
import "./admin.css";

/**
 * Danh mục kỹ năng dùng chung cho JD, hồ sơ năng lực, độ khớp và đồng bộ LMS.
 * Mã không đổi sau khi tạo (các nơi khác tham chiếu theo id/mã) và không xoá kỹ năng; chỉ sửa tên, nhóm.
 */
export function AdminSkills() {
  const catalog = useSkillCatalogAdmin();
  const [group, setGroup] = useState<string | null>(null);
  const [q, setQ] = useState("");
  const [adding, setAdding] = useState(false);
  const [editing, setEditing] = useState<string | null>(null);

  const skills = useMemo(() => catalog.data ?? [], [catalog.data]);
  const groups = useMemo(() => {
    const counts = new Map<string, number>();
    for (const skill of skills) counts.set(skill.group, (counts.get(skill.group) ?? 0) + 1);
    return [...counts.entries()].sort((a, b) => a[0].localeCompare(b[0], "vi"));
  }, [skills]);
  const visible = skills.filter(skill => (!group || skill.group === group)
    && (!q.trim() || normalizeText(`${skill.name} ${skill.code}`).includes(normalizeText(q.trim()))));

  return <div className="adm">
    <TaskBanner eyebrow="Cấu hình hệ thống" title="Danh mục kỹ năng" illustration="applications" compact
      description="Kỹ năng dùng chung cho tin tuyển dụng, hồ sơ năng lực, tính độ khớp và đồng bộ điểm từ LMS.">
      <Button data-primary onClick={() => { setAdding(true); setEditing(null); }}><Plus aria-hidden="true" />Thêm kỹ năng</Button>
    </TaskBanner>

    {adding && <section className="adm-panel" aria-label="Thêm kỹ năng">
      <div className="adm-panel-head"><h2><Sparkles aria-hidden="true" />Thêm kỹ năng mới</h2>
        <button type="button" className="adm-btn" onClick={() => setAdding(false)} aria-label="Đóng"><X aria-hidden="true" /></button></div>
      <div className="adm-panel-body"><SkillForm groups={groups.map(([name]) => name)} onDone={() => setAdding(false)} /></div>
    </section>}

    <div className="adm-toolbar">
      <label className="adm-search"><Search aria-hidden="true" /><span className="sr-only">Tìm kỹ năng</span>
        <input type="search" value={q} onChange={event => setQ(event.target.value)} placeholder="Tìm theo tên hoặc mã kỹ năng" /></label>
      <span className="adm-muted">{visible.length} / {skills.length} kỹ năng · {groups.length} nhóm</span>
    </div>
    <div className="adm-chips" role="group" aria-label="Lọc theo nhóm">
      <button type="button" className="adm-chip" aria-pressed={!group} onClick={() => setGroup(null)}>Tất cả <small>{skills.length}</small></button>
      {groups.map(([name, count]) => <button key={name} type="button" className="adm-chip" aria-pressed={group === name} onClick={() => setGroup(name)}>{name} <small>{count}</small></button>)}
    </div>

    {catalog.error ? <div role="alert" className="adm-panel adm-panel-body"><p>{getErrorMessage(catalog.error)}</p><Button variant="outline" className="mt-3" onClick={() => catalog.refetch()}>Thử lại</Button></div>
      : catalog.isPending ? <div className="h-64 rounded-xl bg-muted motion-safe:animate-pulse" role="status" aria-label="Đang tải" />
      : <div className="adm-table-wrap">
        <table className="adm-table">
          <thead><tr><th>Mã</th><th>Tên kỹ năng</th><th>Nhóm</th><th><span className="sr-only">Thao tác</span></th></tr></thead>
          <tbody>{visible.map(skill => editing === skill.id
            ? <tr key={skill.id} data-open><td colSpan={4}><SkillForm skill={skill} groups={groups.map(([name]) => name)} onDone={() => setEditing(null)} /></td></tr>
            : <tr key={skill.id}>
              <td><span className="adm-code">{skill.code}</span></td>
              <td><strong className="text-[var(--heading)]">{skill.name}</strong></td>
              <td>{skill.group}</td>
              <td><div className="adm-actions"><button type="button" className="adm-btn" onClick={() => { setEditing(skill.id); setAdding(false); }}><Pencil aria-hidden="true" />Sửa</button></div></td>
            </tr>)}
            {visible.length === 0 && <tr><td colSpan={4} className="adm-muted py-8 text-center">Không có kỹ năng khớp bộ lọc.</td></tr>}
          </tbody>
        </table>
      </div>}
  </div>;
}

function SkillForm({ skill, groups, onDone }: { skill?: Skill; groups: string[]; onDone(): void }) {
  const save = useSaveSkill();
  const [code, setCode] = useState(skill?.code ?? "");
  const [name, setName] = useState(skill?.name ?? "");
  const [group, setGroup] = useState(skill?.group ?? "");
  const codeValid = /^[A-Za-z0-9_]{2,100}$/.test(code.trim());
  const valid = (skill || codeValid) && name.trim() && group.trim();
  const listId = `groups-${skill?.id ?? "new"}`;

  function submit(event: FormEvent) {
    event.preventDefault();
    if (!valid) return;
    save.mutate({ id: skill?.id, code: code.trim().toUpperCase(), name: name.trim(), group: group.trim() }, { onSuccess: onDone });
  }

  return <form className="adm-inline" onSubmit={submit}>
    <div className="adm-form-grid">
      <label className="adm-label">Mã kỹ năng
        <input className="adm-input font-mono" value={code} disabled={Boolean(skill)} maxLength={100} placeholder="VD: KOTLIN"
          onChange={event => setCode(event.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, ""))} aria-invalid={!skill && code !== "" && !codeValid || undefined} /></label>
      <label className="adm-label">Tên hiển thị<input className="adm-input" value={name} maxLength={255} placeholder="VD: Kotlin" onChange={event => setName(event.target.value)} autoFocus /></label>
      <label className="adm-label">Nhóm<input className="adm-input" list={listId} value={group} maxLength={100} placeholder="Chọn hoặc nhập nhóm mới" onChange={event => setGroup(event.target.value)} />
        <datalist id={listId}>{groups.map(name => <option key={name} value={name} />)}</datalist></label>
      <div className="flex gap-2">
        <button type="button" className="adm-btn" onClick={onDone} disabled={save.isPending}>Huỷ</button>
        <button type="submit" className="adm-btn" data-tone="primary" disabled={!valid || save.isPending}>{save.isPending ? "Đang lưu…" : skill ? "Lưu" : "Thêm"}</button>
      </div>
    </div>
    {skill ? <p className="adm-muted">Mã kỹ năng không đổi được vì tin tuyển dụng, điểm kỹ năng và dữ liệu đồng bộ đang tham chiếu tới.</p>
      : <p className="adm-muted">Mã viết hoa, chỉ gồm chữ, số và gạch dưới; không đổi được sau khi tạo.</p>}
    {save.isError && <p role="alert" className="adm-error flex items-center gap-1.5"><AlertCircle className="size-4" aria-hidden="true" />{getErrorMessage(save.error)}</p>}
  </form>;
}
