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
  visibility: "PRIVATE" | "PENDING" | "PUBLIC" | "REJECTED";
  consentAt: string | null;
  consentWithdrawnAt: string | null;
  updatedAt: string;
}

export interface ProfileReview {
  id: number;
  userId: string;
  action: "SUBMIT" | "APPROVE" | "REJECT" | "WITHDRAW";
  reason: string | null;
  actorId: string;
  createdAt: string;
}

export type UpdateCandidateProfile = Pick<CandidateProfile,
  "fullName" | "email" | "dob" | "gender" | "phone" | "address" | "headline" | "summary">;
