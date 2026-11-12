import { api } from "@/services/api";
import type { CvChoice } from "@/interface/application";

/** Consumer adapter for HS-04. Replace with its shared CV client when that module lands. */
export async function listApplicationCvs(): Promise<CvChoice[]> {
  const { data } = await api.get<CvChoice[] | { items: CvChoice[] }>(process.env.NEXT_PUBLIC_CV_API_PATH ?? "/profile/cvs");
  const items = Array.isArray(data) ? data : data.items;
  if (!Array.isArray(items)) throw new Error("Không đọc được danh sách CV. Vui lòng thử lại.");
  return items;
}
