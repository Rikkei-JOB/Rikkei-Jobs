package com.rikkei.jobs.company.mapper;

import com.rikkei.jobs.company.dto.response.CompanyResponse;
import com.rikkei.jobs.company.entity.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanyResponse toResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .taxCode(company.getTaxCode())
                .industry(company.getIndustry())
                .size(company.getSize())
                .address(company.getAddress())
                .city(company.getCity())
                .website(company.getWebsite())
                .logoUrl(company.getLogoUrl())
                .description(company.getDescription())
                .status(company.getStatus())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }

    /** Trường tuỳ chọn: chuỗi rỗng hoặc chỉ có khoảng trắng được lưu là null, còn lại cắt khoảng trắng hai đầu. */
    public static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
