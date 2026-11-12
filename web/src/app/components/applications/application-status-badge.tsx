import { APPLICATION_STATUS } from "@/config/application-status";
import type { ApplicationStatus } from "@/interface/application";

export function ApplicationStatusBadge({ status }: { status: ApplicationStatus }) {
  const appearance = APPLICATION_STATUS[status];
  return <span className={`inline-flex items-center rounded-full border px-3 py-1 text-xs font-medium ${appearance.className}`}>{appearance.label}</span>;
}
