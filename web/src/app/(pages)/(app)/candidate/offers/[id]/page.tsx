import { CandidateOfferDetail } from "@/app/components/offers/candidate-offer-detail";

export default async function CandidateOfferPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <CandidateOfferDetail id={id} />;
}
