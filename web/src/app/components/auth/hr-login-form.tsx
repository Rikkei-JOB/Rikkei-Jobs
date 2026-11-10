"use client"

import * as React from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useForm } from "react-hook-form"
import { Alert } from "@/app/components/ui/alert"
import { Input } from "@/app/components/ui/input"
import { FormField } from "@/app/components/auth/form-field"
import { PasswordInput } from "@/app/components/auth/password-input"
import { SubmitButton } from "@/app/components/auth/submit-button"
import { ROLE_HOME } from "@/config/roles"
import { zodResolver } from "@/helper/zod-resolver"
import { useAuthStore } from "@/hooks/use-auth-store"
import { hrLoginSchema, type HrLoginValues } from "@/schemas/auth.schema"
import { getErrorMessage } from "@/services/api"
import { authService } from "@/services/auth.service"

/** Đăng nhập HR doanh nghiệp bằng email + mật khẩu do cán bộ cấp. */
export function HrLoginForm() {
  const router = useRouter()
  const setSession = useAuthStore((s) => s.setSession)
  const [serverError, setServerError] = React.useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<HrLoginValues>({
    resolver: zodResolver(hrLoginSchema),
    defaultValues: { email: "", password: "" },
  })

  const onSubmit = handleSubmit(async (values) => {
    setServerError(null)
    try {
      const result = await authService.login(values)
      setSession(result.accessToken, { ...result.user, mustChangePassword: result.mustChangePassword })
      // Lần đầu đăng nhập HR bị bắt đổi mật khẩu trước khi vào hệ thống.
      router.replace(result.mustChangePassword ? "/change-password" : ROLE_HOME[result.user.role])
    } catch (error) {
      setServerError(getErrorMessage(error))
    }
  })

  return (
    <form onSubmit={onSubmit} noValidate className="space-y-3">
      {serverError ? <Alert variant="destructive">{serverError}</Alert> : null}
      <FormField label="Email" htmlFor="hr-email" error={errors.email?.message}>
        <Input
          id="hr-email"
          type="email"
          autoComplete="username"
          placeholder="hr@congty.com"
          aria-invalid={errors.email ? true : undefined}
          {...register("email")}
        />
      </FormField>
      <FormField label="Mật khẩu" htmlFor="hr-password" error={errors.password?.message}>
        <PasswordInput
          id="hr-password"
          autoComplete="current-password"
          aria-invalid={errors.password ? true : undefined}
          {...register("password")}
        />
      </FormField>
      <div className="text-right">
        <Link href="/forgot-password" className="text-xs/relaxed text-primary underline-offset-4 hover:underline">
          Quên mật khẩu?
        </Link>
      </div>
      <SubmitButton loading={isSubmitting}>Đăng nhập</SubmitButton>
    </form>
  )
}
