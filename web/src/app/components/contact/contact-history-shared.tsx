"use client";
import { Button } from "@/app/components/ui/button";
export const statusLabels = { PENDING: "Chờ duyệt", APPROVED: "Đã duyệt", REJECTED: "Đã từ chối" };
export const viaLabel = (via: string) => via === "APPLICATION" ? "Đã ứng tuyển" : "Yêu cầu được duyệt";
export const contactDate = (value: string) => new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: "short", timeZone: "Asia/Ho_Chi_Minh" }).format(new Date(value));
export const controlClass = "mt-2 min-h-11 w-full rounded-lg border bg-background px-3 text-base";
export function ContactPagination({ page, size, total, onPage }: { page: number; size: number; total: number; onPage(page: number): void }) {
  return <nav aria-label="Phân trang" className="mt-5 flex flex-wrap items-center justify-between gap-3 text-sm">
    <span>Trang {page} / {Math.max(1, Math.ceil(total / size))} · {total} bản ghi</span>
    <div className="flex gap-2"><Button className="min-h-11" variant="outline" disabled={page <= 1} onClick={() => onPage(page - 1)}>Trang trước</Button>
      <Button className="min-h-11" variant="outline" disabled={page * size >= total} onClick={() => onPage(page + 1)}>Trang tiếp</Button></div>
  </nav>;
}
