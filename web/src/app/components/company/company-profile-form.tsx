"use client";

import * as React from "react";
import { useForm } from "react-hook-form";
import { Alert } from "@/app/components/ui/alert";
import { Button } from "@/app/components/ui/button";
import { Card, CardContent } from "@/app/components/ui/card";
import { Input } from "@/app/components/ui/input";
import { FormField } from "@/app/components/auth/form-field";
import { zodResolver } from "@/helper/zod-resolver";
import { useUpdateMyCompany } from "@/hooks/use-my-company";
import { COMPANY_SIZES, companyProfileSchema, toUpdatePayload, type CompanyProfileValues } from "@/schemas/company.schema";
import { ApiError, getErrorMessage } from "@/services/api";
import type { Company } from "@/interface/company";

interface CompanyProfileFormProps {
  company: Company;
  disabled: boolean;
  onSaved: (company: Company) => void;
}

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

export function CompanyProfileForm({ company, disabled, onSaved }: CompanyProfileFormProps) {
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
  const [saved, setSaved] = React.useState(false);
  const [formError, setFormError] = React.useState<string | null>(null);

  // Đồng bộ form khi dữ liệu từ server (hoặc sau khi đổi logo) thay đổi, nhưng giữ các ô HR đang sửa dở.
  React.useEffect(() => {
    reset(toFormValues(company), { keepDirtyValues: true });
  }, [company, reset]);

  function onSubmit(values: CompanyProfileValues) {
    setSaved(false);
    setFormError(null);
    update.mutate(toUpdatePayload(values), {
      onSuccess: (updated) => {
        // Form về trạng thái sạch với giá trị server vừa lưu (không còn "dirty" cũ).
        reset(toFormValues(updated));
        setSaved(true);
        onSaved(updated);
      },
      onError: (error) => {
        if (error instanceof ApiError && error.code === "VALIDATION_ERROR" && error.errors) {
          const fieldErrors = error.errors as Record<string, string>;
          let assigned = false;
          for (const field of FIELDS) {
            if (fieldErrors[field]) {
              setError(field, { message: fieldErrors[field] });
              assigned = true;
            }
          }
          if (assigned) return;
        }
        setFormError(getErrorMessage(error));
      },
    });
  }

  return (
    <Card>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
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
          <FormField label="Giới thiệu" htmlFor="description" error={errors.description?.message}>
            <textarea
              id="description"
              rows={5}
              disabled={disabled}
              aria-invalid={Boolean(errors.description)}
              className={CONTROL_CLASS}
              {...register("description")}
            />
          </FormField>

          {formError ? <Alert variant="destructive">{formError}</Alert> : null}
          {saved && !isDirty ? <Alert variant="success">Đã lưu</Alert> : null}

          <Button type="submit" disabled={disabled || update.isPending}>
            {update.isPending ? "Đang lưu..." : "Lưu thay đổi"}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
