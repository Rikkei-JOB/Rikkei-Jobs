"use client";

import * as React from "react";
import { useForm } from "react-hook-form";
import { Button } from "@/app/components/ui/button";
import { Input } from "@/app/components/ui/input";
import { FormField } from "@/app/components/auth/form-field";
import { CompanyLogoField } from "@/app/components/company/company-logo-field";
import { zodResolver } from "@/helper/zod-resolver";
import { useUpdateMyCompany } from "@/hooks/use-my-company";
import { COMPANY_SIZES, companyProfileSchema, toUpdatePayload, type CompanyProfileValues } from "@/schemas/company.schema";
import { ApiError, getErrorMessage } from "@/services/api";
import type { Company } from "@/interface/company";

interface CompanyProfileFormProps {
  company: Company;
  disabled: boolean;
  /** Tab đang mở; hai khối đầu luôn nằm trong DOM (chỉ ẩn) để chữ đang gõ không mất khi đổi tab. */
  tab: CompanyTab;
  /** Yêu cầu trang cha chuyển sang tab có ô bị lỗi. */
  onTabChange: (tab: CompanyTab) => void;
}

export type CompanyTab = "intro" | "contact" | "jobs";

const CONTROL_CLASS =
  "w-full min-w-0 rounded-md border border-input bg-input/20 px-2 py-1 text-sm transition-colors outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/30 disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:border-destructive md:text-xs/relaxed dark:bg-input/30";

function toFormValues(company: Company): CompanyProfileValues {
  return {
    name: company.name,
    industry: company.industry ?? "",
    size: company.size ?? "",
    address: company.address ?? "",
    city: company.city ?? "",
    website: company.website ?? "",
    description: company.description ?? "",
  };
}

const FIELDS = ["name", "industry", "size", "address", "city", "website", "description"] as const;
const CONTACT_FIELDS: readonly string[] = ["address", "city", "website"];

export function CompanyProfileForm({ company, disabled, tab, onTabChange }: CompanyProfileFormProps) {
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isDirty },
  } = useForm<CompanyProfileValues>({
    resolver: zodResolver(companyProfileSchema),
    defaultValues: toFormValues(company),
  });
  const update = useUpdateMyCompany();
  const [savedAt, setSavedAt] = React.useState<Date | null>(null);
  const [formError, setFormError] = React.useState<string | null>(null);

  // Đồng bộ form khi dữ liệu từ server (hoặc sau khi đổi logo) thay đổi, nhưng giữ các ô HR đang sửa dở.
  React.useEffect(() => {
    reset(toFormValues(company), { keepDirtyValues: true });
  }, [company, reset]);

  function showTabOf(fields: string[]) {
    if (fields.length === 0) return;
    onTabChange(fields.every((f) => CONTACT_FIELDS.includes(f)) ? "contact" : "intro");
  }

  function onSubmit(values: CompanyProfileValues) {
    setSavedAt(null);
    setFormError(null);
    update.mutate(toUpdatePayload(values), {
      onSuccess: (updated) => {
        // Form về trạng thái sạch với giá trị server vừa lưu (không còn "dirty" cũ).
        reset(toFormValues(updated));
        setSavedAt(new Date());
      },
      onError: (error) => {
        if (error instanceof ApiError && error.code === "VALIDATION_ERROR" && error.errors) {
          const fieldErrors = error.errors as Record<string, string>;
          const assigned: string[] = [];
          for (const field of FIELDS) {
            if (fieldErrors[field]) {
              setError(field, { message: fieldErrors[field] });
              assigned.push(field);
            }
          }
          if (assigned.length > 0) {
            showTabOf(assigned);
            return;
          }
        }
        setFormError(getErrorMessage(error));
      },
    });
  }

  const cardClass = "space-y-4 rounded-xl border border-border bg-white p-4";

  return (
    <form
      onSubmit={handleSubmit(onSubmit, (invalid) => showTabOf(Object.keys(invalid)))}
      noValidate
      className={tab === "jobs" ? "hidden" : "space-y-4"}
    >
      <section className={tab === "intro" ? cardClass : "hidden"}>
        <CompanyLogoField company={company} disabled={disabled} />
        <FormField label="Tên công ty *" htmlFor="name" error={errors.name?.message}>
          <Input id="name" disabled={disabled} aria-invalid={Boolean(errors.name)} {...register("name")} />
        </FormField>
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Ngành" htmlFor="industry" error={errors.industry?.message}>
            <Input id="industry" disabled={disabled} aria-invalid={Boolean(errors.industry)} {...register("industry")} />
          </FormField>
          <FormField label="Quy mô" htmlFor="size" error={errors.size?.message}>
            <select id="size" disabled={disabled} className={`${CONTROL_CLASS} h-7`} {...register("size")}>
              <option value="">Chưa chọn</option>
              {COMPANY_SIZES.map((s) => (
                <option key={s} value={s}>
                  {s} nhân sự
                </option>
              ))}
            </select>
          </FormField>
        </div>
        <FormField label="Giới thiệu" htmlFor="description" error={errors.description?.message}>
          <textarea
            id="description"
            rows={6}
            disabled={disabled}
            aria-invalid={Boolean(errors.description)}
            className={CONTROL_CLASS}
            {...register("description")}
          />
        </FormField>
      </section>

      <section className={tab === "contact" ? cardClass : "hidden"}>
        <FormField label="Địa chỉ" htmlFor="address" error={errors.address?.message}>
          <Input id="address" disabled={disabled} aria-invalid={Boolean(errors.address)} {...register("address")} />
        </FormField>
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Thành phố" htmlFor="city" error={errors.city?.message}>
            <Input id="city" disabled={disabled} aria-invalid={Boolean(errors.city)} {...register("city")} />
          </FormField>
          <FormField label="Website" htmlFor="website" error={errors.website?.message}>
            <Input
              id="website"
              placeholder="https://"
              disabled={disabled}
              aria-invalid={Boolean(errors.website)}
              {...register("website")}
            />
          </FormField>
        </div>
      </section>

      <div className="sticky bottom-0 z-10 flex flex-wrap items-center justify-between gap-2 rounded-xl border border-border bg-white px-4 py-3 shadow-sm">
        <div role="status" className="text-xs">
          {isDirty ? (
            <span className="text-amber-700">Có thay đổi chưa lưu</span>
          ) : savedAt && !update.isPending ? (
            <span className="text-emerald-700">Đã lưu lúc {savedAt.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}</span>
          ) : null}
          {formError ? (
            <p role="alert" className="text-destructive">
              {formError}
            </p>
          ) : null}
        </div>
        <Button type="submit" disabled={disabled || update.isPending}>
          {update.isPending ? (
            <>
              <span
                aria-hidden="true"
                className="size-3 animate-spin rounded-full border-2 border-current border-t-transparent"
              />
              Đang lưu...
            </>
          ) : (
            "Lưu thay đổi"
          )}
        </Button>
      </div>
    </form>
  );
}
