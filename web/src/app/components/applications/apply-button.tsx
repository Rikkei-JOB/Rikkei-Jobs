"use client";

import { useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Button } from "@/app/components/ui/button";
import { useAuthStore } from "@/hooks/use-auth-store";
import { ApplyDialog } from "./apply-dialog";

export function ApplyButton({ jobId, acceptingApplications, jobTitle, companyName, className = "" }: {
  jobId: string; acceptingApplications: boolean; jobTitle?: string; companyName?: string; className?: string;
}) {
  const [open, setOpen] = useState(false);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const user = useAuthStore(s => s.user);
  const hydrated = useAuthStore(s => s.hasHydrated);
  const router = useRouter();
  if (hydrated && !user) return <Button render={<Link href="/login" />} nativeButton={false} className={`career-primary h-11 px-5 ${className}`}>Đăng nhập để ứng tuyển</Button>;
  return <>
    <Button ref={triggerRef} data-application-open={open ? "" : undefined} aria-haspopup="dialog" aria-expanded={open}
      disabled={!hydrated || !acceptingApplications || user?.role !== "CANDIDATE" || user.mustChangePassword}
      onClick={() => setOpen(true)} className={`career-primary h-11 px-5 ${className}`}>{acceptingApplications ? "Ứng tuyển" : "Đã đóng nhận hồ sơ"}</Button>
    {open && <ApplyDialog jobId={jobId} jobTitle={jobTitle} companyName={companyName} returnFocus={triggerRef}
      open={open} onOpenChange={setOpen} onApplied={a => router.push(`/candidate/applications/${a.id}`)} />}
  </>;
}
