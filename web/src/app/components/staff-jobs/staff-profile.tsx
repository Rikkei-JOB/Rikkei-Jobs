"use client";

import { useQuery } from "@tanstack/react-query";
import { WorkProfilePage } from "@/app/components/work-profile/work-profile";
import { useAuthStore } from "@/hooks/use-auth-store";
import { useStaffJobSummary } from "@/hooks/use-staff-jobs";
import { staffCompanyService } from "@/services/staff-company.service";

/** Hồ sơ cán bộ: trang hồ sơ nghề nghiệp chung, số liệu là khối lượng công việc đang phụ trách. */
export function StaffProfilePage() {
  const userId = useAuthStore(state => state.user?.id);
  const jobs = useStaffJobSummary();
  const companies = useQuery({ queryKey: ["staff-overview", userId ?? null, "companies-total"], enabled: Boolean(userId),
    queryFn: async () => (await staffCompanyService.list({ page: 1, size: 1 })).total });

  return <WorkProfilePage role={{
    roleLabel: "Cán bộ tuyển dụng",
    badge: "Cán bộ quản lý tuyển dụng",
    suggestions: ["Kiểm duyệt JD", "Duyệt hồ sơ ứng viên", "Quan hệ doanh nghiệp", "Thực tập IOC", "Đồng bộ dữ liệu LMS", "Tư vấn nghề nghiệp", "Tổ chức tuyển dụng"],
    bioPlaceholder: "Mảng phụ trách, doanh nghiệp đối tác, kinh nghiệm hỗ trợ sinh viên…",
    stats: [
      { label: "JD chờ duyệt", value: jobs.data?.byStatus.PENDING ?? null },
      { label: "JD đang tuyển", value: jobs.data?.byStatus.APPROVED ?? null },
      { label: "Doanh nghiệp", value: companies.data ?? null },
    ],
  }} />;
}
