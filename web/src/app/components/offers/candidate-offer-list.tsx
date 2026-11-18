"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Button } from "@/app/components/ui/button";
import { useAuthGuard } from "@/hooks/use-auth-guard";
import type { Offer } from "@/interface/offer";
import { formatOfferSalary, offerStatusLabel } from "@/interface/offer";
import { getErrorMessage } from "@/services/api";
import { offerService } from "@/services/offer.service";

export function CandidateOfferList() {
  const allowed = useAuthGuard({ roles: ["CANDIDATE"] });
  const [items, setItems] = useState<Offer[] | null>(null);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [error, setError] = useState("");
  const [version, setVersion] = useState(0);
  useEffect(() => {
    if (!allowed) return;
    let active = true;
    offerService.list({ page, size: 10 }).then(value => { if (active) { setItems(value.items); setTotal(value.total); setError(""); } })
      .catch(cause => { if (active) setError(getErrorMessage(cause)); });
    return () => { active = false; };
  }, [allowed, page, version]);
  if (!allowed) return null;
  return <main className="mx-auto max-w-4xl space-y-5">
    <h1 className="text-2xl font-semibold">Offer của tôi</h1>
    {error && <div role="alert" className="space-y-2 rounded-lg border p-4"><p>{error}</p><Button variant="outline" onClick={() => setVersion(v => v + 1)}>Thử lại</Button></div>}
    {!items && !error && <p role="status">Đang tải offer…</p>}
    {items?.length === 0 && !error && <p>Bạn chưa có offer.</p>}
    {items?.map(item => <Link key={item.id} href={`/candidate/offers/${item.id}`} className="block rounded-xl border bg-background p-5 hover:border-primary">
      <div className="flex justify-between gap-3"><h2 className="font-semibold">{item.position}</h2><span className="text-sm">{offerStatusLabel[item.status]}</span></div>
      <p className="mt-2 text-sm">{formatOfferSalary(item.salary)}</p>
      <p className="mt-1 text-xs text-muted-foreground">Hạn trả lời: {new Date(item.responseDeadline).toLocaleString("vi-VN")}</p>
    </Link>)}
    {total > 10 && <div className="flex items-center gap-3"><Button variant="outline" disabled={page === 1} onClick={() => setPage(p => p - 1)}>Trước</Button><span>Trang {page}</span><Button variant="outline" disabled={page * 10 >= total} onClick={() => setPage(p => p + 1)}>Sau</Button></div>}
  </main>;
}
