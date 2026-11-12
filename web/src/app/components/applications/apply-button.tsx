"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Button } from "@/app/components/ui/button";
import { useAuthStore } from "@/hooks/use-auth-store";
import { ApplyDialog } from "./apply-dialog";

export function ApplyButton({ jobId, acceptingApplications }: { jobId: string; acceptingApplications: boolean }) {
  const [open, setOpen] = useState(false);
  const user = useAuthStore(s => s.user);
  const hydrated = useAuthStore(s => s.hasHydrated);
  const router = useRouter();
  if (hydrated && !user) return <Button render={<Link href="/login" />} nativeButton={false} className="h-10 px-5">Đăng nhập để ứng tuyển</Button>;
  return <>
    <Button disabled={!hydrated || !acceptingApplications || user?.role !== "CANDIDATE" || user.mustChangePassword}
      onClick={() => setOpen(true)} className="h-10 px-5">{acceptingApplications ? "Ứng tuyển" : "Đã đóng nhận hồ sơ"}</Button>
    {open && <ApplyDialog jobId={jobId} open={open} onOpenChange={setOpen} onApplied={a => router.push(`/candidate/applications/${a.id}`)} />}
  </>;
}
