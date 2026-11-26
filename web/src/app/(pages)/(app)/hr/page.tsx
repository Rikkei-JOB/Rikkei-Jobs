import type { Metadata } from "next";
import { HrDashboard } from "@/app/components/hr-dashboard/hr-dashboard";

export const metadata: Metadata = { title: "Tổng quan tuyển dụng | Rikkei Jobs" };

export default function HrHomePage() {
  return <HrDashboard />;
}
