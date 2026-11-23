"use client";

import Link from "next/link";
import { ArrowLeft, ArrowRight, Building2, CalendarDays, Check, FileCheck2, FileText, Send } from "lucide-react";
import { applicationLinks, formatApplicationDate } from "@/config/application-status";
import type { CandidateJob } from "@/services/candidate-job.service";
import { ApplyButton } from "./apply-button";

export function CandidateJobDetail({ job }: { job: CandidateJob }) {
  return <>
    <nav className="career-breadcrumb" aria-label="Đường dẫn"><Link href={applicationLinks.jobs}><ArrowLeft aria-hidden="true" />Việc làm</Link><span aria-hidden="true">/</span><span aria-current="page">{job.jobTitle}</span></nav>
    <div className="career-detail-grid">
      <div className="career-detail-content">
        <section className="career-summary career-panel">
          <div className="career-detail-company"><div className="career-company-mark" aria-hidden="true">{job.companyName.trim().charAt(0).toLocaleUpperCase("vi-VN")}</div>
            <div><p className="career-eyebrow">CƠ HỘI DÀNH CHO BẠN</p><p className="career-company">{job.companyName}</p></div></div>
          <h1>{job.jobTitle}</h1>
          <div className="career-summary-facts"><div><CalendarDays aria-hidden="true" /><div><span>Hạn nộp hồ sơ</span><strong>{formatApplicationDate(job.deadline)}</strong></div></div>
            <div><Building2 aria-hidden="true" /><div><span>Doanh nghiệp</span><strong>{job.companyName}</strong></div></div></div>
          <span className={`career-status ${job.acceptingApplications ? "career-status-open" : "career-status-closed"}`}>{job.acceptingApplications ? "Đang nhận hồ sơ" : "Đã đóng nhận hồ sơ"}</span>
        </section>
        <section className="career-panel career-preparation"><div className="career-section-title"><FileCheck2 aria-hidden="true" /><h2>Chuẩn bị ứng tuyển</h2></div>
          <p className="career-description">Một hồ sơ chỉn chu giúp bạn thể hiện rõ năng lực và định hướng của mình.</p>
          <ol className="career-preparation-steps">
            <li><span className="career-step-icon"><FileText aria-hidden="true" /></span><div><h3>Chọn CV phù hợp</h3><p>Chọn một CV đã có để giới thiệu kinh nghiệm và kỹ năng của bạn.</p></div></li>
            <li><span className="career-step-icon"><Send aria-hidden="true" /></span><div><h3>Thêm lời giới thiệu</h3><p>Chia sẻ lý do bạn quan tâm đến vị trí này. Phần này không bắt buộc.</p></div></li>
            <li><span className="career-step-icon"><Check aria-hidden="true" /></span><div><h3>Gửi và theo dõi hồ sơ</h3><p>CV được lưu thành bản PDF tại thời điểm nộp. Bạn có thể theo dõi tiến trình trong mục Ứng tuyển.</p></div></li>
          </ol>
          <Link className="career-text-link" href="/candidate/applications">Xem ứng tuyển của tôi <ArrowRight aria-hidden="true" /></Link>
        </section>
      </div>
      <aside className="career-action-aside career-panel" aria-label="Thông tin ứng tuyển">
        <div className="career-aside-meta"><p className="career-eyebrow">BƯỚC TIẾP THEO</p><h2>{job.acceptingApplications ? "Sẵn sàng cho cơ hội mới?" : "Vị trí đã ngừng nhận hồ sơ"}</h2>
          <p className="career-description">{job.acceptingApplications ? "Chọn CV và gửi lời giới thiệu đến nhà tuyển dụng." : "Bạn có thể khám phá những vị trí khác đang tuyển dụng."}</p>
          <div className="career-aside-deadline"><span>Hạn nộp hồ sơ</span><strong>{formatApplicationDate(job.deadline)}</strong></div></div>
        <div className="career-apply-action" data-mobile-apply><ApplyButton jobId={job.jobId} acceptingApplications={job.acceptingApplications} jobTitle={job.jobTitle} companyName={job.companyName} />
          <span className="career-mobile-caption">{job.acceptingApplications ? "Chọn CV để bắt đầu" : "Khám phá thêm cơ hội khác"}</span></div>
        <Link className="career-text-link career-aside-meta" href="/candidate/applications">Ứng tuyển của tôi <ArrowRight aria-hidden="true" /></Link>
      </aside>
    </div>
  </>;
}
