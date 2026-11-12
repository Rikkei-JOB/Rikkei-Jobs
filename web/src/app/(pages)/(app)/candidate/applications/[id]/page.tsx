import { ApplicationDetailView } from "@/app/components/applications/application-detail-view";
export default async function ApplicationPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <ApplicationDetailView id={id} />;
}
