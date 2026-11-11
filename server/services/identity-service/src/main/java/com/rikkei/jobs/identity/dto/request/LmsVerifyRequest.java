package com.rikkei.jobs.identity.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Nội dung gọi POST /lms/sso/verify. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LmsVerifyRequest {
    private String ticket;
}
