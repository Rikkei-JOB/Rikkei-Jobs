import type { Metadata } from "next";
import { AdminOverview } from "@/app/components/admin/admin-overview";

export const metadata: Metadata = { title: "Quản trị | Rikkei Jobs" };

export default function AdminHomePage() {
  return <AdminOverview />;
}
