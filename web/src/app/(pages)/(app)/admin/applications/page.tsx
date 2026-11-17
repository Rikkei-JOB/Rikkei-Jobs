import { Suspense } from "react";
import { ApplicationTable } from "@/app/components/recruitment/application-table";

export default function AdminApplicationsPage() {
  return <Suspense fallback={<p className="p-8">Đang tải…</p>}><ApplicationTable role="ADMIN" /></Suspense>;
}
