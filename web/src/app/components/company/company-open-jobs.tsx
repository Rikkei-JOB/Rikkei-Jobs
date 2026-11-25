import Link from "next/link";
import { LEVEL_LABEL, formatDate } from "@/app/components/job/job-labels";
import { deadlineHint } from "@/app/components/job/job-deadline";
import type { JobSummary } from "@/interface/job";

export const LOCKED_JOBS_NOTICE = "Công ty đang bị khoá, ứng viên hiện không thấy việc làm";

/** Danh sách JD đang tuyển của công ty; mỗi dòng mở trang JD của HR. */
export function CompanyOpenJobs({ jobs, locked }: { jobs: JobSummary[]; locked: boolean }) {
  return (
    <div className="space-y-3">
      {locked ? <p className="text-xs text-muted-foreground">{LOCKED_JOBS_NOTICE}</p> : null}
      {jobs.length === 0 ? (
        <p className="text-xs text-muted-foreground">
          Chưa có vị trí đang tuyển.
          {locked ? null : (
            <>
              {" "}
              <Link href="/hr/jobs/new" className="text-primary underline underline-offset-2">
                Tạo JD
              </Link>
            </>
          )}
        </p>
      ) : (
        <ul className="divide-y divide-border rounded-lg border border-border bg-white">
          {jobs.map((job) => {
            const hint = deadlineHint(job.deadline);
            return (
              <li key={job.id}>
                <Link
                  href={`/hr/jobs/${job.id}`}
                  className="flex flex-wrap items-center justify-between gap-x-4 gap-y-1 px-4 py-3 text-sm hover:bg-muted/50"
                >
                  <span className="min-w-0 font-medium break-words">
                    {job.title}
                    {job.level ? <span className="ml-2 text-xs text-muted-foreground">{LEVEL_LABEL[job.level]}</span> : null}
                  </span>
                  <span className="text-xs text-muted-foreground">
                    Hạn nộp {formatDate(job.deadline)}{" "}
                    <span className={hint.urgent ? "font-medium text-destructive" : ""}>({hint.text})</span>
                  </span>
                </Link>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
