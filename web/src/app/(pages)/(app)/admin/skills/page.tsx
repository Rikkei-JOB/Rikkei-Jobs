import type { Metadata } from "next";
import { AdminSkills } from "@/app/components/admin/admin-skills";

export const metadata: Metadata = { title: "Danh mục kỹ năng | Quản trị Rikkei Jobs" };

export default function AdminSkillsPage() {
  return <AdminSkills />;
}
