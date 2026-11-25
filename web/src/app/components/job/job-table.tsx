"use client"

import Link from "next/link"
import { useRouter } from "next/navigation"
import type { JobSummary } from "@/interface/job"
import { JobStatusBadge } from "@/app/components/job/job-status-badge"
import { JobRowMenu, type JobRowFeedback } from "@/app/components/job/job-row-menu"
import { deadlineHint } from "@/app/components/job/job-deadline"
import { LEVEL_LABEL, formatDate } from "@/app/components/job/job-labels"

interface JobTableProps {
  items: JobSummary[]
  companyLocked?: boolean
  onFeedback?: (feedback: JobRowFeedback | null) => void
}

/** Bảng JD của HR; bấm một dòng (hoặc liên kết ở tiêu đề, cho bàn phím) mở /hr/jobs/{id}. */
export function JobTable({ items, companyLocked = false, onFeedback }: JobTableProps) {
  const router = useRouter()
  if (items.length === 0) {
    return <p className="rounded-md border border-dashed py-10 text-center text-xs text-muted-foreground">Chưa có JD nào</p>
  }
  return (
    <div className="overflow-x-auto rounded-md border">
      <table className="w-full text-left text-xs">
        <thead className="bg-muted/50 text-muted-foreground">
          <tr>
            <th className="px-3 py-2 font-medium">Tiêu đề</th>
            <th className="px-3 py-2 font-medium">Cấp độ</th>
            <th className="px-3 py-2 font-medium">Hạn nộp</th>
            <th className="px-3 py-2 font-medium">Trạng thái</th>
            <th className="px-3 py-2 font-medium">Cập nhật</th>
            <th className="w-10 px-3 py-2 font-medium">
              <span className="sr-only">Thao tác</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {items.map((job) => {
            const href = `/hr/jobs/${job.id}`
            const hint = job.deadline ? deadlineHint(job.deadline) : null
            return (
              <tr key={job.id} onClick={() => router.push(href)} className="cursor-pointer border-t hover:bg-muted/40 focus-within:bg-muted/40">
                <td className="max-w-72 px-3 py-2">
                  {/* stopPropagation: không để onClick của dòng điều hướng lần nữa. */}
                  <Link href={href} onClick={(e) => e.stopPropagation()} className="font-medium text-primary hover:underline focus-visible:underline focus-visible:outline-none">
                    {job.title}
                  </Link>
                  {job.status === "REJECTED" && job.rejectReason ? (
                    <p className="truncate text-[11px] text-red-600" title={job.rejectReason}>
                      Lý do trả lại: {job.rejectReason}
                    </p>
                  ) : (
                    <p className="text-[11px] text-muted-foreground">
                      {job.requiredSkillCount > 0 ? `${job.requiredSkillCount} kỹ năng bắt buộc` : "Chưa có kỹ năng bắt buộc"}
                    </p>
                  )}
                </td>
                <td className="px-3 py-2">{job.level ? LEVEL_LABEL[job.level] : "—"}</td>
                <td className="px-3 py-2">
                  {formatDate(job.deadline)}
                  {hint ? <span className={hint.urgent ? "text-red-600" : "text-muted-foreground"}> · {hint.text}</span> : null}
                </td>
                <td className="px-3 py-2">
                  <JobStatusBadge status={job.status} />
                </td>
                <td className="px-3 py-2">{formatDate(job.updatedAt)}</td>
                <td className="px-3 py-2">
                  <JobRowMenu job={job} companyLocked={companyLocked} onFeedback={onFeedback} />
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
