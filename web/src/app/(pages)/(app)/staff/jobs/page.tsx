import type { Metadata } from "next";
import { StaffJobList } from "@/app/components/staff-jobs/staff-job-list";

export const metadata: Metadata = { title: "Duyệt JD | Rikkei Jobs" };

export default function StaffJobsPage() {
  return <StaffJobList />;
}
