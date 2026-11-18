import type { JobStatus } from "@/interface/job"
import { STATUS_CLASS, STATUS_LABEL } from "@/app/components/job/job-labels"

export function JobStatusBadge({ status }: { status: JobStatus }) {
  return <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_CLASS[status]}`}>{STATUS_LABEL[status]}</span>
}
