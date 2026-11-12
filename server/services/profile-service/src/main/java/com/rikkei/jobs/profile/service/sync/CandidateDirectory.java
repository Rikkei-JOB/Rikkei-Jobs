package com.rikkei.jobs.profile.service.sync;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Đọc ứng viên và lms_user_id từ bảng users của identity-service (schema public, cùng database), giống cách
 * HS-01 làm trong ProfileDemoDataSeeder. Chỉ đọc, không ghi.
 */
@Component
@RequiredArgsConstructor
public class CandidateDirectory {
    private static final String COLUMNS = "select id, full_name, email, lms_user_id from public.users ";
    private static final RowMapper<Candidate> ROW = (rs, i) -> new Candidate(
            rs.getObject("id", UUID.class), rs.getString("full_name"), rs.getString("email"), rs.getString("lms_user_id"));

    private final NamedParameterJdbcTemplate jdbc;

    public record Candidate(UUID userId, String fullName, String email, String lmsUserId) { }

    public Optional<Candidate> find(UUID userId) {
        return jdbc.query(COLUMNS + "where id = :id and role = 'CANDIDATE'", Map.of("id", userId), ROW)
                .stream().findFirst();
    }

    public Map<UUID, Candidate> findAll(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return jdbc.query(COLUMNS + "where id in (:ids)", Map.of("ids", userIds), ROW).stream()
                .collect(Collectors.toMap(Candidate::userId, Function.identity()));
    }

    /** Tìm theo tên, email hoặc mã LMS (không phân biệt hoa thường). */
    public List<Candidate> search(String query, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource("limit", limit);
        String where = "where role = 'CANDIDATE' ";
        if (query != null && !query.isBlank()) {
            params.addValue("q", "%" + escapeLike(query.trim().toLowerCase(Locale.ROOT)) + "%");
            where += "and (lower(full_name) like :q escape '!' or lower(email) like :q escape '!' "
                    + "or lower(coalesce(lms_user_id, '')) like :q escape '!') ";
        }
        return jdbc.query(COLUMNS + where + "order by full_name, email limit :limit", params, ROW);
    }

    /** Ứng viên đang hoạt động và đã liên kết LMS: đối tượng của job đêm. */
    public List<Candidate> syncable() {
        return jdbc.query(COLUMNS + "where role = 'CANDIDATE' and status = 'ACTIVE' and lms_user_id is not null "
                + "order by lms_user_id", Map.of(), ROW);
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
