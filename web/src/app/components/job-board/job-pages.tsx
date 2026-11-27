"use client";

import { useMemo, type ReactNode } from "react";
import Link from "next/link";
import { AlertCircle, Briefcase, Building2, ChevronRight, Globe, MapPin, SearchX, Users } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { useBoardCompanies, useBoardCompany, useBoardJob, useBoardSearch } from "@/hooks/use-job-board";
import { ApiError, getErrorMessage } from "@/services/api";
import { EMPTY_FILTERS, type BoardCompanyItem } from "@/services/job-board.service";
import { CompanySummary, JobApplyAction, JobDetailView, companyHref, jobHref } from "./job-detail";
import { CompanyLogo, JobCard, SaveJobButton } from "./job-parts";
import { CompaniesIllustration, EmptySearchArt } from "./illustrations";
import "./job-board.css";

const isNotFound = (error: unknown) => error instanceof ApiError && error.status === 404;

function Failure({ error, onRetry }: { error: unknown; onRetry(): void }) {
  return <div className="jb-empty" role="alert"><AlertCircle aria-hidden="true" /><h3>Chưa thể tải dữ liệu</h3><p>{getErrorMessage(error)}</p>
    <Button variant="outline" onClick={onRetry}>Thử lại</Button></div>;
}

function NotFound({ title, description, href, action }: { title: string; description: string; href: string; action: string }) {
  return <div className="jb-empty"><EmptySearchArt className="jb-empty-art" /><h3>{title}</h3><p>{description}</p>
    <Button variant="outline" render={<Link href={href} />} nativeButton={false}>{action}</Button></div>;
}

export function JobDetailPage({ jobId, basePath }: { jobId: string; basePath: string }) {
  const { data: job, error, isPending, refetch } = useBoardJob(jobId);
  // Tin tương tự: cùng kỹ năng chính của tin đang xem (backend khớp từ khoá theo kỹ năng).
  const similarFilters = useMemo(() => ({ ...EMPTY_FILTERS, q: job?.skills[0] ?? "" }), [job]);
  const similarSearch = useBoardSearch(similarFilters, { size: 5 });
  const similar = (job?.skills.length ? similarSearch.data?.pages[0]?.items ?? [] : []).filter(other => other.id !== jobId).slice(0, 4);

  return <div className="jb">
    <div className="jb-container jb-page">
      <nav className="jb-breadcrumb" aria-label="Đường dẫn">
        <Link href={basePath}>Việc làm</Link><ChevronRight aria-hidden="true" />
        {job && <><Link href={companyHref(job.company.id)}>{job.company.name}</Link><ChevronRight aria-hidden="true" /><span aria-current="page">{job.title}</span></>}
      </nav>
      {isNotFound(error) ? <NotFound title="Không tìm thấy việc làm" description="Tin có thể đã hết hạn, đã đóng hoặc bị gỡ." href={basePath} action="Xem việc làm khác" />
        : error ? <Failure error={error} onRetry={() => refetch()} />
        : isPending || !job ? <div className="jb-skeleton" role="status" aria-label="Đang tải" style={{ height: 320 }} />
        : <>
          <div className="jb-page-grid">
            <div>
              <div className="jb-panel"><JobDetailView job={job} basePath={basePath} variant="page" /></div>
              {similar.length > 0 && <section className="jb-similar" aria-label="Việc làm tương tự"><h2>Việc làm tương tự</h2>
                <div className="jb-grid jb-grid-2">{similar.map(other => <JobCard key={other.id} job={other} href={jobHref(basePath, other.id)} />)}</div></section>}
            </div>
            <aside className="jb-page-aside jb-panel jb-panel-pad" aria-label={`Về ${job.company.name}`}>
              <div className="jb-detail-company"><CompanyLogo company={job.company} size="md" /><h3 className="text-[17px] font-bold">{job.company.name}</h3></div>
              <CompanySummary company={job.company} />
            </aside>
          </div>
          <div className="jb-mobile-apply"><JobApplyAction job={job} /><SaveJobButton jobId={job.id} jobTitle={job.title} large /></div>
        </>}
    </div>
  </div>;
}

export function CompanyDirectory() {
  const { data, error, isPending, refetch } = useBoardCompanies();
  return <div className="jb">
    <section className="jb-hero" aria-label="Công ty"><div className="jb-container jb-hero-inner jb-hero-split">
      <div><h1>Khám phá <em>công ty công nghệ</em> đang tuyển dụng</h1>
        <p className="jb-hero-lead">Tìm hiểu lĩnh vực, quy mô và các vị trí đang mở trước khi ứng tuyển.</p></div>
      <CompaniesIllustration className="jb-hero-art" />
    </div></section>
    <div className="jb-container jb-section">
      {error ? <Failure error={error} onRetry={() => refetch()} />
        : isPending ? <div className="jb-grid jb-grid-3">{[0, 1, 2].map(i => <div key={i} className="jb-skeleton" />)}</div>
        : data.length === 0 ? <div className="jb-empty"><Building2 aria-hidden="true" /><h3>Chưa có công ty</h3><p>Công ty sẽ xuất hiện khi cán bộ tạo hồ sơ doanh nghiệp.</p></div>
        : <CompanyGrid entries={data} />}
    </div>
  </div>;
}

export function CompanyGrid({ entries }: { entries: BoardCompanyItem[] }) {
  return <div className="jb-grid jb-grid-3">{entries.map(company =>
    <Link key={company.id} href={companyHref(company.id)} className="jb-company-tile">
      <CompanyLogo company={company} size="md" />
      <div><h3>{company.name}</h3>{company.industry && <p>{company.industry}</p>}</div>
      <div className="jb-meta">{company.city && <span><MapPin aria-hidden="true" />{company.city}</span>}{company.size && <span><Users aria-hidden="true" />{company.size}</span>}</div>
      <footer><strong>{company.openJobs} việc làm</strong><span className="jb-link">Xem công ty <ChevronRight aria-hidden="true" /></span></footer>
    </Link>)}</div>;
}

export function CompanyProfile({ companyId }: { companyId: string }) {
  const { data: company, error, isPending, refetch } = useBoardCompany(companyId);
  const search = useBoardSearch(EMPTY_FILTERS, { companyId });
  const jobs = search.data?.pages.flatMap(page => page.items) ?? [];

  const wrap = (content: ReactNode) => <div className="jb"><div className="jb-container jb-page">{content}</div></div>;
  if (isNotFound(error)) return wrap(<NotFound title="Không tìm thấy công ty" description="Công ty không tồn tại hoặc đang tạm khoá." href="/companies" action="Xem công ty khác" />);
  if (error) return wrap(<Failure error={error} onRetry={() => refetch()} />);
  if (isPending) return wrap(<div className="jb-skeleton" role="status" aria-label="Đang tải" />);

  return <div className="jb">
    <section className="jb-company-hero"><div className="jb-container jb-company-hero-inner">
      <CompanyLogo company={company} size="lg" />
      <div><h1>{company.name}</h1>
        <div className="jb-meta">
          {company.city && <span><MapPin aria-hidden="true" />{company.city}</span>}
          {company.industry && <span><Building2 aria-hidden="true" />{company.industry}</span>}
          {company.size && <span><Users aria-hidden="true" />{company.size}</span>}
          <span><Briefcase aria-hidden="true" />{company.openJobs} việc làm đang tuyển</span>
        </div></div>
    </div></section>
    <div className="jb-container jb-page">
      <nav className="jb-breadcrumb" aria-label="Đường dẫn"><Link href="/companies">Công ty</Link><ChevronRight aria-hidden="true" /><span aria-current="page">{company.name}</span></nav>
      <div className="jb-page-grid">
        <section aria-label="Việc làm đang tuyển">
          <div className="jb-results-head"><h2><em>{company.openJobs}</em> việc làm đang tuyển</h2></div>
          {search.error ? <Failure error={search.error} onRetry={() => search.refetch()} />
            : search.isPending ? <div className="jb-skeleton" role="status" aria-label="Đang tải" />
            : jobs.length ? <div className="jb-list">{jobs.map(job => <JobCard key={job.id} job={job} href={jobHref("/jobs", job.id)} />)}
              {search.hasNextPage && <Button variant="outline" className="h-11" disabled={search.isFetchingNextPage} onClick={() => search.fetchNextPage()}>Xem thêm</Button>}</div>
            : <div className="jb-empty"><SearchX aria-hidden="true" /><h3>Chưa có vị trí đang mở</h3><p>Hãy quay lại sau để cập nhật cơ hội mới.</p></div>}
        </section>
        <aside className="jb-page-aside jb-panel jb-panel-pad jb-company-card" aria-label="Giới thiệu công ty">
          <h3>Giới thiệu</h3>
          <p className="jb-about">{company.description || "Công ty chưa cập nhật phần giới thiệu."}</p>
          <dl>
            {company.industry && <div><dt>Lĩnh vực</dt><dd>{company.industry}</dd></div>}
            {company.size && <div><dt>Quy mô</dt><dd>{company.size}</dd></div>}
            {company.address && <div><dt>Địa chỉ</dt><dd>{company.address}</dd></div>}
          </dl>
          {company.website && <a className="jb-link" href={/^https?:\/\//.test(company.website) ? company.website : `https://${company.website}`} target="_blank" rel="noopener noreferrer"><Globe aria-hidden="true" />Trang web công ty</a>}
        </aside>
      </div>
    </div>
  </div>;
}
