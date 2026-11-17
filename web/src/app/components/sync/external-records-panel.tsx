"use client";

import * as React from "react";
import { Pencil } from "lucide-react";
import { Alert } from "@/app/components/ui/alert";
import { Button } from "@/app/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/app/components/ui/card";
import { Input } from "@/app/components/ui/input";
import { Label } from "@/app/components/ui/label";
import { useExternalRecords, useOverrideRecord } from "@/hooks/use-sync";
import { ApiError, getErrorMessage } from "@/services/api";
import type { ExternalRecord, OverrideField } from "@/interface/sync";
import { SOURCE_LABEL, formatDate, formatDateTime } from "./sync-format";

const FIELD_LABEL: Record<OverrideField, string> = { score: "Điểm", title: "Tiêu đề" };

/** Bản ghi nguồn của một ứng viên; cán bộ sửa tay điểm hoặc tiêu đề kèm lý do. */
export function ExternalRecordsPanel({ candidate }: { candidate: { userId: string; fullName: string } }) {
  const { data: records, error, isPending } = useExternalRecords(candidate.userId);
  const [editing, setEditing] = React.useState<string | null>(null);

  return (
    <Card>
      <CardHeader>
        <CardTitle>Bản ghi nguồn của {candidate.fullName}</CardTitle>
        <CardDescription>
          Trường đã sửa tay được giữ nguyên ở các lần đồng bộ sau; cột &quot;Giá trị nguồn&quot; cho biết hệ thống ngoài đang trả gì.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        {error ? <Alert variant="destructive">{getErrorMessage(error)}</Alert> : null}
        {isPending ? <p className="text-xs text-muted-foreground">Đang tải...</p> : null}
        {records && records.length === 0 ? (
          <p className="text-xs text-muted-foreground">Chưa có bản ghi. Bấm &quot;Đồng bộ lại&quot; để lấy dữ liệu.</p>
        ) : null}

        <div className="overflow-x-auto">
          <table className="w-full min-w-[760px] text-left text-xs">
            <thead className="border-b border-border text-muted-foreground">
              <tr>
                <th className="py-2 pr-3 font-medium">Nguồn</th>
                <th className="py-2 pr-3 font-medium">Mã</th>
                <th className="py-2 pr-3 font-medium">Tiêu đề</th>
                <th className="py-2 pr-3 font-medium">Điểm</th>
                <th className="py-2 pr-3 font-medium">Giá trị nguồn</th>
                <th className="py-2 pr-3 font-medium">Ngày</th>
                <th className="py-2 pr-3 font-medium">Đồng bộ lúc</th>
                <th className="py-2 font-medium" />
              </tr>
            </thead>
            <tbody>
              {records?.map((record) => (
                <React.Fragment key={record.id}>
                  <tr className="border-b border-border/60 align-top">
                    <td className="py-2 pr-3">{SOURCE_LABEL[record.source]}</td>
                    <td className="py-2 pr-3">{record.sourceCode}</td>
                    <td className="py-2 pr-3">
                      {record.title}
                      {record.overriddenFields.includes("title") ? <OverriddenTag /> : null}
                    </td>
                    <td className="py-2 pr-3 whitespace-nowrap">
                      {record.score}/{record.scoreMax}
                      {record.overriddenFields.includes("score") ? <OverriddenTag /> : null}
                    </td>
                    <td className="py-2 pr-3 text-muted-foreground">{sourceValue(record)}</td>
                    <td className="py-2 pr-3 whitespace-nowrap">{formatDate(record.recordedAt)}</td>
                    <td className="py-2 pr-3 whitespace-nowrap">{formatDateTime(record.syncedAt)}</td>
                    <td className="py-2 text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        aria-expanded={editing === record.id}
                        onClick={() => setEditing(editing === record.id ? null : record.id)}
                      >
                        <Pencil />
                        Sửa tay
                      </Button>
                    </td>
                  </tr>
                  {editing === record.id ? (
                    <tr className="border-b border-border/60">
                      <td colSpan={8} className="bg-muted/30 p-3">
                        <OverrideForm record={record} userId={candidate.userId} onDone={() => setEditing(null)} />
                      </td>
                    </tr>
                  ) : null}
                </React.Fragment>
              ))}
            </tbody>
          </table>
        </div>
      </CardContent>
    </Card>
  );
}

function OverriddenTag() {
  return (
    <span className="ml-1.5 rounded-full border border-amber-500/30 bg-amber-500/10 px-1.5 py-0.5 text-[0.625rem] text-amber-700 dark:text-amber-400">
      sửa tay
    </span>
  );
}

/** Điểm và tên gốc trong payload của lần đồng bộ cuối (khác với giá trị đang áp dụng khi đã sửa tay). */
function sourceValue(record: ExternalRecord): string {
  const payload = record.payload ?? {};
  const score = payload.score ?? payload.finalScore;
  return score === undefined || score === null ? "—" : `Điểm ${String(score)}`;
}

function OverrideForm({ record, userId, onDone }: { record: ExternalRecord; userId: string; onDone: () => void }) {
  const override = useOverrideRecord(userId);
  const [field, setField] = React.useState<OverrideField>("score");
  const [value, setValue] = React.useState(String(record.score));
  const [reason, setReason] = React.useState("");
  const fieldErrors = override.error instanceof ApiError ? (override.error.errors as Record<string, string> | undefined) : undefined;

  function changeField(next: OverrideField) {
    setField(next);
    setValue(next === "score" ? String(record.score) : record.title);
  }

  function submit(event: React.FormEvent) {
    event.preventDefault();
    override.mutate({ recordId: record.id, payload: { field, value, reason } }, { onSuccess: onDone });
  }

  return (
    <div className="space-y-3">
      <form onSubmit={submit} className="grid gap-3 md:grid-cols-[140px_1fr_2fr_auto] md:items-end">
        <div className="space-y-1">
          <Label htmlFor={`field-${record.id}`}>Trường</Label>
          <select
            id={`field-${record.id}`}
            value={field}
            onChange={(e) => changeField(e.target.value as OverrideField)}
            className="h-7 w-full rounded-md border border-input bg-input/20 px-2 text-xs"
          >
            <option value="score">Điểm (thang {record.scoreMax})</option>
            <option value="title">Tiêu đề</option>
          </select>
        </div>
        <div className="space-y-1">
          <Label htmlFor={`value-${record.id}`}>Giá trị mới</Label>
          <Input
            id={`value-${record.id}`}
            value={value}
            onChange={(e) => setValue(e.target.value)}
            inputMode={field === "score" ? "decimal" : undefined}
            aria-invalid={Boolean(fieldErrors?.value)}
            required
          />
        </div>
        <div className="space-y-1">
          <Label htmlFor={`reason-${record.id}`}>Lý do</Label>
          <Input
            id={`reason-${record.id}`}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Ví dụ: điểm phúc khảo theo quyết định số 12"
            maxLength={500}
            aria-invalid={Boolean(fieldErrors?.reason)}
            required
          />
        </div>
        <div className="flex gap-2">
          <Button type="submit" size="sm" disabled={override.isPending || !reason.trim() || !value.trim()}>
            {override.isPending ? "Đang lưu..." : "Lưu"}
          </Button>
          <Button type="button" variant="outline" size="sm" onClick={onDone}>
            Huỷ
          </Button>
        </div>
      </form>

      {override.error ? (
        <Alert variant="destructive">
          {fieldErrors ? Object.values(fieldErrors).join(". ") : getErrorMessage(override.error)}
        </Alert>
      ) : null}

      {record.overrides.length > 0 ? (
        <div className="space-y-1">
          <div className="text-xs font-medium">Lịch sử sửa tay</div>
          <ul className="space-y-1 text-xs text-muted-foreground">
            {record.overrides.map((entry) => (
              <li key={`${entry.createdAt}-${entry.field}`}>
                {formatDateTime(entry.createdAt)}: {FIELD_LABEL[entry.field]} = {entry.value}. Lý do: {entry.reason}
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </div>
  );
}
