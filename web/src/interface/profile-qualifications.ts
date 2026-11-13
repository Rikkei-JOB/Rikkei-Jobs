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
