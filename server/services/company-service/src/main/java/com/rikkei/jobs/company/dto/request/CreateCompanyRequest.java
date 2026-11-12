package com.rikkei.jobs.company.dto.request;

import static com.rikkei.jobs.company.util.CompanyRules.SIZE_REGEX;
import static com.rikkei.jobs.company.util.CompanyRules.TAX_CODE_MESSAGE;
import static com.rikkei.jobs.company.util.CompanyRules.TAX_CODE_REGEX;
import static com.rikkei.jobs.company.util.CompanyRules.WEBSITE_REGEX;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCompanyRequest {

    @NotBlank(message = "Tên công ty không được để trống")
    @Size(max = 255, message = "Tên công ty tối đa 255 ký tự")
    private String name;

    @NotBlank(message = TAX_CODE_MESSAGE)
    @Pattern(regexp = TAX_CODE_REGEX, message = TAX_CODE_MESSAGE)
    private String taxCode;

    @Size(max = 100, message = "Lĩnh vực tối đa 100 ký tự")
    private String industry;

    @Pattern(regexp = SIZE_REGEX, message = "Quy mô phải là 1-50, 51-200, 201-1000 hoặc 1000+")
    private String size;

    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    @Size(max = 100, message = "Thành phố tối đa 100 ký tự")
    private String city;

    @Size(max = 255, message = "Website tối đa 255 ký tự")
    @Pattern(regexp = WEBSITE_REGEX, message = "Website phải bắt đầu bằng http:// hoặc https://")
    private String website;

    private String description;
}
