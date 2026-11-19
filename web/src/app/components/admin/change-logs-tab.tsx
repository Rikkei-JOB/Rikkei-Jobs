"use client";

import * as React from "react";
import { Alert } from "@/app/components/ui/alert";
import { Button } from "@/app/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/app/components/ui/card";
import { Input } from "@/app/components/ui/input";
import { useChangeLogs } from "@/hooks/use-admin";
import type { ChangeEntity } from "@/interface/admin";
import { ENTITY_LABEL, errorText, formatDateTime, selectClass } from "./admin-format";

const PAGE_SIZE = 20;

/** Tab Lịch sử thay đổi: mọi lần ADMIN sửa dữ liệu, giá trị cũ/mới và lý do. */
export function ChangeLogsTab() {
  const [filter, setFilter] = React.useState<{ entity: ChangeEntity | ""; from: string; to: string }>({
    entity: "",
    from: "",
    to: "",
  });
  const [page, setPage] = React.useState(1);
  const { data, error, isPending, isFetching } = useChangeLogs({
    entity: filter.entity || undefined,
    from: filter.from,
    to: filter.to,
    page,
    size: PAGE_SIZE,
  });
  const pages = data ? Math.max(1, Math.ceil(data.total / PAGE_SIZE)) : 1;
  const change = (patch: Partial<typeof filter>) => {
    setFilter((prev) => ({ ...prev, ...patch }));
    setPage(1);
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Lịch sử thay đổi</CardTitle>
        <CardDescription>Chỉ thêm, không sửa hay xoá được. Ngày tính theo giờ Việt Nam.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="flex flex-wrap items-end gap-3 text-xs">
          <div className="space-y-1">
            <label htmlFor="log-entity" className="block">Loại dữ liệu</label>
            <select id="log-entity" className={selectClass} value={filter.entity}
              onChange={(e) => change({ entity: e.target.value as ChangeEntity | "" })}>
              <option value="">Tất cả</option>
              {(Object.keys(ENTITY_LABEL) as ChangeEntity[]).map((e) => (
                <option key={e} value={e}>{ENTITY_LABEL[e]}</option>
              ))}
            </select>
          </div>
          <div className="space-y-1">
            <label htmlFor="log-from" className="block">Từ ngày</label>
            <Input id="log-from" type="date" value={filter.from} onChange={(e) => change({ from: e.target.value })} />
          </div>
          <div className="space-y-1">
            <label htmlFor="log-to" className="block">Đến ngày</label>
            <Input id="log-to" type="date" value={filter.to} onChange={(e) => change({ to: e.target.value })} />
          </div>
          {data ? <span className="pb-1.5 text-muted-foreground">{data.total} dòng</span> : null}
        </div>

        {error ? <Alert variant="destructive">{errorText(error)}</Alert> : null}
        {isPending ? <p className="text-xs text-muted-foreground">Đang tải...</p> : null}

        <div className="overflow-x-auto">
          <table className="w-full min-w-[820px] text-left text-xs" aria-busy={isFetching}>
            <thead className="border-b border-border text-muted-foreground">
              <tr>
                <th className="py-2 pr-3 font-medium">Thời điểm</th>
                <th className="py-2 pr-3 font-medium">Loại</th>
                <th className="py-2 pr-3 font-medium">Đối tượng</th>
                <th className="py-2 pr-3 font-medium">Trường</th>
                <th className="py-2 pr-3 font-medium">Giá trị cũ</th>
                <th className="py-2 pr-3 font-medium">Giá trị mới</th>
                <th className="py-2 font-medium">Lý do</th>
              </tr>
            </thead>
            <tbody>
              {data?.items.map((log) => (
                <tr key={log.id} className="border-b border-border/60 align-top">
                  <td className="py-2 pr-3 whitespace-nowrap">{formatDateTime(log.createdAt)}</td>
                  <td className="py-2 pr-3 whitespace-nowrap">{ENTITY_LABEL[log.entity]}</td>
                  <td className="max-w-[10rem] truncate py-2 pr-3 font-mono" title={log.entityId}>{log.entityId}</td>
                  <td className="py-2 pr-3">{log.field}</td>
                  <td className="max-w-[14rem] py-2 pr-3 break-words text-muted-foreground">{log.oldValue ?? "—"}</td>
                  <td className="max-w-[14rem] py-2 pr-3 break-words">{log.newValue ?? "—"}</td>
                  <td className="max-w-[14rem] py-2 break-words">{log.reason ?? <span className="text-muted-foreground">—</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {data && data.items.length === 0 ? <p className="text-xs text-muted-foreground">Chưa có thay đổi nào.</p> : null}

        {pages > 1 ? (
          <div className="flex items-center justify-end gap-2 text-xs">
            <Button variant="outline" size="sm" disabled={page <= 1} onClick={() => setPage(page - 1)}>Trước</Button>
            <span>Trang {page}/{pages}</span>
            <Button variant="outline" size="sm" disabled={page >= pages} onClick={() => setPage(page + 1)}>Sau</Button>
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}
