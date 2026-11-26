"use client"

import { Suspense } from "react"
import { usePathname, useRouter, useSearchParams } from "next/navigation"
import { Building2, GraduationCap, LogIn } from "lucide-react"
import { AuthCard } from "@/app/components/auth/auth-card"
import { HrLoginForm } from "@/app/components/auth/hr-login-form"
import { LmsLoginPanel } from "@/app/components/auth/lms-login-panel"
import { LoginShowcase } from "@/app/components/auth/login-showcase"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/app/components/ui/tabs"
import { useRedirectIfAuthenticated } from "@/hooks/use-auth-guard"

type LoginTab = "lms" | "hr"

export default function LoginPage() {
  useRedirectIfAuthenticated()

  return (
    <div className="grid w-full max-w-[1080px] items-center gap-12 lg:grid-cols-[minmax(0,1fr)_440px]">
      <LoginShowcase />
      <div className="flex justify-center lg:justify-end">
        <AuthCard title="Đăng nhập" description="Chọn cách đăng nhập phù hợp với tài khoản của bạn." icon={LogIn}>
          <Suspense fallback={<LoginTabs tab="lms" />}>
            <LoginTabsFromUrl />
          </Suspense>
        </AuthCard>
      </div>
    </div>
  )
}

/** Tab mở theo ?tab=hr (liên kết "Đăng nhập doanh nghiệp"), đổi tab cập nhật URL để tải lại vẫn giữ. */
function LoginTabsFromUrl() {
  const params = useSearchParams()
  const router = useRouter()
  const pathname = usePathname()
  const tab: LoginTab = params.get("tab") === "hr" ? "hr" : "lms"
  return (
    <LoginTabs
      tab={tab}
      onChange={(next) => {
        const query = new URLSearchParams(params)
        if (next === "hr") query.set("tab", "hr")
        else query.delete("tab")
        router.replace(query.size ? `${pathname}?${query}` : pathname, { scroll: false })
      }}
    />
  )
}

function LoginTabs({ tab, onChange }: { tab: LoginTab; onChange?: (tab: LoginTab) => void }) {
  return (
    <Tabs value={tab} onValueChange={(value) => onChange?.(value as LoginTab)}>
      <TabsList>
        <TabsTrigger value="lms" className="gap-2"><GraduationCap className="size-4" aria-hidden="true" />Qua LMS</TabsTrigger>
        <TabsTrigger value="hr" className="gap-2"><Building2 className="size-4" aria-hidden="true" />Doanh nghiệp</TabsTrigger>
      </TabsList>
      <TabsContent value="lms">
        <LmsLoginPanel />
      </TabsContent>
      <TabsContent value="hr">
        <HrLoginForm />
      </TabsContent>
    </Tabs>
  )
}
