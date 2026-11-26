"use client"

import * as React from "react"
import Link from "next/link"
import { notFound } from "next/navigation"
import { ArrowRight, Sparkles } from "lucide-react"
import { ROLE_MENU } from "@/config/menu"
import { ROLE_LABEL, roleFromSlug } from "@/config/roles"
import { useAuthStore } from "@/hooks/use-auth-store"

const ITEM_HINT: Record<string, string> = {
  "/candidate/profile": "Cập nhật thông tin cá nhân, bằng cấp, dự án portfolio và hồ sơ kỹ năng xác thực.",
  "/candidate/jobs": "Tìm kiếm vị trí tuyển dụng, thực tập đang mở và gửi hồ sơ trực tuyến.",
  "/candidate/applications": "Theo dõi trạng thái sàng lọc, phỏng vấn và kết quả các đơn đã nộp.",
  "/candidate/interviews": "Chọn khung giờ phỏng vấn phù hợp và tham gia phòng họp trực tuyến.",
  "/candidate/offers": "Xem thư mời nhận việc, mức đãi ngộ và phản hồi xác nhận đúng hạn.",
  "/candidate/roadmap": "Định hướng lộ trình học tập bổ sung kỹ năng còn thiếu qua LMS.",
  "/candidate/assistant": "Gợi ý nghề nghiệp, rà soát CV và luyện tập phỏng vấn cùng AI.",
  "/hr/company": "Quản lý thương hiệu, quy mô, địa chỉ và giới thiệu doanh nghiệp tuyển dụng.",
  "/hr/jobs": "Tạo tin tuyển dụng mới, thiết lập trọng số kỹ năng và theo dõi duyệt JD.",
  "/hr/recruitment": "Sàng lọc ứng viên, đối chiếu điểm khớp kỹ năng, xếp lịch phỏng vấn & gửi offer.",
  "/hr/interviews": "Theo dõi lịch phỏng vấn toàn công ty theo tuần hoặc danh sách.",
  "/hr/interns": "Quản lý danh sách thực tập sinh IOC và kết quả nghiệm thu kỳ thực tập.",
  "/mentor/reviews": "Chấm điểm chuyên môn và ghi nhận xét cho các vòng phỏng vấn được phân công.",
  "/staff/profile-reviews": "Rà soát, chỉnh sửa có lý do và phê duyệt công khai hồ sơ năng lực ứng viên.",
  "/staff/jobs": "Kiểm duyệt nội dung tin tuyển dụng từ doanh nghiệp đối tác.",
  "/staff/applications": "Giám sát toàn bộ quy trình ứng tuyển giữa ứng viên và doanh nghiệp.",
  "/staff/companies": "Tạo hồ sơ doanh nghiệp, cấp và quản lý trạng thái tài khoản HR.",
  "/staff/internships": "Điều phối đợt thực tập IOC và ghép ứng viên vào doanh nghiệp.",
  "/staff/sync": "Đồng bộ kết quả học tập LMS, Khảo thí và thực tập làm minh chứng kỹ năng.",
  "/staff/reports": "Thống kê tỷ lệ việc làm, thực tập và mức độ đáp ứng kỹ năng.",
  "/admin/settings": "Quản trị danh mục kỹ năng, trọng số đánh giá và cấu hình toàn hệ thống.",
  "/admin/applications": "Tra cứu và đối soát dữ liệu đơn ứng tuyển trên toàn nền tảng.",
}

/**
 * Trang chủ của từng vai trò (/candidate, /hr, /mentor, /staff, /admin).
 */
export default function RoleHomePage({ params }: { params: Promise<{ role: string }> }) {
  const { role: slug } = React.use(params)
  const role = roleFromSlug(slug)
  const user = useAuthStore((s) => s.user)
  if (!role) notFound()
  if (!user) return null

  const menu = ROLE_MENU[role]
  const primaryAction = menu[0]

  return (
    <div className="ptit-page">
      <section className="ptit-hero-surface rounded-2xl border border-[#FECDD3] p-6 sm:p-8 md:p-10">
        <div className="flex flex-wrap items-center justify-between gap-6">
          <div className="max-w-2xl space-y-2.5">
            <p className="ptit-eyebrow">
              <Sparkles className="size-3.5" aria-hidden="true" />
              KHÔNG GIAN LÀM VIỆC · {ROLE_LABEL[role]}
            </p>
            <h1 className="text-2xl font-extrabold tracking-tight text-heading sm:text-3xl">
              Xin chào, <span className="text-[#DA251D]">{user.fullName}</span>
            </h1>
            <p className="text-sm leading-relaxed text-body">
              {ROLE_LABEL[role]} {user.email ? `· ${user.email}` : ""} — Chọn phân hệ bên dưới để tiếp tục công việc của
              bạn trên cổng Rikkei Jobs.
            </p>
          </div>
          {primaryAction ? (
            <Link
              href={primaryAction.href}
              className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-white shadow-xs transition-colors hover:bg-[#941C25]"
            >
              Mở {primaryAction.label}
              <ArrowRight className="size-4" aria-hidden="true" />
            </Link>
          ) : null}
        </div>
      </section>

      <section aria-label="Danh mục chức năng" className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-bold text-heading">Danh mục chức năng</h2>
          <span className="text-xs font-medium text-muted-foreground">{menu.length} phân hệ</span>
        </div>
        <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {menu.map((item) => {
            const Icon = item.icon
            return (
              <li key={item.href}>
                <Link
                  href={item.href}
                  className="group flex h-full flex-col justify-between rounded-xl border border-border bg-white p-5 shadow-[0_2px_10px_rgb(15_23_42/0.04)] transition-all duration-150 outline-none hover:-translate-y-0.5 hover:border-primary/40 hover:shadow-[0_8px_24px_rgb(15_23_42/0.08)] focus-visible:ring-2 focus-visible:ring-ring/30"
                >
                  <div>
                    <div className="flex items-center justify-between gap-3">
                      <span className="flex size-11 items-center justify-center rounded-xl border border-[#FECDD3] bg-[#FFF1F2] text-primary transition-colors group-hover:bg-primary group-hover:text-white">
                        <Icon className="size-5 shrink-0" aria-hidden="true" />
                      </span>
                      <ArrowRight
                        className="size-4 text-muted-foreground transition-transform group-hover:translate-x-0.5 group-hover:text-primary"
                        aria-hidden="true"
                      />
                    </div>
                    <h3 className="mt-4 text-base font-bold text-heading group-hover:text-primary">{item.label}</h3>
                    <p className="mt-1.5 text-xs leading-relaxed text-muted-foreground">
                      {ITEM_HINT[item.href] ?? "Truy cập và thao tác dữ liệu của phân hệ này."}
                    </p>
                  </div>
                </Link>
              </li>
            )
          })}
        </ul>
      </section>
    </div>
  )
}
