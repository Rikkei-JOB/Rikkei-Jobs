"use client";

import { useId, useState } from "react";
import Link from "next/link";
import { AlertCircle, Building2, CheckCircle2, CircleDashed, Clock, Save, XCircle } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { useSaveMentorRound } from "@/hooks/use-mentor-rounds";
import type { MentorRoundItem, RoundResult } from "@/interface/hr-application";
import { getErrorMessage } from "@/services/api";
import { RESULT_INFO, dateParts, dayKey, formatTime, relativeDay, scoreBand, stageOf, type RoundStage } from "./mentor-utils";
import "./mentor.css";

export function DateBlock({ value }: { value: string | null }) {
  if (!value) return <span className="mtr-date" data-empty aria-hidden="true"><small>Lịch</small><strong>?</strong></span>;
  const { day, month, weekday } = dateParts(value);
  const today = dayKey(value) === dayKey(new Date());
  return <span className="mtr-date" data-today={today || undefined} aria-hidden="true"><small>{weekday}</small><strong>{day}</strong><small>Th {Number(month)}</small></span>;
}

export function ResultBadge({ result }: { result: RoundResult }) {
  const info = RESULT_INFO[result];
  const Icon = result === "PASS" ? CheckCircle2 : result === "FAIL" ? XCircle : Clock;
  return <span className="mtr-badge" data-tone={info.tone}><Icon aria-hidden="true" />{info.label}</span>;
}

const STAGE_BADGE: Record<RoundStage, { label: string; tone: string } | null> = {
  review: { label: "Cần chấm", tone: "review" },
  upcoming: { label: "Sắp diễn ra", tone: "upcoming" },
  unscheduled: { label: "Chưa hẹn lịch", tone: "muted" },
  done: null,
  locked: { label: "Đã khoá", tone: "muted" },
};

/** Nhãn trạng thái: đã có kết quả thì hiện kết quả, chưa có thì hiện bước hiện tại. */
export function StageBadge({ item }: { item: MentorRoundItem }) {
  const stage = stageOf(item);
  if (item.round.result !== "PENDING") return <ResultBadge result={item.round.result} />;
  const badge = STAGE_BADGE[stage];
  return badge ? <span className="mtr-badge" data-tone={badge.tone}>{badge.label}</span> : null;
}

export function RoundInfo({ item, as: Title = "h3" }: { item: MentorRoundItem; as?: "h2" | "h3" }) {
  const { round } = item;
  return <div className="mtr-row-main">
    <p className="inline-flex items-center gap-1"><Building2 className="size-3.5 text-[var(--brand)]" aria-hidden="true" />{item.companyName}</p>
    <Title>Vòng {round.roundNo} · {round.name}</Title>
    <div className="mtr-row-meta">
      <span>{item.jobTitle}</span>
      {round.scheduledAt
        ? <span><Clock aria-hidden="true" />{formatTime(round.scheduledAt)} · {relativeDay(round.scheduledAt)}</span>
        : <span><CircleDashed aria-hidden="true" />Chưa hẹn lịch</span>}
      {round.score != null && <span className="mtr-score">{round.score}<small>/100</small></span>}
    </div>
  </div>;
}

/** Một dòng vòng phỏng vấn dẫn tới thẻ chấm điểm tương ứng. */
export function RoundRow({ item }: { item: MentorRoundItem }) {
  return <Link href={`/mentor/reviews?round=${item.round.id}`} className="mtr-row">
    <DateBlock value={item.round.scheduledAt} />
    <RoundInfo item={item} />
    <StageBadge item={item} />
  </Link>;
}

const RESULT_OPTIONS: { value: RoundResult; label: string; icon: typeof Clock }[] = [
  { value: "PASS", label: "Đạt", icon: CheckCircle2 },
  { value: "FAIL", label: "Không đạt", icon: XCircle },
  { value: "PENDING", label: "Chờ kết quả", icon: Clock },
];

/**
 * Khung chấm điểm: kết quả (bắt buộc), điểm 0–100 (tuỳ chọn, chỉ khi đã có kết quả), ghi chú tối đa 2.000 ký tự.
 * Backend chỉ nhận khi đơn còn ở bước phỏng vấn; lỗi 409 hiện thông báo và danh sách tự tải lại.
 */
export function RoundEvaluation({ item }: { item: MentorRoundItem }) {
  const { round } = item;
  const id = useId();
  const [result, setResult] = useState<RoundResult>(round.result);
  const [score, setScore] = useState(round.score?.toString() ?? "");
  const [note, setNote] = useState(round.note ?? "");
  const [savedAt, setSavedAt] = useState<string | null>(null);
  const save = useSaveMentorRound();

  const numericScore = score === "" ? null : Number(score);
  const scoreValid = numericScore == null || (Number.isFinite(numericScore) && numericScore >= 0 && numericScore <= 100);
  const dirty = result !== round.result || (round.score?.toString() ?? "") !== score || (round.note ?? "") !== note;
  const band = numericScore != null && scoreValid && result !== "PENDING" ? scoreBand(numericScore) : null;

  function submit() {
    if (!scoreValid) return;
    save.mutate({ roundId: round.id, result, score: result === "PENDING" ? null : numericScore, note: note.trim() || null }, {
      onSuccess: () => setSavedAt(formatTime(new Date().toISOString())),
    });
  }
  function reset() {
    setResult(round.result); setScore(round.score?.toString() ?? ""); setNote(round.note ?? ""); save.reset();
  }

  return <div className="mtr-editor">
    <fieldset>
      <legend className="mtr-field-label">Kết quả vòng phỏng vấn</legend>
      <div className="mtr-results">
        {RESULT_OPTIONS.map(option => <label key={option.value} className="mtr-result" data-value={option.value}>
          <input type="radio" name={`${id}-result`} value={option.value} checked={result === option.value} disabled={save.isPending}
            onChange={() => setResult(option.value)} />
          <option.icon aria-hidden="true" />{option.label}
        </label>)}
      </div>
    </fieldset>

    <div>
      <label className="mtr-field-label" htmlFor={`${id}-score`}>Điểm chuyên môn <span className="font-normal text-[var(--text-muted)]">(0–100, không bắt buộc)</span></label>
      <div className="mtr-score-row">
        <input type="range" className="mtr-range" min={0} max={100} step={1} aria-label="Kéo để chọn điểm"
          value={numericScore != null && scoreValid ? numericScore : 0} disabled={result === "PENDING" || save.isPending}
          onChange={event => setScore(event.target.value)} />
        <input id={`${id}-score`} type="number" inputMode="decimal" min={0} max={100} step={0.5} placeholder="—"
          className="mtr-score-input rounded-lg border border-input bg-white px-3 text-base md:text-sm" value={result === "PENDING" ? "" : score}
          disabled={result === "PENDING" || save.isPending} aria-invalid={!scoreValid || undefined} aria-describedby={`${id}-score-hint`}
          onChange={event => setScore(event.target.value)} />
        {band && <span className="mtr-band" data-tone={band.tone}>{band.label}</span>}
      </div>
      <p id={`${id}-score-hint`} className="mtr-hint" role={scoreValid ? undefined : "alert"}>
        {!scoreValid ? "Điểm phải từ 0 đến 100." : result === "PENDING" ? "Chọn Đạt hoặc Không đạt để nhập điểm." : "≥ 85 Xuất sắc · ≥ 70 Tốt · ≥ 50 Đạt yêu cầu"}
      </p>
    </div>

    <div>
      <label className="mtr-field-label" htmlFor={`${id}-note`}>Nhận xét chuyên môn <span className="mtr-counter">{note.length}/2000</span></label>
      <textarea id={`${id}-note`} rows={4} maxLength={2000} value={note} disabled={save.isPending} onChange={event => setNote(event.target.value)}
        placeholder="Điểm mạnh, điểm cần cải thiện và đề xuất cho vòng tiếp theo…"
        className="w-full rounded-lg border border-input bg-white px-3 py-2 text-base md:text-sm" />
    </div>

    {save.isError && <p role="alert" className="flex items-center gap-2 text-sm font-medium text-destructive"><AlertCircle className="size-4" aria-hidden="true" />{getErrorMessage(save.error)}</p>}

    <div className="mtr-editor-foot">
      <span className="mtr-save-state" role="status" data-tone={dirty ? "dirty" : savedAt ? "saved" : undefined}>
        {dirty ? <><CircleDashed aria-hidden="true" />Có thay đổi chưa lưu</> : savedAt ? <><CheckCircle2 aria-hidden="true" />Đã lưu lúc {savedAt}</> : null}
      </span>
      <div className="flex gap-2">
        {dirty && <Button variant="outline" disabled={save.isPending} onClick={reset}>Huỷ thay đổi</Button>}
        <Button disabled={!dirty || !scoreValid || save.isPending} onClick={submit}><Save aria-hidden="true" />{save.isPending ? "Đang lưu…" : "Lưu đánh giá"}</Button>
      </div>
    </div>
  </div>;
}
