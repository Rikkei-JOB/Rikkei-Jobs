import type { Metadata } from "next";
import { AdminUsers } from "@/app/components/admin/admin-users";

export const metadata: Metadata = { title: "Tài khoản | Quản trị Rikkei Jobs" };

export default function AdminUsersPage() {
  return <AdminUsers />;
}
