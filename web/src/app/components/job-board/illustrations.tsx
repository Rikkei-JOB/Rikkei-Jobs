/**
 * Minh hoạ SVG tự vẽ cho các trang ứng viên (trang chủ, công ty, trạng thái trống).
 * Chỉ để trang trí (aria-hidden); màu theo bảng màu của job-board.css; chữ trong hình dùng font chung.
 */
import type { CSSProperties, ReactNode } from "react";

const RED = "#B4232D";
const NAVY = "#051A53";
const GREEN = "#15803D";
const MUTED = "#64748B";
const LINE = "#E2E8F0";
const GOLD = "#F2CB87";
const TEXT_STYLE: CSSProperties = { fontFamily: "var(--font-sans), sans-serif" };

function Art({ viewBox, className, children }: { viewBox: string; className?: string; children: ReactNode }) {
  return <svg viewBox={viewBox} className={className} fill="none" aria-hidden="true" focusable="false" style={TEXT_STYLE}>{children}</svg>;
}

function Sparkle({ x, y, size = 8, color = GOLD }: { x: number; y: number; size?: number; color?: string }) {
  return <path d={`M${x} ${y - size}Q${x} ${y} ${x + size} ${y}Q${x} ${y} ${x} ${y + size}Q${x} ${y} ${x - size} ${y}Q${x} ${y} ${x} ${y - size}Z`} fill={color} />;
}

/** Hero trang chủ (nền tối): hồ sơ năng lực, thẻ việc làm, độ khớp và lịch phỏng vấn. */
export function HeroIllustration({ className }: { className?: string }) {
  return <Art viewBox="0 0 440 380" className={className}>
    <defs>
      <filter id="hero-shadow" x="-20%" y="-20%" width="140%" height="150%"><feDropShadow dx="0" dy="14" stdDeviation="14" floodColor="#000" floodOpacity=".35" /></filter>
    </defs>
    <circle cx="230" cy="190" r="172" fill="#fff" fillOpacity=".05" />
    <circle cx="230" cy="190" r="124" stroke="#fff" strokeOpacity=".14" strokeDasharray="4 8" />

    {/* Hồ sơ năng lực đã xác thực */}
    <g transform="rotate(-6 140 185)" filter="url(#hero-shadow)">
      <rect x="40" y="70" width="200" height="230" rx="18" fill="#fff" />
      <circle cx="82" cy="112" r="22" fill="#FFE4E6" />
      <circle cx="82" cy="106" r="8" fill={RED} />
      <path d="M68 126C68 117 96 117 96 126" fill={RED} />
      <rect x="114" y="100" width="96" height="9" rx="4.5" fill={NAVY} fillOpacity=".85" />
      <rect x="114" y="116" width="64" height="7" rx="3.5" fill="#CBD5E1" />
      <circle cx="214" cy="90" r="13" fill={GREEN} />
      <path d="M208 90L212.5 94.5L220 86" stroke="#fff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
      <text x="60" y="160" fill={MUTED} fontSize="11" fontWeight="600">Kỹ năng xác thực</text>
      {[{ y: 174, w: 96, c: RED, l: "Java" }, { y: 198, w: 80, c: "#D9464E", l: "SQL" }, { y: 222, w: 62, c: "#F08A8F", l: "Git" }].map(bar => <g key={bar.l}>
        <text x="60" y={bar.y + 8} fill={NAVY} fontSize="10" fontWeight="600">{bar.l}</text>
        <rect x="100" y={bar.y} width="118" height="9" rx="4.5" fill="#F1F5F9" />
        <rect x="100" y={bar.y} width={bar.w} height="9" rx="4.5" fill={bar.c} />
      </g>)}
      <rect x="60" y="252" width="122" height="24" rx="12" fill="#F0FDF4" />
      <text x="72" y="268" fill={GREEN} fontSize="10" fontWeight="700">Đồng bộ từ LMS</text>
    </g>

    {/* Thẻ việc làm */}
    <g transform="rotate(4 300 245)" filter="url(#hero-shadow)">
      <rect x="190" y="150" width="220" height="190" rx="18" fill="#fff" />
      <rect x="210" y="170" width="40" height="40" rx="10" fill="#FFF1F2" stroke="#FECDD3" />
      <path d="M222 200V182H238V200M226 187H229M231 187H234M226 192H229M231 192H234" stroke={RED} strokeWidth="2" strokeLinecap="round" />
      <rect x="262" y="174" width="118" height="10" rx="5" fill={NAVY} />
      <rect x="262" y="192" width="80" height="8" rx="4" fill="#CBD5E1" />
      <rect x="210" y="224" width="112" height="24" rx="12" fill="#DCFCE7" />
      <text x="222" y="240" fill={GREEN} fontSize="11" fontWeight="700">15 - 25 triệu</text>
      {[{ x: 210, w: 48, l: "Java" }, { x: 264, w: 62, l: "Spring" }, { x: 332, w: 46, l: "SQL" }].map(chip => <g key={chip.l}>
        <rect x={chip.x} y="260" width={chip.w} height="22" rx="11" stroke={LINE} fill="#fff" />
        <text x={chip.x + chip.w / 2} y="275" textAnchor="middle" fill="#334155" fontSize="10" fontWeight="500">{chip.l}</text>
      </g>)}
      <rect x="210" y="298" width="180" height="28" rx="9" fill={RED} />
      <text x="300" y="316" textAnchor="middle" fill="#fff" fontSize="11" fontWeight="700">Ứng tuyển ngay</text>
    </g>

    {/* Độ khớp kỹ năng */}
    <g filter="url(#hero-shadow)">
      <rect x="290" y="46" width="140" height="66" rx="16" fill="#fff" />
      <circle cx="322" cy="79" r="17" stroke="#F1F5F9" strokeWidth="6" />
      <circle cx="322" cy="79" r="17" stroke={RED} strokeWidth="6" strokeLinecap="round" strokeDasharray="98 107" transform="rotate(-90 322 79)" />
      <text x="322" y="83" textAnchor="middle" fill={NAVY} fontSize="10" fontWeight="800">92%</text>
      <text x="348" y="74" fill={MUTED} fontSize="10">Độ khớp</text>
      <text x="348" y="90" fill={GREEN} fontSize="10" fontWeight="700">Rất phù hợp</text>
    </g>

    {/* Lịch phỏng vấn */}
    <g filter="url(#hero-shadow)">
      <rect x="18" y="302" width="150" height="50" rx="14" fill="#fff" />
      <rect x="32" y="315" width="24" height="24" rx="6" fill="#FFF1F2" />
      <path d="M37 322H51M40 318V321M48 318V321M38 327H42M45 327H49M38 332H42" stroke={RED} strokeWidth="1.8" strokeLinecap="round" />
      <text x="66" y="324" fill={MUTED} fontSize="10">Phỏng vấn</text>
      <text x="66" y="340" fill={NAVY} fontSize="11" fontWeight="700">Thứ 5 · 10:00</text>
    </g>

    <Sparkle x={60} y={42} size={9} />
    <Sparkle x={412} y={150} size={6} color="#FF9AA1" />
    <Sparkle x={420} y={330} size={8} />
    <circle cx="290" cy="40" r="3" fill="#FF9AA1" />
    <circle cx="24" cy="200" r="3" fill={GOLD} />
  </Art>;
}

/** Bước 1: hồ sơ năng lực với thanh kỹ năng và dấu xác thực. */
export function StepProfileArt({ className }: { className?: string }) {
  return <Art viewBox="0 0 200 140" className={className}>
    <circle cx="100" cy="72" r="62" fill="#FFF1F2" />
    <rect x="46" y="26" width="108" height="92" rx="12" fill="#fff" stroke={LINE} />
    <circle cx="70" cy="52" r="12" fill="#FFE4E6" />
    <circle cx="70" cy="49" r="4.5" fill={RED} />
    <path d="M62 60C62 55 78 55 78 60" fill={RED} />
    <rect x="88" y="44" width="50" height="6" rx="3" fill={NAVY} fillOpacity=".8" />
    <rect x="88" y="55" width="34" height="5" rx="2.5" fill="#CBD5E1" />
    {[{ y: 76, w: 72 }, { y: 88, w: 56 }, { y: 100, w: 40 }].map(bar => <g key={bar.y}>
      <rect x="60" y={bar.y} width="80" height="6" rx="3" fill="#F1F5F9" />
      <rect x="60" y={bar.y} width={bar.w} height="6" rx="3" fill={RED} fillOpacity={bar.w / 72} />
    </g>)}
    <circle cx="150" cy="30" r="13" fill={GREEN} />
    <path d="M144 30L148.5 34.5L156 26" stroke="#fff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
    <Sparkle x={34} y={34} size={6} />
  </Art>;
}

/** Bước 2: kính lúp trên chồng thẻ việc làm và máy bay giấy gửi hồ sơ. */
export function StepApplyArt({ className }: { className?: string }) {
  return <Art viewBox="0 0 200 140" className={className}>
    <circle cx="100" cy="72" r="62" fill="#EFF6FF" />
    <rect x="58" y="42" width="96" height="64" rx="10" fill="#fff" stroke={LINE} transform="rotate(-8 106 74)" />
    <rect x="48" y="36" width="96" height="64" rx="10" fill="#fff" stroke={LINE} />
    <rect x="58" y="46" width="20" height="20" rx="5" fill="#FFF1F2" />
    <rect x="84" y="48" width="48" height="6" rx="3" fill={NAVY} fillOpacity=".8" />
    <rect x="84" y="59" width="30" height="5" rx="2.5" fill="#CBD5E1" />
    <rect x="58" y="76" width="44" height="12" rx="6" fill="#DCFCE7" />
    <circle cx="128" cy="94" r="17" fill="#fff" stroke={NAVY} strokeWidth="5" />
    <path d="M140 106L154 120" stroke={NAVY} strokeWidth="7" strokeLinecap="round" />
    <path d="M150 34L178 22L168 52L160 40Z" fill={RED} />
    <path d="M160 40L178 22" stroke="#fff" strokeWidth="1.5" />
    <path d="M150 50C142 56 140 62 142 68" stroke={RED} strokeDasharray="3 4" strokeLinecap="round" />
  </Art>;
}

/** Bước 3: lịch phỏng vấn đã chốt và thư mời nhận việc. */
export function StepOfferArt({ className }: { className?: string }) {
  return <Art viewBox="0 0 200 140" className={className}>
    <circle cx="100" cy="72" r="62" fill="#F0FDF4" />
    <rect x="40" y="38" width="74" height="72" rx="10" fill="#fff" stroke={LINE} />
    <rect x="40" y="38" width="74" height="18" rx="10" fill={RED} />
    <rect x="40" y="48" width="74" height="8" fill={RED} />
    <path d="M56 32V44M98 32V44" stroke={NAVY} strokeWidth="4" strokeLinecap="round" />
    {[0, 1, 2].flatMap(r => [0, 1, 2, 3].map(c => <rect key={`${r}${c}`} x={50 + c * 15} y={64 + r * 13} width="9" height="8" rx="2" fill={r === 1 && c === 2 ? GREEN : "#E2E8F0"} />))}
    <g transform="rotate(8 140 84)">
      <rect x="104" y="58" width="64" height="48" rx="8" fill="#fff" stroke={LINE} />
      <path d="M104 64L136 86L168 64" stroke={RED} strokeWidth="2.5" strokeLinejoin="round" />
    </g>
    <circle cx="160" cy="44" r="14" fill={GOLD} />
    <path d="M154 44L158.5 48.5L166 40" stroke={NAVY} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
    <Sparkle x={30} y={100} size={6} color="#FF9AA1" />
  </Art>;
}

/** Banner hoàn thiện hồ sơ: huy hiệu xác thực trên nền hồ sơ. */
export function VerifiedProfileArt({ className }: { className?: string }) {
  return <Art viewBox="0 0 240 180" className={className}>
    <circle cx="120" cy="92" r="80" fill="#fff" fillOpacity=".08" />
    <rect x="46" y="34" width="120" height="132" rx="14" fill="#fff" />
    <circle cx="80" cy="66" r="16" fill="#FFE4E6" />
    <circle cx="80" cy="62" r="6" fill={RED} />
    <path d="M70 76C70 70 90 70 90 76" fill={RED} />
    <rect x="104" y="56" width="48" height="7" rx="3.5" fill={NAVY} fillOpacity=".85" />
    <rect x="104" y="69" width="32" height="6" rx="3" fill="#CBD5E1" />
    {[{ y: 98, w: 84 }, { y: 114, w: 64 }, { y: 130, w: 96 }].map(bar => <g key={bar.y}>
      <rect x="62" y={bar.y} width="88" height="7" rx="3.5" fill="#F1F5F9" />
      <rect x="62" y={bar.y} width={bar.w * 0.9} height="7" rx="3.5" fill={RED} />
    </g>)}
    <g transform="translate(160 104)">
      <path d="M0 -40L32 -28V-4C32 18 16 32 0 38C-16 32 -32 18 -32 -4V-28Z" fill={GREEN} />
      <path d="M-12 0L-3 9L14 -10" stroke="#fff" strokeWidth="5" strokeLinecap="round" strokeLinejoin="round" />
    </g>
    <Sparkle x={36} y={28} size={9} />
    <Sparkle x={212} y={44} size={7} color="#FF9AA1" />
    <circle cx="208" cy="160" r="4" fill={GOLD} />
  </Art>;
}

/** Hero trang công ty (nền tối): cụm toà nhà và thẻ công ty đang tuyển. */
export function CompaniesIllustration({ className }: { className?: string }) {
  const windows = (x: number, y: number, cols: number, rows: number) =>
    Array.from({ length: cols * rows }, (_, i) => <rect key={`${x}-${i}`} x={x + (i % cols) * 14} y={y + Math.floor(i / cols) * 16} width="8" height="9" rx="1.5"
      fill={(i * 7) % 5 === 0 ? GOLD : "#fff"} fillOpacity={(i * 7) % 5 === 0 ? 0.9 : 0.25} />);
  return <Art viewBox="0 0 360 230" className={className}>
    <circle cx="190" cy="120" r="104" fill="#fff" fillOpacity=".05" />
    <rect x="70" y="90" width="64" height="120" rx="6" fill="#fff" fillOpacity=".1" />
    {windows(80, 102, 4, 6)}
    <rect x="144" y="42" width="78" height="168" rx="6" fill="#fff" fillOpacity=".16" />
    {windows(156, 56, 4, 9)}
    <rect x="232" y="110" width="56" height="100" rx="6" fill="#fff" fillOpacity=".1" />
    {windows(242, 122, 3, 5)}
    <path d="M183 42V22M183 22H203L198 28L203 34H183" stroke="#FF9AA1" strokeWidth="2.5" strokeLinejoin="round" />
    <path d="M40 210H320" stroke="#fff" strokeOpacity=".3" strokeWidth="2" strokeLinecap="round" />
    <g>
      <rect x="20" y="44" width="120" height="44" rx="12" fill="#fff" />
      <rect x="30" y="54" width="24" height="24" rx="6" fill="#FFF1F2" />
      <text x="42" y="70" textAnchor="middle" fill={RED} fontSize="11" fontWeight="800">A</text>
      <rect x="62" y="56" width="58" height="7" rx="3.5" fill={NAVY} fillOpacity=".8" />
      <text x="62" y="78" fill={GREEN} fontSize="10" fontWeight="700">3 việc làm</text>
    </g>
    <g>
      <rect x="236" y="150" width="116" height="44" rx="12" fill="#fff" />
      <rect x="246" y="160" width="24" height="24" rx="6" fill="#EFF6FF" />
      <text x="258" y="176" textAnchor="middle" fill="#1D4ED8" fontSize="11" fontWeight="800">B</text>
      <rect x="278" y="162" width="56" height="7" rx="3.5" fill={NAVY} fillOpacity=".8" />
      <text x="278" y="184" fill={GREEN} fontSize="10" fontWeight="700">Đang tuyển</text>
    </g>
    <Sparkle x={320} y={40} size={8} />
    <Sparkle x={30} y={170} size={6} color="#FF9AA1" />
  </Art>;
}

/** Trạng thái trống khi tìm kiếm không có kết quả. */
export function EmptySearchArt({ className }: { className?: string }) {
  return <Art viewBox="0 0 180 120" className={className}>
    <circle cx="90" cy="62" r="52" fill="#F1F5F9" />
    <rect x="38" y="30" width="80" height="56" rx="10" fill="#fff" stroke="#CBD5E1" strokeDasharray="5 5" />
    <rect x="50" y="42" width="40" height="6" rx="3" fill="#E2E8F0" />
    <rect x="50" y="54" width="56" height="6" rx="3" fill="#E2E8F0" />
    <rect x="50" y="66" width="28" height="6" rx="3" fill="#E2E8F0" />
    <circle cx="118" cy="74" r="18" fill="#fff" stroke={RED} strokeWidth="5" />
    <path d="M131 87L146 102" stroke={RED} strokeWidth="7" strokeLinecap="round" />
    <text x="118" y="80" textAnchor="middle" fill={RED} fontSize="18" fontWeight="800">?</text>
    <Sparkle x={150} y={30} size={6} />
  </Art>;
}
