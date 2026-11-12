import { APPLICATION_STEPS, APPLICATION_STATUS } from "@/config/application-status";
import type { ApplicationHistoryItem, ApplicationStatus } from "@/interface/application";

export function ApplicationProgress({ status, history }: { status: ApplicationStatus; history: ApplicationHistoryItem[] }) {
  const terminal = status === "REJECTED" || status === "WITHDRAWN";
  const lastStep = terminal ? history.findLast(item => item.toStatus === status)?.fromStatus ?? "SUBMITTED" : status;
  const current = APPLICATION_STEPS.indexOf(lastStep);
  return <section aria-label="Tiến trình ứng tuyển" className="rounded-xl border bg-background p-5 sm:p-6">
    <h2 className="mb-5 font-semibold">Tiến trình ứng tuyển</h2>
    <ol className="grid grid-cols-5 gap-1 sm:gap-3">
      {APPLICATION_STEPS.map((step, index) => <li key={step} aria-current={!terminal && index === current ? "step" : undefined} className="min-w-0 text-center">
        <div className={`mx-auto mb-2 flex h-8 w-8 items-center justify-center rounded-full text-sm font-semibold ${index <= current ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground"}`}>{index + 1}</div>
        <span className={`text-[11px] sm:text-sm ${index <= current ? "font-medium" : "text-muted-foreground"}`}>{APPLICATION_STATUS[step].label}</span>
      </li>)}
    </ol>
    {terminal && <p className="mt-5 border-t pt-4 text-sm text-muted-foreground">Đơn đã kết thúc: {APPLICATION_STATUS[status].label}.</p>}
  </section>;
}
