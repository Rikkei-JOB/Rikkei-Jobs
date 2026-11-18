import { z } from "zod";
import type { Job, JobLevel, JobPayload, JobType, WorkMode } from "@/interface/job";

export const JOB_LEVELS = ["INTERN", "FRESHER", "JUNIOR", "MIDDLE", "SENIOR"] as const satisfies readonly JobLevel[];
export const JOB_TYPES = ["FULLTIME", "PARTTIME", "INTERNSHIP"] as const satisfies readonly JobType[];
export const WORK_MODES = ["ONSITE", "REMOTE", "HYBRID"] as const satisfies readonly WorkMode[];

export const MAX_JOB_SKILLS = 20;
export const DEFAULT_MIN_SCORE = 40;

/** Lớn hơn mức này thì số JSON mất chính xác ở trình duyệt (backend lưu BIGINT). */
const MAX_INTEGER = Number.MAX_SAFE_INTEGER;

// Cùng câu chữ với JobRules ở backend.
const MSG_REQUIRED = "Không được để trống";
const MSG_DEADLINE = "Hạn nộp không được ở quá khứ";
const MSG_SALARY = "Lương tối đa phải lớn hơn hoặc bằng lương tối thiểu";

/** Hôm nay theo giờ Việt Nam, dạng yyyy-MM-dd (giống JobRules.today ở backend). */
export function todayInVietnam(now: Date = new Date()): string {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Ho_Chi_Minh",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(now);
}

const optionalText = (max: number, label: string) => z.string().trim().max(max, `${label} tối đa ${max} ký tự`);

/** Integer của Java (cột quantity). */
const MAX_JAVA_INT = 2_147_483_647;

/** Ô số nguyên dạng chuỗi: "" = chưa nhập. */
const optionalInteger = (min: number, message: string, max = MAX_INTEGER) =>
  z
    .string()
    .trim()
    .refine((v) => v === "" || /^-?\d+$/.test(v), "Vui lòng nhập số nguyên")
    .refine((v) => v === "" || Number(v) >= min, message)
    .refine((v) => v === "" || Number(v) <= max, "Giá trị quá lớn");

const jobSkillSchema = z.object({
  skillId: z.string().min(1),
  required: z.boolean(),
  minScore: z
    .number({ invalid_type_error: "Điểm tối thiểu từ 1 đến 100" })
    .int("Điểm tối thiểu từ 1 đến 100")
    .min(1, "Điểm tối thiểu từ 1 đến 100")
    .max(100, "Điểm tối thiểu từ 1 đến 100"),
});

/**
 * Kiểm tra mức lưu nháp (giống JobRequest + JobRules.saveErrors): chỉ Tiêu đề bắt buộc, các ô khác chỉ kiểm định dạng.
 * Đủ trường khi gửi duyệt kiểm bằng {@link submitIssues}.
 */
export const jobFormSchema = z
  .object({
    title: z.string().trim().min(1, "Vui lòng nhập tiêu đề").max(255, "Tiêu đề tối đa 255 ký tự"),
    position: optionalText(255, "Vị trí"),
    level: z.union([z.literal(""), z.enum(JOB_LEVELS)]),
    type: z.union([z.literal(""), z.enum(JOB_TYPES)]),
    salaryMin: optionalInteger(0, "Lương tối thiểu không được âm"),
    salaryMax: optionalInteger(0, "Lương tối đa không được âm"),
    workMode: z.union([z.literal(""), z.enum(WORK_MODES)]),
    city: optionalText(100, "Thành phố"),
    address: optionalText(255, "Địa chỉ"),
    quantity: optionalInteger(1, "Số lượng tuyển tối thiểu là 1", MAX_JAVA_INT),
    deadline: z
      .string()
      .refine((v) => v === "" || /^\d{4}-\d{2}-\d{2}$/.test(v), "Ngày không hợp lệ")
      .refine((v) => v === "" || v >= todayInVietnam(), MSG_DEADLINE),
    description: z.string(),
    requirements: z.string(),
    benefits: z.string(),
    skills: z
      .array(jobSkillSchema)
      .max(MAX_JOB_SKILLS, `Tối đa ${MAX_JOB_SKILLS} kỹ năng`)
      .refine((skills) => new Set(skills.map((s) => s.skillId)).size === skills.length, "Kỹ năng bị trùng"),
  })
  .superRefine((values, ctx) => {
    if (salaryInverted(values.salaryMin, values.salaryMax)) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["salaryMax"], message: MSG_SALARY });
    }
  });

export type JobFormValues = z.infer<typeof jobFormSchema>;
export type JobFormField = keyof JobFormValues;

export const EMPTY_JOB_FORM: JobFormValues = {
  title: "",
  position: "",
  level: "",
  type: "",
  salaryMin: "",
  salaryMax: "",
  workMode: "",
  city: "",
  address: "",
  quantity: "",
  deadline: "",
  description: "",
  requirements: "",
  benefits: "",
  skills: [],
};

function salaryInverted(min: string, max: string): boolean {
  return min.trim() !== "" && max.trim() !== "" && Number(min) > Number(max);
}

/**
 * Lỗi khi gửi duyệt, cùng danh sách trường và thứ tự với JobRules.submitErrors ở backend.
 * `today` dạng yyyy-MM-dd theo giờ Việt Nam (xem {@link todayInVietnam}).
 */
export function submitIssues(values: JobFormValues, today: string): Partial<Record<JobFormField, string>> {
  const issues: Partial<Record<JobFormField, string>> = {};
  const requireText = (field: JobFormField, value: string) => {
    if (value.trim() === "") issues[field] = MSG_REQUIRED;
  };
  requireText("title", values.title);
  requireText("position", values.position);
  requireText("level", values.level);
  requireText("type", values.type);
  requireText("quantity", values.quantity);
  requireText("city", values.city);
  requireText("workMode", values.workMode);
  requireText("deadline", values.deadline);
  requireText("description", values.description);
  if (values.deadline !== "" && values.deadline < today) {
    issues.deadline = MSG_DEADLINE;
  }
  if (salaryInverted(values.salaryMin, values.salaryMax)) {
    issues.salaryMax = MSG_SALARY;
  }
  if (!values.skills.some((s) => s.required)) {
    issues.skills = "Cần ít nhất một kỹ năng bắt buộc";
  }
  return issues;
}

const textOrNull = (value: string) => {
  const trimmed = value.trim();
  return trimmed === "" ? null : trimmed;
};
const numberOrNull = (value: string) => (value.trim() === "" ? null : Number(value.trim()));

/** Chuỗi rỗng (sau trim) -> null; luôn gửi `skills` để PUT thay cả danh sách kỹ năng. */
export function toJobPayload(values: JobFormValues): JobPayload {
  return {
    title: values.title.trim(),
    position: textOrNull(values.position),
    level: values.level === "" ? null : values.level,
    type: values.type === "" ? null : values.type,
    quantity: numberOrNull(values.quantity),
    city: textOrNull(values.city),
    address: textOrNull(values.address),
    workMode: values.workMode === "" ? null : values.workMode,
    salaryMin: numberOrNull(values.salaryMin),
    salaryMax: numberOrNull(values.salaryMax),
    description: textOrNull(values.description),
    requirements: textOrNull(values.requirements),
    benefits: textOrNull(values.benefits),
    deadline: values.deadline === "" ? null : values.deadline,
    skills: values.skills.map(({ skillId, required, minScore }) => ({ skillId, required, minScore })),
  };
}

const str = (value: string | number | null) => (value === null ? "" : String(value));

export function fromJob(job: Job): JobFormValues {
  return {
    title: job.title,
    position: str(job.position),
    level: job.level ?? "",
    type: job.type ?? "",
    salaryMin: str(job.salaryMin),
    salaryMax: str(job.salaryMax),
    workMode: job.workMode ?? "",
    city: str(job.city),
    address: str(job.address),
    quantity: str(job.quantity),
    deadline: job.deadline ?? "",
    description: str(job.description),
    requirements: str(job.requirements),
    benefits: str(job.benefits),
    skills: job.skills.map(({ skillId, required, minScore }) => ({ skillId, required, minScore })),
  };
}
