import Link from "next/link"
import { BrandLogo } from "@/app/components/ui/brand-logo"
import { Mail, MapPin, Phone } from "lucide-react"

/** Cột liên kết của footer đầy đủ; trang cần đăng nhập tự đưa khách sang /login. */
const COLUMNS = [
  {
    title: "Dành cho ứng viên",
    links: [
      { href: "/jobs", label: "Tìm việc làm IT" },
      { href: "/companies", label: "Khám phá công ty" },
      { href: "/candidate/profile", label: "Hồ sơ năng lực" },
      { href: "/candidate/cvs", label: "Quản lý CV" },
      { href: "/candidate/applications", label: "Theo dõi đơn ứng tuyển" },
    ],
  },
  {
    title: "Nhà tuyển dụng",
    links: [
      { href: "/login?tab=hr", label: "Đăng nhập doanh nghiệp" },
      { href: "/hr/jobs", label: "Đăng tin tuyển dụng" },
      { href: "/hr/recruitment", label: "Sàng lọc hồ sơ ứng tuyển" },
      { href: "/hr/interviews", label: "Lịch phỏng vấn" },
    ],
  },
]

const QUICK_LINKS = [
  { href: "/jobs", label: "Việc làm IT" },
  { href: "/companies", label: "Công ty" },
]

/**
 * Footer chung. Đầy đủ: trang công khai, đăng nhập và khung ứng viên (nền tối cùng tông hero).
 * compact: khung làm việc HR/Mentor/Admin, một hàng sáng để không lấn nội dung công việc.
 */
export function SiteFooter({ compact = false }: { compact?: boolean }) {
  if (compact) {
    return (
      <footer className="border-t border-border bg-white text-xs text-muted-foreground">
        <div className="mx-auto flex max-w-[1440px] flex-col gap-3 px-4 py-4 sm:px-8 md:flex-row md:items-center md:justify-between">
          <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
            <BrandLogo height={16} label="Rikkei Jobs" />
            <span>Nền tảng tuyển dụng dựa trên năng lực thực</span>
          </div>
          <nav aria-label="Liên kết nhanh" className="flex flex-wrap items-center gap-x-5 gap-y-1">
            {QUICK_LINKS.map((link) => (
              <Link key={link.href} href={link.href} className="font-medium text-heading hover:text-[#b4232d]">{link.label}</Link>
            ))}
            <span>© 2026 Rikkei Jobs</span>
          </nav>
        </div>
      </footer>
    )
  }

  return (
    <footer className="relative overflow-hidden bg-[#0b1024] text-sm text-slate-300">
      {/* Vạch màu thương hiệu và quầng đỏ nhẹ, cùng tông với hero trang chủ. */}
      <div aria-hidden="true" className="h-1 bg-gradient-to-r from-[#b4232d] via-[#e0313a] to-[#1d4ed8]" />
      <div aria-hidden="true" className="pointer-events-none absolute -top-24 right-0 h-72 w-72 rounded-full bg-[#b4232d]/20 blur-3xl" />

      <div className="relative mx-auto max-w-[1200px] px-4 pt-12 pb-8 sm:px-6">
        <div className="grid gap-10 lg:grid-cols-[1.3fr_1fr_1fr_1.1fr]">
          <div className="space-y-4">
            <Link href="/" aria-label="Rikkei Jobs, trang chủ" className="inline-block">
              <BrandLogo height={30} inverse />
            </Link>
            <p className="max-w-sm leading-relaxed text-slate-400">
              Kết nối hồ sơ năng lực xác thực từ LMS, thực tập IOC và đánh giá của Mentor với doanh nghiệp công nghệ
              đang tuyển dụng.
            </p>
          </div>

          <div className="grid grid-cols-2 gap-8 sm:gap-10 lg:contents">
            {COLUMNS.map((column) => (
              <nav key={column.title} aria-label={column.title}>
                <h2 className="mb-4 text-xs font-semibold tracking-[0.12em] text-white uppercase">{column.title}</h2>
                <ul className="space-y-2.5">
                  {column.links.map((link) => (
                    <li key={link.href}>
                      <Link href={link.href} className="transition-colors hover:text-white hover:underline hover:underline-offset-4">
                        {link.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              </nav>
            ))}
          </div>

          <address className="space-y-3 not-italic">
            <h2 className="mb-4 text-xs font-semibold tracking-[0.12em] text-white uppercase">Liên hệ</h2>
            <p className="flex items-center gap-2.5">
              <Phone className="size-4 shrink-0 text-[#F0444B]" aria-hidden="true" />
              <a href="tel:02437562186" className="hover:text-white">024 3756 2186</a>
            </p>
            <p className="flex items-center gap-2.5">
              <Mail className="size-4 shrink-0 text-[#F0444B]" aria-hidden="true" />
              <a href="mailto:support@rikkeijobs.edu.vn" className="break-all hover:text-white">support@rikkeijobs.edu.vn</a>
            </p>
            <p className="flex items-start gap-2.5">
              <MapPin className="mt-0.5 size-4 shrink-0 text-[#F0444B]" aria-hidden="true" />
              <span>Tòa nhà công nghệ Rikkei &amp; Trung tâm Đổi mới Sáng tạo, Hà Nội &amp; TP. Hồ Chí Minh</span>
            </p>
          </address>
        </div>

        <div className="mt-10 flex flex-col gap-2 border-t border-white/10 pt-6 text-xs text-slate-400 sm:flex-row sm:items-center sm:justify-between">
          <p>© 2026 Rikkei Jobs Platform. Bảo lưu mọi quyền.</p>
          <p>Tuyển dụng dựa trên năng lực thực · Đổi mới Sáng tạo &amp; Sự nghiệp</p>
        </div>
      </div>
    </footer>
  )
}
