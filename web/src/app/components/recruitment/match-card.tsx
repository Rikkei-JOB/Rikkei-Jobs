import type { MatchScore } from "@/interface/hr-application";

export function MatchCard({ match }: { match: MatchScore | null }) {
  return <section className="rounded-xl border bg-background p-5"><h2 className="text-lg font-semibold">Điểm khớp với JD</h2>
    {!match ? <p className="mt-3 text-sm text-muted-foreground">Chưa có điểm khớp</p> : <div className="mt-4 flex flex-wrap gap-6">
      <div className="flex h-28 w-28 shrink-0 items-center justify-center rounded-full border-8 border-primary/30 text-2xl font-bold" aria-label={`Điểm khớp ${match.score}%`}>
        {Math.round(match.score)}%</div>
      <div className="min-w-0 flex-1 space-y-3">{match.belowMinimum && <p className="text-sm font-medium text-red-700">Có kỹ năng bắt buộc dưới mức tối thiểu.</p>}
        {match.met.map((item, index) => <p key={`met-${index}`} className="text-sm text-emerald-700">✓ {item.skill}: {item.candidateScore}/{item.minScore}</p>)}
        {match.missing.map((item, index) => <p key={`missing-${index}`} className="text-sm text-red-700">✕ {item.skill}: {item.candidateScore}/{item.minScore} · thiếu {Math.max(0, item.minScore - item.candidateScore)} điểm</p>)}
      </div></div>}
  </section>;
}
