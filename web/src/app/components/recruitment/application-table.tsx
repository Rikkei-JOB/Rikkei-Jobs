"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { ArrowUpRight } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { TaskBanner } from "@/app/components/ui/task-banner";
import { ApplicationStatusBadge } from "@/app/components/applications/application-status-badge";
import { APPLICATION_STATUS, formatApplicationDate, isApplicationStatus } from "@/config/application-status";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import type { HrApplicationPage } from "@/interface/hr-application";
import { hrApplicationService } from "@/services/hr-application.service";
import { getErrorMessage } from "@/services/api";
import { HrApplicationList } from "./hr-application-list";
import { SelectMenu } from "@/app/components/ui/select-menu";

export function ApplicationTable({ role }: { role: "HR" | "STAFF" | "ADMIN" }) {
  return role === "HR" ? <HrApplicationList /> : <StaffApplicationTable role={role} />;
}

function StaffApplicationTable({ role }: { role: "STAFF" | "ADMIN" }) {
  const allowed = useAuthGuard({ roles: [role] });
  const router = useRouter();
  const search = useSearchParams();
  const status = isApplicationStatus(search.get("status"))
    ? (search.get("status") as keyof typeof APPLICATION_STATUS)
    : undefined;
  const rawPage = Number(search.get("page") ?? "1");
  const page = Number.isInteger(rawPage) && rawPage > 0 ? rawPage : 1;
  const companyId = search.get("companyId") ?? "";
  const jobId = search.get("jobId") ?? "";
  const [data, setData] = useState<HrApplicationPage | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const root = role === "ADMIN" ? "/admin/applications" : "/staff/applications";

  useEffect(() => {
    if (!allowed) return;
    let active = true;
    const params = { companyId: companyId || undefined, jobId: jobId || undefined, status, page, size: 20 };
    hrApplicationService
      .listStaff(params)
      .then((value) => {
        if (active) {
          setData(value);
          setError("");
        }
      })
      .catch((reason) => {
        if (active) setError(getErrorMessage(reason));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [allowed, role, companyId, jobId, status, page]);

  function navigate(changes: Record<string, string>) {
    const next = new URLSearchParams(search.toString());
    for (const [key, value] of Object.entries(changes)) {
      if (value) next.set(key, value);
      else next.delete(key);
    }
    setLoading(true);
    router.push(`${root}${next.size ? `?${next}` : ""}`);
  }

  if (!allowed) return null;

  return (
    <div className={role === "ADMIN" ? "hr-page" : "ptit-page max-w-6xl"}>
      {/* Admin dùng .hr-main như HR (AdminShell) nên đầu trang theo banner của HR; Staff giữ tiêu đề hiện có. */}
      {role === "ADMIN" ? (
        <TaskBanner
          title="Toàn bộ đơn ứng tuyển"
          eyebrow="Giám sát tuyển dụng"
          description="Theo dõi tiến trình tuyển dụng giữa ứng viên và doanh nghiệp, mở từng đơn để xem chi tiết."
          illustration="applications"
          compact
        />
      ) : (
        <TaskBanner
          eyebrow="Giám sát tuyển dụng"
          title="Toàn bộ đơn ứng tuyển"
          description="Theo dõi tiến trình tuyển dụng giữa ứng viên và doanh nghiệp, mở từng đơn để xem chi tiết."
          illustration="applications"
          compact
        />
      )}

      <section className="ptit-filter-bar grid gap-4 sm:grid-cols-3">
        <label className="text-sm font-semibold text-heading">
          Mã công ty
          <input
            aria-label="Mã công ty"
            defaultValue={companyId}
            key={`company-${companyId}`}
            onBlur={(event) => navigate({ companyId: event.target.value.trim(), page: "" })}
            onKeyDown={(event) => {
              if (event.key === "Enter") event.currentTarget.blur();
            }}
            className="mt-1.5 min-h-10 w-full rounded-lg border border-input bg-white px-3.5 py-2 text-base font-normal text-foreground outline-none focus:border-ring focus:ring-2 focus:ring-ring/20 md:text-sm"
            placeholder="UUID công ty"
          />
        </label>
        <label className="text-sm font-semibold text-heading">
          Mã JD
          <input
            aria-label="Mã JD"
            defaultValue={jobId}
            key={`job-${jobId}`}
            onBlur={(event) => navigate({ jobId: event.target.value.trim(), page: "" })}
            onKeyDown={(event) => {
              if (event.key === "Enter") event.currentTarget.blur();
            }}
            className="mt-1.5 min-h-10 w-full rounded-lg border border-input bg-white px-3.5 py-2 text-base font-normal text-foreground outline-none focus:border-ring focus:ring-2 focus:ring-ring/20 md:text-sm"
            placeholder="UUID JD"
          />
        </label>
        <label className="text-sm font-semibold text-heading">
          Trạng thái
          <SelectMenu
            aria-label="Trạng thái"
            value={status ?? ""}
            onValueChange={(value) => navigate({ status: value, page: "" })}
            className="ui-select-trigger mt-1.5 font-normal"
            options={[{ value: "", label: "Tất cả" }, ...Object.entries(APPLICATION_STATUS).map(([value, info]) => ({ value, label: info.label }))]}
          />
        </label>
      </section>

      {error && (
        <p role="alert" className="rounded-xl border border-[#FECACA] bg-[#FEF2F2] p-4 text-sm text-[#B91C1C]">
          {error}
        </p>
      )}
      {loading && <p role="status" className="text-sm text-muted-foreground">Đang tải đơn ứng tuyển…</p>}

      {data && !loading && (
        <section className="ptit-surface overflow-hidden">
          {data.items.length === 0 ? (
            <p className="p-10 text-center text-sm text-muted-foreground">Không có đơn phù hợp.</p>
          ) : (
            <div className="divide-y divide-border">
              {data.items.map((item) => (
                <Link
                  key={item.id}
                  href={`${root}/${item.id}`}
                  className="group grid gap-2 p-5 transition-colors hover:bg-[#FFF8F8] sm:grid-cols-[1.3fr_1.2fr_1fr_auto] sm:items-center sm:gap-4"
                >
                  <div className="min-w-0">
                    <p className="inline-flex items-center gap-1 font-bold text-heading group-hover:text-primary">
                      <span className="truncate">{item.candidateName}</span>
                      <ArrowUpRight className="size-3.5 shrink-0 opacity-0 transition-opacity group-hover:opacity-100" aria-hidden="true" />
                    </p>
                    <p className="mt-0.5 truncate text-xs text-muted-foreground">{item.companyName}</p>
                  </div>
                  <p className="truncate text-sm font-medium text-body">{item.jobTitle}</p>
                  <p className="text-xs text-muted-foreground">{formatApplicationDate(item.createdAt)}</p>
                  <ApplicationStatusBadge status={item.status} />
                </Link>
              ))}
            </div>
          )}
        </section>
      )}

      {data && (
        <nav className="flex items-center justify-between pt-2 text-sm">
          <span className="text-muted-foreground">
            <strong className="font-semibold text-heading">{data.total}</strong> đơn · Trang {page}
          </span>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={loading || page <= 1} onClick={() => navigate({ page: String(page - 1) })}>
              Trước
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={loading || page * data.size >= data.total}
              onClick={() => navigate({ page: String(page + 1) })}
            >
              Sau
            </Button>
          </div>
        </nav>
      )}
    </div>
  );
}
