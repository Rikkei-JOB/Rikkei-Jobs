"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Card, CardContent, CardHeader } from "@/app/components/ui/card";
import { Button } from "@/app/components/ui/button";
import { Alert } from "@/app/components/ui/alert";
import { candidateProfileService as service } from "@/services/candidate-profile.service";
import { ApiError, getErrorMessage } from "@/services/api";
import type { CandidateProfile } from "@/interface/candidate-profile";

const statuses = { PRIVATE: "Riêng tư", PENDING: "Chờ duyệt", PUBLIC: "Đã công khai", REJECTED: "Đã trả lại" };

export function PublicationPanel({ initialProfile }: { initialProfile: CandidateProfile }) {
  const client = useQueryClient();
  const [consent, setConsent] = useState(false);
  const key = ["profile-publication", initialProfile.userId];
  const profile = useQuery({ queryKey: key, queryFn: service.getOwn, initialData: initialProfile,
    staleTime: 0, refetchOnMount: false, refetchOnWindowFocus: true });
  // Lịch sử không hiển thị nữa, chỉ dùng để lấy lý do trả lại gần nhất.
  const history = useQuery({ queryKey: [...key, "history"], queryFn: service.history,
    staleTime: 0, refetchOnWindowFocus: true });
  const visibility = profile.data.visibility;
  const canSubmit = visibility === "PRIVATE" || visibility === "REJECTED";
  const latestReason = visibility === "REJECTED" && history.data?.find(event => event.action === "REJECT")?.reason;
  const mutation = useMutation({
    mutationFn: canSubmit ? service.publish : service.withdraw,
    onSuccess: async updated => {
      client.setQueryData(key, updated);
      setConsent(false);
      await client.invalidateQueries({ queryKey: key });
    },
    onError: async error => {
      if (error instanceof ApiError && error.status === 409) {
        setConsent(false);
        await client.invalidateQueries({ queryKey: key });
      }
    },
  });
  return (
    <section aria-labelledby="publication-title">
      <Card>
        <CardHeader>
          <h2 id="publication-title" className="text-lg font-semibold">Công khai hồ sơ</h2>
          <p role="status" className="text-sm">Trạng thái hiện tại · <strong>{statuses[visibility]}</strong></p>
        </CardHeader>
        <CardContent className="space-y-4">
          {latestReason && <p className="break-words text-sm"><strong>Lý do trả lại</strong> · {latestReason}</p>}
          {canSubmit && <label className="flex items-start gap-2 text-sm">
            <input type="checkbox" className="mt-1" checked={consent} disabled={mutation.isPending}
              onChange={event => setConsent(event.target.checked)} />
            Tôi đồng ý cho doanh nghiệp xem hồ sơ của tôi sau khi cán bộ duyệt.
          </label>}
          <Button disabled={mutation.isPending || (canSubmit && !consent)} onClick={() => mutation.mutate()}>
            {mutation.isPending ? "Đang xử lý…" : ({ PRIVATE: "Gửi duyệt", REJECTED: "Gửi lại", PENDING: "Rút yêu cầu", PUBLIC: "Rút công khai" })[visibility]}
          </Button>
          {mutation.error && <Alert variant="destructive">{getErrorMessage(mutation.error)}</Alert>}
          {(profile.isError || history.isError) && <Alert variant="destructive">
            Chưa tải được dữ liệu mới nhất. <Button variant="outline" onClick={() => client.invalidateQueries({ queryKey: key })}>Tải lại</Button>
          </Alert>}
        </CardContent>
      </Card>
    </section>
  );
}
