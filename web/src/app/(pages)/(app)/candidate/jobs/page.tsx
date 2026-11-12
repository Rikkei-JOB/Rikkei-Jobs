import { Construction } from "lucide-react";
import { Card, CardDescription, CardHeader, CardTitle } from "@/app/components/ui/card";
import { CandidateJobs } from "@/app/components/applications/candidate-jobs";

export default function CandidateJobsPage() {
  if (process.env.NEXT_PUBLIC_JOB_API_PATH) return <CandidateJobs />;

  return (
    <Card className="mx-auto max-w-4xl">
      <CardHeader>
        <Construction className="mb-1 size-6 text-muted-foreground" aria-hidden />
        <CardTitle className="text-base">Việc làm</CardTitle>
        <CardDescription>Đang phát triển. Chức năng này sẽ được bổ sung ở task tương ứng.</CardDescription>
      </CardHeader>
    </Card>
  );
}
