"use client";

import * as React from "react";
import { Alert } from "@/app/components/ui/alert";
import { Button } from "@/app/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/app/components/ui/card";
import { Input } from "@/app/components/ui/input";
import { useSaveWeights, useScoreWeights } from "@/hooks/use-admin";
import type { ScoreWeights, WeightSource } from "@/interface/admin";
import { cn } from "cn";
import { Field } from "./skills-tab";
import { WEIGHT_SOURCE_LABEL, errorText, formatDateTime } from "./admin-format";

/** Tab Trọng số: 4 nguồn điểm, tổng phải bằng 100 (máy chủ kiểm lại). */
export function WeightsTab() {
  const { data, error, isPending } = useScoreWeights();
  // Thông báo nằm ngoài form: lưu xong dữ liệu tải lại làm form dựng lại (key theo updatedAt).
  const [saved, setSaved] = React.useState(false);
  if (error) return <Alert variant="destructive">{errorText(error)}</Alert>;
  if (isPending || !data) return <p className="text-xs text-muted-foreground">Đang tải...</p>;
  return (
    <div className="space-y-3">
      {saved ? <Alert variant="success">Đã lưu trọng số.</Alert> : null}
      <WeightsForm key={data.updatedAt ?? "initial"} current={data} onSaved={setSaved} />
    </div>
  );
}

function WeightsForm({ current, onSaved }: { current: ScoreWeights; onSaved: (saved: boolean) => void }) {
  const save = useSaveWeights();
  const [values, setValues] = React.useState<Record<string, string>>(
    Object.fromEntries(current.weights.map((w) => [w.source, String(w.weight)])),
  );
  const [reason, setReason] = React.useState("");

  const total = Object.values(values).reduce((sum, v) => sum + (Number(v) || 0), 0);
  const totalOk = Math.abs(total - 100) < 1e-9;

  function submit(e: React.FormEvent) {
    e.preventDefault();
    onSaved(false);
    save.mutate(
      {
        weights: current.weights.map((w) => ({ source: w.source, weight: Number(values[w.source]) })),
        reason: reason.trim(),
      },
      { onSuccess: () => onSaved(true) },
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Trọng số nguồn điểm</CardTitle>
        <CardDescription>
          Tỉ lệ (%) của từng nguồn khi gộp điểm đánh giá ứng viên. Tổng phải bằng 100.
          {current.updatedAt ? ` Cập nhật lần cuối ${formatDateTime(current.updatedAt)}.` : null}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={submit} className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {current.weights.map((w) => (
              <Field key={w.source} label={WEIGHT_SOURCE_LABEL[w.source as WeightSource]} id={`weight-${w.source}`}>
                <Input
                  id={`weight-${w.source}`}
                  type="number"
                  inputMode="decimal"
                  min={0}
                  max={100}
                  step="0.01"
                  value={values[w.source] ?? ""}
                  onChange={(e) => setValues((prev) => ({ ...prev, [w.source]: e.target.value }))}
                  required
                />
              </Field>
            ))}
          </div>
          <p className={cn("text-xs font-medium", totalOk ? "text-emerald-700 dark:text-emerald-400" : "text-destructive")}>
            Tổng: {Number(total.toFixed(2))} / 100 {totalOk ? "" : "(phải bằng 100)"}
          </p>
          <Field label="Lý do (không bắt buộc)" id="weight-reason">
            <Input id="weight-reason" value={reason} maxLength={500} onChange={(e) => setReason(e.target.value)} />
          </Field>
          {save.error ? <Alert variant="destructive">{errorText(save.error)}</Alert> : null}
          <Button type="submit" size="sm" disabled={save.isPending}>
            {save.isPending ? "Đang lưu..." : "Lưu trọng số"}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
