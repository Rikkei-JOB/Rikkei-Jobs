"use client";
import { useEffect, useState, type FormEvent } from "react";
import { Button } from "@/app/components/ui/button";
import { TaskBanner } from "@/app/components/ui/task-banner";
import { contactService } from "@/services/contact.service";
import { getErrorMessage } from "@/services/api";
import type { ContactPage, ContactLog, ContactLogFilters } from "@/interface/contact";
import { contactDate, viaLabel, controlClass, ContactPagination } from "./contact-history-shared";

export function ContactAccessLogs({ staff = false }: { staff?: boolean }) {
  const [data, setData] = useState<ContactPage<ContactLog> | null>(null);
  const [query, setQuery] = useState<ContactLogFilters>({ page: 1 });
  const [revision, setRevision] = useState(0);
  const [error, setError] = useState("");
  useEffect(() => {
    const abort = new AbortController();
    const result = staff ? contactService.staffLogs(query, abort.signal) : contactService.myLogs(query.page, abort.signal);
    result.then(setData).catch(err => { if (!abort.signal.aborted) setError(getErrorMessage(err)); });
    return () => abort.abort();
  }, [staff, query, revision]);
  function apply(e: FormEvent<HTMLFormElement>) {
    e.preventDefault(); const form = new FormData(e.currentTarget);
    const from = String(form.get("from") || ""), to = String(form.get("to") || "");
    if (from && to && from > to) { setError("Ngày kết thúc phải từ ngày bắt đầu trở đi."); return; }
    const filters: ContactLogFilters = { page: 1 };
    for (const key of ["companyId", "userId"] as const) { const value = String(form.get(key) || "").trim(); if (value) filters[key] = value; }
    if (from) filters.from = new Date(from + "T00:00:00+07:00").toISOString();
    if (to) filters.to = new Date(new Date(to + "T00:00:00+07:00").getTime() + 86400000).toISOString();
    setError(""); setData(null); setQuery(filters);
  }
  const groups = new Map<string, ContactLog[]>();
  data?.items.forEach(log => groups.set(log.companyId, [...(groups.get(log.companyId) || []), log]));
  return <div className={staff ? "w-full space-y-5" : "mx-auto w-full max-w-6xl space-y-5"}>
    <TaskBanner compact={staff} eyebrow={staff ? "Bảo mật liên hệ" : "Quyền riêng tư"} illustration="applications"
      title={staff ? "Nhật ký xem liên hệ" : "Công ty đã xem liên hệ"}
      description={staff ? "Tra cứu từng lần công ty nhận thông tin liên hệ của ứng viên." : "Mỗi dòng là một lần công ty đã xem thông tin liên hệ của bạn."} />
    {staff && <form onSubmit={apply} className="grid gap-4 rounded-xl border bg-card p-5 sm:grid-cols-2 lg:grid-cols-4">
      <label className="text-sm font-medium">ID công ty<input name="companyId" className={controlClass} placeholder="UUID công ty" pattern="[0-9a-fA-F-]{36}" /></label>
      <label className="text-sm font-medium">ID ứng viên<input name="userId" className={controlClass} placeholder="UUID ứng viên" pattern="[0-9a-fA-F-]{36}" /></label>
      <label className="text-sm font-medium">Từ ngày<input name="from" type="date" className={controlClass} /></label>
      <label className="text-sm font-medium">Đến ngày<input name="to" type="date" className={controlClass} /></label>
      <p className="text-xs text-muted-foreground sm:col-span-2">Ngày được tính theo giờ Việt Nam, gồm cả ngày kết thúc.</p>
      <div className="flex flex-wrap gap-3 sm:col-span-2 lg:justify-end"><Button className="min-h-11" type="reset" variant="outline" onClick={() => { setError(""); setData(null); setQuery({ page: 1 }); }}>Xóa bộ lọc</Button><Button className="min-h-11" type="submit">Áp dụng bộ lọc</Button></div>
    </form>}
    {error && <p role="alert" className="break-words text-sm text-destructive">{error}</p>}
    {!data && !error && <p role="status">Đang tải…</p>}
    {!data && error && <Button className="min-h-11" variant="outline" onClick={() => { setError(""); setRevision(r => r + 1); }}>Thử lại</Button>}
    {data && <>
      {data.items.length === 0 && <p className="rounded-xl border bg-card p-6 text-muted-foreground">{staff ? "Không có lượt xem phù hợp bộ lọc." : "Chưa có công ty nào xem liên hệ của bạn."}</p>}
      {staff ? <div className="overflow-x-auto rounded-xl border bg-card"><table className="w-full min-w-[760px] text-left text-sm"><caption className="sr-only">Nhật ký truy cập liên hệ ứng viên</caption><thead className="border-b bg-muted"><tr>{["Công ty", "HR", "Ứng viên", "Nguồn cấp quyền", "Thời gian"].map(label => <th key={label} className="p-4 font-medium">{label}</th>)}</tr></thead><tbody>{data.items.map(log => <tr key={log.id} className="border-b last:border-0">
        <td className="max-w-64 break-words p-4">{log.companyName}<span className="mt-1 block break-all text-xs text-muted-foreground">{log.companyId}</span></td><td className="max-w-48 break-words p-4">{log.hrName || log.hrId}</td><td className="max-w-64 break-words p-4">{log.candidateName || log.userId}<span className="mt-1 block break-all text-xs text-muted-foreground">{log.userId}</span></td><td className="p-4">{viaLabel(log.via)}</td><td className="whitespace-nowrap p-4">{contactDate(log.createdAt)}</td>
      </tr>)}</tbody></table></div> : <div className="space-y-4">{[...groups.entries()].map(([id, logs]) => <section key={id} className="rounded-xl border bg-card p-5 text-card-foreground"><h2 className="break-words text-lg font-semibold">{logs[0].companyName}</h2><p className="mt-1 break-all text-xs text-muted-foreground">{id}</p><ul className="mt-4 divide-y">{logs.map(log => <li key={log.id} className="flex flex-wrap items-center justify-between gap-2 py-3 text-sm"><time dateTime={log.createdAt}>{contactDate(log.createdAt)}</time><span className="rounded-full bg-muted px-3 py-1">{viaLabel(log.via)}</span></li>)}</ul></section>)}</div>}
      {!staff && data.total > data.size && <p className="text-xs text-muted-foreground">Các lượt xem được nhóm theo công ty trong trang hiện tại.</p>}
      <ContactPagination {...data} onPage={page => { setData(null); setError(""); setQuery(q => ({ ...q, page })); }} />
    </>}
  </div>;
}
