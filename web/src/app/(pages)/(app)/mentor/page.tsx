import type { Metadata } from "next";
import { MentorOverview } from "@/app/components/mentor/mentor-overview";

export const metadata: Metadata = { title: "Tổng quan Mentor | Rikkei Jobs" };

export default function MentorHomePage() {
  return <MentorOverview />;
}
