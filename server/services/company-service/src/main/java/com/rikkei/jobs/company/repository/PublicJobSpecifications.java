package com.rikkei.jobs.company.repository;

import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobSkill;
import com.rikkei.jobs.company.entity.JobStatus;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Điều kiện tìm tin công khai. Dùng Criteria thay vì JPQL "(:x is null or ...)" vì PostgreSQL không suy được kiểu
 * của tham số null (xem CompanyRepository). Hàm trả null khi bộ lọc trống; Specification.and bỏ qua null.
 */
public final class PublicJobSpecifications {

    /** Ký tự thoát của LIKE, khớp với cách PublicJobServiceImpl thoát % và _. */
    private static final char ESCAPE = '\\';

    private PublicJobSpecifications() {
    }

    /** Tin ứng viên được thấy: APPROVED, chưa quá hạn (deadline là ngày cuối còn nộp), công ty đang ACTIVE. */
    public static Specification<Job> visible(LocalDate today) {
        return (root, query, cb) -> {
            Subquery<UUID> active = query.subquery(UUID.class);
            Root<Company> company = active.from(Company.class);
            active.select(company.get("id")).where(cb.equal(company.get("status"), CompanyStatus.ACTIVE));
            return cb.and(
                    cb.equal(root.get("status"), JobStatus.APPROVED),
                    cb.or(cb.isNull(root.get("deadline")), cb.greaterThanOrEqualTo(root.get("deadline"), today)),
                    root.get("companyId").in(active));
        };
    }

    /**
     * Từ khoá khớp tiêu đề, vị trí hoặc tên công ty; hoặc JD có một trong {@code skillIds}
     * (tên kỹ năng nằm ở profile-service nên frontend đổi từ khoá thành id kỹ năng).
     * {@code pattern} đã viết thường và thoát ký tự LIKE.
     */
    public static Specification<Job> keyword(String pattern, Collection<UUID> skillIds) {
        if (pattern == null && (skillIds == null || skillIds.isEmpty())) {
            return null;
        }
        return (root, query, cb) -> {
            Predicate any = cb.disjunction();
            if (pattern != null) {
                Subquery<UUID> byName = query.subquery(UUID.class);
                Root<Company> company = byName.from(Company.class);
                byName.select(company.get("id")).where(cb.like(cb.lower(company.get("name")), pattern, ESCAPE));
                any = cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, ESCAPE),
                        cb.like(cb.lower(cb.coalesce(root.get("position"), "")), pattern, ESCAPE),
                        root.get("companyId").in(byName));
            }
            if (skillIds != null && !skillIds.isEmpty()) {
                Subquery<UUID> bySkill = query.subquery(UUID.class);
                Root<JobSkill> skill = bySkill.from(JobSkill.class);
                bySkill.select(skill.get("jobId")).where(skill.get("skillId").in(skillIds));
                any = cb.or(any, root.get("id").in(bySkill));
            }
            return any;
        };
    }

    public static Specification<Job> equal(String field, Object value) {
        return value == null ? null : (root, query, cb) -> cb.equal(root.get(field), value);
    }

    /** Thành phố chứa chuỗi đã cho (không phân biệt hoa thường) để "Hồ Chí Minh" khớp "TP. Hồ Chí Minh". */
    public static Specification<Job> cityContains(String pattern) {
        return pattern == null ? null : (root, query, cb) -> cb.like(cb.lower(root.get("city")), pattern, ESCAPE);
    }

    /** Khoảng lương giao nhau với [from, to]; JD không ghi lương bị loại khi có lọc lương. */
    public static Specification<Job> salaryOverlaps(Long from, Long to) {
        if (from == null && to == null) {
            return null;
        }
        return (root, query, cb) -> {
            Expression<Long> top = cb.coalesce(root.get("salaryMax"), root.<Long>get("salaryMin"));
            Expression<Long> bottom = cb.coalesce(root.get("salaryMin"), root.<Long>get("salaryMax"));
            Predicate result = cb.isNotNull(top);
            if (from != null) {
                result = cb.and(result, cb.greaterThanOrEqualTo(top, from));
            }
            if (to != null) {
                result = cb.and(result, cb.lessThanOrEqualTo(bottom, to));
            }
            return result;
        };
    }

    /**
     * Sắp xếp đặt trong Specification vì cần COALESCE (JD không ghi lương xếp cuối); Spring tự bỏ ORDER BY
     * khi đếm tổng. {@code bySalary=false}: mới đăng trước.
     */
    public static Specification<Job> ordered(boolean bySalary) {
        return (root, query, cb) -> {
            if (bySalary) {
                query.orderBy(
                        cb.desc(cb.coalesce(root.get("salaryMax"), cb.coalesce(root.<Long>get("salaryMin"), 0L))),
                        cb.desc(root.get("createdAt")), cb.desc(root.get("id")));
            } else {
                query.orderBy(cb.desc(root.get("createdAt")), cb.desc(root.get("id")));
            }
            return null;
        };
    }
}
