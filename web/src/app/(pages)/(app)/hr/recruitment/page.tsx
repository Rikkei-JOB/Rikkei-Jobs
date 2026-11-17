import { Suspense } from "react";
import { ApplicationTable } from "@/app/components/recruitment/application-table";
export default function HrRecruitmentPage() {
  return <Suspense fallback={<p className="p-8">Đang tải…</p>}><ApplicationTable role="HR" /></Suspense>;
}
