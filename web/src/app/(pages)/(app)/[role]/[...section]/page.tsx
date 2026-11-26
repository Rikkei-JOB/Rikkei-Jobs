import Link from "next/link"
import { notFound } from "next/navigation"
import { Construction, Home } from "lucide-react"
import { Button } from "@/app/components/ui/button"
import { findMenuItem } from "@/config/menu"
import { ROLE_HOME, roleFromSlug } from "@/config/roles"

/**
 * Trang tạm cho mục menu chưa có trang thật (ví dụ /hr/interns). Task tính năng tạo
 * trang tĩnh tương ứng sẽ được ưu tiên hơn trang này.
 * Đường dẫn không có trong menu của vai trò → 404.
 */
export default async function SectionPlaceholderPage({
  params,
}: {
  params: Promise<{ role: string; section: string[] }>
}) {
  const { role: slug, section } = await params
  const role = roleFromSlug(slug)
  const item = role ? findMenuItem(role, `/${slug}/${section.join("/")}`) : undefined
  if (!role || !item) notFound()

  return (
    <div className="ptit-page items-center py-8">
      <div className="ptit-surface relative w-full max-w-xl overflow-hidden px-6 py-12 text-center sm:px-10">
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-x-0 top-0 h-28 bg-[radial-gradient(ellipse_at_top,#FFE4E6_0%,transparent_75%)]"
        />
        <div className="relative mx-auto mb-5 flex size-16 items-center justify-center rounded-2xl border border-[#FECDD3] bg-[#FFF1F2] text-primary">
          <Construction className="size-8" aria-hidden="true" />
        </div>
        <p className="ptit-eyebrow justify-center">PHÂN HỆ ĐANG HOÀN THIỆN</p>
        <h1 className="relative text-2xl font-extrabold text-heading">{item.label}</h1>
        <p className="relative mx-auto mt-3 max-w-md text-sm leading-relaxed text-muted-foreground">
          Đang phát triển. Chức năng này sẽ được tích hợp và mở khoá ở hạng mục nghiệp vụ tương ứng.
        </p>
        <div className="relative mt-7">
          <Button variant="outline" size="lg" render={<Link href={ROLE_HOME[role]} />} nativeButton={false}>
            <Home aria-hidden="true" />
            Về trang tổng quan
          </Button>
        </div>
      </div>
    </div>
  )
}
