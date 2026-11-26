"use client";

import { Suspense, useEffect, useMemo, useRef } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { AlertCircle } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { SelectMenu } from "@/app/components/ui/select-menu";
import { LEVEL_LABEL, TYPE_LABEL, WORK_MODE_LABEL } from "@/app/components/job/job-labels";
import { useBoardJob, useBoardSearch } from "@/hooks/use-job-board";
import { getErrorMessage } from "@/services/api";
import { EMPTY_FILTERS, type BoardFilters } from "@/services/job-board.service";
import { JobDetailView, jobHref } from "./job-detail";
import { EmptySearchArt } from "./illustrations";
import { JobCard, JobSearchForm, SkillSuggestions, topSkills } from "./job-parts";
import "./job-board.css";

const SALARY_LABEL = { lt15: "Dưới 15 triệu", "15to30": "15 - 30 triệu", gt30: "Trên 30 triệu" } as const;

/** Tên tham số trên URL cho từng bộ lọc, để link chia sẻ được và nút Quay lại hoạt động đúng. */
const PARAM: Record<keyof BoardFilters, string> = { q: "q", city: "city", level: "level", workMode: "mode", type: "type", salary: "salary", sort: "sort" };

function oneOf<T extends string>(value: string | null, allowed: readonly T[]): T | "all" {
  return value && (allowed as readonly string[]).includes(value) ? (value as T) : "all";
}

function readFilters(params: URLSearchParams): BoardFilters {
  return {
    q: params.get(PARAM.q) ?? "",
    city: params.get(PARAM.city) || "all",
    level: oneOf(params.get(PARAM.level), Object.keys(LEVEL_LABEL) as (keyof typeof LEVEL_LABEL)[]),
    workMode: oneOf(params.get(PARAM.workMode), Object.keys(WORK_MODE_LABEL) as (keyof typeof WORK_MODE_LABEL)[]),
    type: oneOf(params.get(PARAM.type), Object.keys(TYPE_LABEL) as (keyof typeof TYPE_LABEL)[]),
    salary: oneOf(params.get(PARAM.salary), Object.keys(SALARY_LABEL) as (keyof typeof SALARY_LABEL)[]),
    sort: params.get(PARAM.sort) === "salary" ? "salary" : "newest",
  };
}

export function JobBoard({ basePath }: { basePath: string }) {
  return <Suspense fallback={null}><JobBoardContent basePath={basePath} /></Suspense>;
}

function JobBoardContent({ basePath }: { basePath: string }) {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const filters = useMemo(() => readFilters(new URLSearchParams(params.toString())), [params]);
  const search = useBoardSearch(filters);
  const jobs = useMemo(() => search.data?.pages.flatMap(page => page.items) ?? [], [search.data]);
  const total = search.data?.pages[0]?.total ?? 0;
  const selectedId = jobs.find(job => job.id === params.get("job"))?.id ?? jobs[0]?.id;
  const preview = useBoardJob(selectedId);
  const suggestions = useMemo(() => topSkills(jobs).map(([skill]) => skill), [jobs]);
  const hasFilters = (Object.keys(EMPTY_FILTERS) as (keyof BoardFilters)[]).some(key => key !== "sort" && filters[key] !== EMPTY_FILTERS[key]);

  const previewRef = useRef<HTMLElement>(null);
  // Đổi tin đang xem nhanh thì đưa khung xem nhanh về đầu, như mở một trang mới.
  useEffect(() => { previewRef.current?.scrollTo({ top: 0 }); }, [selectedId]);

  /** Đổi bộ lọc thay thế mục lịch sử hiện tại (không chồng lịch sử mỗi lần chọn) và giữ vị trí cuộn. */
  function update(changes: Partial<Record<keyof BoardFilters | "job", string | null>>) {
    const next = new URLSearchParams(params.toString());
    for (const [key, value] of Object.entries(changes)) {
      const name = key === "job" ? "job" : PARAM[key as keyof BoardFilters];
      if (!value || value === "all" || (key === "sort" && value === "newest")) next.delete(name); else next.set(name, value);
    }
    if (!("job" in changes)) next.delete("job");
    router.replace(`${pathname}${next.size ? `?${next}` : ""}`, { scroll: false });
  }
  const clearAll = () => update({ q: null, city: null, level: null, workMode: null, type: null, salary: null });

  const filterSelects: { key: keyof BoardFilters; label: string; allLabel: string; options: [string, string][] }[] = [
    { key: "level", label: "Cấp bậc", allLabel: "Tất cả cấp bậc", options: Object.entries(LEVEL_LABEL) },
    { key: "workMode", label: "Hình thức", allLabel: "Mọi hình thức", options: Object.entries(WORK_MODE_LABEL) },
    { key: "type", label: "Loại hợp đồng", allLabel: "Mọi loại hợp đồng", options: Object.entries(TYPE_LABEL) },
    { key: "salary", label: "Mức lương", allLabel: "Mọi mức lương", options: Object.entries(SALARY_LABEL) },
  ];

  return <div className="jb">
    <section className="jb-hero jb-hero-compact" aria-label="Tìm việc làm">
      <div className="jb-container jb-hero-inner">
        <JobSearchForm key={`${filters.q}:${filters.city}`} basePath={basePath} initialQuery={filters.q} initialCity={filters.city} />
        <SkillSuggestions basePath={basePath} skills={suggestions} />
      </div>
    </section>

    <div className="jb-container">
      <div className="jb-filters" role="group" aria-label="Bộ lọc">
        {filterSelects.map(filter => <SelectMenu key={filter.key} aria-label={filter.label} value={filters[filter.key]}
          onValueChange={value => update({ [filter.key]: value })} active={filters[filter.key] !== "all"} className="ui-select-trigger jb-filter-trigger"
          renderValue={(value, label) => (value === "all" ? filter.label : label)}
          options={[{ value: "all", label: filter.allLabel }, ...filter.options.map(([value, label]) => ({ value, label }))]} />)}
        {hasFilters && <button type="button" className="jb-filter-clear" onClick={clearAll}>Xoá bộ lọc</button>}
      </div>

      <div className="jb-results-head">
        <h2 aria-live="polite">{search.isPending ? "Đang tìm việc làm…" : <><em>{total}</em> việc làm{filters.q ? <> cho “{filters.q}”</> : ""}{filters.city !== "all" ? ` tại ${filters.city}` : ""}</>}</h2>
        <div className="jb-sort"><span id="jb-sort-label">Sắp xếp</span>
          <SelectMenu aria-label="Sắp xếp" value={filters.sort} onValueChange={value => update({ sort: value })} className="ui-select-trigger jb-sort-trigger"
            options={[{ value: "newest", label: "Mới nhất" }, { value: "salary", label: "Lương cao nhất" }]} /></div>
      </div>

      {search.error ? <div className="jb-empty" role="alert"><AlertCircle aria-hidden="true" /><h3>Chưa thể tải việc làm</h3><p>{getErrorMessage(search.error)}</p>
        <Button variant="outline" disabled={search.isFetching} onClick={() => search.refetch()}>Thử lại</Button></div>
        : search.isPending ? <div className="jb-board"><div className="jb-list" role="status" aria-label="Đang tải">{[0, 1, 2].map(i => <div key={i} className="jb-skeleton" />)}</div></div>
        : jobs.length === 0 ? <div className="jb-empty"><EmptySearchArt className="jb-empty-art" />
          <h3>{hasFilters ? "Không tìm thấy việc làm phù hợp" : "Chưa có tin tuyển dụng đang mở"}</h3>
          <p>{hasFilters ? "Thử từ khoá ngắn hơn, đổi thành phố hoặc bỏ bớt bộ lọc." : "Tin sẽ xuất hiện ở đây khi doanh nghiệp đăng và được cán bộ duyệt."}</p>
          {hasFilters && <Button variant="outline" onClick={clearAll}>Xoá bộ lọc</Button>}</div>
        : <div className="jb-board" aria-busy={search.isPlaceholderData}>
          <div className="jb-list" style={search.isPlaceholderData ? { opacity: .6 } : undefined}>
            {jobs.map(job => <JobCard key={job.id} job={job} href={jobHref(basePath, job.id)}
              selected={job.id === selectedId} onSelect={id => update({ job: id })} />)}
            {search.hasNextPage && <Button variant="outline" className="h-11" disabled={search.isFetchingNextPage} onClick={() => search.fetchNextPage()}>
              {search.isFetchingNextPage ? "Đang tải…" : `Xem thêm việc làm (còn ${total - jobs.length})`}</Button>}
          </div>
          <aside ref={previewRef} className="jb-preview" aria-label="Xem nhanh việc làm" aria-busy={preview.isFetching}>
            {preview.data ? <JobDetailView key={preview.data.id} job={preview.data} basePath={basePath} variant="preview" />
              : preview.error ? <div className="jb-empty" role="alert"><AlertCircle aria-hidden="true" /><h3>Chưa thể tải chi tiết</h3><p>{getErrorMessage(preview.error)}</p></div>
              : <div className="jb-detail-body"><div className="jb-skeleton" /><div className="jb-skeleton" style={{ marginTop: 16 }} /></div>}
          </aside>
        </div>}
    </div>
  </div>;
}
