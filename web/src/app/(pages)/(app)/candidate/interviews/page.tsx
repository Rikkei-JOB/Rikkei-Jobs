import { CandidateInterviews } from "@/app/components/interviews/candidate-interviews";
import { Suspense } from "react";
export default function CandidateInterviewsPage() { return <Suspense fallback={<p>Đang tải…</p>}><CandidateInterviews /></Suspense>; }
