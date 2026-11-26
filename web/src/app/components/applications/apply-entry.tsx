"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { BriefcaseBusiness } from "lucide-react";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import { applicationLinks } from "@/config/application-status";
import { ApplyDialog } from "./apply-dialog";

export function ApplyEntry() {
  const allowed = useAuthGuard({ roles: ["CANDIDATE"] });
  const jobId = useSearchParams().get("jobId");
  const router = useRouter();
  if (!allowed) return null;

  return (
    <div className="ptit-page max-w-3xl py-4">
      <div className="ptit-surface p-6 sm:p-8">
        <div className="flex items-center gap-3">
          <span className="flex size-11 items-center justify-center rounded-xl border border-[#FECDD3] bg-[#FFF1F2] text-primary">
            <BriefcaseBusiness className="size-5" aria-hidden="true" />
          </span>
          <div>
            <p className="ptit-eyebrow mb-0.5">NỘP HỒ SƠ</p>
            <h1 className="text-2xl font-bold text-heading">Ứng tuyển</h1>
          </div>
        </div>
        {!jobId ? (
          <p className="mt-4 text-sm leading-relaxed text-body">
            Chọn vị trí từ{" "}
            <Link href={applicationLinks.jobs} className="font-semibold text-primary underline underline-offset-4">
              trang Việc làm
            </Link>{" "}
            để ứng tuyển.
          </p>
        ) : (
          <ApplyDialog
            jobId={jobId}
            open
            onOpenChange={(open) => {
              if (!open) router.push("/candidate/applications");
            }}
            onApplied={(a) => router.push(`/candidate/applications/${a.id}`)}
          />
        )}
      </div>
    </div>
  );
}
