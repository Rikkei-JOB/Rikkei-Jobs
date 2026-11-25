import type { ReactNode } from "react";
import styles from "./task-banner.module.css";

type TaskBannerProps = {
  title: string;
  description: string;
  eyebrow: string;
  illustration: "applications" | "interviews";
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
      {illustration === "applications" ? <>
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
      {compact && <g transform={illustration === "applications" ? "translate(41 115) rotate(-4)" : "translate(220 108) rotate(6)"}>
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
