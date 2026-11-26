import { api } from "@/services/api";
import type { EmploymentRecord, Offer, OfferPage } from "@/interface/offer";

const root = "/recruitment/me";
const offerPath = (id: string) => `${root}/offers/${encodeURIComponent(id)}`;

export const offerService = {
  async list(params: { page?: number; size?: number } = {}) {
    return (await api.get<OfferPage>(`${root}/offers`, { params })).data;
  },
  async detail(id: string, signal?: AbortSignal) { return (await api.get<Offer>(offerPath(id), { signal })).data; },
  async accept(id: string) { return (await api.post<Offer>(`${offerPath(id)}/accept`)).data; },
  async decline(id: string, reason: string) {
    return (await api.post<Offer>(`${offerPath(id)}/decline`, { reason })).data;
  },
  async employmentRecords() { return (await api.get<EmploymentRecord[]>(`${root}/employment-records`)).data; },
};
