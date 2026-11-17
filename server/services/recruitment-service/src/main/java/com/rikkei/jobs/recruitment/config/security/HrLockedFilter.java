package com.rikkei.jobs.recruitment.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.common.dto.response.ErrorResponse;
import com.rikkei.jobs.recruitment.client.HrAccountStatusClient;
import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Reject an already locked HR even when their JWT has not expired. */
public class HrLockedFilter extends OncePerRequestFilter {
    private final HrAccountStatusClient accounts;
    private final ObjectMapper json;

    public HrLockedFilter(HrAccountStatusClient accounts, ObjectMapper json) {
        this.accounts = accounts;
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal principal && principal.getRole() == Role.HR) {
            try {
                if (accounts.state(principal.getUserId(), principal.getCompanyId()) == HrAccountStatusClient.State.LOCKED) {
                    reject(response, 401, "ACCOUNT_DISABLED", "Tài khoản đã bị khoá");
                    return;
                }
            } catch (RecruitmentDependencyException ex) {
                reject(response, ex.getStatus(), ex.getCode(), ex.getMessage());
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getOutputStream(), ErrorResponse.of(code, message));
        response.flushBuffer();
    }
}
