package com.rikkei.jobs.company.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.common.dto.response.ErrorResponse;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.repository.CompanyHrRepository;
import com.rikkei.jobs.company.repository.HrAccessState;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Chặn HR dù JWT vẫn còn hạn khi: cán bộ khoá riêng HR (company_hr.locked_at khác null), hoặc công ty của HR đang
 * LOCKED (DN-07: HR của công ty bị khoá mất mọi quyền truy cập company-service, kể cả đọc). Không khai báo là bean
 * để Spring Boot không đăng ký thêm lần nữa ngoài chuỗi bảo mật; được gắn ngay sau JwtAuthenticationFilter.
 */
public class HrLockedFilter extends OncePerRequestFilter {

    static final String ACCOUNT_LOCKED_MESSAGE = "Tài khoản đã bị khoá";
    static final String COMPANY_LOCKED_MESSAGE = "Công ty của bạn đang bị khoá, liên hệ cán bộ tuyển dụng";

    private final CompanyHrRepository repo;
    private final ObjectMapper json;

    public HrLockedFilter(CompanyHrRepository repo, ObjectMapper json) {
        this.repo = repo;
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal principal
                && principal.getRole() == Role.HR) {
            Optional<HrAccessState> state = repo.findAccessState(principal.getUserId());
            // Khoá riêng báo trước (giữ message cũ của DN-05), rồi mới tới khoá theo công ty.
            String message = state.map(s -> s.lockedAt() != null ? ACCOUNT_LOCKED_MESSAGE
                    : s.companyStatus() == CompanyStatus.LOCKED ? COMPANY_LOCKED_MESSAGE : null).orElse(null);
            if (message != null) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                json.writeValue(response.getOutputStream(), ErrorResponse.of("ACCOUNT_DISABLED", message));
                response.flushBuffer();
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
