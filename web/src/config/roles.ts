import type { Role } from "@/interface/auth";

/** Trang chủ của từng vai trò. Các task khác thay trang placeholder bằng trang thật ở đúng đường dẫn này. */
export const ROLE_HOME: Record<Role, string> = {
  CANDIDATE: "/candidate",
  HR: "/hr",
  MENTOR: "/mentor",
  STAFF: "/staff",
  ADMIN: "/admin",
};

export const ROLE_LABEL: Record<Role, string> = {
  CANDIDATE: "Ứng viên",
  HR: "Doanh nghiệp (HR)",
  MENTOR: "Mentor",
  STAFF: "Cán bộ tuyển dụng",
  ADMIN: "Quản trị",
};

export function roleFromSlug(slug: string): Role | null {
  const entry = (Object.entries(ROLE_HOME) as [Role, string][]).find(([, path]) => path === `/${slug}`);
  return entry ? entry[0] : null;
}
