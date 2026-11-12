import { Suspense } from "react";
import { ApplicationsList } from "@/app/components/applications/applications-list";
export default function ApplicationsPage() {
  return <Suspense fallback={<p className="p-8">Đang tải…</p>}><ApplicationsList /></Suspense>;
}
