import { api } from "@/services/api";
import type { ContactAccess, ContactView, ContactRequest, ContactRequestStatus, ContactLog, ContactPage, ContactLogFilters } from "@/interface/contact";
const candidate = (id: string) => `/candidates/${encodeURIComponent(id)}`;
export const contactService = {
  status: async (id: string, signal?: AbortSignal) => (await api.get<ContactAccess>(`${candidate(id)}/contact-access`, { signal })).data,
  request: async (id: string, payload: { jobId: string; reason: string }) => (await api.post<ContactRequest>(`${candidate(id)}/contact-requests`, payload)).data,
  reveal: async (id: string) => (await api.get<ContactView>(`${candidate(id)}/contact`)).data,
  listRequests: async (params: { status?: ContactRequestStatus; page?: number }, signal?: AbortSignal) =>
    (await api.get<ContactPage<ContactRequest>>("/staff/contact-requests", { params, signal })).data,
  approve: async (id: string) => (await api.post<ContactRequest>(`/staff/contact-requests/${encodeURIComponent(id)}/approve`)).data,
  reject: async (id: string, reason: string) => (await api.post<ContactRequest>(`/staff/contact-requests/${encodeURIComponent(id)}/reject`, { reason })).data,
  staffLogs: async (params: ContactLogFilters, signal?: AbortSignal) => (await api.get<ContactPage<ContactLog>>("/staff/contact-access-logs", { params, signal })).data,
  myLogs: async (page = 1, signal?: AbortSignal) => (await api.get<ContactPage<ContactLog>>("/me/contact-access-logs", { params: { page }, signal })).data,
};
