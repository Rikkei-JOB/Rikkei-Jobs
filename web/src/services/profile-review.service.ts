import { api } from "@/services/api";
import type { PageResult } from "@/interface/company";
import type { CandidateProfile } from "@/interface/candidate-profile";
import type { SkillProfileItem } from "@/interface/skill";

type ReviewRow = Pick<CandidateProfile, "userId" | "fullName" | "classCode" | "courseName" | "consentAt" | "visibility"> & {
  rejectCount: number;
};
export type ReviewPreview = Pick<CandidateProfile, "userId" | "fullName" | "classCode" | "courseName" | "headline" | "summary"> & {
  skills: SkillProfileItem[];
};
export type ReviewEdit =
  | { field: "headline" | "summary"; value: string; reason: string }
  | { field: "selfSkills"; value: { skillId: string; selfScore: number }[]; reason: string };

export type ReviewSearch = Record<string, string | string[] | undefined>;
export function queueFilters(search: ReviewSearch) {
  const value = (name: string) => typeof search[name] === "string" ? search[name].trim() : "";
  const page = Number(value("page"));
  return {
    q: value("q"), classCode: value("classCode"), courseName: value("courseName"),
    page: Number.isSafeInteger(page) && page > 0 ? page : 1,
  };
}
export type QueueFilters = ReturnType<typeof queueFilters>;

export function reviewHref(filters: QueueFilters, userId?: string, index = 0) {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(filters)) {
    if (value && !(key === "page" && value === 1)) params.set(key, String(value));
  }
  if (userId && index > 0) params.set("index", String(index));
  const path = "/staff/profile-reviews" + (userId ? "/" + encodeURIComponent(userId) : "");
  return params.size ? path + "?" + params.toString() : path;
}

const profilePath = (id: string) => "/staff/profiles/" + encodeURIComponent(id);
export const profileReviewService = {
  async queue(filters: QueueFilters, signal?: AbortSignal) {
    return (await api.get<PageResult<ReviewRow>>("/staff/profile-reviews", {
      params: { status: "PENDING", ...filters }, signal,
    })).data;
  },
  async preview(id: string, signal?: AbortSignal) {
    return (await api.get<ReviewPreview>(profilePath(id) + "/preview", { signal })).data;
  },
  async edit(id: string, change: ReviewEdit) {
    await api.put(profilePath(id) + "/self-declared", change);
  },
  async decide(id: string, action: "approve" | "reject", reason: string) {
    await api.post(profilePath(id) + "/" + action, action === "reject" ? { reason } : undefined);
  },
};
