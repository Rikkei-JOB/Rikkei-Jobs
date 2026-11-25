import { companyService } from "@/services/company.service";

// Bảng 6 màu cố định cho ô chữ cái đầu khi công ty chưa có logo.
const MARK_COLORS = ["#2563eb", "#059669", "#d97706", "#7c3aed", "#db2777", "#0891b2"];

interface CompanyMarkProps {
  name: string;
  logoUrl?: string | null;
  size: number;
  /** Nền đỏ thương hiệu thay vì màu chọn theo tên. */
  brand?: boolean;
}

/** Logo công ty; chưa có logo thì ô chữ cái đầu của tên. */
export function CompanyMark({ name, logoUrl, size, brand }: CompanyMarkProps) {
  const src = companyService.logoSrc(logoUrl ?? null);
  const box = { width: size, height: size };
  if (src) {
    return (
      <span
        className="inline-flex shrink-0 items-center justify-center overflow-hidden rounded-lg border border-border bg-white"
        style={box}
      >
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src={src} alt={`Logo ${name}`} className="h-full w-full object-contain" />
      </span>
    );
  }
  const sum = Array.from(name).reduce((total, ch) => total + (ch.codePointAt(0) ?? 0), 0);
  return (
    <span
      aria-hidden="true"
      className={`inline-flex shrink-0 items-center justify-center rounded-lg font-semibold text-white ${brand ? "bg-primary" : ""}`}
      style={{
        ...box,
        fontSize: Math.round(size * 0.42),
        ...(brand ? {} : { backgroundColor: MARK_COLORS[sum % MARK_COLORS.length] }),
      }}
    >
      {name.trim().charAt(0).toUpperCase() || "?"}
    </span>
  );
}
