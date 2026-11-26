"use client";
import { CandidateContactPanel } from "./candidate-contact-panel";
export function HrContactPage({ userId }: { userId: string }) {
  return <div className="mx-auto w-full max-w-3xl space-y-4 p-4 md:p-6"><h1 className="text-2xl font-bold text-heading">Liên hệ ứng viên</h1><p className="text-sm text-muted-foreground">Xin mở liên hệ cho JD của công ty hoặc xem thông tin khi đã có quyền.</p><CandidateContactPanel userId={userId} /></div>;
}
