import { JobDetailPage } from "@/app/components/job-board/job-pages";

export default async function JobPage({ params }: { params: Promise<{ jobId: string }> }) {
  const { jobId } = await params;
  return <JobDetailPage key={jobId} jobId={jobId} basePath="/jobs" />;
}
