import Link from "next/link"
import type { CompanyStatus, StaffCompanyItem } from "@/interface/company"

const STATUS_LABEL: Record<CompanyStatus, string> = { ACTIVE: "Hoạt động", LOCKED: "Đã khoá" }

/** Bảng công ty của cán bộ; tên công ty dẫn tới trang chi tiết. */
export function CompanyTable({ items }: { items: StaffCompanyItem[] }) {
  if (items.length === 0) {
    return <p className="rounded-md border border-dashed py-10 text-center text-xs text-muted-foreground">Không tìm thấy công ty phù hợp</p>
  }
  return (
    <div className="overflow-x-auto rounded-md border">
      <table className="w-full text-left text-xs">
        <thead className="bg-muted/50 text-muted-foreground">
          <tr>
            <th className="px-3 py-2 font-medium">Tên công ty</th>
            <th className="px-3 py-2 font-medium">Mã số thuế</th>
            <th className="px-3 py-2 text-right font-medium">Số HR</th>
            <th className="px-3 py-2 text-right font-medium">Số JD</th>
            <th className="px-3 py-2 font-medium">Trạng thái</th>
          </tr>
        </thead>
        <tbody>
          {items.map((company) => (
            <tr key={company.id} className="border-t">
              <td className="px-3 py-2">
                <Link href={`/staff/companies/${company.id}`} className="font-medium text-primary hover:underline">
                  {company.name}
                </Link>
              </td>
              <td className="px-3 py-2">{company.taxCode}</td>
              <td className="px-3 py-2 text-right">{company.hrCount}</td>
              <td className="px-3 py-2 text-right">{company.jdCount}</td>
              <td className="px-3 py-2">
                <span
                  className={
                    company.status === "LOCKED"
                      ? "rounded-full bg-destructive/10 px-2 py-0.5 text-destructive"
                      : "rounded-full bg-primary/10 px-2 py-0.5 text-primary"
                  }
                >
                  {STATUS_LABEL[company.status]}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
