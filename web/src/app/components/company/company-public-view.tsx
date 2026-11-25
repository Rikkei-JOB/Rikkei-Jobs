import { CompanyMark } from "@/app/components/company/company-mark";
import { LOCKED_JOBS_NOTICE } from "@/app/components/company/company-open-jobs";
import { LEVEL_LABEL, formatDate } from "@/app/components/job/job-labels";
import { deadlineHint } from "@/app/components/job/job-deadline";
import type { Company } from "@/interface/company";
import type { JobSummary } from "@/interface/job";

/** Chỉ cho phép liên kết http/https để không render URL kiểu javascript:. */
function isHttpUrl(value: string | null): value is string {
  return value !== null && /^https?:\/\//i.test(value);
}

const SECTION_CLASS = "space-y-2 rounded-xl border border-border bg-white p-4";

/**
 * Phần thân trang công ty như ứng viên thấy (phần đầu trang do CompanyHeader hiển thị).
 * Không hiện thông tin nội bộ (mã số thuế, trạng thái, ngày tạo).
 */
export function CompanyPublicView({ company, jobs }: { company: Company; jobs: JobSummary[] }) {
  const locked = company.status === "LOCKED";
  return (
    <div className="space-y-4">
      <section className={SECTION_CLASS}>
        <h2 className="font-heading text-sm font-semibold">Giới thiệu</h2>
        {company.description ? (
          <p className="text-xs/relaxed break-words whitespace-pre-line">{company.description}</p>
        ) : (
          <p className="text-xs text-muted-foreground">Công ty chưa có phần giới thiệu.</p>
        )}
      </section>

      <section className={SECTION_CLASS}>
        <h2 className="font-heading text-sm font-semibold">Thông tin liên hệ</h2>
        {isHttpUrl(company.website) || company.address || company.city ? (
          <dl className="space-y-1 text-xs/relaxed">
            {company.address ? (
              <div>
                <dt className="inline text-muted-foreground">Địa chỉ: </dt>
                <dd className="inline">{company.address}</dd>
              </div>
            ) : null}
            {company.city ? (
              <div>
                <dt className="inline text-muted-foreground">Thành phố: </dt>
                <dd className="inline">{company.city}</dd>
              </div>
            ) : null}
            {isHttpUrl(company.website) ? (
              <div>
                <dt className="inline text-muted-foreground">Website: </dt>
                <dd className="inline break-all">
                  <a
                    href={company.website}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-primary underline underline-offset-2"
                  >
                    {company.website}
                  </a>
                </dd>
              </div>
            ) : null}
          </dl>
        ) : (
          <p className="text-xs text-muted-foreground">Công ty chưa có thông tin liên hệ.</p>
        )}
      </section>

      <section className={SECTION_CLASS}>
        <h2 className="font-heading text-sm font-semibold">Việc làm đang tuyển ({jobs.length})</h2>
        {locked ? <p className="text-xs text-muted-foreground">{LOCKED_JOBS_NOTICE}</p> : null}
        {jobs.length === 0 ? (
          <p className="text-xs text-muted-foreground">Hiện chưa có vị trí nào đang tuyển.</p>
        ) : null}
        {jobs.length > 0 ? (
          <ul className="grid gap-3 sm:grid-cols-2">
            {jobs.map((job) => {
              const hint = deadlineHint(job.deadline);
              return (
                <li key={job.id} className="flex items-start gap-3 rounded-lg border border-border p-3">
                  <CompanyMark name={company.name} logoUrl={company.logoUrl} size={24} />
                  <div className="min-w-0 space-y-0.5">
                    <p className="text-sm font-medium break-words">{job.title}</p>
                    {job.level ? <p className="text-xs text-muted-foreground">{LEVEL_LABEL[job.level]}</p> : null}
                    <p className="text-xs text-muted-foreground">
                      Hạn nộp {formatDate(job.deadline)}{" "}
                      <span className={hint.urgent ? "font-medium text-destructive" : ""}>({hint.text})</span>
                    </p>
                  </div>
                </li>
              );
            })}
          </ul>
        ) : null}
      </section>
    </div>
  );
}
