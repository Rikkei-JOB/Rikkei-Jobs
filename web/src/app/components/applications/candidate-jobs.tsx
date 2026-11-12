"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Button } from "@/app/components/ui/button";
import { ApplyButton } from "./apply-button";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import { applicationLinks, formatApplicationDate } from "@/config/application-status";
import { candidateJobService, type CandidateJob } from "@/services/candidate-job.service";
import { getErrorMessage } from "@/services/api";

export function CandidateJobs({ jobId }: { jobId?: string }) {
  const allowed = useAuthGuard({ roles: ["CANDIDATE"] });
  const [jobs, setJobs] = useState<CandidateJob[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    if (!allowed) return;
    let active = true;
    const request = jobId
      ? candidateJobService.get(jobId).then(job => [job])
      : candidateJobService.list();
    request.then(data => {
      if (active) {
        setJobs(data);
        setError("");
      }
    }).catch(reason => {
      if (active) setError(getErrorMessage(reason));
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, [allowed, jobId, version]);

  if (!allowed) return null;

  return (
    <main className="mx-auto w-full max-w-4xl space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold">Việc làm</h1>
          <p className="mt-2 text-sm text-muted-foreground">Dữ liệu mẫu cho luồng ứng tuyển.</p>
        </div>
        <Link href="/candidate/applications" className="text-sm font-medium text-primary underline">
          Ứng tuyển của tôi
        </Link>
      </div>
      {jobId && <Link href={applicationLinks.jobs} className="inline-block text-sm underline">Tất cả việc làm</Link>}
      {loading && <p role="status">Đang tải việc làm…</p>}
      {error && (
        <div role="alert" className="space-y-3 rounded-lg border border-destructive/30 p-4 text-sm">
          <p>{error}</p>
          <Button variant="outline" onClick={() => { setLoading(true); setVersion(value => value + 1); }}>Thử lại</Button>
        </div>
      )}
      {!loading && !error && jobs.length === 0 && <p>Hiện chưa có vị trí đang nhận hồ sơ.</p>}
      {!loading && !error && jobs.map(job => (
        <article key={job.jobId} className="space-y-4 rounded-xl border bg-card p-6">
          <div>
            <h2 className="text-xl font-semibold">
              <Link href={applicationLinks.job(job.jobId)} className="hover:underline">{job.jobTitle}</Link>
            </h2>
            <p className="mt-1 text-muted-foreground">{job.companyName}</p>
          </div>
          <p className="text-sm">Hạn ứng tuyển {formatApplicationDate(job.deadline)}</p>
          <ApplyButton jobId={job.jobId} acceptingApplications={job.acceptingApplications} />
        </article>
      ))}
    </main>
  );
}
