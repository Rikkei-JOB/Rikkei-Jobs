"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import { ApplyDialog } from "./apply-dialog";

export function ApplyEntry() {
  const allowed = useAuthGuard({ roles: ["CANDIDATE"] });
  const jobId = useSearchParams().get("jobId");
  const router = useRouter();
  if (!allowed) return null;
  return <main className="mx-auto max-w-5xl px-6 py-10"><h1 className="text-2xl font-semibold">Ứng tuyển</h1>
    {!jobId ? <p className="mt-4">Chọn vị trí từ <Link href="/jobs" className="text-primary underline">trang Việc làm</Link> để ứng tuyển.</p>
      : <ApplyDialog jobId={jobId} open onOpenChange={open => { if (!open) router.push("/candidate/applications"); }} onApplied={a => router.push(`/candidate/applications/${a.id}`)} />}
  </main>;
}
