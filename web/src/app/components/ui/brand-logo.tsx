/**
 * Logo "RIKKEI JOBS": phần RIKKEI vẽ lại theo logo Rikkei. JOBS theo cùng ngôn ngữ: chữ hoa cao 66 (y 11–77),
 * nét dày 9, ghép từ các mảnh rời như RIKKEI (vạch đầu J, thân B), nét cong mềm như bụng chữ R; cả chữ JOBS màu đỏ.
 * Cùng hình với /brand/rikkei-jobs-logo-compact.svg (dùng khi cần file ảnh, ví dụ email).
 * Mặc định ẩn với trình đọc màn hình vì đặt trong link đã có aria-label; truyền `label` nếu đứng một mình.
 */
const VIEW_WIDTH = 396;
const VIEW_HEIGHT = 85;

/** Phần đỏ của JOBS vẽ bằng nét (đường tâm, dày 9, đầu bằng): J cong, O bo tròn, hai bụng B, S hai khúc cong. */
const JOBS_RED_STROKES = [
  "M235.5 11V62A10.5 10.5 0 0 1 214.5 62V55",
  "M314 15.5H322A12 12 0 0 1 322 39.5H314M322 39.5H325A16.5 16.5 0 0 1 325 72.5H314",
  "M383.5 15.5H364A12 12 0 0 0 364 39.5H370A16.5 16.5 0 0 1 370 72.5H354.5",
];

export function BrandLogo({ height = 28, label, className, inverse = false }: { height?: number; label?: string; className?: string; inverse?: boolean }) {
  // inverse: đặt trên nền tối (footer), phần xám đổi sang trắng ngà, phần đỏ sáng hơn để đủ tương phản.
  const red = inverse ? "#F0444B" : "#BC2026";
  const gray = inverse ? "#E2E8F0" : "#959293";
  return (
    <svg
      viewBox={`-4 -4 ${VIEW_WIDTH} ${VIEW_HEIGHT}`}
      width={Math.round((height * VIEW_WIDTH) / VIEW_HEIGHT)}
      height={height}
      className={className}
      role={label ? "img" : undefined}
      aria-label={label}
      aria-hidden={label ? undefined : true}
      focusable="false"
    >
      <path
        fill={red}
        fillRule="evenodd"
        d="M0 0H28C40 0 47 8 47 20.5C47 31 41 38.5 31 40.5L56 77H45L21 41.5H9V77H0ZM9 8V33.5H27C34 33.5 38 28.5 38 20.5C38 12.5 34 8 27 8Z"
      />
      <g fill={gray}>
        <rect x="61" y="0" width="9" height="8" />
        <rect x="61" y="11" width="9" height="66" />
        <path d="M101 11H111L84 44L111 77H101L74 44Z" />
        <rect x="135" y="11" width="38" height="9" />
        <rect x="135" y="40" width="25" height="8" />
        <rect x="135" y="68" width="38" height="9" />
        <rect x="178" y="11" width="9" height="66" />
      </g>
      <path fill={red} d="M119 11H130L103 44L130 77H119L93 44Z" />
      <g fill="none" stroke={red} strokeWidth="9">
        {JOBS_RED_STROKES.map(d => <path key={d} d={d} />)}
        <rect x="251.5" y="15.5" width="37" height="57" rx="18.5" />
      </g>
      {/* Mảnh rời như chữ I và E của RIKKEI (vạch đầu chữ J, thân chữ B); JOBS giữ toàn màu đỏ. */}
      <g fill={red}>
        <rect x="212" y="11" width="14" height="9" />
        <rect x="300" y="11" width="9" height="66" />
      </g>
    </svg>
  );
}
