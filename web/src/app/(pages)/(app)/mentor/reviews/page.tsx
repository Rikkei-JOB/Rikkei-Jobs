import type { Metadata } from "next";
import { MentorReviews } from "@/app/components/mentor/mentor-reviews";

export const metadata: Metadata = { title: "Đánh giá phỏng vấn | Rikkei Jobs" };

export default function MentorReviewsPage() {
  return <MentorReviews />;
}
