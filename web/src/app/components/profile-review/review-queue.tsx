"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { Users } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { Input } from "@/app/components/ui/input";
import { Alert } from "@/app/components/ui/alert";
import { getErrorMessage } from "@/services/api";
import { profileReviewService, reviewHref, type QueueFilters } from "@/services/profile-review.service";
import { TaskBanner } from "@/app/components/ui/task-banner";

export function ReviewQueue({ filters }: { filters: QueueFilters }) {
  const router = useRouter();
  const queue = useQuery({
    queryKey: ["profile-reviews", filters],
    queryFn: ({ signal }) => profileReviewService.queue(filters, signal),
    refetchOnMount: "always",
  });
  const data = queue.data;

  return (
    <section className="ptit-page max-w-6xl">
      <TaskBanner
        eyebrow="Kiểm duyệt hồ sơ công khai"
        title="Hồ sơ chờ duyệt"
        description="Rà soát thông tin tự khai và phê duyệt trước khi công khai hồ sơ năng lực cho nhà tuyển dụng."
        illustration="applications"
        compact
      />

      <form
        className="ptit-filter-bar flex flex-wrap items-end gap-4"
        onSubmit={(event) => {
          event.preventDefault();
          const values = new FormData(event.currentTarget);
          const field = (name: string) => String(values.get(name) ?? "").trim();
          router.push(reviewHref({ q: field("q"), classCode: field("classCode"), courseName: field("courseName"), page: 1 }));
        }}
      >
        {([
          ["q", "Tên ứng viên"],
          ["classCode", "Lớp"],
          ["courseName", "Khóa học"],
        ] as const).map(([field, label]) => (
          <label key={field} className="min-w-[180px] flex-1 space-y-1.5 text-sm font-semibold text-heading">
            {label}
            <Input name={field} defaultValue={filters[field]} />
          </label>
        ))}
        <div className="flex items-center gap-3">
          <Button type="submit">Lọc hồ sơ</Button>
          <Link href="/staff/profile-reviews" className="inline-flex min-h-10 items-center px-2 text-sm font-semibold text-primary underline-offset-4 hover:underline">
            Xóa bộ lọc
          </Link>
        </div>
      </form>

      {queue.isPending ? (
        <p role="status" className="text-sm text-muted-foreground">Đang tải hàng đợi...</p>
      ) : queue.isError ? (
        <Alert variant="destructive" className="flex items-center justify-between gap-2">
          <span>{getErrorMessage(queue.error)}</span>
          <Button variant="outline" size="sm" onClick={() => queue.refetch()}>
            Thử lại
          </Button>
        </Alert>
      ) : (
        data && (
          <>
            {data.items.length ? (
              <div className="ptit-table-wrap overflow-x-auto">
                <table className="ptit-table whitespace-nowrap">
                  <thead>
                    <tr>
                      {["Ứng viên", "Lớp", "Khóa học", "Ngày gửi duyệt", "Số lần bị từ chối"].map((label) => (
                        <th key={label}>{label}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {data.items.map((row, index) => (
                      <tr key={row.userId}>
                        <td>
                          <Link className="font-bold text-primary hover:underline" href={reviewHref(filters, row.userId, index)}>
                            {row.fullName}
                          </Link>
                        </td>
                        <td>{row.classCode || "—"}</td>
                        <td>{row.courseName || "—"}</td>
                        <td className="text-muted-foreground">
                          {row.consentAt ? new Date(row.consentAt).toLocaleString("vi-VN") : "—"}
                        </td>
                        <td>
                          <span className="inline-flex min-w-6 items-center justify-center rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold text-heading">
                            {row.rejectCount}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div role="status" className="ptit-surface px-6 py-12 text-center">
                <Users className="mx-auto mb-3 size-9 text-primary" aria-hidden="true" />
                <p className="text-sm font-medium text-heading">Không có hồ sơ chờ duyệt. Đã xử lý hết hồ sơ theo bộ lọc này.</p>
              </div>
            )}
            <nav aria-label="Phân trang hồ sơ" className="flex items-center justify-between gap-4 pt-2 text-sm">
              <span className="text-muted-foreground">
                Trang {filters.page} · <strong className="font-semibold text-heading">{data.total}</strong> hồ sơ
              </span>
              <div className="flex items-center gap-3">
                {filters.page > 1 && (
                  <Link className="font-semibold text-primary hover:underline" href={reviewHref({ ...filters, page: filters.page - 1 })}>
                    Trang trước
                  </Link>
                )}
                {filters.page * data.size < data.total && (
                  <Link className="font-semibold text-primary hover:underline" href={reviewHref({ ...filters, page: filters.page + 1 })}>
                    Trang sau
                  </Link>
                )}
              </div>
            </nav>
          </>
        )
      )}
    </section>
  );
}
