"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Popover } from "@base-ui/react/popover";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell, Check, CheckCheck, ChevronLeft, ChevronRight, X } from "lucide-react";
import { useAuthStore } from "@/hooks/use-auth-store";
import { notificationService, type CandidateNotification } from "@/services/notification.service";
import { getErrorMessage } from "@/services/api";

export function CandidateNotifications({ userId, token }: { userId: string; token: string | null }) {
  const [page, setPage] = useState(1);
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [writeError, setWriteError] = useState("");
  const router = useRouter();
  const client = useQueryClient();
  const prefix = ["candidate-notifications", userId, token];
  const feed = useQuery({
    queryKey: [...prefix, page],
    queryFn: ({ signal }) => notificationService.list(page, signal),
    enabled: Boolean(token), refetchInterval: 60_000, refetchIntervalInBackground: false,
    refetchOnWindowFocus: true,
  });
  function currentSession() {
    const current = useAuthStore.getState();
    return current.user?.id === userId && current.token === token;
  }
  async function read(item?: CandidateNotification, navigate = false) {
    if (busy) return;
    setBusy(true); setWriteError("");
    try {
      if (item) { if (!item.readAt) await notificationService.read(item.id); }
      else await notificationService.readAll();
      if (!currentSession()) return;
      await client.invalidateQueries({ queryKey: prefix });
      if (navigate && item && currentSession()) {
        setOpen(false);
        router.push(item.href.startsWith("/candidate/") && !item.href.includes("\\") ? item.href : "/candidate/applications");
      }
    } catch (error) {
      if (currentSession()) setWriteError(getErrorMessage(error, "Chưa cập nhật được thông báo. Vui lòng thử lại."));
    } finally { if (currentSession()) setBusy(false); }
  }
  const data = feed.data;
  const unread = data?.unreadCount;

  return <Popover.Root open={open} onOpenChange={value => {
    setOpen(value); setWriteError(""); setPage(1);
    if (value) void client.invalidateQueries({ queryKey: prefix });
  }}>
    <Popover.Trigger className="candidate-icon-button candidate-bell" aria-label={unread === undefined ? "Thông báo" : `Thông báo, ${unread} chưa đọc`}>
      <Bell aria-hidden="true" />{Boolean(unread) && <span className="candidate-badge" aria-hidden="true">{unread! > 99 ? "99+" : unread}</span>}
    </Popover.Trigger>
    <Popover.Portal><Popover.Positioner side="bottom" align="end" sideOffset={12} collisionPadding={16} className="candidate-overlay">
      <Popover.Popup className="candidate-popup candidate-notification-panel" role="dialog">
        <div className="candidate-panel-heading"><div><Popover.Title>Thông báo</Popover.Title><Popover.Description>{unread === undefined ? "Cập nhật hành trình ứng tuyển của bạn" : `${unread} thông báo chưa đọc`}</Popover.Description></div>
          <Popover.Close className="candidate-icon-button" aria-label="Đóng thông báo"><X aria-hidden="true" /></Popover.Close></div>
        <div className="candidate-panel-toolbar"><button disabled={busy || !unread} onClick={() => void read()}><CheckCheck aria-hidden="true" />Đánh dấu tất cả đã đọc</button></div>
        {writeError && <p role="alert" className="candidate-panel-error">{writeError}</p>}
        {feed.isError && <div className="candidate-panel-error" role="alert"><p>{getErrorMessage(feed.error, "Không tải được thông báo.")}</p><button onClick={() => void feed.refetch()}>Thử lại</button></div>}
        {feed.isPending && <p className="candidate-panel-empty" role="status">Đang tải thông báo…</p>}
        {data && <>
          <div className="candidate-notification-list" aria-busy={busy}>
            {data.items.length ? data.items.map(item => <article key={item.id} className={`candidate-notification ${item.readAt ? "" : "is-unread"}`}>
              <button disabled={busy} className="candidate-notification-link" onClick={() => void read(item, true)}>
                <span className="candidate-notification-title">{item.title}{!item.readAt && <span className="candidate-unread-dot" aria-label="Chưa đọc" />}</span>
                <span className="candidate-notification-body">{item.body}</span>
                <time dateTime={item.createdAt}>{new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short", timeZone: "Asia/Ho_Chi_Minh" }).format(new Date(item.createdAt))}</time>
              </button>
              {!item.readAt && <button disabled={busy} className="candidate-icon-button" aria-label="Đánh dấu đã đọc" title="Đánh dấu đã đọc" onClick={() => void read(item)}><Check aria-hidden="true" /></button>}
            </article>) : <div className="candidate-panel-empty"><Bell aria-hidden="true" /><h3>Bạn chưa có thông báo</h3><p>Các cập nhật về hồ sơ, phỏng vấn và offer sẽ xuất hiện tại đây.</p></div>}
          </div>
          <div className="candidate-panel-pagination"><span>Trang {page} · {data.total} thông báo</span><div>
            <button className="candidate-icon-button" aria-label="Trang thông báo trước" disabled={busy || feed.isFetching || page <= 1} onClick={() => setPage(p => p - 1)}><ChevronLeft aria-hidden="true" /></button>
            <button className="candidate-icon-button" aria-label="Trang thông báo tiếp" disabled={busy || feed.isFetching || page * 10 >= data.total} onClick={() => setPage(p => p + 1)}><ChevronRight aria-hidden="true" /></button>
          </div></div>
        </>}
      </Popover.Popup>
    </Popover.Positioner></Popover.Portal>
  </Popover.Root>;
}
