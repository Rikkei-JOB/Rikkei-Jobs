import type { FieldErrors, FieldValues, Resolver } from "react-hook-form";
import type { ZodSchema } from "zod";

/** Cầu nối react-hook-form <-> zod (dự án chưa dùng @hookform/resolvers). */
export function zodResolver<TFieldValues extends FieldValues>(schema: ZodSchema<TFieldValues>): Resolver<TFieldValues> {
  return async (values) => {
    const result = schema.safeParse(values);
    if (result.success) {
      return { values: result.data, errors: {} };
    }
    const errors: Record<string, { type: string; message: string }> = {};
    for (const issue of result.error.issues) {
      const key = issue.path.join(".");
      if (!errors[key]) {
        errors[key] = { type: issue.code, message: issue.message };
      }
    }
    return { values: {}, errors: errors as FieldErrors<TFieldValues> };
  };
}
