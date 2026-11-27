import type { Metadata } from "next";
import { MentorProfilePage } from "@/app/components/mentor/mentor-profile";

export const metadata: Metadata = { title: "Hồ sơ Mentor | Rikkei Jobs" };

export default function MentorProfileRoute() {
  return <MentorProfilePage />;
}
