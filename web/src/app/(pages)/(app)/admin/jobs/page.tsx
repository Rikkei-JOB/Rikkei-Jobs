import type { Metadata } from "next";
import { StaffJobList } from "@/app/components/staff-jobs/staff-job-list";

export const metadata: Metadata = { title: "Duyệt JD | Quản trị Rikkei Jobs" };

/** Quản trị dùng chung màn Duyệt JD của cán bộ (DN-07), theo ma trận quyền NT-01. */
export default function AdminJobsPage() {
  return <StaffJobList base="/admin/jobs" />;
}
