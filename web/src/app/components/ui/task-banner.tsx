import type { ReactNode } from "react";
import styles from "./task-banner.module.css";

type TaskBannerProps = {
  title: string;
  description: string;
  eyebrow: string;
  /** applications, interviews: hình gốc (UT-02); company, jobs, dashboard, interns: bổ sung cho khu HR cùng phong cách. */
  illustration: "applications" | "interviews" | "company" | "jobs" | "dashboard" | "interns";
  compact?: boolean;
  focusableTitle?: boolean;
  children?: ReactNode;
};

export function TaskBanner({ title, description, eyebrow, illustration, compact = false, focusableTitle = false, children }: TaskBannerProps) {
  return <header className={`${styles.banner} ${compact ? styles.compact : styles.spacious}`}>
    <div className={styles.copy}>
      <p className={styles.eyebrow}>{eyebrow}</p>
      <h1 className={styles.title} tabIndex={focusableTitle ? -1 : undefined}>{title}</h1>
      <p className={styles.description}>{description}</p>
      {children && <div className={styles.meta}>{children}</div>}
    </div>
    <svg className={styles.art} viewBox="0 0 360 210" fill="none" aria-hidden="true" focusable="false">
      <circle cx="198" cy="105" r="97" stroke="white" strokeOpacity=".14" />
      <circle cx="198" cy="105" r="75" stroke="white" strokeOpacity=".09" />
      <path d="M44 66H91L122 98M269 63H303V107M96 169H135L159 145" stroke="#F6C6CB" strokeOpacity=".65" strokeWidth="1.5" strokeDasharray="4 5" />
      <circle cx="44" cy="66" r="4" fill="#F2CB87" /><circle cx="303" cy="107" r="4" fill="#F2CB87" />
      {illustration in EXTRA_ART ? EXTRA_ART[illustration as keyof typeof EXTRA_ART] : illustration === "applications" ? <>
        <g transform="rotate(-7 187 108)">
          <rect x="118" y="18" width="142" height="182" rx="14" fill="#641727" fillOpacity=".18" transform="translate(0 7)" />
          <rect x="118" y="18" width="142" height="182" rx="14" fill="#FFF5F2" />
          <rect x="133" y="34" width="35" height="35" rx="10" fill="#F7D6DB" />
          <circle cx="150" cy="46" r="6" fill="#B4232D" />
          <path d="M140 62C140 51 160 51 160 62" fill="#B4232D" />
          <path d="M181 43H241M181 55H224" stroke="#BA7A84" strokeWidth="5" strokeLinecap="round" />
          <path d="M134 84H244" stroke="#E9C5CB" />
          <path d="M135 102H192M135 145H187" stroke="#A44555" strokeWidth="5" strokeLinecap="round" />
          <path d="M135 116H240M135 127H221M135 159H234M135 170H212" stroke="#DEC0C6" strokeWidth="4" strokeLinecap="round" />
        </g>
        <g transform="rotate(7 278 118)">
          <rect x="244" y="82" width="69" height="74" rx="13" fill="#FFE5E8" />
          <circle cx="273" cy="111" r="13" stroke="#B4232D" strokeWidth="3" />
          <path d="M283 121L295 133" stroke="#B4232D" strokeWidth="4" strokeLinecap="round" />
        </g>
        {!compact && <g><rect x="54" y="113" width="79" height="47" rx="12" fill="#FFF5F2" />
        <path d="M69 136H116" stroke="#E7B7BE" strokeWidth="2" />
        <circle cx="70" cy="136" r="5" fill="#B4232D" /><circle cx="93" cy="136" r="5" fill="#B4232D" /><circle cx="116" cy="136" r="5" fill="#D8A3AD" /></g>}
      </> : <>
        <g transform="rotate(-5 187 109)">
          <rect x="101" y="30" width="165" height="157" rx="14" fill="#641727" fillOpacity=".18" transform="translate(0 7)" />
          <rect x="101" y="30" width="165" height="157" rx="14" fill="#FFF5F2" />
          <path d="M101 44Q101 30 115 30H252Q266 30 266 44V71H101Z" fill="#F8D8DD" />
          <path d="M136 22V42M229 22V42" stroke="#FFF5F2" strokeWidth="7" strokeLinecap="round" />
          {[0, 1, 2].map(row => [0, 1, 2, 3].map(col => <rect key={`${row}-${col}`} x={121 + col * 34} y={86 + row * 29} width="19" height="15" rx="4" fill={row === 1 && col === 1 ? "#B4232D" : "#F2DCE0"} />))}
          <path d="M158 122L162 126L169 118" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </g>
        {!compact && <g transform="rotate(6 278 135)">
          <rect x="226" y="104" width="104" height="65" rx="12" fill="#FFE5E8" />
          <rect x="238" y="115" width="55" height="42" rx="7" fill="#FFF5F2" />
          <circle cx="266" cy="128" r="6" fill="#B4232D" />
          <path d="M254 147C254 134 278 134 278 147" fill="#B4232D" />
          <path d="M299 130L316 120V151L299 141Z" fill="#B4232D" />
        </g>}
        <circle cx="79" cy="111" r="24" fill="#FFF5F2" />
        <circle cx="79" cy="111" r="15" stroke="#B4232D" strokeWidth="2" />
        <path d="M79 102V111L86 115" stroke="#B4232D" strokeWidth="2" strokeLinecap="round" />
      </>}
      {compact && (illustration === "applications" || illustration === "interviews") && <g transform={illustration === "applications" ? "translate(41 115) rotate(-4)" : "translate(220 108) rotate(6)"}>
        <rect width="112" height="65" rx="12" fill="#FFE5E8" />
        <path d="M23 47H89" stroke="#C5818F" strokeWidth="1.5" strokeDasharray="3 3" />
        {[23, 56, 89].map(x => <g key={x}>
          <circle cx={x} cy="25" r="14" fill="#FFF5F2" />
          <circle cx={x} cy="21" r="4" fill="#B4232D" />
          <path d={`M${x - 7} 33C${x - 7} 25 ${x + 7} 25 ${x + 7} 33`} fill="#B4232D" />
          <circle cx={x} cy="47" r="3" fill="#B4232D" />
        </g>)}
      </g>}
      <circle cx="260" cy="40" r="17" fill="#F2CB87" />
      <path d="M253 40L258 45L268 35" stroke="#7E2533" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M76 40V52M70 46H82M290 180V190M285 185H295" stroke="#F2CB87" strokeWidth="2" strokeLinecap="round" />
    </svg>
  </header>;
}

/* Hình bổ sung: khung 360x210 giống hình gốc, giấy màu kem #FFF5F2, hồng #FFE5E8/#F7D6DB, đỏ #B4232D, vàng #F2CB87. */
const PAPER = "#FFF5F2";
const SHADOW = { fill: "#641727", fillOpacity: ".18" } as const;
const EXTRA_ART = {
  company: <>
    <rect x="152" y="36" width="92" height="160" rx="10" {...SHADOW} transform="translate(0 7)" />
    <rect x="152" y="36" width="92" height="160" rx="10" fill={PAPER} />
    <path d="M152 46Q152 36 162 36H234Q244 36 244 46V58H152Z" fill="#F7D6DB" />
    {[0, 1, 2, 3, 4].map(row => [0, 1, 2].map(col => <rect key={`c-${row}-${col}`} x={166 + col * 24} y={70 + row * 22} width="16" height="13" rx="3" fill={row === 2 && col === 1 ? "#B4232D" : "#F2DCE0"} />))}
    <rect x="186" y="176" width="24" height="20" rx="3" fill="#B4232D" />
    <path d="M198 36V16M198 16H220L214 23L220 30H198" stroke="#FFF5F2" strokeWidth="3" strokeLinejoin="round" />
    <rect x="100" y="96" width="56" height="100" rx="9" fill="#FFE5E8" />
    {[0, 1, 2, 3].map(row => [0, 1].map(col => <rect key={`l-${row}-${col}`} x={110 + col * 20} y={108 + row * 20} width="13" height="11" rx="3" fill="#F7D6DB" />))}
    <path d="M86 196H300" stroke="#F6C6CB" strokeOpacity=".7" strokeWidth="2" strokeLinecap="round" />
    <g transform="rotate(6 286 126)"><rect x="246" y="98" width="82" height="54" rx="11" fill="#FFE5E8" />
      <rect x="256" y="109" width="22" height="22" rx="6" fill={PAPER} /><path d="M262 120H272M267 115V125" stroke="#B4232D" strokeWidth="2.5" strokeLinecap="round" />
      <path d="M286 114H316M286 124H306" stroke="#C5818F" strokeWidth="4" strokeLinecap="round" /><path d="M258 141H316" stroke="#E7B7BE" strokeWidth="2" /></g>
  </>,
  jobs: <>
    <g transform="rotate(-6 182 110)">
      <rect x="112" y="26" width="150" height="170" rx="14" {...SHADOW} transform="translate(0 7)" />
      <rect x="112" y="26" width="150" height="170" rx="14" fill={PAPER} />
      <rect x="128" y="42" width="34" height="34" rx="10" fill="#F7D6DB" />
      <path d="M137 66V52H153V66M141 57H144M147 57H150M141 61H144M147 61H150" stroke="#B4232D" strokeWidth="2" strokeLinecap="round" />
      <path d="M174 50H240M174 63H218" stroke="#BA7A84" strokeWidth="5" strokeLinecap="round" />
      <rect x="128" y="90" width="62" height="18" rx="9" fill="#DDF3E4" />
      <path d="M137 99H181" stroke="#4E9A6A" strokeWidth="4" strokeLinecap="round" />
      {[0, 1, 2].map(i => <rect key={i} x={128 + i * 42} y="118" width="36" height="16" rx="8" fill="#F2DCE0" />)}
      <path d="M128 150H244M128 162H226" stroke="#DEC0C6" strokeWidth="4" strokeLinecap="round" />
      <rect x="128" y="174" width="116" height="12" rx="6" fill="#B4232D" />
    </g>
    <g transform="rotate(-12 286 118)">
      <path d="M260 106L300 88V146L260 128Z" fill="#B4232D" />
      <rect x="244" y="104" width="20" height="26" rx="5" fill="#FFE5E8" />
      <path d="M252 130L258 150H268L264 130" fill="#F7D6DB" />
      <path d="M312 104Q322 117 312 130M322 96Q338 117 322 138" stroke="#FFE5E8" strokeWidth="3" strokeLinecap="round" />
    </g>
  </>,
  dashboard: <>
    <rect x="104" y="30" width="170" height="150" rx="14" {...SHADOW} transform="translate(0 7)" />
    <rect x="104" y="30" width="170" height="150" rx="14" fill={PAPER} />
    <path d="M120 48H178" stroke="#BA7A84" strokeWidth="5" strokeLinecap="round" />
    <path d="M120 160H258" stroke="#E9C5CB" />
    {[42, 64, 52, 86, 74, 104].map((h, i) => <rect key={i} x={124 + i * 22} y={160 - h} width="14" height={h} rx="4" fill={i === 5 ? "#B4232D" : "#F2C9CF"} />)}
    <path d="M131 112L153 98L175 106L197 80L219 88L241 60" stroke="#B4232D" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
    {[[131, 112], [175, 106], [219, 88], [241, 60]].map(([cx, cy]) => <circle key={cx} cx={cx} cy={cy} r="4" fill="#FFF5F2" stroke="#B4232D" strokeWidth="2" />)}
    <g transform="rotate(-6 76 128)"><rect x="40" y="96" width="74" height="70" rx="12" fill="#FFE5E8" />
      <circle cx="77" cy="131" r="20" stroke="#F7D6DB" strokeWidth="8" />
      <circle cx="77" cy="131" r="20" stroke="#B4232D" strokeWidth="8" strokeDasharray="82 126" transform="rotate(-90 77 131)" strokeLinecap="round" /></g>
    <g transform="rotate(5 300 140)"><rect x="268" y="116" width="66" height="44" rx="10" fill="#FFE5E8" />
      <path d="M280 146L292 134L302 140L320 126" stroke="#4E9A6A" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" /></g>
  </>,
  interns: <>
    <g transform="rotate(-5 196 118)">
      <rect x="116" y="52" width="164" height="124" rx="12" {...SHADOW} transform="translate(0 7)" />
      <rect x="116" y="52" width="164" height="124" rx="12" fill={PAPER} />
      <rect x="126" y="62" width="144" height="104" rx="8" stroke="#F2C9CF" strokeWidth="2" />
      <path d="M150 84H246" stroke="#BA7A84" strokeWidth="6" strokeLinecap="round" />
      <path d="M160 104H236M168 118H228" stroke="#DEC0C6" strokeWidth="4" strokeLinecap="round" />
      <path d="M142 148H184" stroke="#C5818F" strokeWidth="2" />
      <circle cx="238" cy="144" r="15" fill="#B4232D" /><path d="M231 156L228 172L238 166L248 172L245 156" fill="#B4232D" />
      <path d="M232 144L236 148L244 140" stroke="#FFF5F2" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
    </g>
    <g transform="rotate(-10 92 92)">
      <path d="M52 86L94 68L136 86L94 104Z" fill="#B4232D" />
      <path d="M70 94V112Q94 124 118 112V94L94 104Z" fill="#7E2533" />
      <path d="M134 87V112" stroke="#F2CB87" strokeWidth="2.5" /><circle cx="134" cy="115" r="4" fill="#F2CB87" />
    </g>
    <g transform="rotate(7 298 92)"><rect x="268" y="70" width="62" height="44" rx="10" fill="#FFE5E8" />
      <circle cx="286" cy="88" r="7" fill="#B4232D" /><path d="M277 104C277 96 295 96 295 104" fill="#B4232D" />
      <path d="M302 86H320M302 96H314" stroke="#C5818F" strokeWidth="3" strokeLinecap="round" /></g>
  </>,
};
