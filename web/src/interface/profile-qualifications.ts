export interface Education {
  id: string;
  school: string;
  major: string | null;
  fromDate: string;
  toDate: string | null;
  gpa: number | null;
  note: string | null;
}
export type EducationPayload = Omit<Education, "id">;

export interface Certificate {
  id: string;
  name: string;
  issuer: string;
  issuedAt: string;
  expiresAt: string | null;
  fileUrl: string | null;
  fileType: string | null;
}
export type CertificatePayload = Omit<Certificate, "id" | "fileType">;

/** Trình độ theo khung CEFR, thấp đến cao. */
export const CEFR_LEVELS = ["A1", "A2", "B1", "B2", "C1", "C2"] as const;
export type CefrLevel = typeof CEFR_LEVELS[number];
export interface Language {
  id: string;
  name: string;
  level: CefrLevel;
  note: string | null;
}
export type LanguagePayload = Omit<Language, "id">;
