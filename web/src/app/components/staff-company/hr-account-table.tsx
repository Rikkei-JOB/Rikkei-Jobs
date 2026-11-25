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

/** Bảng tài khoản HR của một công ty; mỗi dòng có nút Khoá hoặc Mở khoá theo trạng thái. */
export function HrAccountTable({ accounts, onLock, onUnlock }: HrAccountTableProps) {
  if (accounts.length === 0) {
    return <p className="rounded-md border border-dashed py-8 text-center text-xs text-muted-foreground">Công ty chưa có tài khoản HR</p>
  }
  return (
    <div className="overflow-x-auto rounded-xl border bg-white">
      <table className="w-full text-left text-xs">
        <thead className="bg-muted/50 text-muted-foreground">
          <tr>
            <th className="px-3 py-2 font-medium">Tài khoản</th>
            <th className="px-3 py-2 font-medium">Trạng thái</th>
            <th className="px-3 py-2 text-right font-medium">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {accounts.map((account) => (
            <tr key={account.userId} className="border-t align-top">
              <td className="px-3 py-2">
                <div className="font-bold">{account.fullName}</div>
                <div className="break-all text-muted-foreground">{account.email}</div>
              </td>
              <td className="px-3 py-2">
                {account.status === "LOCKED" ? (
                  <div className="space-y-0.5">
                    <span className="inline-block rounded-full bg-destructive/10 px-2 py-0.5 text-destructive">Đã khoá</span>
                    {account.lockedAt ? <div className="text-muted-foreground">Khoá lúc {formatDateTime(account.lockedAt)}</div> : null}
                    {account.lockedReason ? <div className="text-muted-foreground">Lý do: {account.lockedReason}</div> : null}
                  </div>
                ) : (
                  <div className="space-y-0.5">
                    <span className="inline-block rounded-full bg-primary/10 px-2 py-0.5 text-primary">Hoạt động</span>
                    {account.tempLockedUntil ? (
                      <div className="text-muted-foreground">Tạm khoá do sai mật khẩu đến {formatTime(account.tempLockedUntil)}</div>
                    ) : null}
                  </div>
                )}
              </td>
              <td className="px-3 py-2 text-right">
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
