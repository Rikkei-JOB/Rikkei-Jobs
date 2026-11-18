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

/** Body của POST /staff/companies (cán bộ tạo công ty): như hồ sơ công ty, thêm mã số thuế. */
export interface CreateCompanyPayload extends UpdateCompanyPayload {
  taxCode: string;
}

/** Danh sách phân trang của backend: { items, total, page, size }, page bắt đầu từ 1. */
export interface PageResult<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

/** Một dòng trong GET /staff/companies. */
export interface StaffCompanyItem {
  id: string;
  name: string;
  taxCode: string;
  status: CompanyStatus;
  hrCount: number;
  jdCount: number;
  createdAt: string;
}

/** Tài khoản HR dưới góc nhìn cán bộ. LOCKED = cán bộ khoá; tempLockedUntil = khoá tạm do nhập sai mật khẩu. */
export interface StaffHrAccount {
  userId: string;
  email: string;
  fullName: string;
  status: "ACTIVE" | "LOCKED";
  lockedReason: string | null;
  lockedAt: string | null;
  tempLockedUntil: string | null;
}

/** GET /staff/companies/{id}. */
export interface StaffCompanyDetail extends Company {
  hrAccounts: StaffHrAccount[];
}

export interface StaffCompanyListParams {
  q?: string;
  status?: CompanyStatus;
  page: number;
  size: number;
}
