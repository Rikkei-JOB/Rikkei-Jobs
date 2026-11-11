"use client";

import * as React from "react";
import Link from "next/link";
import { ArrowLeft, LogOut } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import { useAuthStore } from "@/hooks/use-auth-store";
import { useLogout } from "@/hooks/use-logout";
import { ROLE_HOME } from "@/config/roles";
import { SkillProfile } from "@/app/components/skills/skill-profile";

export default function CandidateProfilePage({
  params,
}: {
  params: Promise<{ userId: string }>;
}) {
  const { userId } = React.use(params);
  const allowed = useAuthGuard();
  const user = useAuthStore((s) => s.user);
  const logout = useLogout();

  if (!allowed || !user) return null;

  const homePath = ROLE_HOME[user.role] ?? "/";

  return (
    <div className="min-h-screen bg-muted/20">
      <header className="sticky top-0 z-40 border-b bg-background/95 backdrop-blur-sm">
        <div className="mx-auto flex h-14 max-w-6xl items-center justify-between px-4 sm:px-6">
          <div className="flex items-center gap-3">
            <Link
              href={homePath}
              className="inline-flex items-center gap-1.5 text-xs font-medium text-muted-foreground hover:text-foreground"
            >
              <ArrowLeft className="h-4 w-4" />
              <span>Quay lại trang chủ</span>
            </Link>
          </div>

          <div className="flex items-center gap-3 text-xs">
            <span className="text-muted-foreground hidden sm:inline">
              {user.fullName} ({user.role})
            </span>
            <Button variant="outline" size="sm" onClick={logout}>
              <LogOut className="mr-1.5 h-3.5 w-3.5" />
              Đăng xuất
            </Button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <SkillProfile userId={userId} readOnly={true} />
      </main>
    </div>
  );
}

