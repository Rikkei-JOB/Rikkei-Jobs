"use client";

import { useMemo } from "react";
import { WorkProfilePage } from "@/app/components/work-profile/work-profile";
import { useMentorRounds } from "@/hooks/use-mentor-rounds";

/** Hồ sơ Mentor: trang hồ sơ nghề nghiệp chung, số liệu lấy từ các vòng phỏng vấn được mời. */
export function MentorProfilePage() {
  const rounds = useMentorRounds();
  const stats = useMemo(() => {
    if (!rounds.data) return { total: null, evaluated: null, average: null };
    const items = rounds.data.items;
    const scored = items.filter(item => item.round.score != null);
    return {
      total: items.length,
      evaluated: items.filter(item => item.round.result !== "PENDING").length,
      average: scored.length ? Math.round(scored.reduce((sum, item) => sum + Number(item.round.score), 0) / scored.length) : null,
    };
  }, [rounds.data]);

  return <WorkProfilePage role={{
    roleLabel: "Mentor",
    badge: "Mentor hội đồng chuyên môn",
    suggestions: "skills",
    bioPlaceholder: "Kinh nghiệm, dự án tiêu biểu, phong cách đánh giá phỏng vấn…",
    stats: [
      { label: "Vòng được mời", value: stats.total },
      { label: "Đã đánh giá", value: stats.evaluated },
      { label: "Điểm trung bình", value: stats.average },
    ],
  }} />;
}
