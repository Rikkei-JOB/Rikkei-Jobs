import { ReviewDetail } from "@/app/components/profile-review/review-detail";
import { queueFilters, type ReviewSearch } from "@/services/profile-review.service";

export default async function Page({ params, searchParams }: {
  params: Promise<{ userId: string }>; searchParams: Promise<ReviewSearch>;
}) {
  const [{ userId }, search] = await Promise.all([params, searchParams]);
  const index = Number(search.index);
  return <ReviewDetail key={userId} userId={userId} filters={queueFilters(search)}
    index={Number.isSafeInteger(index) && index >= 0 ? index : 0} />;
}
