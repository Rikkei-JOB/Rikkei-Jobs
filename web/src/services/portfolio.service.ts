import { api } from "@/services/api";
import type { PortfolioMedia, PortfolioPayload, PortfolioProject } from "@/interface/portfolio";

const MEDIA_URL = /^\/api\/v1\/profiles\/[0-9a-f-]{36}\/portfolio\/media\/[0-9a-f-]{36}$/i;

export const portfolioService = {
  mine: async () => (await api.get<PortfolioProject[]>("/profiles/me/portfolio")).data,
  ofUser: async (userId: string) => (await api.get<PortfolioProject[]>(`/profiles/${userId}/portfolio`)).data,
  save: async (id: string | null, body: PortfolioPayload) => id
    ? (await api.put<PortfolioProject>(`/profiles/me/portfolio/${id}`, body)).data
    : (await api.post<PortfolioProject>("/profiles/me/portfolio", body)).data,
  remove: async (id: string) => { await api.delete(`/profiles/me/portfolio/${id}`); },
  reorder: async (ids: string[]) => (await api.put<PortfolioProject[]>("/profiles/me/portfolio/order", { ids })).data,
  addImage: async (projectId: string, file: File) => {
    const form = new FormData(); form.append("file", file);
    return (await api.post<PortfolioMedia>(`/profiles/me/portfolio/${projectId}/media`, form,
      { headers: { "Content-Type": "multipart/form-data" } })).data;
  },
  removeImage: async (projectId: string, mediaId: string) => { await api.delete(`/profiles/me/portfolio/${projectId}/media/${mediaId}`); },
  readImage: async (url: string): Promise<Blob> => {
    if (!MEDIA_URL.test(url)) throw new Error("Đường dẫn ảnh không hợp lệ");
    return (await api.get<Blob>(url.slice("/api/v1".length), { responseType: "blob" })).data;
  },
};
