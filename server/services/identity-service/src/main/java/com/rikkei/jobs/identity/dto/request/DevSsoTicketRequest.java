package com.rikkei.jobs.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DevSsoTicketRequest {
    @NotBlank(message = "lmsUserId không được để trống")
    private String lmsUserId;
}
