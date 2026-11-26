import type { Metadata } from "next";
import { JobBoard } from "@/app/components/job-board/job-board";

export const metadata: Metadata = { title: "Việc làm IT | Rikkei Jobs" };

export default function JobsPage() {
  return <JobBoard basePath="/jobs" />;
}
