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
import { zodResolver } from "@/helper/zod-resolver"
import { resetPasswordSchema, type ResetPasswordValues } from "@/schemas/auth.schema"
import { ApiError, getErrorMessage } from "@/services/api"
import { authService } from "@/services/auth.service"

export default function ResetPasswordPage({ params }: { params: Promise<{ token: string }> }) {
  const { token } = React.use(params)
  const router = useRouter()
  const [done, setDone] = React.useState(false)
  const [expired, setExpired] = React.useState(false)
  const [serverError, setServerError] = React.useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { newPassword: "", confirmPassword: "" },
  })

  const onSubmit = handleSubmit(async ({ newPassword }) => {
    setServerError(null)
    try {
      await authService.resetPassword({ token, newPassword })
      setDone(true)
      setTimeout(() => router.replace("/login"), 2500)
    } catch (error) {
      if (error instanceof ApiError && error.code === "RESET_TOKEN_INVALID") {
        setExpired(true)
      } else {
        setServerError(getErrorMessage(error))
      }
    }
  })

  if (expired) {
    return (
      <AuthCard title="Liên kết đã hết hạn">
        <Alert variant="destructive">
          Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn (có hiệu lực 30 phút và chỉ dùng một lần).
        </Alert>
        <Link href="/forgot-password" className="block text-center text-xs/relaxed text-primary underline-offset-4 hover:underline">
          Yêu cầu liên kết mới
        </Link>
      </AuthCard>
    )
  }

  if (done) {
    return (
      <AuthCard title="Đã đặt lại mật khẩu">
        <Alert variant="success">Mật khẩu đã được cập nhật. Đang chuyển về trang đăng nhập...</Alert>
        <Link href="/login" className="block text-center text-xs/relaxed text-primary underline-offset-4 hover:underline">
          Đăng nhập ngay
        </Link>
      </AuthCard>
    )
  }

  return (
    <AuthCard title="Đặt lại mật khẩu" description="Nhập mật khẩu mới cho tài khoản của bạn.">
      <form onSubmit={onSubmit} noValidate className="space-y-3">
        {serverError ? <Alert variant="destructive">{serverError}</Alert> : null}
        <FormField label="Mật khẩu mới" htmlFor="reset-new" error={errors.newPassword?.message}>
          <PasswordInput
            id="reset-new"
            autoComplete="new-password"
            aria-invalid={errors.newPassword ? true : undefined}
            {...register("newPassword")}
          />
        </FormField>
        <FormField label="Nhập lại mật khẩu mới" htmlFor="reset-confirm" error={errors.confirmPassword?.message}>
          <PasswordInput
            id="reset-confirm"
            autoComplete="new-password"
            aria-invalid={errors.confirmPassword ? true : undefined}
            {...register("confirmPassword")}
          />
        </FormField>
        <p className="text-xs/relaxed text-muted-foreground">Ít nhất 8 ký tự, gồm cả chữ và số.</p>
        <SubmitButton loading={isSubmitting}>Đặt lại mật khẩu</SubmitButton>
      </form>
    </AuthCard>
  )
}
