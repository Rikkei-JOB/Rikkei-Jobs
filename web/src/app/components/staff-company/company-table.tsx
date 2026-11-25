"use client"

import Link from "next/link"
import { useRouter } from "next/navigation"
import { CompanyMark } from "@/app/components/company/company-mark"
import type { CompanyStatus, StaffCompanyItem } from "@/interface/company"

const STATUS_LABEL: Record<CompanyStatus, string> = { ACTIVE: "Hoạt động", LOCKED: "Đã khoá" }

/** Bảng công ty của cán bộ; bấm cả dòng (hoặc tên công ty bằng bàn phím) mở trang chi tiết. */
export function CompanyTable({ items }: { items: StaffCompanyItem[] }) {
  const router = useRouter()
  if (items.length === 0) {
    return <p className="rounded-md border border-dashed py-10 text-center text-xs text-muted-foreground">Không tìm thấy công ty phù hợp</p>
  }
  return (
    <div className="overflow-x-auto rounded-xl border bg-white">
      <table className="w-full text-left text-xs">
        <thead className="bg-muted/50 text-muted-foreground">
          <tr>
            <th className="px-3 py-2 font-medium">Công ty</th>
            <th className="px-3 py-2 font-medium">Mã số thuế</th>
            <th className="px-3 py-2 text-right font-medium">HR</th>
            <th className="px-3 py-2 text-right font-medium">JD</th>
            <th className="px-3 py-2 font-medium">Trạng thái</th>
          </tr>
        </thead>
        <tbody>
          {items.map((company) => {
            const sub = [company.city, company.size].filter(Boolean).join(" · ")
            const href = `/staff/companies/${company.id}`
            return (
              <tr key={company.id} className="cursor-pointer border-t hover:bg-muted/40" onClick={() => router.push(href)}>
                <td className="px-3 py-2">
                  <div className="flex min-w-0 max-w-xs items-center gap-2">
                    <CompanyMark name={company.name} logoUrl={company.logoUrl} size={24} />
                    <div className="min-w-0">
                      <Link
                        href={href}
                        onClick={(e) => e.stopPropagation()}
                        className="block truncate font-bold hover:underline"
                        title={company.name}
                      >
                        {company.name}
                      </Link>
                      {sub ? <div className="truncate text-muted-foreground">{sub}</div> : null}
                    </div>
                  </div>
                </td>
                <td className="px-3 py-2">{company.taxCode}</td>
                <td className="px-3 py-2 text-right">{company.hrCount}</td>
                <td className="px-3 py-2 text-right">{company.jdCount}</td>
                <td className="px-3 py-2">
                  <span
                    className={
                      company.status === "LOCKED"
                        ? "rounded-full bg-destructive/10 px-2 py-0.5 text-destructive"
                        : "rounded-full bg-green-100 px-2 py-0.5 text-green-700"
                    }
                  >
                    {STATUS_LABEL[company.status]}
                  </span>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
