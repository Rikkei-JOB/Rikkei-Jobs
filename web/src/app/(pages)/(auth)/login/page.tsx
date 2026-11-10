"use client"

import { AuthCard } from "@/app/components/auth/auth-card"
import { HrLoginForm } from "@/app/components/auth/hr-login-form"
import { LmsLoginPanel } from "@/app/components/auth/lms-login-panel"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/app/components/ui/tabs"
import { useRedirectIfAuthenticated } from "@/hooks/use-auth-guard"

export default function LoginPage() {
  useRedirectIfAuthenticated()

  return (
    <AuthCard title="Đăng nhập" description="Chọn cách đăng nhập phù hợp với tài khoản của bạn.">
      <Tabs defaultValue="lms">
        <TabsList>
          <TabsTrigger value="lms">Qua LMS</TabsTrigger>
          <TabsTrigger value="hr">Doanh nghiệp</TabsTrigger>
        </TabsList>
        <TabsContent value="lms">
          <LmsLoginPanel />
        </TabsContent>
        <TabsContent value="hr">
          <HrLoginForm />
        </TabsContent>
      </Tabs>
    </AuthCard>
  )
}
