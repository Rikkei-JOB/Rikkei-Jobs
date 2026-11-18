export type OfferStatus = "SENT" | "ACCEPTED" | "DECLINED" | "EXPIRED";

export interface Offer {
  id: string;
  applicationId: string;
  position: string;
  salary: number;
  startDate: string;
  workLocation: string;
  note: string | null;
  responseDeadline: string;
  status: OfferStatus;
  respondedAt: string | null;
  declineReason: string | null;
  createdAt: string;
}

export interface OfferPage { items: Offer[]; total: number; page: number; size: number }

export interface CreateOfferPayload {
  position: string;
  salary: number;
  startDate: string;
  workLocation: string;
  responseDeadline: string;
  note: string | null;
}

export interface EmploymentRecord {
  id: string;
  applicationId: string;
  userId: string;
  companyId: string;
  jobId: string;
  companyName: string;
  position: string;
  startDate: string;
  confirmedAt: string | null;
}

export const formatOfferSalary = (salary: number) => `${new Intl.NumberFormat("vi-VN").format(salary)} VND/tháng (gross)`;

export const offerStatusLabel: Record<OfferStatus, string> = {
  SENT: "Đã gửi",
  ACCEPTED: "Đã nhận",
  DECLINED: "Đã từ chối",
  EXPIRED: "Đã hết hạn",
};
