"use client"

import * as React from "react"
import Link from "next/link"
import { useForm } from "react-hook-form"
import { AuthCard } from "@/app/components/auth/auth-card"
import { FormField } from "@/app/components/auth/form-field"
import { SubmitButton } from "@/app/components/auth/submit-button"
import { Alert } from "@/app/components/ui/alert"
import { Input } from "@/app/components/ui/input"
import { zodResolver } from "@/helper/zod-resolver"
import { forgotPasswordSchema, type ForgotPasswordValues } from "@/schemas/auth.schema"
import { getErrorMessage } from "@/services/api"
import { authService } from "@/services/auth.service"

export default function ForgotPasswordPage() {
  const [sent, setSent] = React.useState(false)
  const [serverError, setServerError] = React.useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: "" },
  })

  const onSubmit = handleSubmit(async ({ email }) => {
    setServerError(null)
    try {
      await authService.forgotPassword(email)
      setSent(true)
    } catch (error) {
      setServerError(getErrorMessage(error))
    }
  })

  const footer = (
    <Link href="/login" className="text-primary underline-offset-4 hover:underline">
      Quay lại đăng nhập
    </Link>
  )

  if (sent) {
    return (
      <AuthCard title="Kiểm tra email của bạn" footer={footer}>
        <Alert variant="success">
          Nếu email có trong hệ thống, chúng tôi đã gửi hướng dẫn đặt lại mật khẩu. Liên kết có hiệu lực 30 phút và chỉ
          dùng được một lần.
        </Alert>
      </AuthCard>
    )
  }

  return (
    <AuthCard
      title="Quên mật khẩu"
      description="Dành cho tài khoản doanh nghiệp. Tài khoản LMS đặt lại mật khẩu trên LMS."
      footer={footer}
    >
      <form onSubmit={onSubmit} noValidate className="space-y-3">
        {serverError ? <Alert variant="destructive">{serverError}</Alert> : null}
        <FormField label="Email" htmlFor="forgot-email" error={errors.email?.message}>
          <Input
            id="forgot-email"
            type="email"
            autoComplete="email"
            placeholder="hr@congty.com"
            aria-invalid={errors.email ? true : undefined}
            {...register("email")}
          />
        </FormField>
        <SubmitButton loading={isSubmitting}>Gửi liên kết đặt lại</SubmitButton>
      </form>
    </AuthCard>
  )
}
