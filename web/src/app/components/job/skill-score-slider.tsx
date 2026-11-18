"use client"

import { Slider } from "@base-ui/react/slider"
import { SKILL_LEVEL_MARKS, skillLevelLabel } from "@/app/components/job/skill-level"

const MIN = 1
const MAX = 100

/** Vị trí (%) của một giá trị trên thanh 1-100. */
const percent = (value: number) => ((value - MIN) / (MAX - MIN)) * 100

const [P40, P70, P85] = SKILL_LEVEL_MARKS.map(percent)

// Nền bốn đoạn màu theo bậc: Cơ bản xám, Khá vàng, Tốt xanh lá, Xuất sắc xanh dương.
const TRACK_BACKGROUND = `linear-gradient(90deg,
  rgb(148 163 184 / 0.45) 0 ${P40}%,
  rgb(234 179 8 / 0.45) ${P40}% ${P70}%,
  rgb(34 197 94 / 0.45) ${P70}% ${P85}%,
  rgb(37 99 235 / 0.5) ${P85}% 100%)`

interface SkillScoreSliderProps {
  value: number
  onChange: (value: number) => void
  disabled?: boolean
  /** Tên kỹ năng, dùng làm nhãn cho trình đọc màn hình. */
  label?: string
}

/** Thanh chọn điểm tối thiểu 1-100, vạch ở 40/70/85, nhãn "<điểm> · <bậc>". */
export function SkillScoreSlider({ value, onChange, disabled = false, label }: SkillScoreSliderProps) {
  return (
    <Slider.Root
      value={value}
      onValueChange={(next) => onChange(next)}
      min={MIN}
      max={MAX}
      step={1}
      disabled={disabled}
      className="flex min-w-0 flex-1 items-center gap-3 data-disabled:opacity-60"
    >
      <Slider.Control className="relative flex h-5 min-w-0 flex-1 touch-none items-center select-none">
        <Slider.Track className="relative h-1.5 w-full rounded-full" style={{ background: TRACK_BACKGROUND }}>
          {SKILL_LEVEL_MARKS.map((mark) => (
            <span
              key={mark}
              aria-hidden="true"
              className="absolute top-1/2 h-3 w-px -translate-y-1/2 bg-foreground/40"
              style={{ left: `${percent(mark)}%` }}
            />
          ))}
          <Slider.Thumb
            getAriaLabel={() => (label ? `Điểm tối thiểu ${label}` : "Điểm tối thiểu")}
            getAriaValueText={(_formatted, v) => `${v} · ${skillLevelLabel(v)}`}
            className="size-3.5 rounded-full border-2 border-background bg-blue-600 shadow outline-none focus-visible:ring-2 focus-visible:ring-ring/50 data-disabled:bg-muted-foreground"
          />
        </Slider.Track>
      </Slider.Control>
      <span className="w-20 shrink-0 text-right text-xs tabular-nums">
        {value} · {skillLevelLabel(value)}
      </span>
    </Slider.Root>
  )
}
