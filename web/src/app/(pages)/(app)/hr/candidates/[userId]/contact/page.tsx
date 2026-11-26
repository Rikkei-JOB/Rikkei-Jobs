import { HrContactPage } from "@/app/components/contact/hr-contact-page";
export default async function Page({ params }: { params: Promise<{ userId: string }> }) {
  const { userId } = await params;
  return <HrContactPage userId={userId} />;
}
