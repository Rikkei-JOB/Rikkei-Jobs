package com.rikkei.jobs.identity.dto.response;

/** Đổi mật khẩu thành công: kèm token mới (không còn cờ mustChangePassword) để FE thay token cũ. */
public record ChangePasswordResponse(String message, String accessToken) {
}
