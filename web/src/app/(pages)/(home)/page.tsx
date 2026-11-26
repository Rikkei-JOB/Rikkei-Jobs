"use client"

import * as React from "react"
import Link from "next/link"
import { ArrowRight, Award, BriefcaseBusiness, ShieldCheck, Sparkles } from "lucide-react"
import { CompanyGrid } from "@/app/components/job-board/job-pages"
import { JobCard, JobSearchForm, SkillSuggestions, topSkills } from "@/app/components/job-board/job-parts"
import { jobHref } from "@/app/components/job-board/job-detail"
import { HeroIllustration, StepApplyArt, StepOfferArt, StepProfileArt, VerifiedProfileArt } from "@/app/components/job-board/illustrations"
import "@/app/components/job-board/job-board.css"
import { PublicHeader } from "@/app/components/layout/public-header"
import { SiteFooter } from "@/app/components/layout/site-footer"
import { ROLE_HOME } from "@/config/roles"
import { useAuthStore } from "@/hooks/use-auth-store"
import { useBoardCompanies, useBoardSearch } from "@/hooks/use-job-board"
import { EMPTY_FILTERS, isHotJob } from "@/services/job-board.service"

const PILLARS = [
  {
    badge: "Hồ sơ xác thực",
    title: "Skill Profile đồng bộ từ LMS & Khảo thí",
    desc: "Điểm học tập, bài thi và kỳ thực tập IOC tự động quy đổi thành bằng chứng năng lực minh bạch cho nhà tuyển dụng.",
    icon: Award,
  },
  {
    badge: "Tuyển dụng thông minh",
    title: "Độ khớp JD & Quy trình khép kín",
    desc: "Đối chiếu yêu cầu kỹ năng của doanh nghiệp với năng lực thực tế, đặt lịch phỏng vấn trực tuyến và quản lý Offer.",
    icon: BriefcaseBusiness,
  },
  {
    badge: "Đánh giá chuyên môn",
    title: "Đồng hành cùng Mentor & Cán bộ đào tạo",
    desc: "Mentor tham gia hội đồng phỏng vấn kỹ thuật, cán bộ rà soát hồ sơ công khai và gợi ý lộ trình bù đắp kỹ năng.",
    icon: ShieldCheck,
  },
]

/** Ba bước của ứng viên; trang cần đăng nhập tự đưa khách sang /login. */
const STEPS = [
  {
    title: "Hoàn thiện hồ sơ năng lực",
    desc: "Điểm học tập LMS, bài thi và kỳ thực tập IOC được đồng bộ thành kỹ năng đã xác thực.",
    href: "/candidate/profile",
    action: "Cập nhật hồ sơ",
    art: StepProfileArt,
  },
  {
    title: "Tìm việc và ứng tuyển",
    desc: "Lọc theo kỹ năng, cấp bậc, mức lương rồi nộp CV đến doanh nghiệp chỉ với vài thao tác.",
    href: "/jobs",
    action: "Tìm việc ngay",
    art: StepApplyArt,
  },
  {
    title: "Phỏng vấn và nhận offer",
    desc: "Chọn lịch phỏng vấn trực tuyến, theo dõi kết quả và phản hồi thư mời nhận việc đúng hạn.",
    href: "/candidate/interviews",
    action: "Xem lịch phỏng vấn",
    art: StepOfferArt,
  },
]

export default function Home() {
  const user = useAuthStore((s) => s.user)
  // Ứng viên đã ở trang chủ của mình, nên nút "Bắt đầu" dẫn tới hồ sơ năng lực thay vì quay lại trang này.
  const startHref = !user ? "/login" : user.role === "CANDIDATE" ? "/candidate/profile" : ROLE_HOME[user.role]
  // 50 tin mới nhất: đủ chọn tin nổi bật và đếm kỹ năng phổ biến cho trang chủ.
  const search = useBoardSearch(EMPTY_FILTERS, { size: 50 })
  const jobs = search.data?.pages[0]?.items
  const totalJobs = search.data?.pages[0]?.total
  const jobsPending = search.isPending
  const { data: companies, isPending: companiesPending } = useBoardCompanies()

  const featured = React.useMemo(
    () => [...(jobs ?? [])].sort((a, b) => Number(isHotJob(b)) - Number(isHotJob(a)) || b.createdAt.localeCompare(a.createdAt)).slice(0, 6),
    [jobs],
  )
  const skills = React.useMemo(() => topSkills(jobs ?? []).slice(0, 14), [jobs])

  return (
    <div className="flex min-h-dvh flex-col bg-[#f6f7fa]">
      <PublicHeader />

      <main className="jb flex-1">
        <section className="jb-hero" aria-labelledby="home-title">
          <div className="jb-container jb-hero-inner jb-hero-split">
            <div className="min-w-0">
            <p className="jb-hero-badge"><span aria-hidden="true"><Sparkles /></span>Nền tảng việc làm IT dựa trên năng lực xác thực</p>
            <h1 id="home-title">
              <span className="jb-hero-line">Việc làm IT chất lượng,</span> <em>tuyển dụng dựa trên năng lực thật</em>
            </h1>
            <p className="jb-hero-lead">
              Kết nối hồ sơ năng lực xác thực từ LMS, thực tập IOC với doanh nghiệp công nghệ đang tuyển dụng.
            </p>
            <JobSearchForm basePath="/jobs" />
            <SkillSuggestions basePath="/jobs" skills={skills.map(([skill]) => skill)} />
            <div className="jb-stats" aria-label="Số liệu nổi bật">
              <div><strong>{totalJobs ?? "–"}</strong><span>việc làm đang tuyển</span></div>
              <div><strong>{companies?.length ?? "–"}</strong><span>công ty công nghệ</span></div>
              <div><strong>{skills.length || "–"}</strong><span>kỹ năng được tìm</span></div>
            </div>
            </div>
            <HeroIllustration className="jb-hero-art" />
          </div>
        </section>

        <section className="jb-container jb-section" aria-labelledby="featured-title">
          <div className="jb-section-head">
            <div><p className="jb-eyebrow">Cơ hội mới nhất</p><h2 id="featured-title">Việc làm nổi bật</h2></div>
            <Link href="/jobs" className="jb-link">Xem tất cả việc làm <ArrowRight aria-hidden="true" /></Link>
          </div>
          {jobsPending ? (
            <div className="jb-grid jb-grid-3" role="status" aria-label="Đang tải">{[0, 1, 2].map((i) => <div key={i} className="jb-skeleton" />)}</div>
          ) : search.error ? (
            <p className="jb-empty" role="alert">Chưa thể tải việc làm. Vui lòng thử lại sau.</p>
          ) : featured.length === 0 ? (
            <p className="jb-empty">Chưa có tin tuyển dụng đang mở.</p>
          ) : (
            <div className="jb-grid jb-grid-3">{featured.map((job) => <JobCard key={job.id} job={job} href={jobHref("/jobs", job.id)} />)}</div>
          )}
        </section>

        <section className="jb-container jb-section" aria-labelledby="steps-title">
          <div className="jb-section-head">
            <div><p className="jb-eyebrow">Dành cho ứng viên</p><h2 id="steps-title">Bắt đầu sự nghiệp trong 3 bước</h2></div>
          </div>
          <ol className="jb-steps">
            {STEPS.map((step, index) => {
              const Art = step.art
              return (
                <li key={step.title} className="jb-step">
                  <Art className="jb-step-art" />
                  <span className="jb-step-number" aria-hidden="true">{String(index + 1).padStart(2, "0")}</span>
                  <h3>{step.title}</h3>
                  <p>{step.desc}</p>
                  <Link href={step.href} className="jb-link">{step.action} <ArrowRight aria-hidden="true" /></Link>
                </li>
              )
            })}
          </ol>
        </section>

        <section className="jb-container jb-section" aria-labelledby="companies-title">
          <div className="jb-section-head">
            <div><p className="jb-eyebrow">Nhà tuyển dụng</p><h2 id="companies-title">Công ty hàng đầu</h2></div>
            <Link href="/companies" className="jb-link">Xem tất cả công ty <ArrowRight aria-hidden="true" /></Link>
          </div>
          {companiesPending ? (
            <div className="jb-grid jb-grid-3" role="status" aria-label="Đang tải">{[0, 1, 2].map((i) => <div key={i} className="jb-skeleton" />)}</div>
          ) : (
            <CompanyGrid entries={(companies ?? []).filter((company) => company.openJobs > 0).slice(0, 6)} />
          )}
        </section>

        {skills.length > 0 && (
          <section className="jb-container jb-section" aria-labelledby="skills-title">
            <div className="jb-section-head"><div><p className="jb-eyebrow">Xu hướng</p><h2 id="skills-title">Kỹ năng được tuyển nhiều</h2></div></div>
            <div className="jb-skill-cloud">
              {skills.map(([skill, count]) => (
                <Link key={skill} href={`/jobs?q=${encodeURIComponent(skill)}`}>{skill}<small>{count} việc</small></Link>
              ))}
            </div>
          </section>
        )}

        <section className="jb-container jb-section" aria-labelledby="cta-title">
          <div className="jb-cta">
            <div className="jb-cta-copy">
              <p className="jb-eyebrow">Hồ sơ năng lực xác thực</p>
              <h2 id="cta-title">Nổi bật trước nhà tuyển dụng bằng kỹ năng đã được xác thực</h2>
              <p>Kết quả học tập, bài thi và kỳ thực tập được quy đổi thành bằng chứng năng lực, giúp doanh nghiệp tin tưởng hồ sơ của bạn ngay từ vòng đầu.</p>
              <div className="jb-cta-actions">
                <Link href={user?.role === "CANDIDATE" ? "/candidate/profile" : "/login"} className="jb-cta-primary">
                  {user?.role === "CANDIDATE" ? "Hoàn thiện hồ sơ" : "Đăng nhập để bắt đầu"}
                </Link>
                <Link href="/jobs" className="jb-cta-secondary">Khám phá việc làm</Link>
              </div>
            </div>
            <VerifiedProfileArt className="jb-cta-art" />
          </div>
        </section>

        <section className="jb-container jb-section" aria-labelledby="pillars-title">
          <div className="jb-section-head">
            <div><p className="jb-eyebrow">Hệ sinh thái Rikkei Jobs</p><h2 id="pillars-title">Nền tảng tuyển dụng dựa trên năng lực thực</h2></div>
            <Link href={startHref} className="jb-link">Khám phá ngay <ArrowRight aria-hidden="true" /></Link>
          </div>
          <div className="grid gap-4 sm:grid-cols-3">
            {PILLARS.map((item) => {
              const Icon = item.icon
              return (
                <article
                  key={item.title}
                  className="flex flex-col justify-between rounded-2xl border border-border bg-white p-5 shadow-[0_2px_10px_rgb(15_23_42/0.04)] transition-all hover:-translate-y-0.5 hover:border-primary/40"
                >
                  <div>
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-[#FFF1F2] px-3 py-1 text-xs font-semibold text-primary">
                      <Icon className="size-3.5" aria-hidden="true" />
                      {item.badge}
                    </span>
                    <h3 className="mt-3.5 text-base font-bold text-heading">{item.title}</h3>
                    <p className="mt-2 text-xs leading-relaxed text-muted-foreground">{item.desc}</p>
                  </div>
                  <Link
                    href={startHref}
                    className="mt-5 inline-flex items-center gap-1 text-xs font-bold text-primary hover:underline"
                  >
                    Bắt đầu ngay <ArrowRight className="size-3.5" aria-hidden="true" />
                  </Link>
                </article>
              )
            })}
          </div>
        </section>
      </main>

      <SiteFooter />
    </div>
  )
}
