import { ApplicationWorkflowDetail } from "@/app/components/recruitment/application-workflow-detail";
export default async function StaffApplicationPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <ApplicationWorkflowDetail id={id} role="STAFF" />;
}
