package com.rikkei.jobs.company.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

/** Một kỹ năng yêu cầu của JD; skillId là id danh mục của profile-service. */
@Data
public class JobSkillRequest {

    @NotNull(message = "Mã kỹ năng không được để trống")
    private UUID skillId;

    @NotNull(message = "Cần chọn kỹ năng bắt buộc hay không")
    private Boolean required;

    @NotNull(message = "Điểm tối thiểu không được để trống")
    @Min(value = 1, message = "Điểm tối thiểu từ 1 đến 100")
    @Max(value = 100, message = "Điểm tối thiểu từ 1 đến 100")
    private Integer minScore;
}
