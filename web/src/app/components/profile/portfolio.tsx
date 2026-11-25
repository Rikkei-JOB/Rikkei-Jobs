"use client";

import * as React from "react";
import { useForm } from "react-hook-form";
import { Dialog } from "@base-ui/react/dialog";
import { ArrowDown, ArrowUp, BadgeCheck, ChevronLeft, ChevronRight, GripVertical, ImageIcon, X } from "lucide-react";
import { Card, CardContent, CardHeader } from "@/app/components/ui/card";
import { Button } from "@/app/components/ui/button";
import { Alert } from "@/app/components/ui/alert";
import { Input } from "@/app/components/ui/input";
import { FormField } from "@/app/components/auth/form-field";
import { zodResolver } from "@/helper/zod-resolver";
import { PORTFOLIO_MAX_IMAGES, portfolioPayload, portfolioSchema, validatePortfolioImage, type PortfolioValues } from "@/schemas/portfolio.schema";
import { portfolioService } from "@/services/portfolio.service";
import { getErrorMessage } from "@/services/api";
import type { PortfolioMedia, PortfolioProject } from "@/interface/portfolio";

/**
 * HS-03 Portfolio. Không truyền userId: màn quản lý của ứng viên (thêm/sửa/xoá, tải ảnh, kéo thả sắp thứ tự).
 * Có userId: bản chỉ đọc cho người xem hồ sơ (backend chỉ trả dự án visible của hồ sơ PUBLIC, ngược lại 404).
 */
export function Portfolio({ userId }: { userId?: string }) {
  const readOnly = userId !== undefined;
  const [items, setItems] = React.useState<PortfolioProject[] | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [reload, setReload] = React.useState(0);
  const [editing, setEditing] = React.useState<PortfolioProject | null | undefined>(undefined);
  const [busy, setBusy] = React.useState(false);
  const [dragId, setDragId] = React.useState<string | null>(null);
  const [viewing, setViewing] = React.useState<PortfolioProject | null>(null);
  React.useEffect(() => {
    let ignore = false;
    (readOnly ? portfolioService.ofUser(userId) : portfolioService.mine())
      .then(data => { if (!ignore) { setItems(data); setError(null); } })
      .catch(err => { if (!ignore) setError(getErrorMessage(err, "Không thể tải portfolio.")); });
    return () => { ignore = true; };
  }, [readOnly, userId, reload]);

  async function persistOrder(next: PortfolioProject[]) {
    const previous = items;
    setItems(next); setBusy(true); setError(null);
    try { setItems(await portfolioService.reorder(next.map(p => p.id))); }
    catch (err) { setItems(previous); setError(getErrorMessage(err, "Không thể lưu thứ tự dự án.")); }
    finally { setBusy(false); }
  }
  function move(fromId: string, toIndex: number) {
    if (!items) return;
    const from = items.findIndex(p => p.id === fromId);
    if (from < 0 || toIndex < 0 || toIndex >= items.length || from === toIndex) return;
    const next = [...items]; const [moved] = next.splice(from, 1); next.splice(toIndex, 0, moved);
    void persistOrder(next);
  }
  async function remove(item: PortfolioProject) {
    if (!window.confirm(`Xoá dự án ${item.name}?`)) return;
    setBusy(true); setError(null);
    try { await portfolioService.remove(item.id); setItems(old => old?.filter(p => p.id !== item.id) ?? []); }
    catch (err) { setError(getErrorMessage(err, "Không thể xoá dự án.")); }
    finally { setBusy(false); }
  }
  const canDrag = !readOnly && editing === undefined && !busy && (items?.length ?? 0) > 1;

  return <Card>
    <CardHeader><div className="flex flex-wrap items-center justify-between gap-3">
      <h2 className="text-lg font-semibold">Portfolio dự án</h2>
      {readOnly ? null : <Button onClick={() => setEditing(null)} disabled={items === null}>Thêm dự án</Button>}
    </div></CardHeader>
    <CardContent className="space-y-4">
      {error ? <div className="space-y-2"><Alert variant="destructive">{error}</Alert>{items === null ? <Button onClick={() => { setError(null); setReload(v => v + 1); }}>Thử lại</Button> : null}</div> : null}
      {items === null && !error ? <p role="status">Đang tải portfolio...</p> : null}
      {editing !== undefined ? <ProjectForm key={editing?.id ?? "new"} item={editing} onCancel={() => setEditing(undefined)} onSaved={saved => {
        setItems(old => (old ?? []).some(p => p.id === saved.id) ? (old ?? []).map(p => p.id === saved.id ? saved : p) : [...old ?? [], saved]);
        setEditing(undefined);
      }} /> : null}
      {viewing ? <ProjectDetail item={viewing} onClose={() => setViewing(null)}
        onEdit={readOnly ? undefined : () => { setEditing(viewing); setViewing(null); }} /> : null}
      {items?.length === 0 ? <p className="text-sm text-muted-foreground">{readOnly ? "Chưa có dự án công khai." : "Chưa có dự án. Thêm dự án đầu tiên để nhà tuyển dụng thấy bạn đã làm gì."}</p> : null}
      {canDrag ? <p className="text-xs text-muted-foreground">Kéo thả thẻ hoặc dùng nút mũi tên để sắp thứ tự; thứ tự được lưu ngay.</p> : null}
      <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {items?.map((item, index) => <li key={item.id} draggable={canDrag}
          onDragStart={e => { setDragId(item.id); e.dataTransfer.effectAllowed = "move"; }}
          onDragOver={e => { if (dragId) e.preventDefault(); }}
          onDrop={e => { e.preventDefault(); if (dragId) move(dragId, index); setDragId(null); }}
          onDragEnd={() => setDragId(null)}
          onClick={e => { if (!(e.target as HTMLElement).closest("button,a")) setViewing(item); }}
          onKeyDown={e => { if (e.target === e.currentTarget && (e.key === "Enter" || e.key === " ")) { e.preventDefault(); setViewing(item); } }}
          tabIndex={0} aria-label={`Xem chi tiết dự án ${item.name}`}
          className={`flex flex-col overflow-hidden rounded-lg border bg-card transition-shadow hover:shadow-md focus-visible:ring-2 focus-visible:ring-ring/50 focus-visible:outline-none ${dragId === item.id ? "opacity-50" : ""} ${canDrag ? "cursor-grab" : "cursor-pointer"}`}
          data-testid="portfolio-card">
          <Cover media={item.media} name={item.name} />
          <div className="flex flex-1 flex-col gap-2 p-3">
            <div className="flex items-start gap-2">
              {canDrag ? <GripVertical aria-hidden className="mt-0.5 size-4 shrink-0 text-muted-foreground" /> : null}
              <h3 className="flex-1 font-medium break-words">{item.name}</h3>
              {!readOnly && !item.visible ? <span className="rounded bg-muted px-1.5 py-0.5 text-[0.625rem] text-muted-foreground">Đang ẩn</span> : null}
            </div>
            {item.verified ? <VerifiedBadge /> : null}
            {item.role ? <p className="text-sm text-muted-foreground break-words">{item.role}</p> : null}
            {item.startDate ? <p className="text-xs text-muted-foreground">{item.startDate}{item.endDate ? ` → ${item.endDate}` : " → nay"}</p> : null}
            {item.description ? <p className="line-clamp-3 text-sm break-words">{item.description}</p> : null}
            {item.techStack.length ? <ul className="flex flex-wrap gap-1" aria-label="Công nghệ">{item.techStack.map(tag =>
              <li key={tag} className="rounded bg-secondary px-1.5 py-0.5 text-xs text-secondary-foreground">{tag}</li>)}</ul> : null}
            <ProjectLinks item={item} />
            {readOnly ? null : <div className="mt-auto flex flex-wrap gap-2 pt-2">
              <Button variant="outline" size="icon-sm" aria-label={`Đưa ${item.name} lên trước`} disabled={busy || index === 0} onClick={() => move(item.id, index - 1)}><ArrowUp /></Button>
              <Button variant="outline" size="icon-sm" aria-label={`Đưa ${item.name} ra sau`} disabled={busy || index === items.length - 1} onClick={() => move(item.id, index + 1)}><ArrowDown /></Button>
              <Button variant="outline" aria-label={`Sửa dự án ${item.name}`} disabled={busy} onClick={() => setEditing(item)}>Sửa</Button>
              {item.verified ? null : <Button variant="destructive" aria-label={`Xoá dự án ${item.name}`} disabled={busy} onClick={() => void remove(item)}>Xoá</Button>}
            </div>}
          </div>
        </li>)}
      </ul>
    </CardContent>
  </Card>;
}

const LINK = "text-primary underline-offset-4 hover:underline";
function ProjectLinks({ item }: { item: PortfolioProject }) {
  return <div className="flex flex-wrap gap-3 text-xs">
    {item.repoUrl ? <a href={item.repoUrl} target="_blank" rel="noopener noreferrer" className={LINK}>Mã nguồn</a> : null}
    {item.demoUrl ? <a href={item.demoUrl} target="_blank" rel="noopener noreferrer" className={LINK}>Demo</a> : null}
    {item.media.filter(m => m.type === "EVIDENCE").map(m => /^https?:\/\//i.test(m.url)
      ? <a key={m.id} href={m.url} target="_blank" rel="noopener noreferrer" className={LINK}>{m.caption ?? "Minh chứng"}</a> : null)}
  </div>;
}

function VerifiedBadge() {
  return <span className="inline-flex w-fit items-center gap-1 rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-medium text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300">
    <BadgeCheck className="size-3.5" aria-hidden />Thực tập IOC – đã xác thực</span>;
}

/** Popup chỉ đọc: toàn bộ ảnh, mô tả đầy đủ, công nghệ, link. Chủ hồ sơ có nút chuyển sang sửa. */
function ProjectDetail({ item, onClose, onEdit }: { item: PortfolioProject; onClose: () => void; onEdit?: () => void }) {
  const images = item.media.filter(m => m.type === "IMAGE");
  const [selected, setSelected] = React.useState(0);
  const src = useImageSrc(images[selected]?.url ?? null);
  const step = (delta: number) => setSelected(i => (i + delta + images.length) % images.length);
  return <Dialog.Root open onOpenChange={open => { if (!open) onClose(); }}>
    <Dialog.Portal>
      <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/45" />
      <Dialog.Popup className="fixed left-1/2 top-1/2 z-50 max-h-[90dvh] w-[calc(100%-2rem)] max-w-3xl -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-2xl border bg-background p-6 shadow-xl">
        <div className="mb-4 flex items-start justify-between gap-3">
          <div className="space-y-1">
            <Dialog.Title className="text-lg font-semibold break-words">{item.name}</Dialog.Title>
            {item.verified ? <VerifiedBadge /> : null}
          </div>
          <Dialog.Close aria-label="Đóng" className="rounded-md p-2 hover:bg-muted"><X className="size-4" /></Dialog.Close>
        </div>
        {images.length ? <div className="mb-4 space-y-2">
          <div className="relative aspect-video overflow-hidden rounded-lg bg-muted"
            onKeyDown={e => { if (e.key === "ArrowLeft") step(-1); if (e.key === "ArrowRight") step(1); }}>
            {src ? <picture><img src={src} alt={images[selected].caption ?? `Ảnh ${selected + 1} của dự án ${item.name}`} className="size-full object-contain" /></picture> : null}
            {images.length > 1 ? <>
              <button type="button" onClick={() => step(-1)} aria-label="Ảnh trước" className="absolute top-1/2 left-2 -translate-y-1/2 rounded-full bg-black/55 p-2 text-white hover:bg-black/75"><ChevronLeft className="size-5" /></button>
              <button type="button" onClick={() => step(1)} aria-label="Ảnh sau" className="absolute top-1/2 right-2 -translate-y-1/2 rounded-full bg-black/55 p-2 text-white hover:bg-black/75"><ChevronRight className="size-5" /></button>
              <span className="absolute bottom-2 left-1/2 -translate-x-1/2 rounded bg-black/55 px-2 py-0.5 text-xs text-white" aria-live="polite">{selected + 1}/{images.length}</span>
            </> : null}
          </div>
          {images.length > 1 ? <ul className="flex flex-wrap gap-2">{images.map((m, i) => <li key={m.id}>
            <button type="button" onClick={() => setSelected(i)} aria-label={`Xem ảnh ${i + 1}`} aria-pressed={i === selected}
              className={`block size-16 overflow-hidden rounded-md border ${i === selected ? "ring-2 ring-primary" : "opacity-70 hover:opacity-100"}`}><Thumb media={m} /></button>
          </li>)}</ul> : null}
        </div> : null}
        <dl className="grid gap-x-6 gap-y-2 text-sm sm:grid-cols-2">
          {item.role ? <div><dt className="text-xs text-muted-foreground">Vai trò</dt><dd className="break-words">{item.role}</dd></div> : null}
          {item.startDate ? <div><dt className="text-xs text-muted-foreground">Thời gian</dt><dd>{item.startDate}{item.endDate ? ` → ${item.endDate}` : " → nay"}</dd></div> : null}
        </dl>
        {item.description ? <p className="mt-3 text-sm whitespace-pre-line break-words">{item.description}</p> : null}
        {item.techStack.length ? <ul className="mt-3 flex flex-wrap gap-1" aria-label="Công nghệ">{item.techStack.map(tag =>
          <li key={tag} className="rounded bg-secondary px-1.5 py-0.5 text-xs text-secondary-foreground">{tag}</li>)}</ul> : null}
        <div className="mt-3"><ProjectLinks item={item} /></div>
        <div className="mt-6 flex gap-2">
          {onEdit ? <Button onClick={onEdit}>Sửa dự án</Button> : null}
          <Dialog.Close render={<Button variant="outline" />}>Đóng</Dialog.Close>
        </div>
      </Dialog.Popup>
    </Dialog.Portal>
  </Dialog.Root>;
}

function Thumb({ media }: { media: PortfolioMedia }) {
  const src = useImageSrc(media.url);
  return src ? <picture><img src={src} alt="" className="size-full object-cover" /></picture> : <span className="block size-full bg-muted" />;
}

/** Ảnh cần JWT nên đọc qua API thành blob, không gắn thẳng vào src. */
function useImageSrc(url: string | null) {
  const [loaded, setLoaded] = React.useState<{ url: string; src: string } | null>(null);
  React.useEffect(() => {
    let ignore = false; let objectUrl: string | null = null;
    if (url) portfolioService.readImage(url).then(blob => {
      if (!ignore) { objectUrl = URL.createObjectURL(blob); setLoaded({ url, src: objectUrl }); }
    }).catch(() => { /* Ảnh lỗi thì giữ khung trống, không chặn cả thẻ. */ });
    return () => { ignore = true; if (objectUrl) URL.revokeObjectURL(objectUrl); };
  }, [url]);
  return loaded && loaded.url === url ? loaded.src : null;
}

function Cover({ media, name }: { media: PortfolioMedia[]; name: string }) {
  const images = media.filter(m => m.type === "IMAGE");
  const src = useImageSrc(images[0]?.url ?? null);
  return <div className="relative aspect-video bg-muted">
    {src ? <picture><img src={src} alt={`Ảnh bìa dự án ${name}`} className="size-full object-cover" /></picture>
      : <div className="flex size-full items-center justify-center text-muted-foreground"><ImageIcon className="size-8" aria-hidden /></div>}
    {images.length > 1 ? <span className="absolute right-2 bottom-2 rounded bg-black/60 px-1.5 py-0.5 text-xs text-white">{images.length} ảnh</span> : null}
  </div>;
}

function StoredImage({ media, onRemove, disabled }: { media: PortfolioMedia; onRemove: () => void; disabled: boolean }) {
  const src = useImageSrc(media.url);
  return <li className="relative size-20 overflow-hidden rounded-md border bg-muted">
    {src ? <picture><img src={src} alt={media.caption ?? "Ảnh dự án"} className="size-full object-cover" /></picture> : null}
    <button type="button" onClick={onRemove} disabled={disabled} aria-label="Xoá ảnh này" className="absolute top-0.5 right-0.5 rounded-full bg-black/60 p-0.5 text-white disabled:opacity-50"><X className="size-3" /></button>
  </li>;
}

function ProjectForm({ item, onCancel, onSaved }: { item: PortfolioProject | null; onCancel: () => void; onSaved: (item: PortfolioProject) => void }) {
  const locked = item?.verified ?? false;
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<PortfolioValues>({
    resolver: zodResolver(portfolioSchema), defaultValues: {
      name: item?.name ?? "", role: item?.role ?? "", description: item?.description ?? "", techStack: item?.techStack.join(", ") ?? "",
      repoUrl: item?.repoUrl ?? "", demoUrl: item?.demoUrl ?? "", startDate: item?.startDate ?? "", endDate: item?.endDate ?? "", visible: item?.visible ?? true,
    },
  });
  const [error, setError] = React.useState<string | null>(null);
  const [saved, setSaved] = React.useState<PortfolioProject | null>(item);
  const [stored, setStored] = React.useState<PortfolioMedia[]>(item?.media.filter(m => m.type === "IMAGE") ?? []);
  const [pending, setPending] = React.useState<{ file: File; preview: string }[]>([]);
  const [removing, setRemoving] = React.useState(false);
  // Thu hồi object URL xem trước còn lại khi đóng form.
  const latestPending = React.useRef(pending);
  React.useEffect(() => { latestPending.current = pending; }, [pending]);
  React.useEffect(() => () => latestPending.current.forEach(p => URL.revokeObjectURL(p.preview)), []);
  function dropPending(p: { preview: string }) {
    URL.revokeObjectURL(p.preview);
    setPending(old => old.filter(x => x.preview !== p.preview));
  }

  function pick(files: FileList | null) {
    setError(null);
    const chosen = Array.from(files ?? []);
    const invalid = chosen.map(validatePortfolioImage).find(Boolean);
    if (invalid) { setError(invalid); return; }
    if (stored.length + pending.length + chosen.length > PORTFOLIO_MAX_IMAGES) { setError(`Mỗi dự án tối đa ${PORTFOLIO_MAX_IMAGES} ảnh.`); return; }
    const added = chosen.map(file => ({ file, preview: URL.createObjectURL(file) }));
    setPending(old => [...old, ...added]);
  }
  async function removeStored(media: PortfolioMedia) {
    if (!saved) return;
    setRemoving(true); setError(null);
    try { await portfolioService.removeImage(saved.id, media.id); setStored(old => old.filter(m => m.id !== media.id)); }
    catch (err) { setError(getErrorMessage(err, "Không thể xoá ảnh.")); }
    finally { setRemoving(false); }
  }
  async function submit(values: PortfolioValues) {
    setError(null);
    try {
      // Lưu dự án trước; nếu tải ảnh lỗi giữa chừng, lần lưu sau dùng lại dự án đã tạo và chỉ tải ảnh còn lại.
      const project = await portfolioService.save(saved?.id ?? null, portfolioPayload(values));
      setSaved(project);
      let images = stored;
      for (const p of pending) {
        const media = await portfolioService.addImage(project.id, p.file);
        images = [...images, media]; setStored(images);
        dropPending(p);
      }
      onSaved({ ...project, media: [...images, ...project.media.filter(m => m.type === "EVIDENCE")] });
    } catch (err) { setError(getErrorMessage(err, "Không thể lưu dự án.")); }
  }
  const busy = isSubmitting || removing;
  function close() {
    if (busy) return;
    // Dự án/ảnh đã lưu dở (lỗi giữa chừng hoặc vừa xoá ảnh) vẫn phải cập nhật lên lưới khi đóng.
    if (saved) onSaved({ ...saved, media: [...stored, ...saved.media.filter(m => m.type === "EVIDENCE")] }); else onCancel();
  }
  return <Dialog.Root open onOpenChange={open => { if (!open) close(); }}>
    <Dialog.Portal>
      <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/45" />
      <Dialog.Popup className="fixed left-1/2 top-1/2 z-50 max-h-[90dvh] w-[calc(100%-2rem)] max-w-3xl -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-2xl border bg-background p-6 shadow-xl">
        <div className="mb-4 flex items-center justify-between gap-3">
          <Dialog.Title className="text-lg font-semibold">{item ? "Sửa dự án" : "Thêm dự án"}</Dialog.Title>
          <Dialog.Close disabled={busy} aria-label="Đóng" className="rounded-md p-2 hover:bg-muted"><X className="size-4" /></Dialog.Close>
        </div>
  <form onSubmit={handleSubmit(submit)} noValidate className="space-y-4">
    <fieldset disabled={busy} className="space-y-4">
      {locked ? <Alert>Dự án thực tập IOC đã xác thực: chỉ sửa được vai trò, mô tả và trạng thái hiển thị.</Alert> : null}
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Tên dự án" htmlFor="portfolio-name" error={errors.name?.message}><Input id="portfolio-name" required maxLength={255} disabled={locked} {...register("name")} /></FormField>
        <FormField label="Vai trò" htmlFor="portfolio-role" error={errors.role?.message}><Input id="portfolio-role" maxLength={255} placeholder="VD: Frontend Developer" {...register("role")} /></FormField>
        <FormField label="Công nghệ (phân cách bằng dấu phẩy)" htmlFor="portfolio-tech" error={errors.techStack?.message}><Input id="portfolio-tech" disabled={locked} placeholder="React, Spring Boot, PostgreSQL" {...register("techStack")} /></FormField>
        <FormField label="Link mã nguồn" htmlFor="portfolio-repo" error={errors.repoUrl?.message}><Input id="portfolio-repo" type="url" disabled={locked} placeholder="https://github.com/..." {...register("repoUrl")} /></FormField>
        <FormField label="Link demo" htmlFor="portfolio-demo" error={errors.demoUrl?.message}><Input id="portfolio-demo" type="url" disabled={locked} placeholder="https://..." {...register("demoUrl")} /></FormField>
        <div className="grid grid-cols-2 gap-2">
          <FormField label="Bắt đầu" htmlFor="portfolio-start" error={errors.startDate?.message}><Input id="portfolio-start" type="date" disabled={locked} {...register("startDate")} /></FormField>
          <FormField label="Kết thúc" htmlFor="portfolio-end" error={errors.endDate?.message}><Input id="portfolio-end" type="date" disabled={locked} {...register("endDate")} /></FormField>
        </div>
      </div>
      <FormField label="Mô tả" htmlFor="portfolio-description" error={errors.description?.message}>
        <textarea id="portfolio-description" rows={4} maxLength={5000} className="w-full rounded-md border bg-background p-2 text-sm" {...register("description")} />
      </FormField>
      <label className="flex items-center gap-2 text-sm"><input type="checkbox" {...register("visible")} />Hiển thị trên hồ sơ công khai</label>
      <div className="space-y-2">
        <FormField label={`Ảnh dự án (${stored.length + pending.length}/${PORTFOLIO_MAX_IMAGES})`} htmlFor="portfolio-images">
          <Input id="portfolio-images" type="file" accept="image/jpeg,image/png" multiple disabled={stored.length + pending.length >= PORTFOLIO_MAX_IMAGES}
            onChange={e => { pick(e.target.files); e.target.value = ""; }} />
        </FormField>
        <p className="text-xs text-muted-foreground">JPG hoặc PNG, tối đa 5MB mỗi ảnh. Ảnh đầu tiên là ảnh bìa.</p>
        {stored.length + pending.length ? <ul className="flex flex-wrap gap-2">
          {stored.map(m => <StoredImage key={m.id} media={m} disabled={busy} onRemove={() => void removeStored(m)} />)}
          {pending.map(p => <li key={p.preview} className="relative size-20 overflow-hidden rounded-md border ring-2 ring-primary/40">
            <picture><img src={p.preview} alt={`Xem trước ${p.file.name}`} className="size-full object-cover" /></picture>
            <button type="button" aria-label={`Bỏ ảnh ${p.file.name}`} onClick={() => dropPending(p)} className="absolute top-0.5 right-0.5 rounded-full bg-black/60 p-0.5 text-white"><X className="size-3" /></button>
          </li>)}
        </ul> : null}
      </div>
      {error ? <Alert variant="destructive">{error}</Alert> : null}
      <div className="flex gap-2"><Button type="submit">{isSubmitting ? "Đang lưu..." : "Lưu dự án"}</Button>
        <Button type="button" variant="outline" onClick={close}>Huỷ</Button></div>
    </fieldset>
  </form>
      </Dialog.Popup>
    </Dialog.Portal>
  </Dialog.Root>;
}
