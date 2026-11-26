import Link from "next/link";
import { ArrowUpRight, Building2, CalendarDays } from "lucide-react";
import { formatApplicationDate } from "@/config/application-status";
import type { ApplicationSummary } from "@/interface/application";
import { ApplicationStatusBadge } from "./application-status-badge";

export function ApplicationCard({ application: a, returnTo }: { application: ApplicationSummary; returnTo?: string }) {
  const href = `/candidate/applications/${a.id}${returnTo ? `?returnTo=${encodeURIComponent(returnTo)}` : ""}`;
  return <article className="journey-card">
    <div className="journey-card-top">
      <div className="min-w-0">
        <Link href={href} className="journey-card-title inline-flex items-center gap-2">{a.jobTitle}<ArrowUpRight className="h-4 w-4 shrink-0" /></Link>
        <p className="journey-company"><Building2 aria-hidden="true" />{a.companyName}</p>
      </div>
      <ApplicationStatusBadge status={a.status} />
    </div>
    <div className="journey-card-footer"><p className="journey-small journey-muted flex items-center gap-2"><CalendarDays className="h-4 w-4" aria-hidden="true" />Ngày nộp: {formatApplicationDate(a.createdAt)}</p><Link href={href} className="journey-link">Xem tiến trình <ArrowUpRight aria-hidden="true" /></Link></div>
  </article>;
}
