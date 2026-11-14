export interface CandidateProfile {
  userId: string;
  fullName: string;
  email: string;
  dob: string | null;
  gender: "MALE" | "FEMALE" | "OTHER" | null;
  phone: string | null;
  address: string | null;
  avatarUrl: string | null;
  headline: string | null;
  summary: string | null;
  program: string;
  classCode: string;
  courseName: string;
  visibility: "PRIVATE";
  updatedAt: string;
}

export type UpdateCandidateProfile = Pick<CandidateProfile,
  "fullName" | "email" | "dob" | "gender" | "phone" | "address" | "headline" | "summary">;
