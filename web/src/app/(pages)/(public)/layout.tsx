import type { ReactNode } from "react";
import { PublicHeader } from "@/app/components/layout/public-header";
import { SiteFooter } from "@/app/components/layout/site-footer";

/** Trang công khai không cần đăng nhập: danh sách việc làm, chi tiết tin, công ty. */
export default function PublicLayout({ children }: { children: ReactNode }) {
  return <div className="flex min-h-dvh flex-col bg-[#f6f7fa]">
    <PublicHeader />
    <main className="flex-1">{children}</main>
    <SiteFooter />
  </div>;
}
