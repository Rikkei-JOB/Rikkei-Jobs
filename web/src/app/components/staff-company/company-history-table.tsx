"use client"

import * as React from "react"
import { Alert } from "@/app/components/ui/alert"
import { Button } from "@/app/components/ui/button"
import { formatDateTime } from "@/app/components/job/job-labels"
import { staffName } from "@/app/components/staff-company/company-lock-banner"
import { useCompanyHistory } from "@/hooks/use-staff-companies"
import type { CompanyActionType } from "@/interface/company"
import { getErrorMessage } from "@/services/api"

const ACTION_LABEL: Record<CompanyActionType, string> = {
  LOCK: "Khoá công ty",
  UNLOCK: "Mở khoá",
  APPROVE_JD: "Duyệt JD",
  REJECT_JD: "Trả lại JD",
  CLOSE_JD: "Đóng JD",
}

/** Cùng hệ màu với nhãn trạng thái JD (job-labels STATUS_CLASS) để một trang chỉ có một tông. */
const ACTION_CLASS: Record<CompanyActionType, string> = {
  LOCK: "bg-red-100 text-red-700",
  UNLOCK: "bg-green-100 text-green-700",
  APPROVE_JD: "bg-green-100 text-green-700",
  REJECT_JD: "bg-yellow-100 text-yellow-800",
  CLOSE_JD: "bg-gray-300 text-gray-800",
}

interface CompanyHistoryTableProps {
  companyId: string
  page: number
  onPage: (page: number) => void
}

/** Tab "Lịch sử thao tác": Thời điểm / Cán bộ / Hành động / JD / Lý do, mới nhất trước, có phân trang. */
export function CompanyHistoryTable({ companyId, page, onPage }: CompanyHistoryTableProps) {
  const { data, error, isPending, isFetching, isPlaceholderData, refetch } = useCompanyHistory(companyId, page)
  const lastPage = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1

  // Trang hiện tại hết dữ liệu (người khác vừa xoá, hoặc vừa đổi bộ lọc): lùi về trang cuối còn dữ liệu.
  React.useEffect(() => {
    if (data && page > 1 && data.items.length === 0) onPage(lastPage)
  }, [data, page, lastPage, onPage])

  if (error) {
    return (
      <Alert variant="destructive" className="flex flex-wrap items-center justify-between gap-2">
        <span>{getErrorMessage(error)}</span>
        <Button variant="outline" size="sm" onClick={() => refetch()}>
          Thử lại
        </Button>
      </Alert>
    )
  }
  if (isPending || !data) return <p role="status" className="text-sm text-muted-foreground">Đang tải...</p>
  if (data.total === 0) {
    return (
      <p className="rounded-xl border border-dashed bg-card px-6 py-10 text-center text-sm text-muted-foreground">
        Chưa có thao tác nào trên doanh nghiệp này.
      </p>
    )
  }

  return (
    <div className={isFetching && isPlaceholderData ? "opacity-60" : undefined}>
      <div className="overflow-x-auto rounded-xl border bg-card">
        <table className="w-full text-left text-sm">
          <thead className="bg-[#F8FAFC] text-xs text-muted-foreground">
            <tr>
              <th className="px-4 py-3 font-semibold whitespace-nowrap">Thời điểm</th>
              <th className="px-4 py-3 font-semibold">Cán bộ</th>
              <th className="px-4 py-3 font-semibold">Hành động</th>
              <th className="px-4 py-3 font-semibold">JD</th>
              <th className="px-4 py-3 font-semibold">Lý do</th>
            </tr>
          </thead>
          <tbody>
            {data.items.map((item) => (
              <tr key={item.id} className="border-t align-top">
                <td className="px-4 py-3 whitespace-nowrap">{formatDateTime(item.createdAt, true)}</td>
                <td className="px-4 py-3">{staffName(item.staff)}</td>
                <td className="px-4 py-3">
                  <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium whitespace-nowrap ${ACTION_CLASS[item.action]}`}>
                    {ACTION_LABEL[item.action]}
                  </span>
                </td>
                <td className="px-4 py-3 text-heading">
                  {item.job ? (item.job.title ?? <span className="font-normal text-muted-foreground">JD không còn</span>) : "—"}
                </td>
                <td className="max-w-xs px-4 py-3 break-words whitespace-pre-line text-muted-foreground">{item.reason ?? "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="mt-3 flex items-center justify-between text-xs text-muted-foreground">
        <span>
          Trang {data.page}/{lastPage} · {data.total} thao tác
        </span>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" disabled={page <= 1} onClick={() => onPage(page - 1)}>
            Trước
          </Button>
          <Button variant="outline" size="sm" disabled={page >= lastPage} onClick={() => onPage(page + 1)}>
            Sau
          </Button>
        </div>
      </div>
    </div>
  )
}
