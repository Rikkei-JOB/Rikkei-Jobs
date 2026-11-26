"use client";
import { TaskBanner } from "@/app/components/ui/task-banner";
import { CandidateContactPanel } from "./candidate-contact-panel";
export function HrContactPage({ userId }: { userId: string }) {
  return <div className="hr-page">
    <TaskBanner compact eyebrow="Hồ sơ ứng viên" title="Liên hệ ứng viên" description="Xin mở liên hệ cho JD của công ty hoặc xem thông tin khi đã có quyền." illustration="applications" />
    <div className="w-full max-w-3xl"><CandidateContactPanel userId={userId} /></div>
  </div>;
}
