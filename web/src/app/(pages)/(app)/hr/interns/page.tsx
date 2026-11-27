import type { Metadata } from "next";
import { HrInterns } from "@/app/components/hr-dashboard/hr-interns";

export const metadata: Metadata = { title: "Thực tập sinh | Rikkei Jobs" };

export default function HrInternsPage() {
  return <HrInterns />;
}
