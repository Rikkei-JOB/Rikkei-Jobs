/** HS-04: CV dựng từ hồ sơ. Khớp CvDtos ở profile-service. */
export type CvSectionType = "SUMMARY" | "SKILLS" | "LANGUAGES" | "INTERNSHIP" | "PROJECTS" | "EDUCATION" | "CERTIFICATES";

export interface CvSummary {
  id: string; name: string; targetPosition: string | null; template: string; publicUrl: string;
  createdAt: string; updatedAt: string;
}

export interface CvItem {
  id: string; title: string; subtitle: string | null; period: string | null; detail: string | null;
  /** null: mục không có khái niệm xác thực (học vấn, chứng chỉ). */
  verified: boolean | null; selected: boolean;
}

export interface CvSection {
  section: CvSectionType; label: string; included: boolean;
  /** true: CV tự lấy mọi mục, kể cả mục thêm vào hồ sơ sau này. */
  allItems: boolean; text: string | null; items: CvItem[];
}

export interface CvDetail extends CvSummary { sections: CvSection[] }

export interface CvPayload { name: string; targetPosition?: string | null }

export interface CvSectionPayload { section: CvSectionType; included: boolean; itemIds: string[] | null }

export interface PublicProfile {
  visible: boolean; fullName: string | null; headline: string | null; program: string | null; courseName: string | null;
  skills: { name: string; group: string; level: string | null; verifiedScore: number }[];
  internships: { name: string; role: string | null; techStack: string[]; period: string | null; description: string | null }[];
}
