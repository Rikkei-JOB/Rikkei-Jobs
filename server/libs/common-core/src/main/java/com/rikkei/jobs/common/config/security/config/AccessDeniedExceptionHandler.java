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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class AccessDeniedExceptionHandler implements AccessDeniedHandler {

    /** Message chung cho 403 FORBIDDEN do sai vai trò. */
    public static final String MESSAGE = "Bạn không có quyền thực hiện thao tác này.";
    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        log.warn("CẢNH BÁO BẢO MẬT: Truy cập bị từ chối vào URL: {}", request.getRequestURI());

        String message = MESSAGE;
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpStatus.FORBIDDEN.value());
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of("FORBIDDEN", message));
        response.flushBuffer();
    }
}
