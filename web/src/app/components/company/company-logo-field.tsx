"use client";

import * as React from "react";
import { Button } from "@/app/components/ui/button";
import { CompanyMark } from "@/app/components/company/company-mark";
import { useUploadMyLogo } from "@/hooks/use-my-company";
import { validateLogoFile } from "@/schemas/company.schema";
import { getErrorMessage } from "@/services/api";
import type { Company } from "@/interface/company";

interface PendingLogo {
  file: File;
  url: string;
}

/** Logo công ty: xem trước ảnh mới ("Dùng ảnh này" / "Huỷ") rồi mới tải lên; JPG/PNG tối đa 2MB. */
export function CompanyLogoField({ company, disabled }: { company: Company; disabled: boolean }) {
  const inputRef = React.useRef<HTMLInputElement>(null);
  const upload = useUploadMyLogo();
  const [pending, setPending] = React.useState<PendingLogo | null>(null);
  const [fileError, setFileError] = React.useState<string | null>(null);
  const [uploadError, setUploadError] = React.useState<string | null>(null);

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

  function handleUse() {
    if (!pending) return;
    setFileError(null);
    setUploadError(null);
    upload.mutate(pending.file, { onSuccess: clearPending, onError: (e) => setUploadError(getErrorMessage(e)) });
  }

  const shownError = fileError ?? uploadError;

  return (
    <div className="space-y-2">
      <p className="text-sm font-medium">Logo công ty</p>
      <div className="flex flex-wrap items-start gap-3">
        <div className="space-y-1 text-center">
          <CompanyMark name={company.name} logoUrl={company.logoUrl} size={96} brand />
          {pending ? <p className="text-xs text-muted-foreground">Hiện tại</p> : null}
        </div>
        {pending ? (
          <div className="space-y-1 text-center">
            <span className="inline-flex size-24 items-center justify-center overflow-hidden rounded-lg border border-ring bg-white">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={pending.url} alt="Logo mới (xem trước)" className="h-full w-full object-contain" />
            </span>
            <p className="text-xs text-muted-foreground">Ảnh mới</p>
          </div>
        ) : null}
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
              <Button type="button" size="sm" disabled={disabled || upload.isPending} onClick={handleUse}>
                {upload.isPending ? "Đang tải lên..." : "Dùng ảnh này"}
              </Button>
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={upload.isPending}
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
              disabled={disabled || upload.isPending}
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
      </div>
    </div>
  );
}
