import type { Metadata } from "next";
import { MentorSchedule } from "@/app/components/mentor/mentor-schedule";

export const metadata: Metadata = { title: "Lịch phỏng vấn | Rikkei Jobs" };

export default function MentorSchedulePage() {
  return <MentorSchedule />;
}
