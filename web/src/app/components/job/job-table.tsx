"use client"

import Link from "next/link"
import { useRouter } from "next/navigation"
import type { JobSummary } from "@/interface/job"
import { JobStatusBadge } from "@/app/components/job/job-status-badge"
import { LEVEL_LABEL, formatDate } from "@/app/components/job/job-labels"

/** Bảng JD của HR; bấm một dòng (hoặc liên kết ở tiêu đề, cho bàn phím) mở /hr/jobs/{id}. */
export function JobTable({ items }: { items: JobSummary[] }) {
  const router = useRouter()
  if (items.length === 0) {
    return <div className="hr-surface py-12 text-center"><h2 className="text-xl font-semibold">Chưa có tin tuyển dụng</h2><p className="hr-description">Tạo tin để bắt đầu kết nối với ứng viên.</p></div>
  }
  return (
    <div className="overflow-x-auto rounded-xl border bg-background" role="region" aria-label="Danh sách tin tuyển dụng" tabIndex={0}>
      <table className="hr-table min-w-[640px]">
        <caption className="sr-only">Tin tuyển dụng của công ty</caption>
        <thead className="bg-muted/50 text-muted-foreground">
          <tr>
            <th className="px-3 py-2 font-medium">Tiêu đề</th>
            <th className="px-3 py-2 font-medium">Cấp độ</th>
            <th className="px-3 py-2 font-medium">Hạn nộp</th>
            <th className="px-3 py-2 font-medium">Trạng thái</th>
            <th className="px-3 py-2 font-medium">Cập nhật</th>
          </tr>
        </thead>
        <tbody>
          {items.map((job) => {
            const href = `/hr/jobs/${job.id}`
            return (
              <tr key={job.id} onClick={() => router.push(href)} className="cursor-pointer border-t hover:bg-muted/40 focus-within:bg-muted/40">
                <td className="px-3 py-2 font-medium text-primary">
                  {/* stopPropagation: không để onClick của dòng điều hướng lần nữa. */}
                  <Link href={href} onClick={(e) => e.stopPropagation()} className="hover:underline focus-visible:underline focus-visible:outline-none">
                    {job.title}
                  </Link>
                </td>
                <td className="px-3 py-2">{job.level ? LEVEL_LABEL[job.level] : "—"}</td>
                <td className="px-3 py-2">{formatDate(job.deadline)}</td>
                <td className="px-3 py-2">
                  <JobStatusBadge status={job.status} />
                </td>
                <td className="px-3 py-2">{formatDate(job.updatedAt)}</td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
