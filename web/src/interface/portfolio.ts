export interface PortfolioMedia {
  id: string;
  type: "IMAGE" | "EVIDENCE";
  /** IMAGE: /api/v1/profiles/{userId}/portfolio/media/{id} (đọc qua API có JWT); EVIDENCE: link minh chứng. */
  url: string;
  caption: string | null;
}

export interface PortfolioProject {
  id: string;
  source: "IOC" | "PERSONAL";
  /** true = dự án thực tập IOC đã xác thực, chỉ sửa được role, description, visible và không xoá được. */
  verified: boolean;
  internshipId: string | null;
  name: string;
  role: string | null;
  description: string | null;
  techStack: string[];
  repoUrl: string | null;
  demoUrl: string | null;
  startDate: string | null;
  endDate: string | null;
  visible: boolean;
  sortOrder: number;
  media: PortfolioMedia[];
}

export type PortfolioPayload = Pick<PortfolioProject,
  "name" | "role" | "description" | "techStack" | "repoUrl" | "demoUrl" | "startDate" | "endDate" | "visible">;
