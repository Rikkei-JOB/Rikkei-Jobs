import { isApplicationStatus } from "@/config/application-status";

export function candidateListReturnUrl(value: string | null, path: string): string {
  if (!value?.startsWith(`${path}?`) && value !== path) return path;
  try {
    const parsed = new URL(value, "https://local.invalid");
    if (parsed.origin !== "https://local.invalid" || parsed.pathname !== path) return path;
    const query = new URLSearchParams();
    const status = parsed.searchParams.get("status");
    if (path === "/candidate/applications" && isApplicationStatus(status)) query.set("status", status);
    const page = Number(parsed.searchParams.get("page"));
    if (Number.isSafeInteger(page) && page > 1) query.set("page", String(page));
    return path + (query.size ? `?${query}` : "");
  } catch { return path; }
}

export function listPage(value: string | null): number {
  const page = Number(value);
  return Number.isSafeInteger(page) && page > 0 ? page : 1;
}
