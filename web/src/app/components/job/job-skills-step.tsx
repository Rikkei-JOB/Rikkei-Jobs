"use client"

import * as React from "react"
import { useWatch, type UseFormReturn } from "react-hook-form"
import { Button } from "@/app/components/ui/button"
import { Input } from "@/app/components/ui/input"
import { SkillScoreSlider } from "@/app/components/job/skill-score-slider"
import type { JobSkill } from "@/interface/job"
import type { Skill } from "@/interface/skill"
import { DEFAULT_MIN_SCORE, MAX_JOB_SKILLS, type JobFormValues } from "@/schemas/job.schema"

/** Bỏ dấu tiếng Việt và viết thường để tìm "giao tiep" ra "Giao tiếp". */
function normalize(text: string): string {
  return text.normalize("NFD").replace(/\p{Diacritic}/gu, "").replace(/đ/g, "d").replace(/Đ/g, "d").toLowerCase()
}

interface JobSkillsStepProps {
  form: UseFormReturn<JobFormValues>
  catalog: Skill[]
  readOnly: boolean
  /** Đang lưu/gửi: hiện như bình thường nhưng không cho sửa. */
  disabled?: boolean
}

/** Bước 2 — Kỹ năng yêu cầu: ô tìm trong danh mục, khối Bắt buộc và khối Ưu tiên. */
export function JobSkillsStep({ form, catalog, readOnly, disabled = false }: JobSkillsStepProps) {
  const {
    control,
    setValue,
    clearErrors,
    formState: { errors },
  } = form
  const skills = useWatch({ control, name: "skills" })
  const [query, setQuery] = React.useState("")
  const [open, setOpen] = React.useState(false)
  const listId = React.useId()

  const byId = React.useMemo(() => new Map(catalog.map((s) => [s.id, s])), [catalog])
  const selectedIds = new Set(skills.map((s) => s.skillId))
  const needle = normalize(query.trim())
  const matches = catalog.filter(
    (s) => !selectedIds.has(s.id) && (needle === "" || normalize(`${s.name} ${s.code} ${s.group}`).includes(needle)),
  )
  const full = skills.length >= MAX_JOB_SKILLS
  const required = skills.filter((s) => s.required)
  const preferred = skills.filter((s) => !s.required)

  function update(next: JobSkill[]) {
    if (readOnly || disabled) return
    setValue("skills", next, { shouldDirty: true })
    clearErrors("skills")
  }
  function add(skill: Skill) {
    if (full || selectedIds.has(skill.id)) return
    update([...skills, { skillId: skill.id, required: true, minScore: DEFAULT_MIN_SCORE }])
    setQuery("")
    // Đóng danh sách gợi ý để không che các dòng vừa thêm; gõ tiếp sẽ mở lại.
    setOpen(false)
  }
  const remove = (skillId: string) => update(skills.filter((s) => s.skillId !== skillId))
  const toggle = (skillId: string) => update(skills.map((s) => (s.skillId === skillId ? { ...s, required: !s.required } : s)))
  const setScore = (skillId: string, minScore: number) => update(skills.map((s) => (s.skillId === skillId ? { ...s, minScore } : s)))

  const skillsError = errors.skills?.message ?? errors.skills?.root?.message

  return (
    <div className="space-y-4">
      {skillsError ? (
        <p id="job-skills-error" role="alert" className="text-xs/relaxed text-destructive">
          {skillsError}
        </p>
      ) : null}

      {readOnly ? null : (
        <div
          className="relative"
          onBlur={(e) => {
            if (!e.currentTarget.contains(e.relatedTarget as Node | null)) setOpen(false)
          }}
        >
          <Input
            aria-label="Thêm kỹ năng"
            aria-controls={listId}
            aria-expanded={open}
            placeholder={full ? `Đã đủ ${MAX_JOB_SKILLS} kỹ năng` : `Thêm kỹ năng… (gõ để tìm trong ${catalog.length} kỹ năng)`}
            disabled={full || disabled}
            value={query}
            onChange={(e) => {
              setQuery(e.target.value)
              setOpen(true)
            }}
            onFocus={() => setOpen(true)}
            onClick={() => setOpen(true)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault()
                if (matches[0]) add(matches[0])
              } else if (e.key === "Escape") {
                setOpen(false)
              }
            }}
          />
          {open && !full && !disabled ? (
            <ul
              id={listId}
              className="absolute inset-x-0 top-full z-10 mt-1 max-h-60 overflow-y-auto rounded-md border bg-background p-1 shadow-md"
            >
              {matches.length === 0 ? (
                <li className="px-2 py-1.5 text-xs text-muted-foreground">Không còn kỹ năng phù hợp</li>
              ) : (
                matches.map((s) => (
                  <li key={s.id}>
                    <button
                      type="button"
                      className="flex w-full items-center justify-between gap-2 rounded px-2 py-1.5 text-left text-xs hover:bg-muted focus-visible:bg-muted focus-visible:outline-none"
                      onMouseDown={(e) => e.preventDefault()}
                      onClick={() => add(s)}
                    >
                      <span className="font-medium">{s.name}</span>
                      <span className="text-muted-foreground">{s.group}</span>
                    </button>
                  </li>
                ))
              )}
            </ul>
          ) : null}
        </div>
      )}

      <SkillBlock
        tone="required"
        items={required}
        byId={byId}
        readOnly={readOnly}
        disabled={disabled}
        onRemove={remove}
        onToggle={toggle}
        onScore={setScore}
      />
      <SkillBlock
        tone="preferred"
        items={preferred}
        byId={byId}
        readOnly={readOnly}
        disabled={disabled}
        onRemove={remove}
        onToggle={toggle}
        onScore={setScore}
      />
    </div>
  )
}

const TONE = {
  required: {
    title: "Bắt buộc",
    hint: "thiếu là không đạt",
    box: "border-red-300 dark:border-red-900",
    heading: "text-red-600 dark:text-red-400",
    moveTo: "Ưu tiên",
  },
  preferred: {
    title: "Ưu tiên",
    hint: "có thì cộng điểm",
    box: "border-blue-300 dark:border-blue-900",
    heading: "text-blue-600 dark:text-blue-400",
    moveTo: "Bắt buộc",
  },
} as const

interface SkillBlockProps {
  tone: keyof typeof TONE
  items: JobSkill[]
  byId: Map<string, Skill>
  readOnly: boolean
  disabled: boolean
  onRemove: (skillId: string) => void
  onToggle: (skillId: string) => void
  onScore: (skillId: string, minScore: number) => void
}

function SkillBlock({ tone, items, byId, readOnly, disabled, onRemove, onToggle, onScore }: SkillBlockProps) {
  const t = TONE[tone]
  return (
    <section className={`rounded-lg border p-3 ${t.box}`} aria-label={`Kỹ năng ${t.title.toLowerCase()}`}>
      <h3 className={`mb-1 flex items-baseline justify-between text-sm font-semibold ${t.heading}`}>
        <span>
          {t.title} ({items.length})
        </span>
        <span className="text-xs font-normal text-muted-foreground">{t.hint}</span>
      </h3>
      {items.length === 0 ? (
        <p className="py-2 text-xs text-muted-foreground">Chưa có kỹ năng</p>
      ) : (
        <ul>
          {items.map((item) => {
            const name = byId.get(item.skillId)?.name ?? "Kỹ năng không còn trong danh mục"
            return (
              <li
                key={item.skillId}
                className="flex flex-wrap items-center gap-x-3 gap-y-1 border-t border-dashed py-2 first:border-t-0"
              >
                <span className="w-28 shrink-0 text-xs font-semibold">{name}</span>
                <div className="flex min-w-40 flex-1">
                  <SkillScoreSlider
                    value={item.minScore}
                    onChange={(v) => onScore(item.skillId, v)}
                    disabled={readOnly || disabled}
                    label={name}
                  />
                </div>
                {readOnly ? null : (
                  <div className="flex shrink-0 gap-1">
                    <Button
                      type="button"
                      variant="outline"
                      size="xs"
                      disabled={disabled}
                      aria-label={`Chuyển ${name} sang ${t.moveTo}`}
                      onClick={() => onToggle(item.skillId)}
                    >
                      → {t.moveTo}
                    </Button>
                    <Button type="button" variant="ghost" size="icon-xs" disabled={disabled} aria-label={`Xoá ${name}`} onClick={() => onRemove(item.skillId)}>
                      ✕
                    </Button>
                  </div>
                )}
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
