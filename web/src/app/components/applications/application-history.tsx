import { APPLICATION_STATUS, formatApplicationDate } from "@/config/application-status";
import type { ApplicationHistoryItem } from "@/interface/application";

export function ApplicationHistory({ items }: { items: ApplicationHistoryItem[] }) {
  return <section className="rounded-xl border bg-background p-5 sm:p-6">
    <h2 className="mb-5 font-semibold">Lịch sử ứng tuyển</h2>
    <ol className="space-y-5">{items.map(item => <li key={item.id} className="border-l-2 border-primary/25 pl-4">
      <p className="text-sm font-medium">{item.fromStatus ? `${APPLICATION_STATUS[item.fromStatus].label} → ` : ""}{APPLICATION_STATUS[item.toStatus].label}</p>
      <time dateTime={item.createdAt} className="mt-1 block text-xs text-muted-foreground">{formatApplicationDate(item.createdAt)}</time>
      {item.note && <p className="mt-2 whitespace-pre-wrap break-words text-sm">{item.note}</p>}
    </li>)}</ol>
  </section>;
}
