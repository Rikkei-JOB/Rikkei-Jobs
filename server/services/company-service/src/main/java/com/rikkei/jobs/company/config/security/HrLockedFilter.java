package com.rikkei.jobs.company.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.common.dto.response.ErrorResponse;
import com.rikkei.jobs.company.repository.CompanyHrRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Chặn HR đã bị cán bộ khoá (company_hr.locked_at khác null) dù JWT vẫn còn hạn. Không khai báo là bean để
 * Spring Boot không đăng ký thêm lần nữa ngoài chuỗi bảo mật; được gắn ngay sau JwtAuthenticationFilter.
 */
public class HrLockedFilter extends OncePerRequestFilter {

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
                && principal.getRole() == Role.HR
                && repo.findById(principal.getUserId()).map(hr -> hr.getLockedAt() != null).orElse(false)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            json.writeValue(response.getOutputStream(), ErrorResponse.of("ACCOUNT_DISABLED", "Tài khoản đã bị khoá"));
            response.flushBuffer();
            return;
        }
        chain.doFilter(request, response);
    }
}
