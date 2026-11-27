import { Button } from "@/app/components/ui/button"
import type { StaffHrAccount } from "@/interface/company"

const VN_TIME_ZONE = "Asia/Ho_Chi_Minh"

function formatDateTime(iso: string): string {
  return new Intl.DateTimeFormat("vi-VN", {
    timeZone: VN_TIME_ZONE,
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(iso))
}

function formatTime(iso: string): string {
  return new Intl.DateTimeFormat("vi-VN", { timeZone: VN_TIME_ZONE, hour: "2-digit", minute: "2-digit", hour12: false }).format(new Date(iso))
}

interface HrAccountTableProps {
  accounts: StaffHrAccount[]
  onLock: (account: StaffHrAccount) => void
  onUnlock: (account: StaffHrAccount) => void
}

/**
 * Bảng tài khoản HR của một công ty; mỗi dòng có nút Khoá hoặc Mở khoá theo khoá riêng (status). HR bị khoá theo
 * công ty (DN-07) có thêm nhãn "Khoá theo công ty"; nhãn này chỉ gỡ được bằng mở khoá công ty.
 */
export function HrAccountTable({ accounts, onLock, onUnlock }: HrAccountTableProps) {
  if (accounts.length === 0) {
    return (
      <p className="rounded-xl border border-dashed bg-card px-6 py-10 text-center text-sm text-muted-foreground">
        Công ty chưa có tài khoản HR nào.
      </p>
    )
  }
  return (
    <div className="overflow-x-auto rounded-xl border bg-card">
      <table className="w-full text-left text-sm">
        <thead className="bg-[#F8FAFC] text-xs text-muted-foreground">
          <tr>
            <th className="px-4 py-3 font-semibold">Tài khoản</th>
            <th className="px-4 py-3 font-semibold">Trạng thái</th>
            <th className="px-4 py-3 text-right font-semibold">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {accounts.map((account) => (
            <tr key={account.userId} className="border-t align-top">
              <td className="px-4 py-3">
                <div className="font-semibold text-heading">{account.fullName}</div>
                <div className="break-all text-xs text-muted-foreground">{account.email}</div>
              </td>
              <td className="px-4 py-3">
                <div className="space-y-1">
                  <div className="flex flex-wrap gap-1.5">
                    {account.companyLocked ? (
                      <span className="inline-block rounded-full border border-red-200 px-2 py-0.5 text-xs font-medium text-red-700">
                        Khoá theo công ty
                      </span>
                    ) : null}
                    {account.status === "LOCKED" ? (
                      <span className="inline-block rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700">Đã khoá</span>
                    ) : account.companyLocked ? null : (
                      // Bị khoá theo công ty thì không ghi "Hoạt động" (dễ hiểu nhầm là đăng nhập được).
                      <span className="inline-block rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700">Hoạt động</span>
                    )}
                  </div>
                  {account.status === "LOCKED" && account.lockedAt ? (
                    <div className="text-xs text-muted-foreground">Khoá lúc {formatDateTime(account.lockedAt)}</div>
                  ) : null}
                  {account.status === "LOCKED" && account.lockedReason ? (
                    <div className="text-xs break-words text-muted-foreground">Lý do: {account.lockedReason}</div>
                  ) : null}
                  {account.status !== "LOCKED" && account.tempLockedUntil ? (
                    <div className="text-xs text-muted-foreground">Tạm khoá do sai mật khẩu đến {formatTime(account.tempLockedUntil)}</div>
                  ) : null}
                </div>
              </td>
              <td className="px-4 py-3 text-right">
                {account.status === "LOCKED" ? (
                  <Button variant="outline" size="sm" onClick={() => onUnlock(account)}>
                    Mở khoá
                  </Button>
                ) : (
                  <Button variant="destructive" size="sm" onClick={() => onLock(account)}>
                    Khoá
                  </Button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
