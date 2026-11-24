"use client";

import { useState, type FormEvent, type ReactNode } from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { ArrowRight, Building2, CalendarDays, Search, SlidersHorizontal } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { applicationLinks, formatApplicationDate } from "@/config/application-status";
import type { CandidateJob } from "@/services/candidate-job.service";
import { ApplyButton } from "./apply-button";
import { CareerBanner } from "./career-banner";

function normalize(text: string) {
  return text.normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[đĐ]/g, "d").toLowerCase();
}

export function CandidateJobList({ jobs, feedback }: { jobs: CandidateJob[]; feedback: ReactNode }) {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const query = params.get("q") ?? "";
  const rawState = params.get("receiving");
  const receiving = rawState === "open" || rawState === "closed" ? rawState : "all";
  const filtered = jobs.filter(job => normalize(`${job.jobTitle} ${job.companyName}`).includes(normalize(query.trim()))
    && (receiving === "all" || job.acceptingApplications === (receiving === "open")));
  const hasFilters = Boolean(query || receiving !== "all");

  function search(q: string, state: string) {
    const next = new URLSearchParams(params.toString());
    next.delete("q"); next.delete("receiving");
    if (q.trim()) next.set("q", q.trim());
    if (state !== "all") next.set("receiving", state);
    router.push(`${pathname}${next.size ? `?${next}` : ""}`, { scroll: false });
  }

  return <>
    <CareerBanner />
    <SearchForm key={`${query}:${receiving}`} query={query} receiving={receiving} onSearch={search} />
    {feedback ?? <>
      <div className="career-results"><h2>{filtered.length} vị trí <span>trong danh sách đã tải</span></h2>
        <div className="career-result-tools"><Link className="career-text-link" href="/candidate/applications">Ứng tuyển của tôi <ArrowRight aria-hidden="true" /></Link>
          {hasFilters && <button className="career-text-link" onClick={() => search("", "all")}>Xoá bộ lọc</button>}</div></div>
      {filtered.length ? <div className="career-job-list">{filtered.map(job => <JobCard key={job.jobId} job={job} />)}</div>
        : <div className="career-feedback"><Search aria-hidden="true" /><h2>{hasFilters ? "Không tìm thấy vị trí phù hợp" : "Chưa có vị trí tuyển dụng"}</h2>
          <p>{hasFilters ? "Thử từ khoá khác hoặc xoá bộ lọc để khám phá thêm cơ hội." : "Hãy quay lại sau để cập nhật những cơ hội mới."}</p></div>}
    </>}
  </>;
}

function SearchForm({ query, receiving, onSearch }: { query: string; receiving: string; onSearch(q: string, state: string): void }) {
  const [q, setQ] = useState(query);
  const [state, setState] = useState(receiving);
  function submit(event: FormEvent) { event.preventDefault(); onSearch(q, state); }
  return <form className="career-search" onSubmit={submit} aria-label="Tìm kiếm việc làm">
    <div className="career-search-field"><label htmlFor="career-query">Vị trí hoặc công ty</label>
      <div className="career-input-icon"><Search aria-hidden="true" /><input id="career-query" type="search" value={q} onChange={e => setQ(e.target.value)} placeholder="Tìm cơ hội tiếp theo của bạn…" /></div></div>
    <div className="career-status-field"><label htmlFor="career-receiving"><SlidersHorizontal aria-hidden="true" /> Trạng thái nhận hồ sơ</label>
      <select id="career-receiving" value={state} onChange={e => setState(e.target.value)}><option value="all">Tất cả vị trí</option><option value="open">Đang nhận hồ sơ</option><option value="closed">Đã đóng nhận hồ sơ</option></select></div>
    <Button type="submit" className="career-primary"><Search aria-hidden="true" />Tìm kiếm</Button>
  </form>;
}

function JobCard({ job }: { job: CandidateJob }) {
  return <article className="career-job-card">
    <div className="career-company-mark" aria-hidden="true">{job.companyName.trim().charAt(0).toLocaleUpperCase("vi-VN")}</div>
    <div className="career-job-info"><p className="career-company"><Building2 aria-hidden="true" />{job.companyName}</p>
      <h2><Link href={applicationLinks.job(job.jobId)}>{job.jobTitle}</Link></h2>
      <div className="career-job-meta"><span><CalendarDays aria-hidden="true" />Hạn nộp: {formatApplicationDate(job.deadline)}</span>
        <span className={`career-status ${job.acceptingApplications ? "career-status-open" : "career-status-closed"}`}>{job.acceptingApplications ? "Đang nhận hồ sơ" : "Đã đóng"}</span></div>
    </div>
    <div className="career-card-actions"><ApplyButton jobId={job.jobId} acceptingApplications={job.acceptingApplications} jobTitle={job.jobTitle} companyName={job.companyName} />
      <Link className="career-text-link" href={applicationLinks.job(job.jobId)}>Xem chi tiết <ArrowRight aria-hidden="true" /></Link></div>
  </article>;
}
