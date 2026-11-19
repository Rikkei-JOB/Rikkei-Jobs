"use client";

import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/app/components/ui/tabs";
import { ChangeLogsTab } from "@/app/components/admin/change-logs-tab";
import { ReportsTab } from "@/app/components/admin/reports-tab";
import { SkillsTab } from "@/app/components/admin/skills-tab";
import { TemplatesTab } from "@/app/components/admin/templates-tab";
import { WeightsTab } from "@/app/components/admin/weights-tab";

const TABS = [
  { value: "skills", label: "Kỹ năng", content: <SkillsTab /> },
  { value: "weights", label: "Trọng số", content: <WeightsTab /> },
  { value: "templates", label: "Mẫu phiếu", content: <TemplatesTab /> },
  { value: "reports", label: "Báo sai", content: <ReportsTab /> },
  { value: "logs", label: "Lịch sử thay đổi", content: <ChangeLogsTab /> },
];

/** Trang Quản trị (NT-12). Vai trò ADMIN do layout (app) kiểm theo đường dẫn /admin/...; API kiểm lại. */
export default function AdminSettingsPage() {
  return (
    <div className="mx-auto max-w-6xl space-y-4">
      <div>
        <h1 className="font-heading text-lg font-semibold">Quản trị</h1>
        <p className="text-xs text-muted-foreground">
          Cấu hình hệ thống không cần sửa code. Mọi thay đổi được ghi vào Lịch sử thay đổi.
        </p>
      </div>
      <Tabs defaultValue="skills">
        <TabsList className="flex w-full justify-start overflow-x-auto sm:grid sm:grid-cols-5">
          {TABS.map((tab) => (
            <TabsTrigger key={tab.value} value={tab.value} className="shrink-0">
              {tab.label}
            </TabsTrigger>
          ))}
        </TabsList>
        {TABS.map((tab) => (
          <TabsContent key={tab.value} value={tab.value}>
            {tab.content}
          </TabsContent>
        ))}
      </Tabs>
    </div>
  );
}
