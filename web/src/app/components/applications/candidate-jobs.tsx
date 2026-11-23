"use client";

import { Suspense, useEffect, useState } from "react";
import { AlertCircle, BriefcaseBusiness } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import { candidateJobService, type CandidateJob } from "@/services/candidate-job.service";
import { getErrorMessage } from "@/services/api";
import { CandidateJobList } from "./candidate-job-list";
import { CandidateJobDetail } from "./candidate-job-detail";
import "./career.css";

export function CandidateJobs({ jobId }: { jobId?: string }) {
  return <Suspense fallback={<p role="status">Đang tải việc làm…</p>}><CandidateJobsContent jobId={jobId} /></Suspense>;
}

function CandidateJobsContent({ jobId }: { jobId?: string }) {
  const allowed = useAuthGuard({ roles: ["CANDIDATE"] });
  const [jobs, setJobs] = useState<CandidateJob[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    if (!allowed) return;
    let active = true;
    const request = jobId ? candidateJobService.get(jobId).then(job => [job]) : candidateJobService.list();
    request.then(data => {
      if (active) { setJobs(data); setError(""); }
    }).catch(reason => {
      if (active) setError(getErrorMessage(reason));
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [allowed, jobId, version]);

  if (!allowed) return null;
  const feedback = loading ? (
    <div className="career-feedback" role="status">
      <BriefcaseBusiness aria-hidden="true" /><p>Đang tải việc làm…</p>
      <div className="career-skeleton" /><div className="career-skeleton" />
    </div>
  ) : error ? (
    <div className="career-feedback career-error" role="alert">
      <AlertCircle aria-hidden="true" /><h2>Chưa thể tải việc làm</h2><p>{error}</p>
      <Button className="career-secondary" variant="outline" onClick={() => { setLoading(true); setVersion(v => v + 1); }}>Thử lại</Button>
    </div>
  ) : null;

  return <section className="career career-page" aria-label="Cơ hội nghề nghiệp">
    {jobId ? feedback ?? (jobs[0] && <CandidateJobDetail job={jobs[0]} />)
      : <CandidateJobList jobs={jobs} feedback={feedback} />}
  </section>;
}
