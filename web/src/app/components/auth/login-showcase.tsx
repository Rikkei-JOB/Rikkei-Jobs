import { BookOpen, Building2, GraduationCap, type LucideIcon } from "lucide-react"

const ROLES: { icon: LucideIcon; title: string; text: string; via: string; tone: string }[] = [
  { icon: GraduationCap, title: "Sinh viên & ứng viên", text: "Hồ sơ kỹ năng và kết quả học tập được đồng bộ từ LMS.", via: "LMS", tone: "bg-[#fff1f2] text-[#b4232d]" },
  { icon: BookOpen, title: "Mentor & cán bộ", text: "Đánh giá phỏng vấn, duyệt hồ sơ và điều phối tuyển dụng.", via: "LMS", tone: "bg-[#eff6ff] text-[#1d4ed8]" },
  { icon: Building2, title: "Doanh nghiệp", text: "Email và mật khẩu do cán bộ Rikkei cấp khi hợp tác.", via: "Email", tone: "bg-[#f0fdf4] text-[#15803d]" },
]

/** Cột giới thiệu bên trái trang đăng nhập (ẩn dưới 1024px): ai đăng nhập bằng cách nào. */
export function LoginShowcase() {
  return (
    <section aria-labelledby="login-showcase-title" className="hidden min-w-0 lg:block">
      <LoginArt className="mb-6 h-auto w-[300px] max-w-full" />
      <p className="text-xs font-bold tracking-[0.14em] text-[#b4232d] uppercase">Rikkei Jobs</p>
      <h2 id="login-showcase-title" className="mt-2 max-w-md text-[32px] leading-[1.2] font-extrabold tracking-tight text-heading text-balance">
        Một tài khoản cho cả hành trình nghề nghiệp
      </h2>
      <p className="mt-3 max-w-md text-[15px] leading-relaxed text-muted-foreground text-pretty">
        Từ học tập trên LMS, thực tập IOC đến lúc nhận offer, mọi bước đều nằm trên cùng một nền tảng.
      </p>
      <ul className="mt-7 grid max-w-md gap-3">
        {ROLES.map((role) => (
          <li key={role.title} className="flex items-start gap-3.5 rounded-2xl border border-border bg-white/80 p-4 shadow-[0_1px_2px_rgb(15_23_42/0.04)]">
            <span className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${role.tone}`}>
              <role.icon className="size-5" aria-hidden="true" />
            </span>
            <span className="min-w-0 flex-1">
              <span className="flex items-center gap-2">
                <strong className="text-sm font-bold text-heading">{role.title}</strong>
                <span className="rounded-md bg-[#f1f5f9] px-1.5 py-0.5 text-[11px] font-semibold text-[#475569]">Qua {role.via}</span>
              </span>
              <span className="mt-0.5 block text-[13px] leading-relaxed text-muted-foreground">{role.text}</span>
            </span>
          </li>
        ))}
      </ul>
    </section>
  )
}

const RED = "#B4232D"
const NAVY = "#051A53"
const GOLD = "#F2CB87"

/** Minh hoạ: thẻ hồ sơ ở giữa nối với LMS (trái) và doanh nghiệp (phải), khiên xác thực. Chỉ trang trí. */
function LoginArt({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 300 170" className={className} fill="none" aria-hidden="true" focusable="false">
      <circle cx="150" cy="88" r="78" fill="#FFF1F2" />
      <circle cx="150" cy="88" r="58" stroke="#FECDD3" strokeDasharray="4 6" />
      <path d="M62 96H104M196 96H238" stroke="#F6C6CB" strokeWidth="2" strokeDasharray="4 5" />
      {/* LMS */}
      <g transform="translate(20 70)">
        <rect width="48" height="52" rx="12" fill="#fff" stroke="#E2E8F0" />
        <path d="M12 22L24 16L36 22L24 28Z" fill={NAVY} />
        <path d="M17 25V32C17 34 31 34 31 32V25" stroke={NAVY} strokeWidth="2.5" />
        <rect x="12" y="38" width="24" height="5" rx="2.5" fill="#CBD5E1" />
      </g>
      {/* Doanh nghiệp */}
      <g transform="translate(232 66)">
        <rect width="48" height="56" rx="12" fill="#fff" stroke="#E2E8F0" />
        <rect x="14" y="14" width="20" height="30" rx="3" fill="#FFE4E6" />
        {[18, 26, 34].map((y) => <g key={y}><rect x="18" y={y} width="4" height="4" rx="1" fill={RED} /><rect x="26" y={y} width="4" height="4" rx="1" fill={RED} fillOpacity=".5" /></g>)}
        <rect x="10" y="44" width="28" height="3" rx="1.5" fill="#CBD5E1" />
      </g>
      {/* Thẻ hồ sơ */}
      <g transform="translate(104 34)">
        <rect width="92" height="112" rx="16" fill="#fff" stroke="#F1D5D8" />
        <rect width="92" height="30" rx="16" fill={RED} />
        <rect y="16" width="92" height="14" fill={RED} />
        <circle cx="46" cy="38" r="15" fill="#fff" stroke="#FECDD3" strokeWidth="3" />
        <circle cx="46" cy="35" r="5.5" fill={RED} />
        <path d="M37 47C37 41 55 41 55 47" fill={RED} />
        <rect x="24" y="62" width="44" height="6" rx="3" fill={NAVY} fillOpacity=".85" />
        <rect x="30" y="73" width="32" height="5" rx="2.5" fill="#CBD5E1" />
        <rect x="16" y="88" width="60" height="12" rx="6" fill="#FFF1F2" />
        <rect x="16" y="88" width="40" height="12" rx="6" fill={RED} fillOpacity=".85" />
      </g>
      {/* Khiên xác thực */}
      <g transform="translate(190 124)">
        <path d="M0 -20L18 -13V0C18 12 9 19 0 22C-9 19 -18 12 -18 0V-13Z" fill="#15803D" />
        <path d="M-7 1L-2 6L8 -5" stroke="#fff" strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round" />
      </g>
      <path d="M96 22Q96 30 104 30Q96 30 96 38Q96 30 88 30Q96 30 96 22Z" fill={GOLD} />
      <circle cx="226" cy="30" r="4" fill={GOLD} />
      <circle cx="58" cy="146" r="3" fill="#FF9AA1" />
    </svg>
  )
}
