"use client"

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

/** Khoá công ty đỏ, Mở khoá xanh lá, Duyệt JD xanh lá, Trả lại JD vàng, Đóng JD xám. */
const ACTION_CLASS: Record<CompanyActionType, string> = {
  LOCK: "bg-red-100 text-red-700",
  UNLOCK: "bg-green-100 text-green-700",
  APPROVE_JD: "bg-green-100 text-green-700",
  REJECT_JD: "bg-yellow-100 text-yellow-800",
  CLOSE_JD: "bg-gray-200 text-gray-700",
}

interface CompanyHistoryTableProps {
  companyId: string
  page: number
  onPage: (page: number) => void
  enabled?: boolean
}

/** Tab "Lịch sử thao tác": Thời điểm / Cán bộ / Hành động / JD / Lý do, mới nhất trước, có phân trang. */
export function CompanyHistoryTable({ companyId, page, onPage, enabled = true }: CompanyHistoryTableProps) {
  const { data, error, isPending, isFetching, isPlaceholderData, refetch } = useCompanyHistory(companyId, page, enabled)

  if (error) {
    return (
      <Alert variant="destructive" className="flex items-center justify-between gap-2">
        <span>{getErrorMessage(error)}</span>
        <Button variant="outline" size="sm" onClick={() => refetch()}>
          Thử lại
        </Button>
      </Alert>
    )
  }
  if (isPending || !data) return <p className="text-xs text-muted-foreground">Đang tải...</p>
  if (data.total === 0) {
    return <p className="rounded-md border border-dashed bg-white py-8 text-center text-xs text-muted-foreground">Chưa có thao tác nào.</p>
  }

  const lastPage = Math.max(1, Math.ceil(data.total / data.size))
  return (
    <div className={isFetching && isPlaceholderData ? "opacity-60" : undefined}>
      <div className="overflow-x-auto rounded-xl border bg-white">
        <table className="w-full text-left text-xs">
          <thead className="bg-muted/50 text-muted-foreground">
            <tr>
              <th className="px-3 py-2 font-medium whitespace-nowrap">Thời điểm</th>
              <th className="px-3 py-2 font-medium">Cán bộ</th>
              <th className="px-3 py-2 font-medium">Hành động</th>
              <th className="px-3 py-2 font-medium">JD</th>
              <th className="px-3 py-2 font-medium">Lý do</th>
            </tr>
          </thead>
          <tbody>
            {data.items.map((item) => (
              <tr key={item.id} className="border-t align-top">
                <td className="px-3 py-2 whitespace-nowrap">{formatDateTime(item.createdAt, true)}</td>
                <td className="px-3 py-2">{staffName(item.staff)}</td>
                <td className="px-3 py-2">
                  <span className={`inline-block rounded-full px-2 py-0.5 whitespace-nowrap ${ACTION_CLASS[item.action]}`}>
                    {ACTION_LABEL[item.action]}
                  </span>
                </td>
                <td className="px-3 py-2">
                  {item.job ? (item.job.title ?? <span className="text-muted-foreground">JD không còn</span>) : "—"}
                </td>
                <td className="max-w-xs px-3 py-2 break-words whitespace-pre-line">{item.reason ?? "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="mt-3 flex items-center justify-between text-xs text-muted-foreground">
        <span>
          Trang {data.page}/{lastPage}
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
