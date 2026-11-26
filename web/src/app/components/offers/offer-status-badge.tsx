import type { Offer } from "@/interface/offer";
import { offerStatusLabel } from "@/interface/offer";

export function OfferStatusBadge({ offer, now }: { offer: Offer; now: number }) {
  const overdue = offer.status === "SENT" && new Date(offer.responseDeadline).getTime() <= now;
  const label = overdue ? "Đã quá hạn phản hồi" : offer.status === "SENT" ? "Chờ phản hồi" : offerStatusLabel[offer.status];
  const tone = overdue || offer.status === "EXPIRED" ? "muted" : offer.status === "ACCEPTED" ? "success" : offer.status === "DECLINED" ? "danger" : "warning";
  return <span className="journey-badge" data-tone={tone}>{label}</span>;
}
