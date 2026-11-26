"use client";

import { useState, type FormEvent, type MouseEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Briefcase, CircleDollarSign, Clock, Flame, Heart, MapPin, Search, Sparkles } from "lucide-react";
import { CompanyMark } from "@/app/components/company/company-mark";
import { SelectMenu } from "@/app/components/ui/select-menu";
import { usePrefetchBoardJob, useSavedJobs } from "@/hooks/use-job-board";
import { LEVEL_LABEL, WORK_MODE_LABEL } from "@/app/components/job/job-labels";
import { CITIES, formatSalary, isHotJob, isNewJob, postedAgo, type BoardCompany, type BoardJob } from "@/services/job-board.service";

const DESKTOP_QUERY = "(min-width: 1024px)";

const MARK_SIZE = { sm: 44, md: 64, lg: 96 } as const;

/** Logo công ty (dùng CompanyMark của trang công ty): ảnh logo nếu có, không thì ô chữ cái. */
export function CompanyLogo({ company, size = "sm" }: { company: Pick<BoardCompany, "name" | "logoUrl">; size?: keyof typeof MARK_SIZE }) {
  return <span className={`jb-logo jb-logo-${size}`}><CompanyMark name={company.name} logoUrl={company.logoUrl} size={MARK_SIZE[size]} /></span>;
}

export function Salary({ job }: { job: BoardJob }) {
  return <p className="jb-salary"><CircleDollarSign aria-hidden="true" />{formatSalary(job)}</p>;
}

export function JobBadges({ job }: { job: BoardJob }) {
  const hot = isHotJob(job);
  const fresh = isNewJob(job.createdAt);
  if (!hot && !fresh) return null;
  return <span className="jb-badges">
    {hot && <span className="jb-badge jb-badge-hot"><Flame aria-hidden="true" />Hot</span>}
    {fresh && <span className="jb-badge jb-badge-new"><Sparkles aria-hidden="true" />Mới</span>}
  </span>;
}

export function SaveJobButton({ jobId, jobTitle, large = false }: { jobId: string; jobTitle: string; large?: boolean }) {
  const { isSaved, toggle } = useSavedJobs();
  const saved = isSaved(jobId);
  return <button type="button" className={large ? "jb-save-lg" : "jb-save"} aria-pressed={saved}
    aria-label={saved ? `Bỏ lưu ${jobTitle}` : `Lưu ${jobTitle}`} title={saved ? "Bỏ lưu" : "Lưu việc làm"} onClick={() => toggle(jobId)}>
    <Heart aria-hidden="true" fill={saved ? "currentColor" : "none"} />
  </button>;
}

/**
 * Thẻ tin: cả thẻ bấm được nhờ link tiêu đề phủ kín thẻ (nút Lưu nằm trên lớp phủ).
 * Có `onSelect` thì trên màn rộng chỉ đổi tin đang xem nhanh thay vì chuyển trang.
 */
export function JobCard({ job, href, selected = false, onSelect }: { job: BoardJob; href: string; selected?: boolean; onSelect?(id: string): void }) {
  const prefetch = usePrefetchBoardJob();
  function handleClick(event: MouseEvent<HTMLAnchorElement>) {
    if (!onSelect || event.metaKey || event.ctrlKey || event.shiftKey || event.button !== 0) return;
    if (!window.matchMedia(DESKTOP_QUERY).matches) return;
    event.preventDefault();
    onSelect(job.id);
  }
  return <article className="jb-card" aria-current={selected ? "true" : undefined}>
    <div className="jb-card-top"><span>Đăng {postedAgo(job.createdAt).toLowerCase()}</span><JobBadges job={job} /></div>
    <h3 className="jb-card-title"><Link href={href} onClick={handleClick} onMouseEnter={() => prefetch(job.id)} onFocus={() => prefetch(job.id)} className="jb-card-link">{job.title}</Link></h3>
    <div className="jb-card-company"><CompanyLogo company={job.company} /><span>{job.company.name}</span></div>
    <Salary job={job} />
    <div className="jb-card-divider" />
    <div className="jb-meta">
      {job.workMode && <span><Briefcase aria-hidden="true" />{WORK_MODE_LABEL[job.workMode]}</span>}
      {job.city && <span><MapPin aria-hidden="true" />{job.city}</span>}
      {job.level && <span><Clock aria-hidden="true" />{LEVEL_LABEL[job.level]}</span>}
    </div>
    {job.skills.length > 0 && <div className="jb-tags">{job.skills.slice(0, 4).map(skill => <span key={skill} className="jb-tag">{skill}</span>)}</div>}
    <SaveJobButton jobId={job.id} jobTitle={job.title} />
  </article>;
}

/** Ô tìm kiếm lớn (thành phố + từ khoá): đưa tới trang danh sách với tham số trên URL. */
export function JobSearchForm({ basePath, initialQuery = "", initialCity = "all" }: { basePath: string; initialQuery?: string; initialCity?: string }) {
  const router = useRouter();
  const [query, setQuery] = useState(initialQuery);
  const [city, setCity] = useState(initialCity);
  function submit(event: FormEvent) {
    event.preventDefault();
    const params = new URLSearchParams();
    if (query.trim()) params.set("q", query.trim());
    if (city !== "all") params.set("city", city);
    router.push(`${basePath}${params.size ? `?${params}` : ""}`, { scroll: false });
  }
  return <form className="jb-search" role="search" onSubmit={submit} aria-label="Tìm việc làm">
    <div className="jb-search-city">
      <SelectMenu aria-label="Thành phố" value={city} onValueChange={setCity} icon={<MapPin />} className="ui-select-trigger jb-city-trigger"
        options={[{ value: "all", label: "Tất cả thành phố" }, ...CITIES.map(name => ({ value: name, label: name }))]} />
    </div>
    <label className="jb-search-keyword"><span className="sr-only">Từ khoá</span><Search aria-hidden="true" />
      <input type="search" value={query} onChange={event => setQuery(event.target.value)} placeholder="Kỹ năng, vị trí, công ty… (vd: Java, React, Data)" /></label>
    <button type="submit" className="jb-search-submit"><Search aria-hidden="true" />Tìm kiếm</button>
  </form>;
}

/** Gợi ý từ khoá: các kỹ năng xuất hiện nhiều nhất trong tin đang tuyển (truyền từ dữ liệu đã tải). */
export function SkillSuggestions({ basePath, skills }: { basePath: string; skills: string[] }) {
  if (skills.length === 0) return null;
  return <div className="jb-suggest"><span>Gợi ý cho bạn:</span>
    {skills.slice(0, 6).map(skill => <Link key={skill} href={`${basePath}?q=${encodeURIComponent(skill)}`}>{skill}</Link>)}
  </div>;
}

/** Đếm kỹ năng theo số tin, nhiều trước. */
export function topSkills(jobs: Pick<BoardJob, "skills">[]) {
  const counts = new Map<string, number>();
  for (const job of jobs) for (const skill of job.skills) counts.set(skill, (counts.get(skill) ?? 0) + 1);
  return [...counts.entries()].sort((a, b) => b[1] - a[1]);
}
