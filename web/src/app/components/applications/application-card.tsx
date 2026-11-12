import Link from "next/link";
import { ArrowUpRight, Building2, CalendarDays } from "lucide-react";
import { formatApplicationDate } from "@/config/application-status";
import type { ApplicationSummary } from "@/interface/application";
import { ApplicationStatusBadge } from "./application-status-badge";

export function ApplicationCard({ application: a }: { application: ApplicationSummary }) {
  return <article className="rounded-xl border bg-background p-5 transition-colors hover:border-primary/40 sm:p-6">
    <div className="flex flex-wrap items-start justify-between gap-3">
      <div className="min-w-0">
        <Link href={`/candidate/applications/${a.id}`} className="inline-flex items-center gap-2 text-lg font-semibold hover:text-primary">{a.jobTitle}<ArrowUpRight className="h-4 w-4 shrink-0" /></Link>
        <p className="mt-2 flex items-center gap-2 text-sm text-muted-foreground"><Building2 className="h-4 w-4 shrink-0" />{a.companyName}</p>
      </div>
      <ApplicationStatusBadge status={a.status} />
    </div>
    <p className="mt-5 flex items-center gap-2 text-xs text-muted-foreground"><CalendarDays className="h-4 w-4" />Ngày nộp: {formatApplicationDate(a.createdAt)}</p>
  </article>;
}
