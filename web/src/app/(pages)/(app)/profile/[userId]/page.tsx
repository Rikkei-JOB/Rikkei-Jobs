"use client";

import * as React from "react";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { useAuthStore } from "@/hooks/use-auth-store";
import { ROLE_HOME } from "@/config/roles";
import { SkillProfile } from "@/app/components/skills/skill-profile";

/**
 * Xem hồ sơ kỹ năng của một ứng viên (chỉ đọc, HS-01). Mọi vai trò đã đăng nhập vào được;
 * backend chỉ trả dữ liệu khi hồ sơ đã công khai và được duyệt (ngược lại 404).
 * Đăng nhập, thanh trên và menu do layout (app) lo.
 */
export default function CandidateProfilePage({
  params,
}: {
  params: Promise<{ userId: string }>;
}) {
  const { userId } = React.use(params);
  const user = useAuthStore((s) => s.user);
  if (!user) return null;

  return (
    <div className="mx-auto max-w-6xl space-y-4">
      <Link
        href={ROLE_HOME[user.role] ?? "/"}
        className="inline-flex items-center gap-1.5 text-xs font-medium text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" />
        <span>Quay lại trang chủ</span>
      </Link>
      <SkillProfile userId={userId} readOnly={true} />
    </div>
  );
}
