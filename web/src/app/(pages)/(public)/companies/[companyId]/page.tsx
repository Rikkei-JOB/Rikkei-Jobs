import { CompanyProfile } from "@/app/components/job-board/job-pages";

export default async function CompanyPage({ params }: { params: Promise<{ companyId: string }> }) {
  const { companyId } = await params;
  return <CompanyProfile key={companyId} companyId={companyId} />;
}
