import { api } from "@/services/api";
import type { CvDetail, CvPayload, CvSectionPayload, CvSummary, PublicProfile } from "@/interface/cv";

export const cvService = {
  list: async () => (await api.get<CvSummary[]>("/profile/cvs")).data,
  get: async (id: string) => (await api.get<CvDetail>(`/profile/cvs/${id}`)).data,
  create: async (body: CvPayload) => (await api.post<CvDetail>("/profile/cvs", body)).data,
  update: async (id: string, body: CvPayload) => (await api.put<CvDetail>(`/profile/cvs/${id}`, body)).data,
  remove: async (id: string) => { await api.delete(`/profile/cvs/${id}`); },
  saveSections: async (id: string, sections: CvSectionPayload[]) =>
    (await api.put<CvDetail>(`/profile/cvs/${id}/sections`, { sections })).data,
  pdf: async (id: string, inline = false) =>
    (await api.get<Blob>(`/profile/cvs/${id}/pdf`, { params: inline ? { inline: true } : undefined, responseType: "blob", timeout: 30000 })).data,
  /** Dựng PDF theo bản nháp chưa lưu để xem trước; không ghi DB. */
  preview: async (id: string, sections: CvSectionPayload[]) =>
    (await api.post<Blob>(`/profile/cvs/${id}/preview`, { sections }, { responseType: "blob", timeout: 30000 })).data,
  publicProfile: async (token: string) => (await api.get<PublicProfile>(`/profile/public/${encodeURIComponent(token)}`)).data,
};
