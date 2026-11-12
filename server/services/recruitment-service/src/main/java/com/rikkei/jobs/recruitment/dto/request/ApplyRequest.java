package com.rikkei.jobs.recruitment.dto.request;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record ApplyRequest(@NotNull(message="Vui lòng chọn CV.") UUID cvId,
    @Size(max=5000, message="Thư giới thiệu tối đa 5.000 ký tự.") String coverLetter) {}
