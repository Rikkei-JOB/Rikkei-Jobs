import { z } from "zod";

/** Cùng quy tắc với backend: tối thiểu 8 ký tự, có ít nhất một chữ và một số. */
export const PASSWORD_MESSAGE = "Mật khẩu phải có ít nhất 8 ký tự, gồm cả chữ và số";

const newPassword = z
  .string()
  .min(1, "Vui lòng nhập mật khẩu mới")
  .regex(/^(?=.*\p{L})(?=.*\d).{8,}$/u, PASSWORD_MESSAGE);

const email = z.string().trim().min(1, "Vui lòng nhập email").email("Email không hợp lệ");

export const hrLoginSchema = z.object({
  email,
  password: z.string().min(1, "Vui lòng nhập mật khẩu"),
});
export type HrLoginValues = z.infer<typeof hrLoginSchema>;

export const forgotPasswordSchema = z.object({ email });
export type ForgotPasswordValues = z.infer<typeof forgotPasswordSchema>;

export const changePasswordSchema = z
  .object({
    oldPassword: z.string().min(1, "Vui lòng nhập mật khẩu hiện tại"),
    newPassword,
    confirmPassword: z.string().min(1, "Vui lòng nhập lại mật khẩu mới"),
  })
  .refine((v) => v.newPassword === v.confirmPassword, {
    message: "Mật khẩu nhập lại không khớp",
    path: ["confirmPassword"],
  })
  .refine((v) => v.newPassword !== v.oldPassword, {
    message: "Mật khẩu mới phải khác mật khẩu hiện tại",
    path: ["newPassword"],
  });
export type ChangePasswordValues = z.infer<typeof changePasswordSchema>;

export const resetPasswordSchema = z
  .object({
    newPassword,
    confirmPassword: z.string().min(1, "Vui lòng nhập lại mật khẩu mới"),
  })
  .refine((v) => v.newPassword === v.confirmPassword, {
    message: "Mật khẩu nhập lại không khớp",
    path: ["confirmPassword"],
  });
export type ResetPasswordValues = z.infer<typeof resetPasswordSchema>;
