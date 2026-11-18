"use client"

import * as React from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useQueryClient } from "@tanstack/react-query"
import { useForm } from "react-hook-form"
import { Alert } from "@/app/components/ui/alert"
import { Button } from "@/app/components/ui/button"
import { Card, CardContent } from "@/app/components/ui/card"
import { ConfirmDialog } from "@/app/components/job/confirm-dialog"
import { clearJobFormHandoff, peekJobFormHandoff, setJobFormHandoff, setJobListNotice } from "@/app/components/job/job-flash"
import { JobGeneralStep } from "@/app/components/job/job-general-step"
import { JobSkillsStep } from "@/app/components/job/job-skills-step"
import { JobStatusBadge } from "@/app/components/job/job-status-badge"
import { zodResolver } from "@/helper/zod-resolver"
import { useAuthStore } from "@/hooks/use-auth-store"
import { companyKeys } from "@/hooks/use-my-company"
import { jobKeys, useCloseJob, useSaveJob, useSubmitJob } from "@/hooks/use-my-jobs"
import { useSkillCatalog } from "@/hooks/use-skill-catalog"
import type { Job } from "@/interface/job"
import {
  EMPTY_JOB_FORM,
  fromJob,
  jobFormSchema,
  submitIssues,
  todayInVietnam,
  toJobPayload,
  type JobFormField,
  type JobFormValues,
} from "@/schemas/job.schema"
import { ApiError, getErrorMessage } from "@/services/api"

type Step = 1 | 2
type FieldErrorMap = Partial<Record<JobFormField, string>>
/** Kết quả một thao tác: "leave" = đang rời trang, giữ các nút khoá tới khi trang đổi. */
type Outcome = "stay" | "leave"
type Action = "save" | "submit" | "dialog"

const STEPS: { step: Step; label: string }[] = [
  { step: 1, label: "Thông tin chung" },
  { step: 2, label: "Kỹ năng yêu cầu" },
]

// "address" có trong JobRequest nhưng §5.2 không đặt ô trên form: lỗi của nó hiện ở thông báo chung.
const RENDERED_FIELDS = new Set<string>(Object.keys(EMPTY_JOB_FORM).filter((f) => f !== "address"))

const stepOf = (field: string): Step => (field.startsWith("skills") ? 2 : 1)
const firstStep = (fields: string[]): Step | null => (fields.length === 0 ? null : (Math.min(...fields.map(stepOf)) as Step))

/** Tách lỗi VALIDATION_ERROR của backend: khoá "skills[0].minScore" gom về "skills", khoá không có ô thì hiện chung. */
function describeError(error: unknown): { fieldErrors?: FieldErrorMap; message?: string } {
  if (error instanceof ApiError && error.code === "VALIDATION_ERROR" && error.errors && typeof error.errors === "object") {
    const fieldErrors: FieldErrorMap = {}
    const others: string[] = []
    for (const [key, value] of Object.entries(error.errors as Record<string, unknown>)) {
      if (typeof value !== "string") continue
      const field = key.startsWith("skills") ? "skills" : key
      if (RENDERED_FIELDS.has(field)) fieldErrors[field as JobFormField] ??= value
      else others.push(value)
    }
    if (Object.keys(fieldErrors).length > 0) {
      return { fieldErrors, message: others.length > 0 ? others.join(" ") : undefined }
    }
  }
  return { message: getErrorMessage(error) }
}

interface JobFormProps {
  /** Không có = tạo JD mới. */
  job?: Job
  companyLocked: boolean
}

/** Form tạo/sửa JD hai bước (§5.2): một react-hook-form cho cả hai bước, chuyển bước không gọi API. */
export function JobForm({ job, companyLocked }: JobFormProps) {
  const router = useRouter()
  const queryClient = useQueryClient()
  const userId = useAuthStore((state) => state.user?.id)
  const { data: catalog = [] } = useSkillCatalog()

  const form = useForm<JobFormValues>({
    resolver: zodResolver(jobFormSchema),
    defaultValues: job ? fromJob(job) : EMPTY_JOB_FORM,
    shouldFocusError: false,
  })
  const {
    handleSubmit,
    reset,
    setError,
    setFocus,
    getValues,
    formState: { errors, isDirty },
  } = form

  // Lần lưu đầu ở /hr/jobs/new chuyển sang /hr/jobs/{id}: nhận lại bước, "Đã lưu" và lỗi từ trang trước.
  const [handoff] = React.useState(() => peekJobFormHandoff(job?.id))
  const [step, setStep] = React.useState<Step>(handoff?.step ?? 1)
  const [savedAt, setSavedAt] = React.useState<Date | null>(handoff?.saved ? new Date() : null)
  const [formError, setFormError] = React.useState<string | null>(handoff?.message ?? null)
  const [notice, setNotice] = React.useState<string | null>(null)
  const [dialog, setDialog] = React.useState<"resubmit" | "close" | null>(null)
  const [action, setAction] = React.useState<Action | null>(null)
  // Chặn bấm lần hai trước khi React kịp render trạng thái khoá (Review Focus 4).
  const workingRef = React.useRef(false)

  const save = useSaveJob()
  const submit = useSubmitJob()
  const close = useCloseJob()
  const busy = action !== null || save.isPending || submit.isPending || close.isPending

  React.useEffect(() => {
    if (!handoff) return
    clearJobFormHandoff()
    for (const [field, message] of Object.entries(handoff.fieldErrors ?? {})) {
      setError(field as JobFormField, { type: "server", message })
    }
  }, [handoff, setError])

  // JD trên server đổi (lưu xong, đóng JD, tải lại sau lỗi 409): cập nhật form, giữ các ô đang sửa dở.
  const lastJob = React.useRef(job)
  React.useEffect(() => {
    if (!job || job === lastJob.current) return
    lastJob.current = job
    reset(fromJob(job), { keepDirtyValues: true })
  }, [job, reset])

  const status = job?.status
  const readOnly = companyLocked || status === "PENDING" || status === "CLOSED"
  const approved = status === "APPROVED"
  const canSubmit = !status || status === "DRAFT" || status === "REJECTED"

  /** Chuyển tới bước có lỗi đầu tiên và đặt con trỏ vào ô đó (sau khi bước hiện ra). */
  function goToError(fields: string[]) {
    const target = firstStep(fields)
    if (!target) return
    setStep(target)
    const first = fields.find((f) => stepOf(f) === target && f !== "skills")
    if (first) requestAnimationFrame(() => setFocus(first as JobFormField))
  }

  function applyFieldErrors(fieldErrors: FieldErrorMap): boolean {
    const entries = Object.entries(fieldErrors).filter(([, message]) => Boolean(message)) as [JobFormField, string][]
    for (const [field, message] of entries) setError(field, { type: "server", message })
    goToError(entries.map(([field]) => field))
    return entries.length > 0
  }

  function showError(error: unknown) {
    const { fieldErrors, message } = describeError(error)
    if (fieldErrors) applyFieldErrors(fieldErrors)
    if (message) setFormError(message)
    if (error instanceof ApiError && (error.status === 409 || error.code === "COMPANY_LOCKED")) {
      // Trạng thái JD hoặc công ty đã đổi ở nơi khác: tải lại để form chuyển sang chỉ đọc nếu cần.
      if (job) void queryClient.invalidateQueries({ queryKey: jobKeys.detail(userId, job.id) })
      void queryClient.invalidateQueries({ queryKey: companyKeys.mine(userId) })
    }
  }

  async function run(kind: Action, task: () => Promise<Outcome>) {
    if (workingRef.current) return
    workingRef.current = true
    setAction(kind)
    setFormError(null)
    setNotice(null)
    let outcome: Outcome = "stay"
    try {
      outcome = await task()
    } catch (error) {
      showError(error)
    } finally {
      if (outcome === "stay") {
        workingRef.current = false
        setAction(null)
      }
    }
  }

  /** Kiểm mức lưu nháp (zod). Lỗi thì chuyển tới bước có lỗi và trả null. */
  async function validated(): Promise<JobFormValues | null> {
    let result: JobFormValues | null = null
    await handleSubmit(
      (values) => {
        result = values
      },
      (fieldErrors) => goToError(Object.keys(fieldErrors)),
    )()
    return result
  }

  /** JD mới vừa tạo: đổi URL sang /hr/jobs/{id}; trang đích nhận lại bước và thông báo qua job-flash. */
  function moveToCreated(saved: Job, error?: unknown) {
    const described = error === undefined ? {} : describeError(error)
    const errorStep = firstStep(Object.keys(described.fieldErrors ?? {}))
    setJobFormHandoff({ jobId: saved.id, step: errorStep ?? step, saved: true, ...described })
    router.replace(`/hr/jobs/${saved.id}`)
  }

  const saveDraft = () =>
    run("save", async () => {
      const values = await validated()
      if (!values) return "stay"
      const saved = await save.mutateAsync({ id: job?.id, payload: toJobPayload(values) })
      if (!job) {
        moveToCreated(saved)
        return "leave"
      }
      reset(fromJob(saved))
      setSavedAt(new Date())
      return "stay"
    })

  const sendForReview = () =>
    run("submit", async () => {
      const values = await validated()
      if (!values) return "stay"
      if (applyFieldErrors(submitIssues(values, todayInVietnam()))) return "stay"
      const saved = await save.mutateAsync({ id: job?.id, payload: toJobPayload(values) })
      try {
        await submit.mutateAsync(saved.id)
      } catch (error) {
        if (!job) {
          moveToCreated(saved, error)
          return "leave"
        }
        reset(fromJob(saved))
        setSavedAt(new Date())
        showError(error)
        return "stay"
      }
      setJobListNotice("Đã gửi duyệt")
      router.push("/hr/jobs")
      return "leave"
    })

  // JD đang tuyển: lưu = gửi duyệt lại (backend chuyển về PENDING), nên kiểm đủ trường rồi hỏi xác nhận.
  const requestResubmit = () =>
    run("save", async () => {
      const values = await validated()
      if (!values) return "stay"
      if (applyFieldErrors(submitIssues(values, todayInVietnam()))) return "stay"
      setDialog("resubmit")
      return "stay"
    })

  const confirmResubmit = () =>
    run("dialog", async () => {
      if (!job) return "stay"
      try {
        await save.mutateAsync({ id: job.id, payload: toJobPayload(getValues()) })
      } catch (error) {
        setDialog(null)
        showError(error)
        return "stay"
      }
      setJobListNotice("Đã lưu và gửi duyệt lại")
      router.push("/hr/jobs")
      return "leave"
    })

  const confirmClose = () =>
    run("dialog", async () => {
      if (!job) return "stay"
      try {
        const closed = await close.mutateAsync(job.id)
        reset(fromJob(closed))
        setNotice("Đã đóng JD")
      } finally {
        setDialog(null)
      }
      return "stay"
    })

  const errorSteps = new Set(Object.keys(errors).map(stepOf))
  const saveLabel = approved ? "Lưu và gửi duyệt lại" : "Lưu nháp"
  const onSave = approved ? requestResubmit : saveDraft

  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <h1 className="font-heading text-lg font-semibold">{!job ? "Tạo JD" : readOnly ? "Xem JD" : "Sửa JD"}</h1>
          {status ? <JobStatusBadge status={status} /> : null}
        </div>
        <div className="flex gap-2">
          {approved && !companyLocked ? (
            <Button variant="destructive" size="sm" disabled={busy} onClick={() => setDialog("close")}>
              Đóng JD
            </Button>
          ) : null}
          <Button variant="outline" size="sm" render={<Link href="/hr/jobs" />} nativeButton={false}>
            Quay lại danh sách
          </Button>
        </div>
      </div>

      {companyLocked ? <Alert>Công ty đang bị khoá, bạn chỉ xem được JD.</Alert> : null}
      {status === "PENDING" ? <Alert>JD đang chờ duyệt, không sửa được.</Alert> : null}
      {status === "CLOSED" ? <Alert>JD đã đóng, không sửa được.</Alert> : null}
      {status === "REJECTED" && job?.rejectReason ? <Alert variant="destructive">Lý do trả lại: {job.rejectReason}</Alert> : null}
      {notice ? <Alert variant="success">{notice}</Alert> : null}

      <ol className="flex flex-wrap gap-2 text-xs" aria-label="Các bước">
        {STEPS.map(({ step: n, label }) => (
          <li key={n}>
            <button
              type="button"
              aria-current={step === n ? "step" : undefined}
              onClick={() => setStep(n)}
              className={`rounded-full border px-3 py-1 font-medium transition-colors focus-visible:ring-2 focus-visible:ring-ring/30 focus-visible:outline-none ${
                step === n ? "border-blue-600 bg-blue-600 text-white" : "border-border hover:bg-muted"
              }`}
            >
              {n}. {label}
              {errorSteps.has(n) ? (
                <span className={step === n ? "ml-1 text-white" : "ml-1 text-destructive"} aria-label="có lỗi">
                  !
                </span>
              ) : null}
            </button>
          </li>
        ))}
      </ol>

      <Card>
        <CardContent>
          <form noValidate onSubmit={(e) => e.preventDefault()} className="space-y-4">
            <fieldset disabled={readOnly} className="min-w-0">
              {/* Giữ cả hai bước trong DOM để chuyển bước không mất trạng thái ô nhập. */}
              <div hidden={step !== 1}>
                <JobGeneralStep form={form} />
              </div>
              <div hidden={step !== 2}>
                <JobSkillsStep form={form} catalog={catalog} readOnly={readOnly} />
              </div>
            </fieldset>

            <div className="flex flex-wrap items-center justify-between gap-2 border-t pt-4">
              {step === 1 ? (
                <>
                  <div>
                    {readOnly ? null : (
                      <Button type="button" variant="outline" disabled={busy} onClick={onSave}>
                        {action === "save" ? "Đang lưu..." : saveLabel}
                      </Button>
                    )}
                  </div>
                  <Button type="button" onClick={() => setStep(2)}>
                    Tiếp: Kỹ năng →
                  </Button>
                </>
              ) : (
                <>
                  <Button type="button" variant="outline" onClick={() => setStep(1)}>
                    ← Quay lại
                  </Button>
                  {readOnly ? null : (
                    <div className="flex gap-2">
                      <Button type="button" variant={canSubmit ? "outline" : "default"} disabled={busy} onClick={onSave}>
                        {action === "save" ? "Đang lưu..." : saveLabel}
                      </Button>
                      {canSubmit ? (
                        <Button type="button" disabled={busy} onClick={sendForReview}>
                          {action === "submit" ? "Đang gửi..." : "Gửi duyệt"}
                        </Button>
                      ) : null}
                    </div>
                  )}
                </>
              )}
            </div>

            {savedAt && !isDirty && !busy ? (
              <p role="status" className="text-xs text-emerald-700 dark:text-emerald-400">
                ✓ Đã lưu lúc {savedAt.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}
              </p>
            ) : null}
            {formError ? (
              <p role="alert" className="text-xs/relaxed text-destructive">
                {formError}
              </p>
            ) : null}
          </form>
        </CardContent>
      </Card>

      {dialog === "resubmit" ? (
        <ConfirmDialog
          title="Lưu và gửi duyệt lại?"
          description="JD sẽ chuyển về Chờ duyệt, ứng viên tạm thời không thấy JD cho tới khi được duyệt lại."
          confirmLabel="Lưu và gửi duyệt lại"
          pending={busy}
          onConfirm={confirmResubmit}
          onCancel={() => setDialog(null)}
        />
      ) : null}
      {dialog === "close" ? (
        <ConfirmDialog
          title="Đóng JD?"
          description="Ứng viên sẽ không còn thấy JD này và không nộp hồ sơ vào được nữa. JD đã đóng không mở lại được."
          confirmLabel="Đóng JD"
          destructive
          pending={busy}
          onConfirm={confirmClose}
          onCancel={() => setDialog(null)}
        />
      ) : null}
    </div>
  )
}
