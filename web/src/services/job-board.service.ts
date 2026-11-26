import { api } from "@/services/api";
import type { JobLevel, JobSkill, JobType, WorkMode } from "@/interface/job";
import type { Skill } from "@/interface/skill";

/** Công ty kèm tin công khai (PublicCompanySummary của company-service). */
export interface BoardCompany {
  id: string;
  name: string;
  logoUrl: string | null;
  industry: string | null;
  size: string | null;
  city: string | null;
}

/** GET /public/companies: công ty ACTIVE kèm số tin đang tuyển. */
export interface BoardCompanyItem extends BoardCompany {
  openJobs: number;
}

/** GET /public/companies/{id}. */
export interface BoardCompanyDetail extends BoardCompanyItem {
  address: string | null;
  website: string | null;
  description: string | null;
}

/** Tin công khai; `skills` đã ghép tên từ danh mục GET /skills (backend chỉ trả id). */
export interface BoardJob {
  id: string;
  title: string;
  level: JobLevel | null;
  type: JobType | null;
  workMode: WorkMode | null;
  city: string | null;
  salaryMin: number | null;
  salaryMax: number | null;
  deadline: string | null;
  createdAt: string;
  skills: string[];
  company: BoardCompany;
}

/** GET /public/jobs/{id}: thêm nội dung JD. */
export interface BoardJobDetail extends BoardJob {
  position: string | null;
  quantity: number | null;
  address: string | null;
  description: string | null;
  requirements: string | null;
  benefits: string | null;
}

interface ApiJob extends Omit<BoardJob, "skills"> { skills: JobSkill[] }
interface ApiJobDetail extends Omit<BoardJobDetail, "skills"> { skills: JobSkill[] }
interface Page<T> { items: T[]; total: number; page: number; size: number }

export type SalaryRange = "all" | "lt15" | "15to30" | "gt30";
export type JobSort = "newest" | "salary";

export interface BoardFilters {
  q: string;
  city: string;
  level: JobLevel | "all";
  workMode: WorkMode | "all";
  type: JobType | "all";
  salary: SalaryRange;
  sort: JobSort;
}

export const EMPTY_FILTERS: BoardFilters = { q: "", city: "all", level: "all", workMode: "all", type: "all", salary: "all", sort: "newest" };

/** Thành phố gợi ý; backend khớp một phần nên "Hồ Chí Minh" khớp cả "TP. Hồ Chí Minh". */
export const CITIES = ["Hà Nội", "Hồ Chí Minh", "Đà Nẵng"] as const;

const MILLION = 1_000_000;
const SALARY_PARAMS: Record<Exclude<SalaryRange, "all">, { salaryFrom?: number; salaryTo?: number }> = {
  lt15: { salaryTo: 15 * MILLION },
  "15to30": { salaryFrom: 15 * MILLION, salaryTo: 30 * MILLION },
  gt30: { salaryFrom: 30 * MILLION },
};

/** Bỏ dấu tiếng Việt để so khớp từ khoá với tên kỹ năng. */
export function normalizeText(text: string) {
  return text.normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/[đĐ]/g, "d").toLowerCase();
}

/** Tên kỹ năng nằm ở profile-service: đổi từ khoá thành id kỹ năng khớp để backend tìm cả theo kỹ năng. */
function matchingSkillIds(q: string, catalog: Skill[]) {
  const needle = normalizeText(q.trim()).replace(/[^a-z0-9+#]/g, "");
  if (needle.length < 2) return [];
  return catalog
    .filter(skill => [skill.name, skill.code].some(text => normalizeText(text).replace(/[^a-z0-9+#]/g, "").includes(needle)))
    .map(skill => skill.id);
}

function withSkillNames<T extends { skills: JobSkill[] }>(job: T, catalog: Skill[]): Omit<T, "skills"> & { skills: string[] } {
  const names = new Map(catalog.map(skill => [skill.id, skill.name]));
  return { ...job, skills: job.skills.map(skill => names.get(skill.skillId)).filter((name): name is string => Boolean(name)) };
}

export const jobBoardService = {
  /** Danh mục kỹ năng (công khai); lỗi thì vẫn hiện tin, chỉ thiếu nhãn kỹ năng. */
  async skillCatalog(): Promise<Skill[]> {
    try { return (await api.get<Skill[]>("/skills")).data; } catch { return []; }
  },

  async search(filters: BoardFilters, page: number, size: number, catalog: Skill[], companyId?: string): Promise<Page<BoardJob>> {
    const skillIds = filters.q ? matchingSkillIds(filters.q, catalog) : [];
    const { data } = await api.get<Page<ApiJob>>("/public/jobs", {
      params: {
        q: filters.q.trim() || undefined,
        skillIds: skillIds.length ? skillIds.join(",") : undefined,
        city: filters.city === "all" ? undefined : filters.city,
        level: filters.level === "all" ? undefined : filters.level,
        workMode: filters.workMode === "all" ? undefined : filters.workMode,
        type: filters.type === "all" ? undefined : filters.type,
        ...(filters.salary === "all" ? {} : SALARY_PARAMS[filters.salary]),
        sort: filters.sort,
        companyId,
        page,
        size,
      },
    });
    return { ...data, items: data.items.map(job => withSkillNames(job, catalog)) };
  },

  async get(id: string, catalog: Skill[]): Promise<BoardJobDetail> {
    const { data } = await api.get<ApiJobDetail>(`/public/jobs/${encodeURIComponent(id)}`);
    return withSkillNames(data, catalog);
  },

  async companies(): Promise<BoardCompanyItem[]> {
    return (await api.get<BoardCompanyItem[]>("/public/companies")).data;
  },

  async company(id: string): Promise<BoardCompanyDetail> {
    return (await api.get<BoardCompanyDetail>(`/public/companies/${encodeURIComponent(id)}`)).data;
  },
};

export function formatSalary(job: Pick<BoardJob, "salaryMin" | "salaryMax">) {
  const toMillion = (value: number) => `${Math.round(value / MILLION).toLocaleString("vi-VN")}`;
  if (job.salaryMin && job.salaryMax) return `${toMillion(job.salaryMin)} - ${toMillion(job.salaryMax)} triệu`;
  if (job.salaryMax) return `Lên đến ${toMillion(job.salaryMax)} triệu`;
  if (job.salaryMin) return `Từ ${toMillion(job.salaryMin)} triệu`;
  return "Thoả thuận";
}

/** "Hôm nay", "3 ngày trước"... theo ngày đăng tin. */
export function postedAgo(iso: string, now = Date.now()) {
  const days = Math.floor((now - new Date(iso).getTime()) / 86_400_000);
  if (days <= 0) return "Hôm nay";
  if (days < 30) return `${days} ngày trước`;
  return `${Math.floor(days / 30)} tháng trước`;
}

export function isNewJob(iso: string, now = Date.now()) {
  return now - new Date(iso).getTime() < 3 * 86_400_000;
}

export function isHotJob(job: Pick<BoardJob, "salaryMax">) {
  return (job.salaryMax ?? 0) >= 35 * MILLION;
}
