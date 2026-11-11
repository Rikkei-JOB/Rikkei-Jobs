package com.rikkei.jobs.common.config.security.jwt;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
public class JwtUtils {
    private final SecretKey key;
    private final long jwtExpirationMs;

    public JwtUtils(JwtProperties properties) {
        if (properties.getSecret() == null
                || properties.getSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) phải được cấu hình và dài ít nhất 32 byte");
        }
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = properties.getExpirationMs();
    }

    public static final String MUST_CHANGE_PASSWORD_CLAIM = "mustChangePassword";

    public String generateJwtToken(UUID userId, Role role, UUID companyId) {
        return generateJwtToken(userId, role, companyId, false);
    }

    /**
     * Token của tài khoản đang bị bắt đổi mật khẩu mang claim mustChangePassword=true; mọi service đọc claim này
     * và trả 403 MUST_CHANGE_PASSWORD. Đổi mật khẩu xong, identity-service cấp token mới không có claim.
     */
    public String generateJwtToken(UUID userId, Role role, UUID companyId, boolean mustChangePassword) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtExpirationMs));
        if (companyId != null) {
            builder.claim("companyId", companyId.toString());
        }
        if (mustChangePassword) {
            builder.claim(MUST_CHANGE_PASSWORD_CLAIM, true);
        }
        return builder.signWith(key).compact();
    }

    /** Chỉ gọi sau khi {@link #validateJwtToken} trả về true. */
    public AuthPrincipal getPrincipalFromJwtToken(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        String companyId = claims.get("companyId", String.class);
        return new AuthPrincipal(
                UUID.fromString(claims.getSubject()),
                Role.valueOf(claims.get("role", String.class)),
                companyId == null ? null : UUID.fromString(companyId),
                Boolean.TRUE.equals(claims.get(MUST_CHANGE_PASSWORD_CLAIM, Boolean.class)));
    }

    public boolean validateJwtToken(String authToken, HttpServletRequest request) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(authToken).getPayload();
            Role.valueOf(claims.get("role", String.class));
            UUID.fromString(claims.getSubject());
            return true;
        } catch (ExpiredJwtException e) {
            request.setAttribute("jwt_error", "Token đã hết hạn");
            log.warn("JWT token is expired: {}", e.getMessage());
        } catch (SignatureException e) {
            request.setAttribute("jwt_error", "Chữ ký token không hợp lệ");
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            request.setAttribute("jwt_error", "Token không đúng định dạng");
            log.warn("Invalid JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            request.setAttribute("jwt_error", "Token không được hỗ trợ");
            log.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException | NullPointerException e) {
            request.setAttribute("jwt_error", "Token không hợp lệ");
            log.warn("JWT claims are invalid: {}", e.getMessage());
        } catch (JwtException e) {
            request.setAttribute("jwt_error", "Token hết hạn hoặc không hợp lệ");
            log.warn("JWT error: {}", e.getMessage());
        }
        return false;
    }
}
