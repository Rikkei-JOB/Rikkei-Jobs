"use client";

import { useEffect, useState } from "react";
import { Button } from "@/app/components/ui/button";
import type { EmploymentRecord } from "@/interface/offer";
import { getErrorMessage } from "@/services/api";
import { offerService } from "@/services/offer.service";

export function EmploymentRecords() {
  const [records, setRecords] = useState<EmploymentRecord[] | null>(null);
  const [error, setError] = useState("");
  const [version, setVersion] = useState(0);
  useEffect(() => {
    let active = true;
    offerService.employmentRecords().then(value => { if (active) { setRecords(value); setError(""); } })
      .catch(cause => { if (active) setError(getErrorMessage(cause)); });
    return () => { active = false; };
  }, [version]);
  return <section className="rounded-xl border bg-background p-5">
    <h2 className="text-lg font-semibold">Kinh nghiệm</h2>
    {error && <div role="alert" className="mt-3 space-y-2 text-sm"><p>{error}</p><Button variant="outline" onClick={() => setVersion(v => v + 1)}>Thử lại</Button></div>}
    {!records && !error && <p role="status" className="mt-3 text-sm">Đang tải kinh nghiệm…</p>}
    {records?.length === 0 && <p className="mt-3 text-sm text-muted-foreground">Chưa có công việc đã nhận.</p>}
    {records?.map(record => <article key={record.id} className="mt-3 rounded-lg border p-3">
      <h3 className="font-medium">{record.position} tại {record.companyName}</h3>
      <p className="mt-1 text-sm text-muted-foreground">Bắt đầu: {record.startDate}{record.confirmedAt ? " · Đã xác nhận đi làm" : ""}</p>
    </article>)}
  </section>;
}
