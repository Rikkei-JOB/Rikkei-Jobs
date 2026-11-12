package com.rikkei.jobs.company.service;

import com.rikkei.jobs.company.dto.request.CreateCompanyRequest;
import com.rikkei.jobs.company.dto.request.UpdateCompanyRequest;
import com.rikkei.jobs.company.dto.response.CompanyResponse;
import com.rikkei.jobs.company.entity.Company;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface CompanyService {

    /** Cán bộ tạo công ty mới (trạng thái ACTIVE); trùng mã số thuế thì ném ConflictException. */
    CompanyResponse create(CreateCompanyRequest request, UUID staffUserId);

    /** Công ty của HR, tra chỉ theo userId qua company_hr; không có thì ném ResourceNotFoundException. */
    Company findMine(UUID userId);

    CompanyResponse getMine(UUID userId);

    /** Thay toàn bộ trường cho phép sửa; công ty LOCKED thì ném CompanyException(COMPANY_LOCKED). */
    CompanyResponse updateMine(UUID userId, UpdateCompanyRequest request);

    /**
     * Đổi logo (JPG/PNG tối đa 2MB, kiểm tra theo chữ ký tệp). LOCKED thì COMPANY_LOCKED, quá cỡ thì FILE_TOO_LARGE,
     * rỗng hoặc sai định dạng thì INVALID_FILE_TYPE.
     */
    CompanyResponse updateMyLogo(UUID userId, MultipartFile file);
}
