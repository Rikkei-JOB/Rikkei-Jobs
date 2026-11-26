import type { Metadata } from "next";
import { StaffProfilePage } from "@/app/components/staff-jobs/staff-profile";

export const metadata: Metadata = { title: "Hồ sơ cán bộ | Rikkei Jobs" };

export default function StaffProfileRoute() {
  return <StaffProfilePage />;
}
