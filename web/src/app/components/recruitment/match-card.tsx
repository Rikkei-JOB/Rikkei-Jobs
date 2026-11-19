"use client";
import { useEffect, useRef, useState } from "react";
import { MatchScoreCard } from "@/app/components/match/match-score-card";
import { Button } from "@/app/components/ui/button";
import { useAuthStore } from "@/hooks/use-auth-store";
import type { MatchScore, MatchStatus, MatchLookupResponse } from "@/interface/hr-application";
import { ApiError } from "@/services/api";
import { hrApplicationService } from "@/services/hr-application.service";

type Scope = { applicationId: string; viewerId: string | undefined; token: string | null };
function sameScope(a: Scope | null | undefined, b: Scope) {
  return a?.applicationId === b.applicationId && a?.viewerId === b.viewerId && a?.token === b.token;
}
export function MatchCard({ applicationId, match, matchStatus, onMatchChanged }: {
  applicationId: string; match: MatchScore | null; matchStatus?: MatchStatus; onMatchChanged: (value: MatchLookupResponse) => void;
}) {
  const viewerId = useAuthStore(state => state.user?.id), token = useAuthStore(state => state.token);
  const scope: Scope = { applicationId, viewerId, token };
  const flight = useRef<{ scope: Scope; controller: AbortController } | null>(null);
  const [pendingScope, setPendingScope] = useState<Scope | null>(null);
  const [retryError, setRetryError] = useState<{ scope: Scope; error: unknown } | null>(null);
  useEffect(() => () => { flight.current?.controller.abort(); flight.current = null; }, [applicationId, viewerId, token]);
  async function retry() {
    if (flight.current && sameScope(flight.current.scope, scope)) return;
    flight.current?.controller.abort();
    const request = { scope, controller: new AbortController() }; flight.current = request;
    setPendingScope(scope); setRetryError(null);
    function current() {
      const auth = useAuthStore.getState();
      return flight.current === request && !request.controller.signal.aborted && auth.user?.id === scope.viewerId && auth.token === scope.token;
    }
    try {
      const result = await hrApplicationService.match(applicationId, request.controller.signal);
      if (current()) onMatchChanged(result);
    } catch (error) {
      if (current()) { setRetryError({ scope, error }); onMatchChanged({ matchStatus: "UNAVAILABLE", match: null }); }
    } finally {
      if (current()) { flight.current = null; setPendingScope(null); }
    }
  }
  const status = matchStatus ?? (match ? "AVAILABLE" : "UNAVAILABLE");
  const pending = sameScope(pendingScope, scope);
  const error = status === "UNAVAILABLE" && retryError && sameScope(retryError.scope, scope) ? retryError.error : null;
  if (status === "NOT_REQUESTED") return null;
  const unavailable = status !== "AVAILABLE" || !match || Boolean(error);
  return <section className="rounded-xl border bg-background p-5" aria-label="Điểm khớp với JD">
    <h2 className="text-lg font-semibold">Điểm khớp với JD</h2>
    <p className="mt-2 text-sm text-muted-foreground">Điểm phù hợp chỉ mang tính tham khảo</p>
    <div className="mt-4"><MatchScoreCard match={unavailable ? undefined : match} isLoading={pending}
      error={unavailable ? error ?? new ApiError(503, "MATCH_UNAVAILABLE", "Tạm thời không lấy được điểm phù hợp. Bạn vẫn có thể tiếp tục xử lý đơn.") : undefined}
      onRetry={() => void retry()} /></div>
    {!pending && !unavailable && <Button type="button" variant="outline" className="mt-3" onClick={retry}>Tải lại điểm</Button>}
  </section>;
}
