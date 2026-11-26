"use client";

import { WorkProfilePage } from "@/app/components/work-profile/work-profile";
import { useAdminUserSummary, useSkillCatalogAdmin } from "@/hooks/use-admin";

/** Hồ sơ quản trị viên: trang hồ sơ nghề nghiệp chung, số liệu là phạm vi hệ thống đang quản lý. */
export function AdminProfilePage() {
  const users = useAdminUserSummary();
  const skills = useSkillCatalogAdmin();
  return <WorkProfilePage role={{
    roleLabel: "Quản trị",
    badge: "Quản trị hệ thống",
    suggestions: ["Quản trị hệ thống", "Bảo mật tài khoản", "Danh mục kỹ năng", "Phân quyền", "Tích hợp LMS", "Vận hành nền tảng"],
    bioPlaceholder: "Phạm vi quản trị, đầu mối liên hệ khi người dùng cần hỗ trợ tài khoản…",
    stats: [
      { label: "Tài khoản", value: users.data?.total ?? null },
      { label: "Đang khoá", value: users.data?.locked ?? null },
      { label: "Kỹ năng", value: skills.data?.length ?? null },
    ],
  }} />;
}
