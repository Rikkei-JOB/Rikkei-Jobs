package com.rikkei.jobs.company.dto.request;

import com.rikkei.jobs.company.entity.JobLevel;
import com.rikkei.jobs.company.entity.JobType;
import com.rikkei.jobs.company.entity.WorkMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

/** HR tạo/sửa JD. Cố ý không có id, companyId, status: các trường này do hệ thống quyết định. */
@Data
public class JobRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    @Size(max = 255, message = "Vị trí tối đa 255 ký tự")
    private String position;

    private JobLevel level;

    private JobType type;

    @Min(value = 1, message = "Số lượng tuyển tối thiểu là 1")
    private Integer quantity;

    @Size(max = 100, message = "Thành phố tối đa 100 ký tự")
    private String city;

    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    private WorkMode workMode;

    @PositiveOrZero(message = "Lương tối thiểu không được âm")
    private Long salaryMin;

    @PositiveOrZero(message = "Lương tối đa không được âm")
    private Long salaryMax;

    private String description;

    private String requirements;

    private String benefits;

    private LocalDate deadline;

    /** null = không đổi kỹ năng. */
    @Size(max = 20, message = "Tối đa 20 kỹ năng")
    private List<@Valid JobSkillRequest> skills;
}
