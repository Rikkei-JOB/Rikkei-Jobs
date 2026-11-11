package com.rikkei.jobs.common.config.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.dto.response.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class AuthenticationEntryPointHandler implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        log.warn("CẢNH BÁO BẢO MẬT: Truy cập chưa xác thực vào URL: {} - Lỗi: {}",
                request.getRequestURI(), authException.getMessage());

        String jwtError = (String) request.getAttribute("jwt_error");
        String message = jwtError != null
                ? jwtError
                : "Bạn cần đăng nhập hoặc cung cấp Token hợp lệ để truy cập tài nguyên này.";

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of("UNAUTHORIZED", message));
        response.flushBuffer();
    }
}
