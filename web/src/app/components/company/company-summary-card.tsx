"use client";

import * as React from "react";
import { Button } from "@/app/components/ui/button";
import { Card, CardContent } from "@/app/components/ui/card";
import { companyService } from "@/services/company.service";
import type { Company } from "@/interface/company";

interface CompanySummaryCardProps {
  company: Company;
  onLogoChange: (file: File) => void;
  uploading: boolean;
  error: string | null;
  disabled: boolean;
}

/** Thẻ cố định bên trái: logo, mã số thuế, trạng thái (các thông tin chỉ đọc). */
export function CompanySummaryCard({ company, onLogoChange, uploading, error, disabled }: CompanySummaryCardProps) {
  const inputRef = React.useRef<HTMLInputElement>(null);
  const logo = companyService.logoSrc(company.logoUrl);

  function handleChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    // Xoá giá trị để chọn lại cùng một file vẫn kích hoạt onChange.
    event.target.value = "";
    if (file) onLogoChange(file);
  }

  return (
    <Card>
      <CardContent className="space-y-4">
        <div className="flex h-28 w-28 items-center justify-center overflow-hidden rounded-lg border bg-muted/40">
          {logo ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={logo} alt={`Logo ${company.name}`} className="h-full w-full object-contain" />
          ) : (
            <span className="text-xs text-muted-foreground">Chưa có logo</span>
          )}
        </div>

        <div className="space-y-1.5">
          <input
            ref={inputRef}
            type="file"
            accept="image/jpeg,image/png"
            className="hidden"
            onChange={handleChange}
            data-testid="logo-input"
          />
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={disabled || uploading}
            onClick={() => inputRef.current?.click()}
          >
            {uploading ? "Đang tải lên..." : "Đổi logo"}
          </Button>
          <p className="text-xs text-muted-foreground">JPG hoặc PNG, tối đa 2MB</p>
          {error ? (
            <p role="alert" className="text-xs/relaxed text-destructive">
              {error}
            </p>
          ) : null}
        </div>

        <dl className="space-y-2 text-xs/relaxed">
          <div>
            <dt className="text-muted-foreground">Mã số thuế</dt>
            <dd className="font-medium">{company.taxCode}</dd>
          </div>
          <div>
            <dt className="text-muted-foreground">Trạng thái</dt>
            <dd className="font-medium">{company.status === "ACTIVE" ? "Đang hoạt động" : "Đã khoá"}</dd>
          </div>
        </dl>

        <p className="text-xs/relaxed text-muted-foreground">Liên hệ cán bộ tuyển dụng để đổi các thông tin này.</p>
      </CardContent>
    </Card>
  );
}
