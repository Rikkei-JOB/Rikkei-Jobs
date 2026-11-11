import { notFound } from "next/navigation"
import { Construction } from "lucide-react"
import { Card, CardDescription, CardHeader, CardTitle } from "@/app/components/ui/card"
import { findMenuItem } from "@/config/menu"
import { roleFromSlug } from "@/config/roles"

/**
 * Trang tạm cho mục menu chưa có trang thật (ví dụ /hr/jobs). Task tính năng tạo
 * `app/(pages)/(app)/hr/jobs/page.tsx`; đường dẫn tĩnh đó được ưu tiên hơn trang này.
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
  if (!item) notFound()

  return (
    <Card className="mx-auto max-w-4xl">
      <CardHeader>
        <Construction className="mb-1 size-6 text-muted-foreground" aria-hidden />
        <CardTitle className="text-base">{item.label}</CardTitle>
        <CardDescription>Đang phát triển. Chức năng này sẽ được bổ sung ở task tương ứng.</CardDescription>
      </CardHeader>
    </Card>
  )
}
