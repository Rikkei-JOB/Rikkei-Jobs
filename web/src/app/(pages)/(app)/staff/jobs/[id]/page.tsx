import { StaffJobReview } from "@/app/components/staff-jobs/staff-job-review";

export default async function StaffJobReviewPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <StaffJobReview key={id} id={id} />;
}
