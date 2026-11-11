export const env = {
  apiUrl: process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1",
  lmsLoginUrl: process.env.NEXT_PUBLIC_LMS_LOGIN_URL ?? "",
  lmsMockEnabled: process.env.NEXT_PUBLIC_LMS_MOCK === "true",
};
