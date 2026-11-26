import { APPLICATION_STEPS, APPLICATION_STATUS } from "@/config/application-status";
import type { ApplicationHistoryItem, ApplicationStatus } from "@/interface/application";

export function ApplicationProgress({ status, history }: { status: ApplicationStatus; history: ApplicationHistoryItem[] }) {
  const terminal = status === "REJECTED" || status === "WITHDRAWN";
  const lastStep = terminal ? history.findLast(item => item.toStatus === status)?.fromStatus ?? "SUBMITTED" : status;
  const current = APPLICATION_STEPS.indexOf(lastStep);
  return <section aria-label="Tiến trình ứng tuyển" className="journey-card">
    <h2>Tiến trình ứng tuyển</h2>
    <p className="journey-muted mt-2">{terminal ? `Đơn đã kết thúc: ${APPLICATION_STATUS[status].label}.` : status === "HIRED" ? "Đã hoàn tất tiến trình hồ sơ." : status === "SUBMITTED" ? "Đang chờ nhà tuyển dụng xem xét hồ sơ." : `Hồ sơ đang ở giai đoạn ${APPLICATION_STATUS[status].label.toLocaleLowerCase("vi-VN")}.`}</p>
    <ol className="journey-steps">
      {APPLICATION_STEPS.map((step, index) => <li key={step} aria-current={!terminal && status !== "HIRED" && index === current ? "step" : undefined} data-reached={index <= current} className="journey-step">
        <span>{index + 1}</span>
        <div>{APPLICATION_STATUS[step].label}</div>
      </li>)}
    </ol>
  </section>;
}
