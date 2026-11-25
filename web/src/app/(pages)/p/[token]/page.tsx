"use client";

import * as React from "react";
import { useParams } from "next/navigation";
import { BadgeCheck, Lock } from "lucide-react";
import { cvService } from "@/services/cv.service";
import { ApiError } from "@/services/api";
import type { PublicProfile } from "@/interface/cv";
import { BADGE, CHIP, META, TONE } from "@/app/components/profile/display";

/** HS-04: trang mà QR trên CV dẫn tới. Công khai, không cần đăng nhập; chỉ hiện dữ liệu đã xác thực. */
export default function PublicCvProfilePage() {
  const { token } = useParams<{ token: string }>();
  const [data, setData] = React.useState<PublicProfile | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  React.useEffect(() => {
    let ignore = false;
    cvService.publicProfile(token).then(d => { if (!ignore) setData(d); })
      .catch(err => { if (!ignore) setError(err instanceof ApiError && err.status === 404 ? "Không tìm thấy hồ sơ." : "Không tải được hồ sơ. Vui lòng thử lại."); });
    return () => { ignore = true; };
  }, [token]);

  return <main className="min-h-screen bg-[#F6F7FA] px-4 py-10">
    <div className="mx-auto max-w-3xl space-y-6">
      <p className="text-lg font-semibold tracking-[0.2em] text-[#B4232D]">RIKKEI <span className="text-sm text-[#64748B]">JOBS</span></p>
      {error ? <Notice title={error} /> : null}
      {!data && !error ? <p role="status">Đang tải hồ sơ...</p> : null}
      {data && !data.visible ? <Notice icon title="Hồ sơ không công khai" text="Ứng viên chưa công khai hồ sơ năng lực trên Rikkei Jobs." /> : null}
      {data?.visible ? <>
        <header className="rounded-2xl border bg-white p-6">
          <h1 className="text-2xl font-semibold text-[#18233A]">{data.fullName}</h1>
          {data.headline ? <p className="mt-1 text-[#B4232D]">{data.headline}</p> : null}
          <p className={`mt-2 ${META}`}>{[data.program, data.courseName].filter(Boolean).join(" · ")}</p>
          <p className={`mt-3 ${BADGE} ${TONE.success}`}><BadgeCheck className="size-3.5" aria-hidden />Dữ liệu dưới đây do Rikkei xác thực</p>
        </header>
        <section className="rounded-2xl border bg-white p-6">
          <h2 className="mb-3 text-lg font-semibold text-[#18233A]">Kỹ năng đã xác thực</h2>
          {data.skills.length === 0 ? <p className={META}>Chưa có kỹ năng đã xác thực.</p> : <ul className="divide-y">
            {data.skills.map(s => <li key={`${s.group}:${s.name}`} className="flex flex-wrap items-center justify-between gap-2 py-2">
              <span><span className="font-medium">{s.name}</span> <span className={META}>· {s.group}</span></span>
              <span className={`${BADGE} ${TONE.success}`}>{Number(s.verifiedScore)} điểm{s.level ? ` · ${s.level}` : ""}</span>
            </li>)}
          </ul>}
        </section>
        <section className="rounded-2xl border bg-white p-6">
          <h2 className="mb-3 text-lg font-semibold text-[#18233A]">Thực tập tại Rikkei</h2>
          {data.internships.length === 0 ? <p className={META}>Chưa có kỳ thực tập.</p> : <ul className="space-y-4">
            {data.internships.map((p, i) => <li key={`${p.name}:${i}`}>
              <div className="flex flex-wrap items-baseline justify-between gap-2">
                <span className="font-medium">{p.name}{p.role ? <span className="font-normal text-[#64748B]"> · {p.role}</span> : null}</span>
                {p.period ? <span className={META}>{p.period}</span> : null}
              </div>
              {p.techStack.length ? <div className="mt-1 flex flex-wrap gap-1.5">{p.techStack.map(t => <span key={t} className={CHIP}>{t}</span>)}</div> : null}
              {p.description ? <p className="mt-1 whitespace-pre-line text-sm text-[#334155]">{p.description}</p> : null}
            </li>)}
          </ul>}
        </section>
      </> : null}
    </div>
  </main>;
}

function Notice({ title, text, icon }: { title: string; text?: string; icon?: boolean }) {
  return <div className="rounded-2xl border bg-white p-8 text-center">
    {icon ? <Lock className="mx-auto mb-3 size-8 text-[#64748B]" aria-hidden /> : null}
    <h1 className="text-lg font-semibold text-[#18233A]">{title}</h1>
    {text ? <p className={`mt-1 ${META}`}>{text}</p> : null}
  </div>;
}
