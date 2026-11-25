"use client"

import * as React from "react"
import { useWatch, type Control } from "react-hook-form"
import { CompanyMark } from "@/app/components/company/company-mark"
import { formatDate, WORK_MODE_LABEL } from "@/app/components/job/job-labels"
import type { JobFormValues } from "@/schemas/job.schema"

/** VND -> triệu: nguyên thì không lẻ, ngược lại một chữ số thập phân. */
function millions(value: string): string | null {
  const n = Number(value.trim())
  if (value.trim() === "" || !Number.isFinite(n)) return null
  const m = n / 1_000_000
  return Number.isInteger(m) ? String(m) : m.toFixed(1)
}

export function formatSalaryText(min: string, max: string): string | null {
  const lo = millions(min)
  const hi = millions(max)
  if (lo && hi) return `${lo}–${hi} triệu`
  if (lo) return `Từ ${lo} triệu`
  if (hi) return `Đến ${hi} triệu`
  return null
}

interface JobPreviewCardProps {
  values: JobFormValues
  companyName: string
  companyLogoUrl?: string | null
  skillNames: Record<string, string>
}

/** Thẻ JD như ứng viên thấy trong danh sách (phong cách theo career.css), cập nhật theo giá trị form. */
export function JobPreviewCard({ values, companyName, companyLogoUrl, skillNames }: JobPreviewCardProps) {
  const title = values.title.trim()
  const meta = [
    formatSalaryText(values.salaryMin, values.salaryMax),
    values.city.trim() || null,
    values.workMode ? WORK_MODE_LABEL[values.workMode] : null,
    values.deadline ? `Hạn ${formatDate(values.deadline)}` : null,
  ].filter((item): item is string => Boolean(item))
  const required = values.skills.filter((s) => s.required)
  const optional = values.skills.filter((s) => !s.required)
  const description = values.description.trim()

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5 text-sm text-slate-700 shadow-[0_2px_8px_rgb(15_23_42/0.04)]">
      <p className="mb-3 text-[11px] font-bold tracking-[0.14em] text-[#b4232d]">XEM TRƯỚC NHƯ ỨNG VIÊN</p>
      <div className="flex items-center gap-2 text-slate-500">
        <CompanyMark name={companyName} logoUrl={companyLogoUrl} size={24} brand />
        <span className="min-w-0 break-words">{companyName}</span>
      </div>
      <h2 className={`mt-2 mb-3 text-lg leading-[26px] font-semibold break-words ${title ? "text-[#18233a]" : "text-slate-400"}`}>
        {title || "Tiêu đề JD"}
      </h2>
      {meta.length > 0 ? <p className="text-xs text-slate-500">{meta.join(" · ")}</p> : null}
      {values.skills.length > 0 ? (
        <ul className="mt-3 flex flex-wrap gap-1.5" aria-label="Kỹ năng">
          {required.map((s) => (
            <li key={s.skillId} className="rounded-md bg-[#fff1f2] px-2 py-0.5 text-xs font-medium text-[#b4232d]">
              {skillNames[s.skillId] ?? s.skillId}
            </li>
          ))}
          {optional.map((s) => (
            <li key={s.skillId} className="rounded-md bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
              {skillNames[s.skillId] ?? s.skillId}
            </li>
          ))}
        </ul>
      ) : null}
      {description ? <p className="mt-3 line-clamp-4 break-words whitespace-pre-line text-slate-500">{description}</p> : null}
    </div>
  )
}

/** Chỉ thành phần này đăng ký theo dõi form, nên gõ phím không render lại cả form. */
export function JobPreviewPane({
  control,
  companyName,
  companyLogoUrl,
  skillNames,
}: {
  control: Control<JobFormValues>
  companyName: string
  companyLogoUrl?: string | null
  skillNames: Record<string, string>
}) {
  const values = useWatch({ control }) as JobFormValues
  return <JobPreviewCard values={values} companyName={companyName} companyLogoUrl={companyLogoUrl} skillNames={skillNames} />
}
