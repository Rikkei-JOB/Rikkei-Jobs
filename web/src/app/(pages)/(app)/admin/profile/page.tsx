import type { Metadata } from "next";
import { AdminProfilePage } from "@/app/components/admin/admin-profile";

export const metadata: Metadata = { title: "Hồ sơ quản trị | Rikkei Jobs" };

export default function AdminProfileRoute() {
  return <AdminProfilePage />;
}
