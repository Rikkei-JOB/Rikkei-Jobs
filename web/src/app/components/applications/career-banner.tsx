import Link from "next/link";
import { ArrowUpRight } from "lucide-react";
import "./career-banner.css";

export function CareerBanner() {
  return <header className="career-banner">
    <div className="career-banner-copy">
      <p className="career-banner-eyebrow">CƠ HỘI NGHỀ NGHIỆP</p>
      <h1>Bước tiếp theo{" "}<span>cho sự nghiệp của bạn</span></h1>
      <p className="career-banner-description">Khám phá cơ hội phù hợp và kết nối với doanh nghiệp.</p>
      <Link href="/candidate/profile" className="career-banner-profile">Hoàn thiện hồ sơ <ArrowUpRight aria-hidden="true" /></Link>
    </div>
    <svg className="career-banner-art" viewBox="0 0 440 260" fill="none" aria-hidden="true" focusable="false">
      <defs>
        <linearGradient id="career-paper" x1="165" y1="38" x2="340" y2="240" gradientUnits="userSpaceOnUse"><stop stopColor="white" /><stop offset="1" stopColor="#FFE8E8" /></linearGradient>
        <filter id="career-paper-shadow" x="100" y="0" width="290" height="285" filterUnits="userSpaceOnUse"><feDropShadow dx="0" dy="12" stdDeviation="12" floodColor="#591524" floodOpacity=".24" /></filter>
      </defs>
      <circle cx="262" cy="130" r="118" stroke="white" strokeOpacity=".12" />
      <circle cx="262" cy="130" r="94" stroke="white" strokeOpacity=".08" />
      <circle cx="262" cy="130" r="69" fill="white" fillOpacity=".035" />
      <path d="M97 83C142 83 133 139 188 139M307 78C354 78 339 133 377 133M260 214C260 247 179 243 153 217" stroke="#F8D8DA" strokeOpacity=".65" strokeWidth="1.5" strokeDasharray="4 6" />
      <circle cx="97" cy="83" r="5" fill="#F2CB87" /><circle cx="377" cy="133" r="5" fill="#F2CB87" />
      <g transform="rotate(-7 247 132)" filter="url(#career-paper-shadow)">
        <rect x="165" y="30" width="164" height="202" rx="14" fill="url(#career-paper)" />
        <rect x="182" y="47" width="38" height="38" rx="10" fill="#F9DADD" />
        <circle cx="201" cy="60" r="6" fill="#B4232D" />
        <path d="M190 77C190 68 212 68 212 77" fill="#B4232D" />
        <text x="233" y="63" fill="#7E2533" fontSize="18" fontWeight="700" style={{ fontFamily: "var(--font-sans), sans-serif" }}>CV</text>
        <rect x="233" y="73" width="66" height="4" rx="2" fill="#D6A7AD" />
        <path d="M183 100H310" stroke="#E9C7CB" />
        <rect x="183" y="114" width="70" height="5" rx="2.5" fill="#B5616C" />
        <rect x="183" y="128" width="124" height="4" rx="2" fill="#E1BEC3" />
        <rect x="183" y="139" width="106" height="4" rx="2" fill="#E1BEC3" />
        <rect x="183" y="162" width="64" height="5" rx="2.5" fill="#B5616C" />
        <rect x="183" y="176" width="94" height="4" rx="2" fill="#E1BEC3" />
        <rect x="183" y="194" width="43" height="17" rx="5" fill="#F6D2D7" />
        <rect x="233" y="194" width="34" height="17" rx="5" fill="#F6D2D7" />
        <rect x="274" y="194" width="36" height="17" rx="5" fill="#F6D2D7" />
      </g>
      <g transform="rotate(5 107 88)">
        <rect x="44" y="56" width="126" height="58" rx="13" fill="#FFF7F5" />
        <rect x="54" y="67" width="36" height="36" rx="10" fill="#FBE4E6" />
        <path d="M68 78L62 85L68 92M78 78L84 85L78 92M75 76L71 94" stroke="#B4232D" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        <text x="99" y="91" fill="#7E2533" fontSize="13" fontWeight="600" style={{ fontFamily: "var(--font-sans), sans-serif" }}>Kỹ năng</text>
      </g>
      <g transform="rotate(7 352 149)">
        <rect x="305" y="123" width="103" height="61" rx="13" fill="#FFF7F5" />
        <rect x="318" y="137" width="29" height="23" rx="4" fill="#F9DADD" stroke="#B4232D" strokeWidth="1.6" />
        <path d="M326 136V133H339V136M318 146H347M330 145V149H335V145" stroke="#B4232D" strokeWidth="1.6" strokeLinejoin="round" />
        <text x="355" y="152" fill="#7E2533" fontSize="12" fontWeight="600" style={{ fontFamily: "var(--font-sans), sans-serif" }}>Cơ hội</text>
      </g>
      <circle cx="146" cy="212" r="22" fill="#F2CB87" />
      <path d="M136 212L143 219L156 205" stroke="#7E2533" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M355 40V54M348 47H362" stroke="#F2CB87" strokeWidth="2" strokeLinecap="round" />
      <circle cx="86" cy="168" r="3" fill="#F4B5BD" /><circle cx="371" cy="215" r="3" fill="#F4B5BD" />
    </svg>
  </header>;
}
