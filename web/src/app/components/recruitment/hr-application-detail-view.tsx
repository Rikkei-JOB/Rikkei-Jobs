"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { ArrowLeft, Download, BriefcaseBusiness } from "lucide-react";
import { CandidateContactPanel } from "@/app/components/contact/candidate-contact-panel";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/app/components/ui/tabs";
import { Button } from "@/app/components/ui/button";
import { ApplicationStatusBadge } from "@/app/components/applications/application-status-badge";
import { formatApplicationDate } from "@/config/application-status";
import type { HrApplicationDetail } from "@/interface/hr-application";
import { hrListReturnUrl } from "./application-navigation";

const tabs = ["profile", "interviews", "offer", "history"];
export function HrApplicationDetailView({ application, errors, matchCard, transitionPanel, rounds, offer, history, downloading, onDownloadCv }: {
  application: HrApplicationDetail; errors: ReactNode; matchCard: ReactNode; transitionPanel: ReactNode;
  rounds: ReactNode; offer: ReactNode; history: ReactNode; downloading: boolean; onDownloadCv(): void;
}) {
  const search = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const tab = tabs.includes(search.get("tab") ?? "") ? search.get("tab")! : "profile";
  function changeTab(value: unknown) {
    const next = new URLSearchParams(search.toString());
    if (value === "profile") next.delete("tab"); else next.set("tab", String(value));
    router.replace(`${pathname}${next.size ? `?${next}` : ""}`, { scroll: false });
  }
  return <div className="hr-page">
    <Link href={hrListReturnUrl(search.get("returnTo"))} className="inline-flex min-h-11 w-fit items-center gap-2 text-sm text-muted-foreground hover:text-primary"><ArrowLeft size={16} aria-hidden="true" />Danh sách đơn ứng tuyển</Link>
    {errors}
    <header className="hr-surface hr-page-heading"><div className="min-w-0 break-words"><p className="hr-eyebrow">HỒ SƠ ỨNG TUYỂN</p><h1>{application.candidate.fullName}</h1>
      <p className="mt-2 flex items-start gap-2 font-medium"><BriefcaseBusiness size={18} className="mt-0.5 shrink-0" aria-hidden="true" />{application.jobTitle}</p>
      <CandidateContactPanel userId={application.candidate.userId} jobId={application.jobId} />
      <p className="mt-3 text-xs text-muted-foreground">{application.companyName} · Nộp ngày {formatApplicationDate(application.createdAt)}</p>
    </div><ApplicationStatusBadge status={application.status} /></header>
    <div className="hr-detail-grid">
      <div className="hr-detail-content"><Tabs value={tab} onValueChange={changeTab} className="hr-detail-tabs">
        <TabsList aria-label="Nội dung hồ sơ"><TabsTrigger value="profile">Hồ sơ</TabsTrigger><TabsTrigger value="interviews">Phỏng vấn{application.rounds.length > 0 ? ` (${application.rounds.length})` : ""}</TabsTrigger><TabsTrigger value="offer">Offer{application.offer ? " (1)" : ""}</TabsTrigger><TabsTrigger value="history">Lịch sử{application.history.length > 0 ? ` (${application.history.length})` : ""}</TabsTrigger></TabsList>
        <TabsContent value="profile" keepMounted><section className="hr-surface"><h2 className="text-xl font-semibold">CV và thư giới thiệu</h2><p className="hr-description">CV được lưu tại thời điểm ứng viên nộp đơn cho vị trí này.</p><Button variant="outline" className="mt-4 gap-2" disabled={downloading} onClick={onDownloadCv}><Download size={18} aria-hidden="true" />{downloading ? "Đang tải…" : "Tải CV đã nộp"}</Button>
          <div className="mt-6 border-t pt-6"><h3 className="font-semibold">Thư giới thiệu</h3><p className="mt-3 whitespace-pre-wrap break-words">{application.coverLetter || "Ứng viên chưa gửi thư giới thiệu."}</p></div>
        </section></TabsContent>
        <TabsContent value="interviews" keepMounted>{rounds}</TabsContent>
        <TabsContent value="offer" keepMounted>{offer}</TabsContent>
        <TabsContent value="history" keepMounted>{history}</TabsContent>
      </Tabs></div>
      <aside className="hr-detail-aside" aria-label="Đánh giá và xử lý hồ sơ">{transitionPanel}{matchCard}</aside>
    </div>
  </div>;
}
