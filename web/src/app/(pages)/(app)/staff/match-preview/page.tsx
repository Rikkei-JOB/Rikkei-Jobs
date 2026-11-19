"use client"

import * as React from "react"
import { MatchScoreCard } from "@/app/components/match/match-score-card"
import { Button } from "@/app/components/ui/button"
import { Input } from "@/app/components/ui/input"
import { useAuthGuard } from "@/hooks/use-auth-guard"
import { useJobMatch } from "@/hooks/use-job-match"

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

const SAMPLE_JDS = [
  { label: "JD6 - Backend Developer Java (mẫu DN-06)", id: "00000000-0000-0000-0003-000000000006" },
  { label: "JD7 - Fresher Java (mẫu DN-06)", id: "00000000-0000-0000-0003-000000000007" },
]

export default function MatchPreviewPage() {
  const allowed = useAuthGuard({ roles: ["STAFF"] })
  const [jobId, setJobId] = React.useState("")
  const [userId, setUserId] = React.useState("")
  // Chỉ khi bấm "Xem" mới gửi cặp mã đã kiểm tra sang hook.
  const [submitted, setSubmitted] = React.useState<{ jobId: string; userId: string } | null>(null)
  const [invalid, setInvalid] = React.useState(false)

  const { data, error, isFetching, refetch } = useJobMatch(submitted?.jobId, submitted?.userId)

  if (!allowed) return null

  function handleView() {
    const j = jobId.trim()
    const u = userId.trim()
    if (!UUID_RE.test(j) || !UUID_RE.test(u)) {
      setInvalid(true)
      setSubmitted(null)
      return
    }
    setInvalid(false)
    setSubmitted({ jobId: j, userId: u })
  }

  return (
    <div className="mx-auto max-w-2xl space-y-4">
      <h1 className="font-heading text-lg font-semibold">Xem điểm phù hợp</h1>

      <div className="space-y-2">
        <label className="block space-y-1 text-xs font-medium">
          Mã JD
          <Input value={jobId} onChange={(e) => setJobId(e.target.value)} placeholder="UUID của JD" />
        </label>
        <div className="flex flex-wrap gap-2">
          {SAMPLE_JDS.map((jd) => (
            <Button key={jd.id} type="button" variant="outline" size="sm" onClick={() => setJobId(jd.id)}>
              {jd.label}
            </Button>
          ))}
        </div>
        <label className="block space-y-1 text-xs font-medium">
          Mã ứng viên
          <Input value={userId} onChange={(e) => setUserId(e.target.value)} placeholder="UUID của ứng viên" />
        </label>
        <Button type="button" onClick={handleView}>
          Xem
        </Button>
        {invalid ? <p className="text-xs text-destructive">Mã không hợp lệ</p> : null}
      </div>

      {submitted ? <MatchScoreCard match={data} isLoading={isFetching && !data} error={error} onRetry={() => void refetch()} /> : null}
    </div>
  )
}
