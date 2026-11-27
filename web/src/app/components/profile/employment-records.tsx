"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { BriefcaseBusiness } from "lucide-react";
import { Alert } from "@/app/components/ui/alert";
import { Button } from "@/app/components/ui/button";
import { Card, CardContent, CardHeader } from "@/app/components/ui/card";
import type { EmploymentRecord } from "@/interface/offer";
import { getErrorMessage } from "@/services/api";
import { offerService } from "@/services/offer.service";
import { ITEM_CARD, ITEM_TITLE, META, SECTION_TITLE } from "./display";

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
  return <Card>
    <CardHeader>
      <h2 className={SECTION_TITLE}>Kinh nghiệm{records?.length ? <span className="font-normal text-[#64748B]"> · {records.length}</span> : null}</h2>
      <p className={META}>Tự cập nhật khi bạn nhận offer và xác nhận đi làm.</p>
    </CardHeader>
    <CardContent className="space-y-4">
      {error ? <div className="space-y-2"><Alert variant="destructive">{error}</Alert><Button onClick={() => setVersion(v => v + 1)}>Thử lại kinh nghiệm</Button></div> : null}
      {!records && !error ? <p role="status">Đang tải kinh nghiệm…</p> : null}
      {records?.length === 0 ? <div className="flex flex-col items-center gap-1 rounded-xl border border-dashed border-[#CBD5E1] p-6 text-center text-[#64748B]">
        <BriefcaseBusiness className="size-6" aria-hidden />
        <span className="text-sm font-medium">Chưa có kinh nghiệm làm việc</span>
        <span className="text-xs">Ứng tuyển và nhận offer, công việc sẽ hiện ở đây sau khi bạn xác nhận đi làm.</span>
        <Link href="/jobs" className="mt-2 text-sm font-semibold text-[#B4232D] underline-offset-4 hover:underline">Tìm việc làm IT</Link>
      </div> : null}
      {records?.length ? <ul className="space-y-3">{records.map(record => <li key={record.id} className={`p-4 ${ITEM_CARD}`}>
        <h3 className={ITEM_TITLE}>{record.position} tại {record.companyName}</h3>
        <p className={`mt-1 ${META}`}>Bắt đầu: {record.startDate}{record.confirmedAt ? " · Đã xác nhận đi làm" : ""}</p>
      </li>)}</ul> : null}
    </CardContent>
  </Card>;
}
