package com.rikkei.jobs.identity.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.dto.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Bảo vệ /internal/** bằng khoá dịch vụ nội bộ (header X-Internal-Key). Gateway không định tuyến /internal/**,
 * nên khoá này là lớp chặn duy nhất. Khoá cấu hình rỗng thì từ chối mọi request (đóng, không mở).
 */
@RequiredArgsConstructor
public class InternalKeyFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Internal-Key";
    private static final String CODE = "INVALID_SERVICE_KEY";
    private static final String MESSAGE = "Thiếu hoặc sai khoá dịch vụ nội bộ";

    private final String serviceKey;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !path.startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        boolean valid = serviceKey != null && !serviceKey.isBlank() && provided != null
                && MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8),
                        serviceKey.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ErrorResponse.of(CODE, MESSAGE));
            return;
        }
        chain.doFilter(request, response);
    }
}
