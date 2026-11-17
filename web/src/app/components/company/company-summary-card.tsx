"use client";

import * as React from "react";
import { Button } from "@/app/components/ui/button";
import { Card, CardContent } from "@/app/components/ui/card";
import { validateLogoFile } from "@/schemas/company.schema";
import { companyService } from "@/services/company.service";
import type { Company } from "@/interface/company";

interface CompanySummaryCardProps {
  company: Company;
  /** Gọi khi HR bấm "Dùng ảnh này"; onSuccess để thẻ bỏ ảnh xem trước sau khi tải lên xong. */
  onLogoUpload: (file: File, onSuccess: () => void) => void;
  uploading: boolean;
  /** Lỗi tải lên từ server. */
  error: string | null;
  disabled: boolean;
}

interface PendingLogo {
  file: File;
  url: string;
}

/** Thẻ cố định bên trái: logo (có xem trước ảnh mới trước khi tải lên), mã số thuế, trạng thái (chỉ đọc). */
export function CompanySummaryCard({ company, onLogoUpload, uploading, error, disabled }: CompanySummaryCardProps) {
  const inputRef = React.useRef<HTMLInputElement>(null);
  const logo = companyService.logoSrc(company.logoUrl);
  const [pending, setPending] = React.useState<PendingLogo | null>(null);
  const [fileError, setFileError] = React.useState<string | null>(null);

  // Thu hồi object URL còn lại khi rời trang (huỷ/xong đã thu hồi ở clearPending).
  const pendingUrlRef = React.useRef<string | null>(null);
  React.useEffect(() => {
    pendingUrlRef.current = pending?.url ?? null;
  }, [pending]);
  React.useEffect(
    () => () => {
      if (pendingUrlRef.current) URL.revokeObjectURL(pendingUrlRef.current);
    },
    [],
  );

  function clearPending() {
    if (pending) URL.revokeObjectURL(pending.url);
    setPending(null);
  }

  function handleChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    // Xoá giá trị để chọn lại cùng một file vẫn kích hoạt onChange.
    event.target.value = "";
    if (!file) return;
    const invalid = validateLogoFile(file);
    setFileError(invalid);
    if (invalid) return;
    if (pending) URL.revokeObjectURL(pending.url);
    setPending({ file, url: URL.createObjectURL(file) });
  }

  const shownError = fileError ?? error;

  return (
    <Card>
      <CardContent className="space-y-4">
        <div className="flex items-start gap-3">
          <div className="space-y-1">
            <div className="flex h-28 w-28 items-center justify-center overflow-hidden rounded-lg border bg-muted/40">
              {logo ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={logo} alt={`Logo ${company.name}`} className="h-full w-full object-contain" />
              ) : (
                <span className="text-xs text-muted-foreground">Chưa có logo</span>
              )}
            </div>
            {pending ? <p className="text-center text-xs text-muted-foreground">Hiện tại</p> : null}
          </div>
          {pending ? (
            <div className="space-y-1">
              <div className="flex h-28 w-28 items-center justify-center overflow-hidden rounded-lg border border-ring bg-muted/40">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img src={pending.url} alt="Logo mới (xem trước)" className="h-full w-full object-contain" />
              </div>
              <p className="text-center text-xs text-muted-foreground">Ảnh mới</p>
            </div>
          ) : null}
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
          {pending ? (
            <div className="flex gap-2">
              <Button
                type="button"
                size="sm"
                disabled={disabled || uploading}
                onClick={() => {
                  setFileError(null);
                  onLogoUpload(pending.file, clearPending);
                }}
              >
                {uploading ? "Đang tải lên..." : "Dùng ảnh này"}
              </Button>
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={uploading}
                onClick={() => {
                  setFileError(null);
                  clearPending();
                }}
              >
                Huỷ
              </Button>
            </div>
          ) : (
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={disabled || uploading}
              onClick={() => inputRef.current?.click()}
            >
              Đổi logo
            </Button>
          )}
          <p className="text-xs text-muted-foreground">JPG hoặc PNG, tối đa 2MB</p>
          {shownError ? (
            <p role="alert" className="text-xs/relaxed text-destructive">
              {shownError}
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
