"use client"

import { Controller, useWatch, type UseFormReturn } from "react-hook-form"
import { SelectMenu } from "@/app/components/ui/select-menu"
import { FormField } from "@/app/components/auth/form-field"
import { Input } from "@/app/components/ui/input"
import { LEVEL_LABEL, TYPE_LABEL, WORK_MODE_LABEL } from "@/app/components/job/job-labels"
import { JOB_LEVELS, JOB_TYPES, WORK_MODES, todayInVietnam, type JobFormField, type JobFormValues } from "@/schemas/job.schema"

const CONTROL_CLASS =
  "w-full min-w-0 rounded-md border border-input bg-input/20 px-2 py-1 text-sm transition-colors outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/30 disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:border-destructive md:text-xs/relaxed dark:bg-input/30"

type GeneralField = Exclude<JobFormField, "skills">

const vnd = new Intl.NumberFormat("vi-VN")

/** "12000000" -> "12.000.000 ₫"; chuỗi không phải số nguyên không âm -> null. */
function formatSalary(value: string): string | null {
  const trimmed = value.trim()
  return /^\d+$/.test(trimmed) ? `${vnd.format(Number(trimmed))} ₫` : null
}

/** Bước 1 — Thông tin chung, một cột theo thứ tự §5.2. Ô có dấu * là bắt buộc khi gửi duyệt. */
export function JobGeneralStep({ form }: { form: UseFormReturn<JobFormValues> }) {
  const {
    register,
    control,
    formState: { errors },
  } = form
  const [salaryMin, salaryMax] = useWatch({ control, name: ["salaryMin", "salaryMax"] })

  const field = (name: GeneralField) => ({
    id: `job-${name}`,
    "aria-invalid": Boolean(errors[name]),
    "aria-describedby": errors[name] ? `job-${name}-error` : undefined,
    ...register(name),
  })
  const error = (name: GeneralField) => errors[name]?.message

  return (
    <div className="space-y-4">
      <FormField label="Tiêu đề *" htmlFor="job-title" error={error("title")}>
        <Input placeholder="Ví dụ: Junior Backend Developer (Java/Spring Boot)" {...field("title")} />
      </FormField>

      <div className="grid gap-4 sm:grid-cols-3">
        <FormField label="Vị trí *" htmlFor="job-position" error={error("position")}>
          <Input placeholder="Ví dụ: Backend Developer" {...field("position")} />
        </FormField>
        <FormField label="Cấp độ *" htmlFor="job-level" error={error("level")}>
          <Controller
            name="level"
            control={control}
            render={({ field: f }) => (
              <SelectMenu
                id={`job-level`}
                aria-invalid={Boolean(errors.level)}
                aria-describedby={errors.level ? `job-level-error` : undefined}
                value={f.value ?? ""}
                onValueChange={f.onChange}
                onBlur={f.onBlur}
                className="ui-select-trigger h-7 min-h-7 rounded-md px-2 text-sm md:text-xs"
                options={[{ value: "", label: "Chưa chọn" }, ...JOB_LEVELS.map((v) => ({ value: v, label: LEVEL_LABEL[v] }))]}
              />
            )}
          />
        </FormField>
        <FormField label="Loại hình *" htmlFor="job-type" error={error("type")}>
          <Controller
            name="type"
            control={control}
            render={({ field: f }) => (
              <SelectMenu
                id={`job-type`}
                aria-invalid={Boolean(errors.type)}
                aria-describedby={errors.type ? `job-type-error` : undefined}
                value={f.value ?? ""}
                onValueChange={f.onChange}
                onBlur={f.onBlur}
                className="ui-select-trigger h-7 min-h-7 rounded-md px-2 text-sm md:text-xs"
                options={[{ value: "", label: "Chưa chọn" }, ...JOB_TYPES.map((v) => ({ value: v, label: TYPE_LABEL[v] }))]}
              />
            )}
          />
        </FormField>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <FormField label="Lương từ (VND)" htmlFor="job-salaryMin" error={error("salaryMin")}>
          <Input inputMode="numeric" placeholder="12000000" {...field("salaryMin")} />
          {formatSalary(salaryMin) ? <p className="text-xs text-muted-foreground">{formatSalary(salaryMin)}</p> : null}
        </FormField>
        <FormField label="đến (VND)" htmlFor="job-salaryMax" error={error("salaryMax")}>
          <Input inputMode="numeric" placeholder="20000000" {...field("salaryMax")} />
          {formatSalary(salaryMax) ? <p className="text-xs text-muted-foreground">{formatSalary(salaryMax)}</p> : null}
        </FormField>
        <FormField label="Hình thức *" htmlFor="job-workMode" error={error("workMode")}>
          <Controller
            name="workMode"
            control={control}
            render={({ field: f }) => (
              <SelectMenu
                id={`job-workMode`}
                aria-invalid={Boolean(errors.workMode)}
                aria-describedby={errors.workMode ? `job-workMode-error` : undefined}
                value={f.value ?? ""}
                onValueChange={f.onChange}
                onBlur={f.onBlur}
                className="ui-select-trigger h-7 min-h-7 rounded-md px-2 text-sm md:text-xs"
                options={[{ value: "", label: "Chưa chọn" }, ...WORK_MODES.map((v) => ({ value: v, label: WORK_MODE_LABEL[v] }))]}
              />
            )}
          />
        </FormField>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 md:grid-cols-[1fr_1.6fr_0.7fr_1fr]">
        <FormField label="Thành phố *" htmlFor="job-city" error={error("city")}>
          <Input placeholder="Ví dụ: Hà Nội" {...field("city")} />
        </FormField>
        <FormField label="Địa chỉ" htmlFor="job-address" error={error("address")}>
          <Input placeholder="Ví dụ: Tầng 21, 1 Phạm Văn Bạch" {...field("address")} />
        </FormField>
        <FormField label="Số lượng *" htmlFor="job-quantity" error={error("quantity")}>
          <Input inputMode="numeric" placeholder="1" {...field("quantity")} />
        </FormField>
        <FormField label="Hạn nộp *" htmlFor="job-deadline" error={error("deadline")}>
          <Input type="date" min={todayInVietnam()} {...field("deadline")} />
        </FormField>
      </div>

      <FormField label="Mô tả công việc *" htmlFor="job-description" error={error("description")}>
        <textarea rows={6} className={CONTROL_CLASS} {...field("description")} />
      </FormField>

      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Yêu cầu khác" htmlFor="job-requirements" error={error("requirements")}>
          <textarea rows={4} className={CONTROL_CLASS} {...field("requirements")} />
          <p className="text-xs text-muted-foreground">Ghi những yêu cầu không có trong danh mục kỹ năng, ví dụ tiếng Nhật N3.</p>
        </FormField>
        <FormField label="Quyền lợi" htmlFor="job-benefits" error={error("benefits")}>
          <textarea rows={4} className={CONTROL_CLASS} {...field("benefits")} />
        </FormField>
      </div>

      <p className="text-xs text-muted-foreground">* Bắt buộc khi gửi duyệt. Lưu nháp chỉ cần Tiêu đề.</p>
    </div>
  )
}
