import { notFound } from "next/navigation";
import { CandidateJobs } from "@/app/components/applications/candidate-jobs";

export default async function CandidateJobPage({ params }: { params: Promise<{ jobId: string }> }) {
  const { jobId } = await params;
  if (!process.env.NEXT_PUBLIC_JOB_API_PATH || !/^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i.test(jobId)) {
    notFound();
  }
  return <CandidateJobs key={jobId} jobId={jobId} />;
}
