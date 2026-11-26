"use client";

import Link from "next/link";
import { Briefcase, Building2, CalendarClock, Clock, Layers, MapPin, Users } from "lucide-react";
import { ApplyButton } from "@/app/components/applications/apply-button";
import { LEVEL_LABEL, TYPE_LABEL, WORK_MODE_LABEL, formatDate } from "@/app/components/job/job-labels";
import { postedAgo, type BoardCompany, type BoardJobDetail } from "@/services/job-board.service";
import { CompanyLogo, JobBadges, Salary, SaveJobButton } from "./job-parts";

export const jobHref = (basePath: string, id: string) => `${basePath}/${encodeURIComponent(id)}`;
export const companyHref = (id: string) => `/companies/${encodeURIComponent(id)}`;

/** Nút ứng tuyển: dùng ApplyButton (DN-04) có sẵn hộp chọn CV; API công khai chỉ trả tin còn hạn. */
export function JobApplyAction({ job }: { job: BoardJobDetail }) {
  return <div className="career"><ApplyButton jobId={job.id} jobTitle={job.title} companyName={job.company.name} acceptingApplications /></div>;
}

/** Nội dung JD lưu dạng văn bản: dòng bắt đầu bằng "-", "•" hoặc "*" hiển thị thành danh sách. */
function RichText({ text }: { text: string }) {
  const lines = text.split(/\r?\n/).map(line => line.trim()).filter(Boolean);
  const bullet = /^[-•*]\s*/;
  if (lines.length > 0 && lines.every(line => bullet.test(line))) {
    return <ul>{lines.map((line, index) => <li key={index}>{line.replace(bullet, "")}</li>)}</ul>;
  }
  return <p>{text.trim()}</p>;
}

/**
 * Chi tiết tin. `preview`: khung xem nhanh bên phải trang danh sách (tiêu đề dính trên cùng, có link mở trang riêng).
 * `page`: trang chi tiết, thông tin công ty nằm ở cột bên.
 */
export function JobDetailView({ job, basePath, variant }: { job: BoardJobDetail; basePath: string; variant: "preview" | "page" }) {
  const TitleTag = variant === "page" ? "h1" : "h2";
  return <article aria-label={job.title}>
    <header className="jb-detail-head">
      <div className="jb-detail-company"><CompanyLogo company={job.company} size="md" />
        <div><Link href={companyHref(job.company.id)}>{job.company.name}</Link>{job.company.industry && <div className="jb-meta"><span>{job.company.industry}</span></div>}</div></div>
      <TitleTag className="jb-detail-title">{variant === "preview" ? <Link href={jobHref(basePath, job.id)}>{job.title}</Link> : job.title}</TitleTag>
      <Salary job={job} />
      <div className="jb-actions"><JobApplyAction job={job} /><SaveJobButton jobId={job.id} jobTitle={job.title} large /></div>
    </header>
    <div className="jb-detail-body">
      <div className="jb-facts">
        {(job.address || job.city) && <div><MapPin aria-hidden="true" /><span>{job.address ?? job.city}</span></div>}
        {job.workMode && <div><Briefcase aria-hidden="true" /><span>{WORK_MODE_LABEL[job.workMode]}{job.type ? ` · ${TYPE_LABEL[job.type]}` : ""}</span></div>}
        {job.level && <div><Layers aria-hidden="true" /><span>Cấp bậc: <strong>{LEVEL_LABEL[job.level]}</strong></span></div>}
        {job.quantity != null && <div><Users aria-hidden="true" /><span>Số lượng: <strong>{job.quantity}</strong></span></div>}
        <div><Clock aria-hidden="true" /><span>Đăng {postedAgo(job.createdAt).toLowerCase()} <JobBadges job={job} /></span></div>
        {job.deadline && <div><CalendarClock aria-hidden="true" /><span>Hạn nộp: <strong>{formatDate(job.deadline)}</strong></span></div>}
      </div>
      {job.skills.length > 0 && <section className="jb-detail-block"><h2>Kỹ năng</h2>
        <div className="jb-skills-row">{job.skills.map(skill => <Link key={skill} className="jb-tag" href={`${basePath}?q=${encodeURIComponent(skill)}`}>{skill}</Link>)}</div></section>}
      {job.description && <section className="jb-detail-block"><h2>Mô tả công việc</h2><RichText text={job.description} /></section>}
      {job.requirements && <section className="jb-detail-block"><h2>Yêu cầu công việc</h2><RichText text={job.requirements} /></section>}
      {job.benefits && <section className="jb-detail-block"><h2>Quyền lợi</h2><RichText text={job.benefits} /></section>}
      {variant === "preview" && <section className="jb-detail-block"><h2>Về {job.company.name}</h2><CompanySummary company={job.company} /></section>}
    </div>
  </article>;
}

export function CompanySummary({ company }: { company: BoardCompany & { description?: string | null } }) {
  return <div className="jb-company-card">
    <dl>
      {company.industry && <div><dt>Lĩnh vực</dt><dd>{company.industry}</dd></div>}
      {company.size && <div><dt>Quy mô</dt><dd>{company.size}</dd></div>}
      {company.city && <div><dt>Trụ sở</dt><dd>{company.city}</dd></div>}
    </dl>
    {company.description && <p>{company.description}</p>}
    <Link className="jb-link" href={companyHref(company.id)}><Building2 aria-hidden="true" />Xem trang công ty</Link>
  </div>;
}
