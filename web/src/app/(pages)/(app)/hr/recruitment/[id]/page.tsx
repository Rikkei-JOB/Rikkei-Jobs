import { ApplicationWorkflowDetail } from "@/app/components/recruitment/application-workflow-detail";
export default async function HrApplicationPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <ApplicationWorkflowDetail key={id} id={id} role="HR" />;
}
