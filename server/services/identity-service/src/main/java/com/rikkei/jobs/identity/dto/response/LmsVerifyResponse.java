package com.rikkei.jobs.identity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Nội dung trả về của POST /lms/sso/verify (role là chuỗi để tolerate giá trị lạ từ LMS). */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LmsVerifyResponse {
    private String lmsUserId;
    private String email;
    private String fullName;
    private String role;
    private Instant issuedAt;
}
