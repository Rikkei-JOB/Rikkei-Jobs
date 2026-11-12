import { Suspense } from "react";
import { ApplyEntry } from "@/app/components/applications/apply-entry";
export default function NewApplicationPage() {
  return <Suspense fallback={<p className="p-8">Đang tải…</p>}><ApplyEntry /></Suspense>;
}
