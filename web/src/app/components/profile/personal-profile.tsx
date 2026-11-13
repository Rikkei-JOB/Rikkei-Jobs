"use client";

import * as React from "react";
import { LockKeyhole } from "lucide-react";
import { useForm } from "react-hook-form";
import { Alert } from "@/app/components/ui/alert";
import { Button } from "@/app/components/ui/button";
import { Card, CardContent, CardHeader } from "@/app/components/ui/card";
import { Input } from "@/app/components/ui/input";
import { FormField } from "@/app/components/auth/form-field";
import { zodResolver } from "@/helper/zod-resolver";
import { candidateProfileSchema, localToday, toProfilePayload, type CandidateProfileValues } from "@/schemas/candidate-profile.schema";
import { candidateProfileService } from "@/services/candidate-profile.service";
import { ApiError, getErrorMessage } from "@/services/api";
import type { CandidateProfile } from "@/interface/candidate-profile";

const CONTROL_CLASS = "w-full min-w-0 rounded-md border border-input bg-input/20 px-2 py-1 text-sm outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/30 disabled:opacity-50 aria-invalid:border-destructive md:text-xs/relaxed";
const FIELDS = ["fullName", "email", "dob", "gender", "phone", "address", "headline", "summary"] as const;

function toValues(profile: CandidateProfile): CandidateProfileValues {
  return {
    fullName: profile.fullName, email: profile.email, dob: profile.dob ?? "", gender: profile.gender ?? "",
    phone: profile.phone ?? "", address: profile.address ?? "", headline: profile.headline ?? "", summary: profile.summary ?? "",
  };
}

export function PersonalProfile() {
  const [profile, setProfile] = React.useState<CandidateProfile | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [reload, setReload] = React.useState(0);
  React.useEffect(() => {
    let ignore = false;
    candidateProfileService.getOwn().then(data => {
      if (!ignore) setProfile(data);
    }).catch(err => {
      if (!ignore) setError(getErrorMessage(err, "Không thể tải hồ sơ cá nhân."));
    });
    return () => { ignore = true; };
  }, [reload]);

  return (
    <Card>
      <CardHeader>
        <h2 className="text-lg font-semibold">Thông tin cá nhân</h2>
        <p className="text-xs text-muted-foreground">Hồ sơ của bạn đang ở chế độ riêng tư.</p>
      </CardHeader>
      <CardContent>
        {profile ? <PersonalProfileForm initialProfile={profile} /> : error ? (
          <div className="space-y-3">
            <Alert variant="destructive">{error}</Alert>
            <Button onClick={() => { setError(null); setReload(value => value + 1); }}>Thử lại</Button>
          </div>
        ) : <p role="status" className="text-sm text-muted-foreground">Đang tải thông tin cá nhân...</p>}
      </CardContent>
    </Card>
  );
}

function PersonalProfileForm({ initialProfile }: { initialProfile: CandidateProfile }) {
  const { register, handleSubmit, reset, setError, formState: { errors, isDirty, isSubmitting } } = useForm<CandidateProfileValues>({
    resolver: zodResolver(candidateProfileSchema), defaultValues: toValues(initialProfile),
  });
  const [formError, setFormError] = React.useState<string | null>(null);
  const [saved, setSaved] = React.useState(false);

  async function onSubmit(values: CandidateProfileValues) {
    setSaved(false);
    setFormError(null);
    try {
      const updated = await candidateProfileService.updateOwn(toProfilePayload(values));
      reset(toValues(updated));
      setSaved(true);
    } catch (error) {
      if (error instanceof ApiError && error.code === "VALIDATION_ERROR" && error.errors) {
        const serverErrors = error.errors as Record<string, string>;
        let assigned = false;
        for (const field of FIELDS) {
          if (serverErrors[field]) { setError(field, { message: serverErrors[field] }); assigned = true; }
        }
        if (assigned) return;
      }
      setFormError(getErrorMessage(error, "Không thể lưu thông tin cá nhân."));
    }
  }

  function fieldProps(field: typeof FIELDS[number]) {
    return { id: `personal-${field}`, "aria-invalid": Boolean(errors[field]), "aria-describedby": errors[field] ? `personal-${field}-error` : undefined };
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
      <div className="rounded-lg bg-muted/50 p-3 space-y-3">
        <p className="flex items-center gap-2 text-xs text-muted-foreground"><LockKeyhole className="size-4" aria-hidden />Lấy từ LMS</p>
        <div className="grid gap-4 sm:grid-cols-3">
          <FormField label="Hệ đào tạo" htmlFor="personal-program"><Input id="personal-program" readOnly value={initialProfile.program} /></FormField>
          <FormField label="Lớp" htmlFor="personal-class"><Input id="personal-class" readOnly value={initialProfile.classCode} /></FormField>
          <FormField label="Khoá" htmlFor="personal-course"><Input id="personal-course" readOnly value={initialProfile.courseName} /></FormField>
        </div>
      </div>
      <fieldset disabled={isSubmitting} className="space-y-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Họ và tên" htmlFor="personal-fullName" error={errors.fullName?.message}>
            <Input {...fieldProps("fullName")} required autoComplete="name" maxLength={255} {...register("fullName")} />
          </FormField>
          <FormField label="Email" htmlFor="personal-email" error={errors.email?.message}>
            <Input {...fieldProps("email")} type="email" required autoComplete="email" maxLength={255} {...register("email")} />
          </FormField>
          <FormField label="Ngày sinh" htmlFor="personal-dob" error={errors.dob?.message}>
            <Input {...fieldProps("dob")} type="date" max={localToday()} {...register("dob")} />
          </FormField>
          <FormField label="Giới tính" htmlFor="personal-gender" error={errors.gender?.message}>
            <select {...fieldProps("gender")} className={`${CONTROL_CLASS} h-7`} {...register("gender")}>
              <option value="">Chưa chọn</option><option value="MALE">Nam</option><option value="FEMALE">Nữ</option><option value="OTHER">Khác</option>
            </select>
          </FormField>
          <FormField label="Số điện thoại" htmlFor="personal-phone" error={errors.phone?.message}>
            <Input {...fieldProps("phone")} type="tel" autoComplete="tel" {...register("phone")} />
          </FormField>
          <FormField label="Địa chỉ" htmlFor="personal-address" error={errors.address?.message}>
            <Input {...fieldProps("address")} autoComplete="street-address" maxLength={500} {...register("address")} />
          </FormField>
        </div>
        <FormField label="Tiêu đề hồ sơ" htmlFor="personal-headline" error={errors.headline?.message}>
          <Input {...fieldProps("headline")} maxLength={255} {...register("headline")} />
        </FormField>
        <FormField label="Giới thiệu bản thân" htmlFor="personal-summary" error={errors.summary?.message}>
          <textarea {...fieldProps("summary")} rows={4} maxLength={5000} className={CONTROL_CLASS} {...register("summary")} />
        </FormField>
        {formError ? <Alert variant="destructive">{formError}</Alert> : null}
        {saved && !isDirty ? <Alert variant="success">Đã lưu thông tin</Alert> : null}
        <Button type="submit" disabled={isSubmitting}>{isSubmitting ? "Đang lưu..." : "Lưu thông tin"}</Button>
      </fieldset>
    </form>
  );
}
