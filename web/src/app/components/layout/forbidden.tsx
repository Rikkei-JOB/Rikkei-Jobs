import Link from "next/link"
import { ShieldX } from "lucide-react"
import { Button } from "@/app/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/app/components/ui/card"
import { ROLE_HOME, ROLE_LABEL } from "@/config/roles"
import type { Role } from "@/interface/auth"
import { SwitchAccountButton } from "@/app/components/auth/switch-account-button"

/** Trang 403: đã đăng nhập nhưng mở trang của vai trò khác (ví dụ ứng viên gõ /hr/jobs). */
export function Forbidden({ role, allowSwitchAccount = false }: { role: Role; allowSwitchAccount?: boolean }) {
  return (
    <div className="flex justify-center pt-8">
      <Card className="w-full max-w-md" data-testid="forbidden">
        <CardHeader>
          <ShieldX className="mb-1 size-8 text-destructive" aria-hidden />
          <CardTitle className="text-base">403 · Không có quyền truy cập</CardTitle>
          <CardDescription>
            Trang này không dành cho vai trò {ROLE_LABEL[role]}. Nếu bạn cho rằng đây là nhầm lẫn, hãy liên hệ cán bộ
            tuyển dụng.
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-3">
          <Button render={<Link href={ROLE_HOME[role]} />} nativeButton={false}>
            Về trang chủ của tôi
          </Button>
          {allowSwitchAccount && <SwitchAccountButton />}
        </CardContent>
      </Card>
    </div>
  )
}
