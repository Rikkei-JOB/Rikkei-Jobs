import type { Metadata } from "next";
import { StaffOverview } from "@/app/components/staff-jobs/staff-overview";

export const metadata: Metadata = { title: "Tổng quan cán bộ | Rikkei Jobs" };

export default function StaffHomePage() {
  return <StaffOverview />;
}
