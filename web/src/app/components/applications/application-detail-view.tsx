"use client";

import Link from "next/link";
import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useSearchParams } from "next/navigation";
import { ArrowLeft, Building2, Download } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import { useAuthStore } from "@/hooks/use-auth-store";
import { applicationLinks, formatApplicationDate } from "@/config/application-status";
import { applicationService } from "@/services/application.service";
import { getErrorMessage } from "@/services/api";
import { ApplicationStatusBadge } from "./application-status-badge";
import { ApplicationProgress } from "./application-progress";
import { ApplicationHistory } from "./application-history";
import { WithdrawDialog } from "./withdraw-dialog";
import { candidateListReturnUrl } from "./journey-navigation";
import "./candidate-journey.css";

export function ApplicationDetailView({ id }: { id: string }) {
  const allowed = useAuthGuard({ roles: ["CANDIDATE"] });
  const user = useAuthStore(s => s.user), token = useAuthStore(s => s.token);
  return allowed ? <ApplicationDetail key={`${user?.id}:${token}:${id}`} id={id} /> : null;
}

function ApplicationDetail({ id }: { id: string }) {
  const params = useSearchParams();
  const user = useAuthStore(s => s.user), token = useAuthStore(s => s.token);
  const client = useQueryClient(), queryKey = ["candidate-application", user?.id, token, id];
  const query = useQuery({ queryKey, queryFn: () => applicationService.detail(id), staleTime: 0, refetchOnMount: "always" });
  const [downloadError, setDownloadError] = useState(""), [downloading, setDownloading] = useState(false);
  async function download() {
    if (downloading) return;
    setDownloading(true); setDownloadError("");
    try {
      const blob = await applicationService.downloadSnapshot(id), url = URL.createObjectURL(blob), link = document.createElement("a");
      link.href = url; link.download = `cv-da-nop-${id}.pdf`; link.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (err) { setDownloadError(getErrorMessage(err)); }
    finally { setDownloading(false); }
  }
  const application = !query.isError ? query.data : undefined;
  return <div className="journey">
    <Link href={candidateListReturnUrl(params.get("returnTo"), "/candidate/applications")} className="journey-back"><ArrowLeft aria-hidden="true" />Ứng tuyển của tôi</Link>
    {query.isPending && <p role="status" aria-busy="true">Đang tải chi tiết đơn…</p>}
    {query.isError && <div role="alert" className="journey-error"><p>{getErrorMessage(query.error)}</p><Button variant="outline" onClick={() => query.refetch()}>Tải lại</Button></div>}
    {application && <div className="journey-stack">
      <section className="journey-card"><div className="journey-card-top"><div><div className="journey-eyebrow">Hồ sơ ứng tuyển</div><h1>{application.jobTitle}</h1><p className="journey-company"><Building2 aria-hidden="true" />{application.companyName}</p><p className="journey-small journey-muted mt-4">Ngày nộp: {formatApplicationDate(application.createdAt)}</p></div><ApplicationStatusBadge status={application.status} /></div></section>
      <div className="journey-detail-grid"><div className="journey-stack">
        <ApplicationProgress status={application.status} history={application.history} />
        {application.status === "INTERVIEW" && <div className="journey-info"><p>Theo dõi lời mời và chọn khung giờ tại trang phỏng vấn.</p><Link href="/candidate/interviews" className="journey-link">Xem lịch phỏng vấn →</Link></div>}
        {application.status === "OFFER" && <div className="journey-info"><p>Bạn có thư mời nhận việc. Vui lòng kiểm tra email hoặc thông báo để xem và phản hồi.</p></div>}
        {application.coverLetter && <section className="journey-card"><h2>Thư giới thiệu đã gửi</h2><p className="journey-note mt-4">{application.coverLetter}</p></section>}
        <ApplicationHistory items={application.history} />
      </div><aside className="journey-aside"><section className="journey-card"><h2>Tài liệu đã nộp</h2><div className="journey-actions"><Button variant="outline" onClick={download} disabled={downloading}><Download className="mr-2 h-4 w-4" />{downloading ? "Đang tải…" : "Tải CV đã nộp"}</Button><p className="journey-small journey-muted">Bản CV đã nộp được giữ nguyên, kể cả khi bạn chỉnh sửa CV sau đó.</p>{downloadError && <p role="alert" className="journey-error">{downloadError}</p>}<Link href={applicationLinks.job(application.jobId)} className="journey-link">Xem tin tuyển dụng →</Link></div></section>
        {(application.canWithdraw || ["REJECTED", "WITHDRAWN"].includes(application.status)) && <section className="journey-card"><h2>Thao tác hồ sơ</h2><div className="journey-actions">{application.canWithdraw && <WithdrawDialog id={id} onWithdrawn={value => { client.setQueryData(queryKey, value); void client.invalidateQueries({ queryKey: ["candidate-applications"] }); }} onConflict={() => { void query.refetch(); }} />}{["REJECTED", "WITHDRAWN"].includes(application.status) && <Link href={applicationLinks.job(application.jobId)} className="journey-link">Ứng tuyển lại →</Link>}</div></section>}
      </aside></div>
    </div>}
  </div>;
}
