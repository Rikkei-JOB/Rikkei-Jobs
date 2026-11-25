"use client";

import Link from "next/link";
import { useEffect, useRef } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Building2 } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import { useAuthStore } from "@/hooks/use-auth-store";
import { formatOfferSalary } from "@/interface/offer";
import type { Offer } from "@/interface/offer";
import { ApiError, getErrorMessage } from "@/services/api";
import { offerService } from "@/services/offer.service";
import { applicationService } from "@/services/application.service";
import { formatApplicationDate } from "@/config/application-status";
import { SwitchAccountButton } from "../auth/switch-account-button";
import { useJourneyNow } from "../applications/use-journey-now";
import { OfferStatusBadge } from "./offer-status-badge";
import { OfferResponseDialog } from "./offer-response-dialog";
import "../applications/candidate-journey.css";

export function CandidateOfferDetail({ id }: { id: string }) {
  const allowed = useAuthGuard({ roles: ["CANDIDATE"] });
  const user = useAuthStore(s => s.user), token = useAuthStore(s => s.token);
  return allowed ? <OfferDetail key={`${user?.id}:${token}:${id}`} id={id} /> : null;
}

function OfferDetail({ id }: { id: string }) {
  const user = useAuthStore(s => s.user), token = useAuthStore(s => s.token);
  const key = ["candidate-offer", user?.id, token, id];
  const client = useQueryClient();
  const query = useQuery({ queryKey: key, queryFn: ({ signal }) => offerService.detail(id, signal), staleTime: 0, refetchOnMount: "always" });
  const inaccessible = query.error instanceof ApiError && [403, 404].includes(query.error.status);
  const offer = inaccessible ? undefined : query.data;
  const context = useQuery({ queryKey: ["candidate-application", user?.id, token, offer?.applicationId], queryFn: () => applicationService.detail(offer!.applicationId), enabled: Boolean(offer?.applicationId), retry: false });
  const now = useJourneyNow(offer?.responseDeadline);
  const expiredReloaded = useRef<string | null>(null);
  const expired = Boolean(offer && new Date(offer.responseDeadline).getTime() <= now);
  const { refetch } = query;
  useEffect(() => {
    if (offer?.status === "SENT" && expired && expiredReloaded.current !== offer.id) {
      expiredReloaded.current = offer.id; void refetch();
    }
  }, [offer?.id, offer?.status, expired, refetch]);
  async function update(value: Offer) {
    // A GET started before a response committed must not replace its authoritative result.
    await client.cancelQueries({ queryKey: key, exact: true });
    client.setQueryData(key, value);
    void client.invalidateQueries({ queryKey: ["candidate-offers"] });
  }
  // Reconciliation is a bounded read, not an online-paused query/retry loop.
  async function refresh() { return offerService.detail(id); }
  const minutes = offer ? Math.max(0, Math.ceil((new Date(offer.responseDeadline).getTime() - now) / 60000)) : 0;
  return <div className="journey">
    <Link href={offer?.applicationId ? `/candidate/applications/${encodeURIComponent(offer.applicationId)}` : "/candidate/applications"} className="journey-back"><ArrowLeft aria-hidden="true" />{offer?.applicationId ? "Quay lại hồ sơ ứng tuyển" : "Ứng tuyển của tôi"}</Link>
    {query.isPending && <p role="status" aria-busy="true">Đang tải offer…</p>}
    {query.isError && <div role="alert" className="journey-error mb-5"><p>{inaccessible ? "Không tìm thấy offer hoặc tài khoản hiện tại không có quyền xem." : getErrorMessage(query.error)}</p><div className="journey-actions mt-3"><Button variant="outline" onClick={() => refetch()}>Thử lại</Button>{inaccessible && <SwitchAccountButton />}</div></div>}
    {offer && <div className="journey-stack"><section className="journey-card"><div className="journey-card-top"><div><div className="journey-eyebrow">Thư mời nhận việc</div><h1>{offer.position}</h1>{context.data?.companyName && !context.isError && <p className="journey-company"><Building2 aria-hidden="true" />{context.data.companyName}</p>}{offer.applicationId && <Link href={`/candidate/applications/${encodeURIComponent(offer.applicationId)}`} className="journey-link mt-2">Xem hồ sơ ứng tuyển →</Link>}</div><OfferStatusBadge offer={offer} now={now} /></div></section>
      <div className="journey-detail-grid"><div className="journey-stack"><section className="journey-card"><h2>Thông tin offer</h2><dl className="journey-metadata mt-6"><div><dt>Lương gross/tháng</dt><dd><span className="journey-salary">{formatOfferSalary(offer.salary)}</span></dd></div><div><dt>Ngày bắt đầu</dt><dd>{new Intl.DateTimeFormat("vi-VN", { dateStyle: "long", timeZone: "Asia/Ho_Chi_Minh" }).format(new Date(`${offer.startDate}T00:00:00+07:00`))}</dd></div><div><dt>Địa điểm làm việc</dt><dd>{offer.workLocation}</dd></div><div><dt>Hạn trả lời · Giờ Việt Nam</dt><dd>{formatApplicationDate(offer.responseDeadline)}</dd></div></dl></section>
        {offer.note && <section className="journey-card"><h2>Nội dung thư mời</h2><p className="journey-note mt-4">{offer.note}</p></section>}
      </div><aside className="journey-aside"><section className="journey-card"><OfferResponseDialog offer={offer} now={now} disabled={query.isError || query.isFetching} onUpdated={update} refresh={refresh} />{offer.status === "SENT" && <p className={`mt-4 ${!expired && minutes <= 48 * 60 ? "journey-warning" : "journey-muted journey-small"}`} role="timer">{expired ? "Đã hết hạn trả lời" : `Còn ${Math.floor(minutes / 1440)} ngày ${Math.floor(minutes % 1440 / 60)} giờ ${minutes % 60} phút để phản hồi.`}</p>}</section></aside></div>
    </div>}
  </div>;
}
