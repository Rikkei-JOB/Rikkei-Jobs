import { api } from "@/services/api";
import type { Certificate, CertificatePayload, Education, EducationPayload } from "@/interface/profile-qualifications";
import type { CandidateProfile } from "@/interface/candidate-profile";

export const qualificationService = {
  educations: async () => (await api.get<Education[]>("/profiles/me/educations")).data,
  saveEducation: async (id: string | null, body: EducationPayload) => id
    ? (await api.put<Education>(`/profiles/me/educations/${id}`, body)).data
    : (await api.post<Education>("/profiles/me/educations", body)).data,
  deleteEducation: async (id: string) => { await api.delete(`/profiles/me/educations/${id}`); },
  certificates: async () => (await api.get<Certificate[]>("/profiles/me/certificates")).data,
  saveCertificate: async (id: string | null, body: CertificatePayload) => id
    ? (await api.put<Certificate>(`/profiles/me/certificates/${id}`, body)).data
    : (await api.post<Certificate>("/profiles/me/certificates", body)).data,
  deleteCertificate: async (id: string) => { await api.delete(`/profiles/me/certificates/${id}`); },
  upload: async (file: File) => {
    const form = new FormData(); form.append("file", file);
    return (await api.post<{ url: string }>("/files", form, { headers: { "Content-Type": "multipart/form-data" } })).data.url;
  },
  readFile: async (url: string): Promise<Blob> => {
    if (!/^\/api\/v1\/files\/[0-9a-f-]{36}$/i.test(url)) throw new Error("Đường dẫn tệp không hợp lệ");
    return (await api.get<Blob>(url.slice("/api/v1".length), { responseType: "blob" })).data;
  },
  saveAvatar: async (fileUrl: string | null) => (await api.put<CandidateProfile>("/profiles/me/avatar", { fileUrl })).data,
};
