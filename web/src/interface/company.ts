export type CompanySize = "1-50" | "51-200" | "201-1000" | "1000+";
export type CompanyStatus = "ACTIVE" | "LOCKED";

/** CompanyResponse của company-service (GET/PUT /companies/me). */
export interface Company {
  id: string;
  name: string;
  taxCode: string;
  industry: string | null;
  size: CompanySize | null;
  address: string | null;
  city: string | null;
  website: string | null;
  logoUrl: string | null;
  description: string | null;
  status: CompanyStatus;
  createdAt: string;
  updatedAt: string;
}

/** Body PUT /companies/me: thay thế toàn bộ, trường để trống gửi null. */
export interface UpdateCompanyPayload {
  name: string;
  industry: string | null;
  size: CompanySize | null;
  address: string | null;
  city: string | null;
  website: string | null;
  description: string | null;
}
