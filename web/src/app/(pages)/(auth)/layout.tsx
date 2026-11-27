import Link from "next/link"
import type { ReactNode } from "react"
import { ArrowLeft, Briefcase, Building2 } from "lucide-react"
import { SiteFooter } from "@/app/components/layout/site-footer"
import { BrandLogo } from "@/app/components/ui/brand-logo"

/** Khung trang đăng nhập/mật khẩu: nền sáng, token đỏ thương hiệu (.auth-theme), header gọn chỉ gồm liên kết thật. */
export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="auth-theme flex min-h-dvh flex-col bg-background">
      <header className="border-b border-border bg-white shadow-[0_2px_10px_rgb(15_23_42/0.03)]">
        <div className="mx-auto flex h-16 max-w-[1440px] items-center justify-between gap-4 px-4 sm:px-8">
          <Link href="/" className="flex flex-col justify-center gap-1 leading-tight" aria-label="Rikkei Jobs, trang chủ">
            <BrandLogo height={28} />
            <span className="block text-[10px] font-bold tracking-[0.1em] text-muted-foreground uppercase max-sm:hidden">
              Đổi mới Sáng tạo &amp; Sự nghiệp
            </span>
          </Link>

          <nav aria-label="Liên kết nhanh" className="flex items-center gap-1 text-sm font-medium text-[#475569]">
            <Link href="/jobs" className="inline-flex min-h-11 items-center gap-2 rounded-lg px-3 hover:bg-[#fff1f2] hover:text-[#b4232d] max-md:hidden">
              <Briefcase className="size-4" aria-hidden="true" />Việc làm IT
            </Link>
            <Link href="/companies" className="inline-flex min-h-11 items-center gap-2 rounded-lg px-3 hover:bg-[#fff1f2] hover:text-[#b4232d] max-md:hidden">
              <Building2 className="size-4" aria-hidden="true" />Công ty
            </Link>
            <Link href="/" className="inline-flex min-h-11 items-center gap-2 rounded-lg px-3 hover:bg-[#fff1f2] hover:text-[#b4232d]">
              <ArrowLeft className="size-4" aria-hidden="true" />Trang chủ
            </Link>
          </nav>
        </div>
      </header>

      <main className="ptit-hero-surface flex flex-1 flex-col items-center justify-center px-4 py-10 sm:py-14">
        {children}
      </main>

      <SiteFooter />
    </div>
  )
}
