/** Only return to the HR list. Rebuild allowed query parameters instead of trusting a URL. */
export function hrListReturnUrl(value: string | null) {
  if (!value || !/^\/hr\/recruitment(?:\?|$)/.test(value) || value.includes("#")) return "/hr/recruitment";
  const url = new URL(value, "https://internal.invalid");
  if (url.origin !== "https://internal.invalid" || url.pathname !== "/hr/recruitment") return "/hr/recruitment";
  const query = new URLSearchParams();
  for (const key of ["jobId", "status", "page"]) { const field = url.searchParams.get(key); if (field) query.set(key, field); }
  return `/hr/recruitment${query.size ? `?${query}` : ""}`;
}
