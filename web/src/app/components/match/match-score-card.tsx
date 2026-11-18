import { Alert } from "@/app/components/ui/alert";
import { Button } from "@/app/components/ui/button";
import type { MatchScore, MatchSkill } from "@/interface/hr-application";
import { getErrorMessage } from "@/services/api";

const RADIUS = 42;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

function ringColor(score: number) {
  if (score < 50) return "text-red-500";
  if (score < 80) return "text-amber-500";
  return "text-emerald-500";
}

function formatScore(value: number) {
  return String(Math.round(value * 10) / 10);
}

function ScoreRing({ score }: { score: number }) {
  const clamped = Math.min(100, Math.max(0, score));
  return (
    <svg viewBox="0 0 100 100" className={`size-24 shrink-0 ${ringColor(score)}`} role="img" aria-label={`Điểm phù hợp ${score}%`}>
      <circle cx="50" cy="50" r={RADIUS} fill="none" stroke="currentColor" strokeWidth="8" opacity="0.15" />
      <circle
        cx="50"
        cy="50"
        r={RADIUS}
        fill="none"
        stroke="currentColor"
        strokeWidth="8"
        strokeLinecap="round"
        strokeDasharray={CIRCUMFERENCE}
        strokeDashoffset={CIRCUMFERENCE * (1 - clamped / 100)}
        transform="rotate(-90 50 50)"
      />
      <text x="50" y="50" textAnchor="middle" dominantBaseline="central" className="fill-foreground text-[22px] font-semibold">
        {score}%
      </text>
    </svg>
  );
}

function SkillRow({ item, missing }: { item: MatchSkill; missing: boolean }) {
  return (
    <li className="flex flex-wrap items-center justify-between gap-2 text-xs">
      <span className="flex items-center gap-2">
        <span className="font-medium">{item.skill}</span>
        <span className="rounded border border-border px-1.5 py-0.5 text-[10px] text-muted-foreground">
          {item.required ? "bắt buộc" : "ưu tiên"}
        </span>
      </span>
      <span className="text-muted-foreground">
        {formatScore(item.candidateScore)}/{formatScore(item.minScore)}
        {missing ? (
          <span className="ml-2 font-medium text-red-600 dark:text-red-400">
            thiếu {Math.ceil(item.minScore - item.candidateScore)} điểm
          </span>
        ) : null}
      </span>
    </li>
  );
}

function SkillSection({ title, items, missing }: { title: string; items: MatchSkill[]; missing: boolean }) {
  if (items.length === 0) return null;
  return (
    <section
      className={
        missing
          ? "space-y-2 rounded-lg border border-red-500/30 bg-red-500/5 p-3"
          : "space-y-2 rounded-lg border border-emerald-500/30 bg-emerald-500/5 p-3"
      }
    >
      <h3 className={`text-xs font-semibold ${missing ? "text-red-700 dark:text-red-400" : "text-emerald-700 dark:text-emerald-400"}`}>
        {title}
      </h3>
      <ul className="space-y-1.5">
        {items.map((item) => (
          <SkillRow key={item.skill} item={item} missing={missing} />
        ))}
      </ul>
    </section>
  );
}

export function MatchScoreCard({
  match,
  isLoading,
  error,
  onRetry,
}: {
  match?: MatchScore;
  isLoading?: boolean;
  error?: unknown;
  onRetry?: () => void;
}) {
  if (isLoading) return <p className="text-xs text-muted-foreground">Đang tải...</p>;

  if (error) {
    return (
      <Alert variant="destructive" className="flex items-center justify-between gap-2">
        <span>{getErrorMessage(error)}</span>
        {onRetry ? (
          <Button type="button" variant="outline" size="sm" onClick={onRetry}>
            Thử lại
          </Button>
        ) : null}
      </Alert>
    );
  }

  if (!match) return null;

  const noSkills = match.met.length === 0 && match.missing.length === 0;

  return (
    <div className="space-y-3 rounded-lg border border-border p-4">
      <div className="flex items-center gap-4">
        <ScoreRing score={match.score} />
        {match.belowMinimum ? <Alert variant="destructive">Thiếu kỹ năng bắt buộc tối thiểu</Alert> : null}
      </div>
      {noSkills ? <p className="text-xs text-muted-foreground">JD chưa khai báo kỹ năng</p> : null}
      <SkillSection title="Kỹ năng đạt" items={match.met} missing={false} />
      <SkillSection title="Kỹ năng thiếu" items={match.missing} missing />
    </div>
  );
}
