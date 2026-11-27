import type { Metadata } from "next";
import { CompanyDirectory } from "@/app/components/job-board/job-pages";

export const metadata: Metadata = { title: "Công ty | Rikkei Jobs" };

export default function CompaniesPage() {
  return <CompanyDirectory />;
}
