"use client";

import * as React from "react";
import Link from "next/link";
import { ArrowDown, ArrowUp, Download, ExternalLink, FileText, Plus, RefreshCw, Save, Trash2 } from "lucide-react";
import { Card, CardContent, CardHeader } from "@/app/components/ui/card";
import { Button } from "@/app/components/ui/button";
import { Alert } from "@/app/components/ui/alert";
import { Input } from "@/app/components/ui/input";
import { Label } from "@/app/components/ui/label";
import { FormDialog } from "@/app/components/profile/form-dialog";
import { ADD_TILE, BADGE, ICON_ACTION, ICON_DANGER, ITEM_CARD, META, PRIMARY_CTA, SECONDARY_CTA, SECTION_TITLE, TONE } from "@/app/components/profile/display";
import { cvService } from "@/services/cv.service";
import { ApiError, getErrorMessage } from "@/services/api";
import type { CvDetail, CvSection, CvSectionPayload, CvSummary } from "@/interface/cv";

const MAX_CVS = 5;

/** Bản nháp phần CV đang chỉnh; itemIds = null là lấy mọi mục, kể cả mục thêm sau này. */
type Draft = CvSectionPayload & { label: string };

function toDraft(cv: CvDetail): Draft[] {
  return cv.sections.map(s => ({ section: s.section, label: s.label, included: s.included,
    itemIds: s.allItems ? null : s.items.filter(i => i.selected).map(i => i.id) }));
}

export function CvBuilder() {
  const [cvs, setCvs] = React.useState<CvSummary[] | null>(null);
  const [selectedId, setSelectedId] = React.useState<string | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [creating, setCreating] = React.useState(false);
  const [reload, setReload] = React.useState(0);

  React.useEffect(() => {
    let ignore = false;
    cvService.list().then(list => {
      if (ignore) return;
      setCvs(list); setError(null);
      setSelectedId(old => old && list.some(c => c.id === old) ? old : list[0]?.id ?? null);
    }).catch(err => { if (!ignore) setError(getErrorMessage(err, "Không thể tải danh sách CV.")); });
    return () => { ignore = true; };
  }, [reload]);

  const full = (cvs?.length ?? 0) >= MAX_CVS;
  return <div className="space-y-6">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 className="text-xl font-semibold">CV của tôi</h1>
        <p className={META}>Mỗi CV lấy dữ liệu trực tiếp từ <Link href="/candidate/profile" className="underline">hồ sơ</Link>; sửa hồ sơ thì CV đổi theo. Tối đa {MAX_CVS} CV.</p>
      </div>
      <Button className={PRIMARY_CTA} onClick={() => setCreating(true)} disabled={cvs === null || full}
        title={full ? `Đã đủ ${MAX_CVS} CV, xoá bớt để tạo mới` : undefined}><Plus aria-hidden />Tạo CV mới</Button>
    </div>
    {error ? <div className="space-y-2"><Alert variant="destructive">{error}</Alert>
      {cvs === null ? <Button onClick={() => { setError(null); setReload(v => v + 1); }}>Thử lại</Button> : null}</div> : null}
    {cvs === null && !error ? <p role="status">Đang tải CV...</p> : null}
    {creating ? <FormDialog title="Tạo CV mới" onClose={() => setCreating(false)}>
      <CvForm submitLabel="Tạo CV" onCancel={() => setCreating(false)} onSubmit={async body => {
        const cv = await cvService.create(body);
        setCreating(false); setSelectedId(cv.id); setReload(v => v + 1);
      }} />
    </FormDialog> : null}
    {cvs?.length === 0 ? <button type="button" onClick={() => setCreating(true)} className={ADD_TILE}>
      <FileText className="size-6" aria-hidden /><span className="text-sm font-medium">Chưa có CV nào</span>
      <span className="text-xs">Tạo CV theo vị trí bạn muốn ứng tuyển, ví dụ &quot;Java Developer&quot;.</span>
    </button> : null}
    {cvs?.length ? <div role="tablist" aria-label="Danh sách CV" className="flex flex-wrap gap-2">
      {cvs.map(cv => <button key={cv.id} type="button" role="tab" aria-selected={cv.id === selectedId} onClick={() => setSelectedId(cv.id)}
        className={`min-h-11 rounded-xl border px-4 py-2 text-left text-sm transition-colors ${cv.id === selectedId ? "border-[#B4232D] bg-[#FFF1F2] text-[#B4232D]" : "border-[#E2E8F0] bg-white hover:border-[#B4232D]/40"}`}>
        <span className="block font-medium">{cv.name}</span>
        {cv.targetPosition ? <span className="block text-xs text-[#64748B]">{cv.targetPosition}</span> : null}
      </button>)}
      <span className={`self-center ${META}`}>{cvs.length}/{MAX_CVS}</span>
    </div> : null}
    {selectedId ? <CvEditor key={selectedId} id={selectedId} onChanged={() => setReload(v => v + 1)} /> : null}
  </div>;
}

function CvEditor({ id, onChanged }: { id: string; onChanged: () => void }) {
  const [cv, setCv] = React.useState<CvDetail | null>(null);
  const [draft, setDraft] = React.useState<Draft[]>([]);
  const [dirty, setDirty] = React.useState(false);
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [editingInfo, setEditingInfo] = React.useState(false);
  const [previewVersion, setPreviewVersion] = React.useState(0);

  React.useEffect(() => {
    let ignore = false;
    cvService.get(id).then(data => { if (!ignore) { setCv(data); setDraft(toDraft(data)); setDirty(false); } })
      .catch(err => { if (!ignore) setError(getErrorMessage(err, "Không thể tải CV.")); });
    return () => { ignore = true; };
  }, [id]);

  function touch() { setDirty(true); setError(null); }
  function change(index: number, patch: Partial<Draft>) {
    setDraft(old => old.map((d, i) => i === index ? { ...d, ...patch } : d)); touch();
  }
  function move(index: number, delta: number) {
    setDraft(old => {
      const next = [...old]; const target = index + delta;
      if (target < 0 || target >= next.length) return old;
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });
    touch();
  }
  async function save() {
    setBusy(true); setError(null);
    try {
      const data = await cvService.saveSections(id, payload(draft));
      setCv(data); setDraft(toDraft(data)); setDirty(false);
    } catch (err) { setError(getErrorMessage(err, "Không thể lưu CV.")); }
    finally { setBusy(false); }
  }
  async function download() {
    setBusy(true); setError(null);
    try {
      const blob = await cvService.pdf(id);
      const url = URL.createObjectURL(blob); const link = document.createElement("a");
      link.href = url; link.download = `${slug(cv?.name ?? "cv")}.pdf`; link.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (err) { setError(await pdfError(err)); }
    finally { setBusy(false); }
  }
  async function remove() {
    if (!cv || !window.confirm(`Xoá CV "${cv.name}"? Đơn đã nộp bằng CV này vẫn giữ bản PDF lúc nộp.`)) return;
    setBusy(true); setError(null);
    try { await cvService.remove(id); onChanged(); }
    catch (err) { setError(getErrorMessage(err, "Không thể xoá CV.")); setBusy(false); }
  }

  if (!cv) return error ? <Alert variant="destructive">{error}</Alert> : <p role="status">Đang tải CV...</p>;
  const byType = new Map(cv.sections.map(s => [s.section, s]));
  return <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.15fr)]">
    <Card>
      <CardHeader><div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <h2 className={`${SECTION_TITLE} break-words`}>{cv.name}</h2>
          <p className={META}>{cv.targetPosition ?? "Chưa ghi vị trí ứng tuyển"}</p>
        </div>
        <div className="flex shrink-0">
          <Button variant="ghost" className={ICON_ACTION} aria-label="Đổi tên, vị trí" disabled={busy} onClick={() => setEditingInfo(true)}><FileText /></Button>
          <Button variant="ghost" className={ICON_DANGER} aria-label="Xoá CV" disabled={busy} onClick={() => void remove()}><Trash2 /></Button>
        </div>
      </div></CardHeader>
      <CardContent className="space-y-3">
        {editingInfo ? <FormDialog title="Sửa CV" onClose={() => setEditingInfo(false)}>
          <CvForm initial={cv} submitLabel="Lưu" onCancel={() => setEditingInfo(false)} onSubmit={async body => {
            const data = await cvService.update(id, body);
            setCv(data); setEditingInfo(false); setPreviewVersion(v => v + 1); onChanged();
          }} />
        </FormDialog> : null}
        <p className={META}>Bật/tắt từng phần, chọn mục đưa vào CV, đổi thứ tự bằng mũi tên. Bản xem trước cập nhật ngay; bấm Lưu để giữ thay đổi.</p>
        {draft.map((d, index) => <SectionEditor key={d.section} draft={d} source={byType.get(d.section)} first={index === 0} last={index === draft.length - 1}
          onChange={patch => change(index, patch)} onMove={delta => move(index, delta)} />)}
        {error ? <Alert variant="destructive">{error}</Alert> : null}
        <div className="sticky bottom-0 flex flex-wrap gap-2 bg-card pt-2">
          <Button className={PRIMARY_CTA} disabled={!dirty || busy} onClick={() => void save()}><Save aria-hidden />{busy ? "Đang lưu..." : "Lưu thay đổi"}</Button>
          {dirty ? <Button variant="outline" className={SECONDARY_CTA} disabled={busy} onClick={() => { setDraft(toDraft(cv)); setDirty(false); }}>Huỷ thay đổi</Button> : null}
        </div>
      </CardContent>
    </Card>
    <div className="space-y-3 lg:sticky lg:top-4">
      <div className="flex flex-wrap gap-2">
        <Button className={PRIMARY_CTA} disabled={busy} onClick={() => void download()}><Download aria-hidden />Tải PDF</Button>
        <a href={cv.publicUrl} target="_blank" rel="noreferrer" className={`inline-flex items-center rounded-md border ${SECONDARY_CTA}`}>
          <ExternalLink aria-hidden className="size-4" />Trang QR dẫn tới</a>
      </div>
      {dirty ? <p className={`${BADGE} ${TONE.warning}`}>Có thay đổi chưa lưu. Tải PDF vẫn dùng bản đã lưu.</p> : null}
      <PdfPreview id={id} version={previewVersion} sections={dirty ? payload(draft) : null} />
    </div>
  </div>;
}

function SectionEditor({ draft, source, first, last, onChange, onMove }: {
  draft: Draft; source: CvSection | undefined; first: boolean; last: boolean;
  onChange: (patch: Partial<Draft>) => void; onMove: (delta: number) => void;
}) {
  const items = source?.items ?? [];
  const selected = new Set(draft.itemIds ?? items.map(i => i.id));
  function toggle(itemId: string) {
    const next = new Set(selected);
    if (next.has(itemId)) next.delete(itemId); else next.add(itemId);
    onChange({ itemIds: items.map(i => i.id).filter(i => next.has(i)) });
  }
  const inputId = `cv-section-${draft.section}`;
  return <section className={`${ITEM_CARD} p-3 hover:translate-y-0 motion-safe:hover:translate-y-0 ${draft.included ? "" : "opacity-60"}`} aria-labelledby={`${inputId}-label`}>
    <div className="flex items-center gap-2">
      <input id={inputId} type="checkbox" className="size-5 accent-[#B4232D]" checked={draft.included} onChange={e => onChange({ included: e.target.checked })} />
      <label id={`${inputId}-label`} htmlFor={inputId} className="flex-1 font-medium">{draft.label}
        {items.length ? <span className="font-normal text-[#64748B]"> · {selected.size}/{items.length}</span> : null}</label>
      <Button variant="ghost" className={ICON_ACTION} aria-label={`Đưa ${draft.label} lên`} disabled={first} onClick={() => onMove(-1)}><ArrowUp /></Button>
      <Button variant="ghost" className={ICON_ACTION} aria-label={`Đưa ${draft.label} xuống`} disabled={last} onClick={() => onMove(1)}><ArrowDown /></Button>
    </div>
    {draft.included ? <div className="mt-2 space-y-1 pl-7">
      {draft.section === "SUMMARY" ? (source?.text
        ? <p className="line-clamp-3 text-sm text-[#334155]">{source.text}</p>
        : <p className={META}>Chưa có giới thiệu. Thêm ở <Link href="/candidate/profile" className="underline">hồ sơ</Link>.</p>) : null}
      {draft.section !== "SUMMARY" && items.length === 0
        ? <p className={META}>Hồ sơ chưa có mục nào. Thêm ở <Link href="/candidate/profile" className="underline">hồ sơ</Link>.</p> : null}
      {items.length ? <>
        <label className="flex min-h-9 items-center gap-2 text-xs text-[#475569]">
          <input type="checkbox" className="size-4 accent-[#B4232D]" checked={draft.itemIds === null}
            onChange={e => onChange({ itemIds: e.target.checked ? null : items.map(i => i.id) })} />
          Lấy mọi mục, tự thêm mục mới của hồ sơ
        </label>
        <ul className="space-y-1">{items.map(item => <li key={item.id}>
          <label className="flex min-h-11 cursor-pointer items-start gap-2 rounded-lg px-2 py-1.5 hover:bg-[#F6F7FA]">
            <input type="checkbox" className="mt-1 size-4 accent-[#B4232D]" checked={selected.has(item.id)} onChange={() => toggle(item.id)} />
            <span className="min-w-0 flex-1">
              <span className="block break-words text-sm font-medium text-[#18233A]">{item.title}
                {item.subtitle ? <span className="font-normal text-[#64748B]"> · {item.subtitle}</span> : null}</span>
              {item.period || (draft.section === "SKILLS" && item.detail) ? <span className={`block ${META}`}>{draft.section === "SKILLS" ? item.detail : item.period}</span> : null}
            </span>
            {item.verified === true ? <span className={`${BADGE} ${TONE.success}`}>Đã xác thực</span> : null}
            {item.verified === false && draft.section === "SKILLS" ? <span className={`${BADGE} ${TONE.neutral}`}>Tự khai</span> : null}
          </label>
        </li>)}</ul>
      </> : null}
    </div> : null}
  </section>;
}

/** sections = bản nháp chưa lưu thì dựng theo nháp (không ghi DB), null thì dựng bản đã lưu. */
function PdfPreview({ id, version, sections }: { id: string; version: number; sections: CvSectionPayload[] | null }) {
  // Vẽ PDF bằng pdf.js lên canvas ngoài DOM, vẽ xong mới thay một lần nên khung không nháy trắng.
  const box = React.useRef<HTMLDivElement>(null);
  const [ready, setReady] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [retry, setRetry] = React.useState(0);
  const body = sections ? JSON.stringify(sections) : null;
  React.useEffect(() => {
    let ignore = false;
    // Chờ ngừng bấm 400ms rồi mới dựng, tránh dựng PDF cho từng cú click.
    const timer = setTimeout(async () => {
      try {
        const blob = await (body ? cvService.preview(id, JSON.parse(body) as CvSectionPayload[]) : cvService.pdf(id, true));
        const canvases = await renderPdf(blob, box.current?.clientWidth || 600, () => ignore);
        if (ignore || !canvases || !box.current) return;
        box.current.replaceChildren(...canvases); setReady(true); setError(null);
      } catch (err) { if (!ignore) setError(await pdfError(err)); }
    }, body ? 400 : 0);
    return () => { ignore = true; clearTimeout(timer); };
  }, [id, version, retry, body]);
  return <div className="space-y-2">
    {error ? <><Alert variant="destructive">{error}</Alert>
      <Button variant="outline" className={SECONDARY_CTA} onClick={() => { setError(null); setRetry(v => v + 1); }}><RefreshCw aria-hidden />Thử lại</Button></> : null}
    <div className="relative aspect-[1/1.414] w-full overflow-y-auto rounded-xl border bg-[#F6F7FA]">
      <div ref={box} role="img" aria-label="Xem trước CV" className="space-y-2" />
      {!ready && !error ? <div role="status" className="absolute inset-0 flex items-center justify-center bg-white text-sm text-[#64748B]">Đang dựng bản xem trước...</div> : null}
    </div>
  </div>;
}

/** Vẽ từng trang PDF ra canvas rộng `width` px (nét theo devicePixelRatio). Trả null nếu bị huỷ giữa chừng. */
async function renderPdf(blob: Blob, width: number, cancelled: () => boolean) {
  const pdfjs = await import("pdfjs-dist");
  if (!pdfjs.GlobalWorkerOptions.workerPort) {
    pdfjs.GlobalWorkerOptions.workerPort = new Worker(new URL("pdfjs-dist/build/pdf.worker.min.mjs", import.meta.url), { type: "module" });
  }
  const task = pdfjs.getDocument({ data: new Uint8Array(await blob.arrayBuffer()) });
  try {
    const doc = await task.promise;
    const canvases: HTMLCanvasElement[] = [];
    for (let n = 1; n <= doc.numPages; n++) {
      if (cancelled()) return null;
      const page = await doc.getPage(n);
      const viewport = page.getViewport({ scale: width / page.getViewport({ scale: 1 }).width * window.devicePixelRatio });
      const canvas = document.createElement("canvas");
      canvas.width = Math.floor(viewport.width); canvas.height = Math.floor(viewport.height);
      canvas.className = "block h-auto w-full bg-white";
      await page.render({ canvas, viewport }).promise;
      canvases.push(canvas);
    }
    return canvases;
  } finally { void task.destroy(); }
}

function CvForm({ initial, submitLabel, onSubmit, onCancel }: {
  initial?: { name: string; targetPosition: string | null }; submitLabel: string;
  onSubmit: (body: { name: string; targetPosition: string | null }) => Promise<void>; onCancel: () => void;
}) {
  const [name, setName] = React.useState(initial?.name ?? "");
  const [position, setPosition] = React.useState(initial?.targetPosition ?? "");
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  return <form className="space-y-4" onSubmit={async e => {
    e.preventDefault();
    if (!name.trim()) { setError("Hãy đặt tên cho CV."); return; }
    setBusy(true); setError(null);
    try { await onSubmit({ name: name.trim(), targetPosition: position.trim() || null }); }
    catch (err) { setError(getErrorMessage(err, "Không thể lưu CV.")); setBusy(false); }
  }}>
    <div className="space-y-1.5"><Label htmlFor="cv-name">Tên CV</Label>
      <Input id="cv-name" value={name} maxLength={255} onChange={e => setName(e.target.value)} placeholder="CV Backend" required /></div>
    <div className="space-y-1.5"><Label htmlFor="cv-position">Vị trí ứng tuyển</Label>
      <Input id="cv-position" value={position} maxLength={255} onChange={e => setPosition(e.target.value)} placeholder="Java Developer" /></div>
    {error ? <Alert variant="destructive">{error}</Alert> : null}
    <div className="flex justify-end gap-2">
      <Button type="button" variant="outline" className={SECONDARY_CTA} onClick={onCancel} disabled={busy}>Huỷ</Button>
      <Button type="submit" className={PRIMARY_CTA} disabled={busy}>{busy ? "Đang lưu..." : submitLabel}</Button>
    </div>
  </form>;
}

/** Lỗi của request blob đến dưới dạng Blob JSON; đọc lại để hiện đúng thông báo backend (ví dụ PROFILE_REQUIRED). */
async function pdfError(err: unknown) {
  if (err instanceof ApiError && err.message) return err.message;
  const data = (err as { response?: { data?: unknown } })?.response?.data;
  if (data instanceof Blob) {
    try { const body = JSON.parse(await data.text()) as { message?: string }; if (body.message) return body.message; } catch { /* bỏ qua */ }
  }
  return "Không thể dựng PDF. Vui lòng thử lại.";
}

function payload(draft: Draft[]): CvSectionPayload[] {
  return draft.map(({ section, included, itemIds }) => ({ section, included, itemIds }));
}

function slug(name: string) {
  return name.normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/đ/g, "d").replace(/Đ/g, "D")
    .replace(/[^A-Za-z0-9]+/g, "-").replace(/^-|-$/g, "").toLowerCase() || "cv";
}
