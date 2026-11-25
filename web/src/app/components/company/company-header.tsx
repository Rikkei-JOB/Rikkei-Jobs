import type { ReactNode } from "react";
import { CompanyMark } from "@/app/components/company/company-mark";

interface CompanyHeaderProps {
  company: {
    name: string;
    logoUrl?: string | null;
    industry?: string | null;
    size?: string | null;
    city?: string | null;
    website?: string | null;
  };
  subline?: ReactNode;
  actions?: ReactNode;
  tabs?: ReactNode;
  /** Cấp tiêu đề của tên công ty: "h2" khi trang đã có h1 riêng (ví dụ /hr/company trong HrShell). */
  titleAs?: "h1" | "h2";
}

/** Đầu trang công ty: thẻ trắng viền trên đỏ, logo + tên + dòng thông tin, nút bên phải, thanh tab bên dưới. */
export function CompanyHeader({ company, subline, actions, tabs, titleAs: Title = "h1" }: CompanyHeaderProps) {
  const info = [company.industry, company.size ? `${company.size} nhân sự` : null, company.city, company.website]
    .filter(Boolean)
    .join(" • ");
  return (
    <div className="overflow-hidden rounded-xl border border-border border-t-4 border-t-primary bg-white">
      <div className="flex flex-wrap items-center gap-4 p-4">
        <CompanyMark name={company.name} logoUrl={company.logoUrl} size={56} brand />
        <div className="min-w-0 flex-1 space-y-0.5">
          {/* "!" để cỡ chữ không bị quy tắc .hr-main h1 (30px) của HrShell ghi đè. */}
          <Title className="text-[18px]! leading-tight! font-bold break-words">{company.name}</Title>
          {info ? <p className="text-xs text-muted-foreground">{info}</p> : null}
          {subline ? <div className="text-xs text-muted-foreground">{subline}</div> : null}
        </div>
        {actions ? <div className="flex flex-wrap gap-2">{actions}</div> : null}
      </div>
      {tabs ? <div className="flex gap-1 overflow-x-auto border-t border-border px-2">{tabs}</div> : null}
    </div>
  );
}
