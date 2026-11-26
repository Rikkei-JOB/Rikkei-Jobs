"use client"

import * as React from "react"
import { CompanyMark } from "@/app/components/company/company-mark"
import { formatDate, LEVEL_LABEL, TYPE_LABEL, WORK_MODE_LABEL } from "@/app/components/job/job-labels"
import { formatSalaryText } from "@/app/components/job/job-preview-card"
import { skillLevelLabel } from "@/app/components/job/skill-level"
import { useSkillCatalog } from "@/hooks/use-skill-catalog"
import type { JobSkill } from "@/interface/job"
import type { PublicJobDetail } from "@/interface/public-job"

interface JobPublicViewProps {
  job: PublicJobDetail
  /** Dải thông báo trên cùng (ví dụ trạng thái duyệt). */
  notice?: React.ReactNode
  /** Nội dung thêm ở cuối (ví dụ nút thao tác). */
  children?: React.ReactNode
}

function Chip({ children }: { children: React.ReactNode }) {
  return <span className="rounded-md bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">{children}</span>
}

function TextSection({ title, text }: { title: string; text: string | null }) {
  if (!text?.trim()) return null
  return (
    <section className="mt-5">
      <h3 className="mb-1.5 text-sm font-semibold text-foreground">{title}</h3>
      <p className="break-words whitespace-pre-line text-sm leading-6 text-muted-foreground">{text}</p>
    </section>
  )
}

function SkillGroup({
  title,
  skills,
  names,
  accent,
}: {
  title: string
  skills: JobSkill[]
  names: Record<string, string>
  accent?: boolean
}) {
  if (skills.length === 0) return null
  return (
    <div>
      <p className="mb-1.5 text-xs font-medium text-muted-foreground">{title}</p>
      <ul className="flex flex-wrap gap-1.5">
        {skills.map((s) => (
          <li
            key={s.skillId}
            className={`rounded-md px-2 py-0.5 text-xs font-medium ${
              accent ? "bg-primary/10 text-primary" : "bg-muted text-muted-foreground"
            }`}
          >
            {names[s.skillId] ?? s.skillId.slice(0, 8)} ≥ {s.minScore} ({skillLevelLabel(s.minScore)})
          </li>
        ))}
      </ul>
    </div>
  )
}

/** Chi tiết JD như ứng viên thấy, dùng cho cán bộ duyệt (và tái sử dụng ở màn khác). */
export function JobPublicView({ job, notice, children }: JobPublicViewProps) {
  const catalog = useSkillCatalog()
  const names = React.useMemo(
    () => Object.fromEntries((catalog.data ?? []).map((s) => [s.id, s.name])),
    [catalog.data],
  )
  const { company } = job
  const companyMeta = [company.industry, company.size, company.city].filter((v): v is string => Boolean(v?.trim()))
  const chips = [
    job.level ? LEVEL_LABEL[job.level] : null,
    job.type ? TYPE_LABEL[job.type] : null,
    job.workMode ? WORK_MODE_LABEL[job.workMode] : null,
    formatSalaryText(job.salaryMin == null ? "" : String(job.salaryMin), job.salaryMax == null ? "" : String(job.salaryMax)),
    job.quantity != null ? `${job.quantity} vị trí` : null,
    job.deadline ? `Hạn nộp ${formatDate(job.deadline)}` : null,
  ].filter((v): v is string => Boolean(v))
  const required = job.skills.filter((s) => s.required)
  const optional = job.skills.filter((s) => !s.required)

  return (
    <div className="rounded-xl border border-border bg-card p-5 text-card-foreground">
      {notice ? <div className="mb-4">{notice}</div> : null}
      <div className="flex items-center gap-3">
        <CompanyMark name={company.name} logoUrl={company.logoUrl} size={40} brand />
        <div className="min-w-0">
          <p className="font-medium break-words text-foreground">{company.name}</p>
          {companyMeta.length > 0 ? <p className="text-xs text-muted-foreground">{companyMeta.join(" · ")}</p> : null}
        </div>
      </div>
      <h2 className="mt-4 text-xl leading-7 font-semibold break-words text-foreground">{job.title}</h2>
      {job.position?.trim() ? <p className="text-sm text-muted-foreground">{job.position}</p> : null}
      {chips.length > 0 ? (
        <div className="mt-3 flex flex-wrap gap-1.5">
          {chips.map((c) => (
            <Chip key={c}>{c}</Chip>
          ))}
        </div>
      ) : null}
      {job.address?.trim() ? <p className="mt-3 text-sm text-muted-foreground">Địa chỉ: {job.address}</p> : null}
      {job.skills.length > 0 ? (
        <section className="mt-5 space-y-3" aria-label="Kỹ năng">
          <h3 className="text-sm font-semibold text-foreground">Kỹ năng</h3>
          <SkillGroup title="Bắt buộc" skills={required} names={names} accent />
          <SkillGroup title="Ưu tiên" skills={optional} names={names} />
        </section>
      ) : null}
      <TextSection title="Mô tả công việc" text={job.description} />
      <TextSection title="Yêu cầu khác" text={job.requirements} />
      <TextSection title="Quyền lợi" text={job.benefits} />
      {children ? <div className="mt-5">{children}</div> : null}
    </div>
  )
}
