import { ReviewQueue } from "@/app/components/profile-review/review-queue";
import { queueFilters, type ReviewSearch } from "@/services/profile-review.service";

export default async function Page({ searchParams }: { searchParams: Promise<ReviewSearch> }) {
  const filters = queueFilters(await searchParams);
  return <ReviewQueue key={JSON.stringify(filters)} filters={filters} />;
}
