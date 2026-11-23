"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { Button } from "@/app/components/ui/button";
import { Input } from "@/app/components/ui/input";
import { Alert } from "@/app/components/ui/alert";
import { getErrorMessage } from "@/services/api";
import { profileReviewService, reviewHref, type QueueFilters } from "@/services/profile-review.service";

export function ReviewQueue({ filters }: { filters: QueueFilters }) {
  const router = useRouter();
  const queue = useQuery({
    queryKey: ["profile-reviews", filters],
    queryFn: ({ signal }) => profileReviewService.queue(filters, signal),
    refetchOnMount: "always",
  });
  const data = queue.data;
  return <section className="mx-auto max-w-6xl space-y-4">
    <h1 className="text-xl font-semibold">Hồ sơ chờ duyệt</h1>
    <form className="flex flex-wrap items-end gap-3 rounded-lg border bg-card p-4" onSubmit={event => {
      event.preventDefault();
      const values = new FormData(event.currentTarget);
      const field = (name: string) => String(values.get(name) ?? "").trim();
      router.push(reviewHref({ q: field("q"), classCode: field("classCode"), courseName: field("courseName"), page: 1 }));
    }}>
      {([["q", "Tên ứng viên"], ["classCode", "Lớp"], ["courseName", "Khóa học"]] as const).map(([field, label]) =>
        <label key={field} className="space-y-1 text-sm">{label}<Input name={field} defaultValue={filters[field]} /></label>)}
      <Button type="submit">Lọc hồ sơ</Button>
      <Link href="/staff/profile-reviews" className="text-sm underline">Xóa bộ lọc</Link>
    </form>
    {queue.isPending ? <p role="status">Đang tải hàng đợi...</p> : queue.isError ?
      <Alert variant="destructive">{getErrorMessage(queue.error)} <Button variant="outline" onClick={() => queue.refetch()}>Thử lại</Button></Alert>
      : data && <>
        {data.items.length ? <div className="overflow-x-auto rounded-lg border bg-card"><table className="w-full whitespace-nowrap text-left text-sm">
          <thead className="border-b bg-muted/50"><tr>{["Ứng viên", "Lớp", "Khóa học", "Ngày gửi duyệt", "Số lần bị từ chối"].map(label =>
            <th key={label} className="px-4 py-3">{label}</th>)}</tr></thead>
          <tbody>{data.items.map((row, index) => <tr key={row.userId} className="border-b last:border-0">
            <td className="p-4"><Link className="font-medium text-primary underline" href={reviewHref(filters, row.userId, index)}>{row.fullName}</Link></td>
            <td className="p-4">{row.classCode || "—"}</td><td className="p-4">{row.courseName || "—"}</td>
            <td className="p-4">{row.consentAt ? new Date(row.consentAt).toLocaleString("vi-VN") : "—"}</td>
            <td className="p-4">{row.rejectCount}</td>
          </tr>)}</tbody>
        </table></div> : <p role="status" className="rounded-lg border bg-card p-5">Không có hồ sơ chờ duyệt. Đã xử lý hết hồ sơ theo bộ lọc này.</p>}
        <nav aria-label="Phân trang hồ sơ" className="flex items-center gap-4 text-sm">
          {filters.page > 1 && <Link className="underline" href={reviewHref({ ...filters, page: filters.page - 1 })}>Trang trước</Link>}
          <span>Trang {filters.page} · {data.total} hồ sơ</span>
          {filters.page * data.size < data.total && <Link className="underline" href={reviewHref({ ...filters, page: filters.page + 1 })}>Trang sau</Link>}
        </nav>
      </>}
  </section>;
}
