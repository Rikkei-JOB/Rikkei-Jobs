package com.rikkei.jobs.identity.config;

import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.identity.entity.User;
import com.rikkei.jobs.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Tài khoản mẫu theo docs/conventions.md (mục 4): 8 tài khoản @rikkeijobs.test, mật khẩu Test@123,
 * cộng thêm một HR "mới" để thử luồng bắt buộc đổi mật khẩu. Chạy lại không tạo bản ghi trùng.
 * Tài khoản LMS (CANDIDATE, MENTOR, STAFF, ADMIN) gắn sẵn lmsUserId khớp mock/data.json và đăng nhập bằng SSO;
 * chúng không có mật khẩu. Chỉ bật ở dev/test (app.dev-data.enabled=true).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.dev-data.enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    public static final String SAMPLE_PASSWORD = "Test@123";
    public static final UUID COMPANY_A_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    public static final UUID COMPANY_B_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    // Id cố định: company-service seed company_hr theo đúng các giá trị này (xem DevDataSeeder của company-service).
    public static final UUID HR1_ID = UUID.fromString("00000000-0000-0000-0001-000000000001");
    public static final UUID HR2_ID = UUID.fromString("00000000-0000-0000-0001-000000000002");
    public static final UUID HR_NEW_ID = UUID.fromString("00000000-0000-0000-0001-000000000003");
    public static final UUID STAFF1_ID = UUID.fromString("00000000-0000-0000-0001-000000000005");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        lmsUser("candidate1@rikkeijobs.test", "Nguyễn Văn An", Role.CANDIDATE, "lms-sv-001");
        lmsUser("candidate2@rikkeijobs.test", "Trần Thị Bình", Role.CANDIDATE, "lms-sv-002");
        lmsUser("candidate3@rikkeijobs.test", "Lê Minh Châu", Role.CANDIDATE, "lms-sv-003");
        lmsUser("mentor1@rikkeijobs.test", "Phạm Quốc Dũng", Role.MENTOR, "lms-mt-001");
        insertWithFixedId(STAFF1_ID, "staff1@rikkeijobs.test", null, "Hoàng Thu Hà", Role.STAFF, "lms-st-001", null, false);
        lmsUser("admin1@rikkeijobs.test", "Đỗ Thanh Hải", Role.ADMIN, "lms-ad-001");
        hr(HR1_ID, "hr1@rikkeijobs.test", "HR Công ty A", COMPANY_A_ID, false);
        hr(HR2_ID, "hr2@rikkeijobs.test", "HR Công ty B", COMPANY_B_ID, false);
        hr(HR_NEW_ID, "hr-new@rikkeijobs.test", "HR Công ty A (tài khoản mới, phải đổi mật khẩu)", COMPANY_A_ID, true);
    }

    private void lmsUser(String email, String fullName, Role role, String lmsUserId) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setLmsUserId(lmsUserId);
        userRepository.save(user);
    }

    private void hr(UUID id, String email, String fullName, UUID companyId, boolean mustChangePassword) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        insertWithFixedId(id, email, passwordEncoder.encode(SAMPLE_PASSWORD), fullName, Role.HR, null, companyId,
                mustChangePassword);
        log.info("Đã tạo HR mẫu {} (mật khẩu: {})", email, SAMPLE_PASSWORD);
    }

    /** Chèn bằng SQL vì Hibernate bỏ qua id gán tay khi User.id dùng @GeneratedValue. */
    private void insertWithFixedId(UUID id, String email, String passwordHash, String fullName, Role role,
            String lmsUserId, UUID companyId, boolean mustChangePassword) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, full_name, role, lms_user_id, company_id, "
                        + "must_change_password) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, email, passwordHash, fullName, role.name(), lmsUserId, companyId, mustChangePassword);
    }
}
