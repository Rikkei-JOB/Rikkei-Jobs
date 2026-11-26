"use client";

import * as React from "react";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { useAuthStore } from "@/hooks/use-auth-store";
import { ROLE_HOME } from "@/config/roles";
import { SkillProfile } from "@/app/components/skills/skill-profile";
import { Portfolio } from "@/app/components/profile/portfolio";

/**
 * Xem hồ sơ kỹ năng của một ứng viên (chỉ đọc, HS-01).
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
    <div className="ptit-page max-w-6xl">
      <Link
        href={ROLE_HOME[user.role] ?? "/"}
        className="inline-flex w-fit items-center gap-1.5 text-sm font-semibold text-primary hover:underline"
      >
        <ArrowLeft className="size-4" aria-hidden="true" />
        <span>Quay lại trang chủ</span>
      </Link>
      <SkillProfile userId={userId} readOnly={true} />
      <Portfolio userId={userId} />
    </div>
  );
}
