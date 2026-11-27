"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { SkillProfile } from "@/app/components/skills/skill-profile";
import { Button } from "@/app/components/ui/button";
import { Alert } from "@/app/components/ui/alert";
import { ApiError, getErrorMessage } from "@/services/api";
import { profileReviewService, reviewHref, type QueueFilters } from "@/services/profile-review.service";
import { ReviewEditor } from "./review-editor";

type DetailProps = { userId: string; filters: QueueFilters; index: number };
const stale = (error: unknown) => error instanceof ApiError && [404, 409].includes(error.status);

function AdvanceReview({ userId, filters, index, message }: DetailProps & { message: string }) {
  const router = useRouter();
  const next = useQuery({
    queryKey: ["next-profile-review", userId, filters, index], gcTime: 0, staleTime: 0,
    queryFn: async ({ signal }) => {
      let page = filters.page;
      let result = await profileReviewService.queue(filters, signal);
      // Removing the last row can also remove its page. Continue from the first pending row.
      if (page > 1 && (!result.items[index] || !result.items.length)) {
        page = 1;
        result = await profileReviewService.queue({ ...filters, page }, signal);
      }
      let position = index;
      if (page !== filters.page || !result.items[position] || result.items[position].userId === userId) {
        position = result.items.findIndex(row => row.userId !== userId);
      }
      return reviewHref({ ...filters, page }, result.items[position]?.userId, position);
    },
  });
  useEffect(() => {
    if (next.data && !next.isFetching) router.replace(next.data);
  }, [next.data, next.isFetching, router]);
  return <section className="space-y-3"><p role="status">{message}</p>
    {next.isError ? <><Alert variant="destructive">{getErrorMessage(next.error)}</Alert>
      <Button onClick={() => next.refetch()}>Tải hồ sơ tiếp theo</Button></> : <p>Đang tải hồ sơ tiếp theo...</p>}
    <Link className="block text-sm underline" href={reviewHref(filters)}>Quay lại hàng đợi</Link>
  </section>;
}

export function ReviewDetail(props: DetailProps) {
  const { userId, filters } = props;
  const client = useQueryClient();
  const [reason, setReason] = useState("");
  const [advanced, setAdvanced] = useState("");
  const [editing, setEditing] = useState(false);
  const key = ["profile-review-preview", userId];
  const preview = useQuery({
    queryKey: key, queryFn: ({ signal }) => profileReviewService.preview(userId, signal),
    staleTime: 0, gcTime: 0, enabled: !advanced,
  });
  const decide = useMutation({
    mutationFn: (action: "approve" | "reject") => profileReviewService.decide(userId, action, reason.trim()),
    onSuccess: (_, action) => {
      void client.invalidateQueries({ queryKey: ["profile-reviews"] });
      setAdvanced(action === "approve" ? "Đã duyệt hồ sơ." : "Đã trả lại hồ sơ.");
    },
  });
  if (advanced || stale(preview.error) || stale(decide.error)) {
    return <AdvanceReview {...props} message={advanced || "Hồ sơ đã rời hàng đợi."} />;
  }
  const data = preview.data;
  return <div className="ptit-page max-w-6xl">
    <Link href={reviewHref(filters)} className="inline-flex w-fit items-center text-sm font-semibold text-primary underline-offset-4 hover:underline">← Quay lại hàng đợi</Link>
    {preview.isPending ? <p role="status" className="text-sm text-muted-foreground">Đang tải hồ sơ...</p> : preview.isError ?
      <Alert variant="destructive" className="flex items-center justify-between gap-2"><span>{getErrorMessage(preview.error)}</span> <Button variant="outline" size="sm" onClick={() => preview.refetch()}>Thử lại</Button></Alert>
      : data && <>
        <header className="ptit-surface space-y-1.5 p-6">
          <p className="ptit-eyebrow">CHI TIẾT HỒ SƠ CHỜ DUYỆT</p>
          <h1 className="text-2xl font-bold text-heading">{data.fullName}</h1>
          <p className="text-sm font-medium text-muted-foreground">{[data.classCode, data.courseName].filter(Boolean).join(" · ")}</p>
          {data.headline && <h2 className="pt-1 font-semibold text-heading">{data.headline}</h2>}
          {data.summary && <p className="whitespace-pre-wrap text-sm leading-relaxed text-body">{data.summary}</p>}
        </header>
        <section aria-label="Bản xem trước của HR" className="ptit-surface p-6">
          <p className="mb-4 text-xs font-bold tracking-wider text-primary uppercase">Bản xem trước của HR</p>
          <SkillProfile readOnly previewSkills={data.skills} />
        </section>
        <ReviewEditor preview={data} disabled={decide.isPending} onBusy={setEditing}
          onStale={() => setAdvanced("Hồ sơ đã rời hàng đợi.")}
          onSaved={() => preview.refetch()} />
        <section className="ptit-surface space-y-4 p-6">
          <h2 className="text-lg font-bold text-heading">Quyết định duyệt hồ sơ</h2>
          <label className="block space-y-1.5 text-sm font-semibold text-heading">Lý do trả lại
            <textarea className="block w-full rounded-xl border border-input bg-white p-3 text-base font-normal text-foreground outline-none focus:border-ring focus:ring-2 focus:ring-ring/20 md:text-sm" rows={3} value={reason}
              onChange={event => setReason(event.target.value)} disabled={decide.isPending} />
          </label>
          <div className="flex flex-wrap gap-3">
            <Button disabled={editing || decide.isPending || preview.isFetching} onClick={() => decide.mutate("approve")}>Duyệt hồ sơ</Button>
            <Button variant="destructive" disabled={!reason.trim() || editing || decide.isPending || preview.isFetching}
              onClick={() => decide.mutate("reject")}>Trả lại hồ sơ</Button>
          </div>
          {decide.isPending && <p role="status" className="text-sm text-muted-foreground">Đang ghi nhận quyết định...</p>}
          {decide.isError && <Alert variant="destructive">{getErrorMessage(decide.error)}</Alert>}
        </section>
      </>}
  </div>;
}
