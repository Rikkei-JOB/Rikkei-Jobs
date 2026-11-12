"use client"

import * as React from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useForm } from "react-hook-form"
import { AuthCard } from "@/app/components/auth/auth-card"
import { FormField } from "@/app/components/auth/form-field"
import { PasswordInput } from "@/app/components/auth/password-input"
import { SubmitButton } from "@/app/components/auth/submit-button"
import { Alert } from "@/app/components/ui/alert"
import { ROLE_HOME } from "@/config/roles"
import { pathAfterLogin } from "@/helper/next-path"
import { zodResolver } from "@/helper/zod-resolver"
import { useAuthGuard } from "@/hooks/use-auth-guard"
import { useAuthStore } from "@/hooks/use-auth-store"
import { changePasswordSchema, type ChangePasswordValues } from "@/schemas/auth.schema"
import { getErrorMessage } from "@/services/api"
import { authService } from "@/services/auth.service"

export default function ChangePasswordPage() {
  const router = useRouter()
  const allowed = useAuthGuard({ allowMustChangePassword: true })
  const user = useAuthStore((s) => s.user)
  const setSession = useAuthStore((s) => s.setSession)
  const [serverError, setServerError] = React.useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { oldPassword: "", newPassword: "", confirmPassword: "" },
  })

  if (!allowed || !user) return null

  const forced = user.mustChangePassword

  const onSubmit = handleSubmit(async ({ oldPassword, newPassword }) => {
    setServerError(null)
    try {
      const { accessToken } = await authService.changePassword({ oldPassword, newPassword })
      setSession(accessToken, { ...user, mustChangePassword: false })
      router.replace(pathAfterLogin(user.role))
    } catch (error) {
      setServerError(getErrorMessage(error))
    }
  })

  return (
    <AuthCard
      title="Đổi mật khẩu"
      description={forced ? "Đây là lần đăng nhập đầu tiên. Bạn cần đổi mật khẩu trước khi tiếp tục." : undefined}
      footer={
        forced ? null : (
          <Link href={ROLE_HOME[user.role]} className="text-primary underline-offset-4 hover:underline">
            Quay lại
          </Link>
        )
      }
    >
      <form onSubmit={onSubmit} noValidate className="space-y-3">
        {serverError ? <Alert variant="destructive">{serverError}</Alert> : null}
        <FormField label="Mật khẩu hiện tại" htmlFor="cp-old" error={errors.oldPassword?.message}>
          <PasswordInput
            id="cp-old"
            autoComplete="current-password"
            aria-invalid={errors.oldPassword ? true : undefined}
            {...register("oldPassword")}
          />
        </FormField>
        <FormField label="Mật khẩu mới" htmlFor="cp-new" error={errors.newPassword?.message}>
          <PasswordInput
            id="cp-new"
            autoComplete="new-password"
            aria-invalid={errors.newPassword ? true : undefined}
            {...register("newPassword")}
          />
        </FormField>
        <FormField label="Nhập lại mật khẩu mới" htmlFor="cp-confirm" error={errors.confirmPassword?.message}>
          <PasswordInput
            id="cp-confirm"
            autoComplete="new-password"
            aria-invalid={errors.confirmPassword ? true : undefined}
            {...register("confirmPassword")}
          />
        </FormField>
        <p className="text-xs/relaxed text-muted-foreground">Ít nhất 8 ký tự, gồm cả chữ và số.</p>
        <SubmitButton loading={isSubmitting}>Đổi mật khẩu</SubmitButton>
      </form>
    </AuthCard>
  )
}
